package ruliov.obliviate.controllers

import ruliov.handleError
import ruliov.jetty.IHTTPController
import ruliov.jetty.createController
import ruliov.jetty.createControllerRespondsJSON
import ruliov.jetty.static.IStaticFilesServer
import ruliov.obliviate.auth.AuthProvider
import ruliov.obliviate.json.toJSON

fun authVkController(staticFilesServer:IStaticFilesServer, authProvider: AuthProvider): IHTTPController =
createController { request, groups ->
    staticFilesServer.serveFile("/authVk.html", request)

    val code = request.getParameter("code") ?: return@createController

    authProvider.checkVk(code).run {
        if (it.isLeft()) handleError(it.left())
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
            handleError(it.left())

            request.response.status = 500
            request.response.writer.write("""{"error":"500"}""")
        }

        asyncContext.complete()
    }


    /*val maybeParsed = extractTwoStringsFromJSONArray(request.inputStream)
    if (maybeParsed == null) {
        request.response.status = 400
        return@createControllerRespondsJSON
    }

    val word = maybeParsed.first
    val translation = maybeParsed.second

    database.createWord(word, translation).run { it.mapR({
        request.response.writer.write("{\"error\":null,\"id\":$it}")
    }, {
        if (it is Database.WordValidationError) {
            request.response.status = 400
            request.response.writer.write("{\"error\":\"validation\"}")
        } else {
            request.response.status = 500
            request.response.writer.write("{\"error\":\"server\"}")
            System.err.println("Word creation error: $it")
        }
    }) }*/
}
