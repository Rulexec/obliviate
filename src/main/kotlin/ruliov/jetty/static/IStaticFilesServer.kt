package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import ruliov.jetty.IHTTPMiddleware

interface IStaticFilesServer : IHTTPMiddleware {
    override fun handle(request: Request, next: () -> Unit) {
        if (!this.serveFile(request.requestURI, request)) next()
    }

    fun serveFile(fileName: String, request: Request): Boolean
}