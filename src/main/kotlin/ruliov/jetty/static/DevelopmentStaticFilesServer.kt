package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import ruliov.jetty.IHTTPMiddleware
import java.io.InputStream

class DevelopmentStaticFilesServer(private var resolver: (String) -> InputStream?) : IHTTPMiddleware {
    override fun handle(request: Request, next: () -> Unit) {
        val inputStream = resolver(request.requestURI)

        if (inputStream == null) {
            next()
            return
        }

        addMimeTypeHeaderByUrl(request)

        val outputStream = request.response.outputStream

        // pipe
        var n: Int
        val BUFFER_SIZE = 16 * 1024
        val buffer = ByteArray(BUFFER_SIZE)

        while (true) {
            n = inputStream.read(buffer)
            if (n <= -1) break

            outputStream.write(buffer, 0, n)
        }

        outputStream.close()
    }
}