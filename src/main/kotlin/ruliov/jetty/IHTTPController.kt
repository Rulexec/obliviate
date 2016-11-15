package ruliov.jetty

import org.eclipse.jetty.server.Request

interface IHTTPController {
    fun handle(request: Request, groups: Array<String>?)
}