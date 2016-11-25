package ruliov.obliviate.controllers

import ruliov.jetty.IHTTPController
import ruliov.jetty.createController
import ruliov.jetty.createControllerRespondsJSON
import ruliov.jetty.static.IStaticFilesServer
import ruliov.obliviate.LOG
import ruliov.obliviate.auth.AuthProvider
import ruliov.obliviate.json.toJSON

fun authVkController(staticFilesServer:IStaticFilesServer, authProvider: AuthProvider): IHTTPController =
createController { request, groups ->
    staticFilesServer.serveFile("/authVk.html", request)

    val code = request.getParameter("code") ?: return@createController

    authProvider.checkVk(code).run {
        if (it.isLeft()) LOG.error("checkVk(code):", it)
    }
}

fun loginVk(authProvider: AuthProvider) = createControllerRespondsJSON { request, groups ->
    val jsonObject = parseJSONObjectOrRespond400(request) ?: return@createControllerRespondsJSON
    val code = getStringFromJSONOrRespond400(request, jsonObject, "code") ?: return@createControllerRespondsJSON

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
            LOG.error("checkVk(code):", it)
            respond500(request)
        }

        asyncContext.complete()
    }
}
