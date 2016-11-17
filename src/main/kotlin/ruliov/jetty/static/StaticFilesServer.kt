package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import ruliov.jetty.IHTTPMiddleware
import java.io.InputStream
import java.util.*

class StaticFilesServer(private var resolver: (String) -> InputStream?) : IHTTPMiddleware {
    private val cacheETag: String

    // TODO: better to inject cache in resolver, but I don't want implement InputStream
    private val cache = HashMap<String, ByteArray>()

    init {
        this.cacheETag = "1-" + java.lang.Long.toHexString(Random().nextLong())
    }

    override fun handle(request: Request, next: () -> Unit) {
        var inputStream: InputStream? = null

        val cachedByteArray = cache[request.requestURI]

        if (cachedByteArray == null) {
            inputStream = resolver(request.requestURI)

            if (inputStream == null) {
                next()
                return
            }
        }

        if (request.getHeader("If-None-Match")?.equals(this.cacheETag) ?: false) {
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
        request.response.setHeader("ETag", this.cacheETag)

        val outputStream = request.response.outputStream

        if (cachedByteArray != null) {
            var i = 0

            while (i < cachedByteArray.size) {
                val n = Math.min(16 * 1024, cachedByteArray.size - i)
                outputStream.write(cachedByteArray, i, n)

                i += n
            }
        } else {
            inputStream ?: throw IllegalStateException("It is impossible")

            val list = LinkedList<ByteArray>()
            var totalSize = 0

            // pipe
            var n: Int
            val BUFFER_SIZE = 16 * 1024
            var buffer = ByteArray(BUFFER_SIZE)
            var bufferPos = 0

            while (true) {
                n = inputStream.read(buffer, bufferPos, BUFFER_SIZE - bufferPos)
                if (n <= -1) break

                outputStream.write(buffer, 0, n)

                totalSize += n
                bufferPos += n

                if (bufferPos == BUFFER_SIZE) {
                    list.add(buffer)
                    buffer = ByteArray(BUFFER_SIZE)

                    bufferPos = 0
                }
            }

            val totalFile = ByteArray(totalSize)
            var chunksCount = 0

            list.forEachIndexed { i, bytes ->
                System.arraycopy(
                        bytes, 0,
                        totalFile, i * BUFFER_SIZE,
                        BUFFER_SIZE)

                chunksCount++
            }

            if (bufferPos != 0) {
                System.arraycopy(
                        buffer, 0,
                        totalFile, chunksCount * BUFFER_SIZE,
                        bufferPos)
            }

            this.cache[request.requestURI] = totalFile
        }

        outputStream.close()
    }
}