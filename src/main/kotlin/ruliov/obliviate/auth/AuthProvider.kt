package ruliov.obliviate.auth

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.json.JSONObject
import org.json.JSONTokener
import ruliov.UTF8
import ruliov.async.IAsync
import ruliov.async.createAsync
import ruliov.data.EitherLeft
import ruliov.data.EitherRight
import ruliov.data.IEither
import ruliov.obliviate.OUR_URI
import ruliov.obliviate.data.users.LoginedUser
import ruliov.obliviate.db.Database
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

private val VK_SECRET = System.getenv("VK_SECRET")

class AuthProvider(private val database: Database) {
    private sealed class CheckStatus {
        class Finished(val result: LoginedUser?) : CheckStatus()
        class NotFinishedYet : CheckStatus() {
            val awaiters = ConcurrentLinkedQueue<(IEither<Any, LoginedUser?>) -> Unit>()
        }
    }

    // TODO: Clear that cache by timeout
    private val vkCodes = ConcurrentHashMap<String, CheckStatus>()
    private val httpClient = HttpClient(SslContextFactory(true))
    private val timer = Timer(true)

    init {
        this.httpClient.start()

        timer.schedule(object : TimerTask() {
            override fun run() {
                // TODO: Actually, we need to remove only old codes, but... later
                vkCodes.clear()
            }
        }, 5000, 5000)
    }

    fun checkVk(code: String): IAsync<LoginedUser?, Any> = createAsync {
        val callback = it

        var result: LoginedUser? = null
        var resultIsResult = false
        var start = false
        var awaiters: ConcurrentLinkedQueue<(IEither<Any, LoginedUser?>) -> Unit>? = null

        this.vkCodes.compute(code, {c, checkStatus ->
            if (checkStatus != null) {
                when (checkStatus) {
                    is CheckStatus.Finished -> {
                        result = checkStatus.result
                        resultIsResult = true
                    }
                    is CheckStatus.NotFinishedYet -> {
                        checkStatus.awaiters.add(callback)
                    }
                }

                return@compute checkStatus
            }

            val newCheckStatus = CheckStatus.NotFinishedYet()
            newCheckStatus.awaiters.add(callback)

            start = true
            awaiters = newCheckStatus.awaiters

            return@compute newCheckStatus
        })

        if (resultIsResult) {
            callback(EitherRight(result))
            return@createAsync
        }

        fun notifyAwaiters(result: IEither<Any, LoginedUser?>) {
            if (result.isRight()) {
                this.vkCodes.put(code, CheckStatus.Finished(result.right()))
            } else {
                this.vkCodes.remove(code)
            }

            awaiters?.forEach { it(result) }
        }

        if (start) {
            val uri = "https://oauth.vk.com/access_token?client_id=5740564" +
                "&client_secret=$VK_SECRET" +
                "&code=$code" +
                "&redirect_uri=$OUR_URI/auth/vk"

            val sb = StringBuilder()

            this.httpClient.newRequest(uri).onResponseContent { response, byteBuffer ->
                sb.append(String(byteBuffer.array(), UTF8))
            }.timeout(4, TimeUnit.SECONDS
            ).send {
                try {
                    if (sb.isNotEmpty()) {
                        val jsonTokener: JSONTokener
                        val jsonObject: JSONObject

                        jsonTokener = JSONTokener(sb.toString())
                        jsonObject = JSONObject(jsonTokener)

                        if (jsonObject.has("error")) {
                            val error = jsonObject.getString("error")
                            if (error == "invalid_grant") {
                                notifyAwaiters(EitherRight(null))
                            } else {
                                notifyAwaiters(EitherLeft(jsonObject))
                            }
                            return@send
                        }

                        val isValid = jsonObject.has("user_id") && jsonObject.has("access_token")

                        if (!isValid) {
                            notifyAwaiters(EitherLeft(jsonObject))
                            return@send
                        }

                        val userId = jsonObject.getLong("user_id")
                        val accessToken = jsonObject.getString("access_token")
                        var expiresIn: Long = 0

                        if (jsonObject.has("expires_in")) {
                            expiresIn = jsonObject.getLong("expires_in")
                        }

                        database.vkAuthorization(userId, accessToken, expiresIn).run(::notifyAwaiters)
                    } else {
                        notifyAwaiters(EitherLeft(it.failure))
                    }
                } catch (e: Throwable) {
                    notifyAwaiters(EitherLeft(e))
                }
            }
        }
    }
}