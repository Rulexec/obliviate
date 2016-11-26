package ruliov.obliviate.controllers

import org.eclipse.jetty.server.Request
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import ruliov.jetty.respondsJSON
import ruliov.obliviate.LOG
import ruliov.obliviate.auth.SessionProvider
import ruliov.obliviate.data.sessions.Session

fun tryAuth(
sessionProvider: SessionProvider,
handler: (Request, Array<String>?, Session?) -> Unit): (Request, Array<String>?) -> Unit =
body@ { request, groups ->
    val token = request.getHeader("X-Auth-Token")

    val asyncContext = request.startAsync()

    if (token.isNullOrBlank()) return@body handler(request, groups, null)

    sessionProvider.getSession(token).run {
        if (it.isLeft()) {
            LOG.error("sessionProvider.getSession(token):", it.left())

            respondsJSON(request)
            respond500(request)
            asyncContext.complete()
        } else {
            handler(request, groups, it.right())
        }
    }
}

fun authRequired(
    sessionProvider: SessionProvider,
    handler: (Request, Array<String>?, Session) -> Unit): (Request, Array<String>?) -> Unit =
    body@ { request, groups ->
        val token = request.getHeader("X-Auth-Token")
        if (token.isNullOrBlank()) {
            respondsJSON(request)
            respond401(request)
            return@body
        }

        val asyncContext = request.startAsync()

        sessionProvider.getSession(token).run {
            if (it.isLeft()) {
                LOG.error("sessionProvider.getSession(token):", it.left())

                respondsJSON(request)
                respond500(request)
                asyncContext.complete()
            } else {
                val session = it.right()
                if (session != null) {
                    handler(request, groups, session)
                } else {
                    respondsJSON(request)
                    respond401(request)
                    asyncContext.complete()
                }
            }
        }
    }

fun parseJSONObjectOrRespond400(request: Request): JSONObject? {
    try {
        val tokener = JSONTokener(request.inputStream)
        val jsonObject = JSONObject(tokener)

        return jsonObject
    } catch (e: JSONException) {
        respond400(request, "parse")
        return null
    }
}

fun getStringFromJSONOrRespond400(request: Request, jsonObject: JSONObject, key: String): String? {
    val value = jsonObject.get(key)

    if (value == null) {
        respond400(request, "parse")
        return null
    }

    if (value is String) {
        return value
    } else {
        return null
    }
}

fun respond400(request: Request, reason: String = "not-specified") {
    request.response.status = 400
    request.response.writer.write("""{"error":"$reason"}""")
    request.response.closeOutput()
}
fun respond401(request: Request) {
    request.response.status = 401
    request.response.writer.write("""{"error":"401"}""")
    request.response.closeOutput()
}
fun respond500(request: Request) {
    request.response.status = 500
    request.response.writer.write("""{"error":"500"}""")
    request.response.closeOutput()
}