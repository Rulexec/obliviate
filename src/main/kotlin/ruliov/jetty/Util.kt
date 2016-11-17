package ruliov.jetty

import org.eclipse.jetty.server.Request

fun createController(handler: (request: Request, groups: Array<String>?) -> Unit): IHTTPController {
    return object : IHTTPController {
        override fun handle(request: Request, groups: Array<String>?) = handler(request, groups)
    }
}

fun createControllerRespondsJSON(handler: (request: Request, groups: Array<String>?) -> Unit): IHTTPController
    = createController(handler).respondsJSON()

fun createControllerRespondsJSONDontCaches(
        handler: (request: Request, groups: Array<String>?) -> Unit): IHTTPController
    = createController(handler).dontCaches().respondsJSON()

fun IHTTPController.respondsJSON(): IHTTPController {
    return createController { request, strings ->
        request.response.setHeader("Content-Type", "application/json; charset=utf-8")
        this@respondsJSON.handle(request, strings)
    }
}

fun IHTTPController.dontCaches(): IHTTPController {
    return createController { request, strings ->
        request.response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        request.response.setHeader("Pragma", "no-cache")
        request.response.setHeader("Expires", "0")
        request.response.setHeader("Content-Type", "application/json; charset=utf-8")
        this@dontCaches.handle(request, strings)
    }
}