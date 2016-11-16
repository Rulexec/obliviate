package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import ruliov.jetty.IHTTPMiddleware
import java.io.InputStream

class StaticFilesServer(private var resolver: (String) -> InputStream?) : IHTTPMiddleware {
    override fun handle(request: Request, next: () -> Unit) {
        val inputStream = resolver(request.requestURI)

        if (inputStream == null) {
            next()
            return
        }

        if (request.getHeader("If-None-Match")?.equals("cached") ?: false) {
            if (System.getenv("LOCAL") == null || !request.requestURI.equals("/bundle.js")) {
                request.response.setStatusWithReason(304, "Not Modified")
                request.response.closeOutput()
                return
            }
        }

        val lastDotPos = request.requestURI.lastIndexOf('.')
        if (lastDotPos != -1) {
            val extension = request.requestURI.substring(lastDotPos + 1)

            when (extension) {
                "png" -> request.response.setHeader("Content-Type", "image/png")
                "ico" -> request.response.setHeader("Content-Type", "image/x-icon")
                "css","less" -> request.response.setHeader("Content-Type", "text/css")
                "html" -> request.response.setHeader("Content-Type", "text/html")
                "js" -> request.response.setHeader("Content-Type", "text/javascript")
            }
        }

        request.response.setHeader("Cache-Control", "public, must-revalidate")
        request.response.setHeader("ETag", "cached")

        val outputStream = request.response.outputStream

        // pipe
        var n: Int
        val buffer = ByteArray(16 * 1024)

        while (true) {
            n = inputStream.read(buffer)
            if (n <= -1) break

            outputStream.write(buffer, 0, n)
        }

        outputStream.close()
    }
}