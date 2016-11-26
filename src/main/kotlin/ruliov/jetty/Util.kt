@file:Suppress("NOTHING_TO_INLINE")

package ruliov.jetty

import org.eclipse.jetty.server.Request

inline fun createController(crossinline handler: (request: Request, groups: Array<String>?) -> Unit): IHTTPController {
    return object : IHTTPController {
        override fun handle(request: Request, groups: Array<String>?) = handler(request, groups)
    }
}

inline fun respondsJSON(request: Request) {
    request.response.setHeader("Content-Type", "application/json; charset=utf-8")
}


inline fun dontCaches(request: Request) {
    request.response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
    request.response.setHeader("Pragma", "no-cache")
    request.response.setHeader("Expires", "0")
    request.response.setHeader("Content-Type", "application/json; charset=utf-8")
}