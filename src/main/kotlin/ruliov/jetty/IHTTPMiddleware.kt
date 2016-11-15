package ruliov.jetty

import org.eclipse.jetty.server.Request

interface IHTTPMiddleware {
    fun handle(request: Request, next: () -> Unit)
}