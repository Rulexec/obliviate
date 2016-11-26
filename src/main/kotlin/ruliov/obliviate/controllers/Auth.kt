package ruliov.obliviate.controllers

import ruliov.UTF8
import ruliov.jetty.IHTTPController
import ruliov.jetty.createController
import ruliov.jetty.respondsJSON
import ruliov.jetty.static.IStaticFilesServer
import ruliov.jetty.static.toByteArray
import ruliov.obliviate.LOG
import ruliov.obliviate.auth.AuthProvider
import ruliov.obliviate.db.Database
import ruliov.obliviate.exceptions.PleaseRetryException
import ruliov.obliviate.json.toJSON

fun authVkController(staticFilesServer:IStaticFilesServer, authProvider: AuthProvider): IHTTPController =
createController { request, groups ->
    staticFilesServer.serveFile("/authVk.html", request)

    val code = request.getParameter("code") ?: return@createController

    authProvider.checkVk(code).run {
        if (it.isLeft()) LOG.error("checkVk(code):", it)
    }
}

fun loginVkController(authProvider: AuthProvider) = createController { request, groups ->
    respondsJSON(request)

    val jsonObject = parseJSONObjectOrRespond400(request) ?: return@createController
    val code = getStringFromJSONOrRespond400(request, jsonObject, "code") ?: return@createController

    val asyncContext = request.startAsync()

    authProvider.checkVk(code).run {
        if (it.isRight()) {
            val loginedUser = it.right()

            if (loginedUser != null) {
                request.response.writer.write("""{"user":${loginedUser.toJSON()}}""")
            } else {
                request.response.writer.write("""{"error":"bad-auth"}""")
            }
        } else {
            val error = it.left()

            if (error is PleaseRetryException) {
                request.response.writer.write("""{"error":"retry"}""")
            } else {
                LOG.error("checkVk(code):", it.left())
                respond500(request)
            }
        }

        asyncContext.complete()
    }
}

fun logoutController(database: Database) = createController { request, groups ->
    respondsJSON(request)

    val token = request.inputStream.toByteArray().toString(UTF8)

    if (token.isEmpty()) return@createController respond400(request, "parse")

    database.logout(token).run { if (it != null) LOG.error(it) }

    request.response.writer.write("""{"error":null}""")
}