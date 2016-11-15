package ruliov.jetty

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.HandlerList
import ruliov.jetty.HTTPRouter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyServer(private val handler: IHTTPMiddleware, port: Int) {
    private var server: Server

    inner class ServerHandler : AbstractHandler() {
        override fun handle(target: String?,
                            baseRequest: Request?,
                            request: HttpServletRequest?,
                            response: HttpServletResponse?)
        {
            if (baseRequest != null && response != null) {
                // origin = baseRequest.getHeader("Origin")
                // if (origin != null && origin.equals("http://localhost:8000") || origin == null) ...
                // FIXME: for development only
                response.setHeader("Access-Control-Allow-Origin", "*")

                /*val routed = this@JettyServer.router.route(baseRequest)

                if (routed) baseRequest.isHandled = true*/

                var handled = true

                handler.handle(baseRequest, { handled = false })

                baseRequest.isHandled = handled
            }
        }
    }

    init {
        this.server = Server(port)

        val handlers = HandlerList()
        handlers.handlers = arrayOf(ServerHandler())
        server.handler = handlers
    }

    fun start() = this.server.start()
    fun join() = this.server.join()
}