package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import ruliov.jetty.IHTTPMiddleware
import ruliov.toHexString
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StaticFilesServer(private var resolver: (String) -> InputStream?) : IHTTPMiddleware {
    private data class CachedFile(val bytes: ByteArray, val etag: String)
    // TODO: better to inject cache in resolver, but I don't want implement InputStream
    private val cache = ConcurrentHashMap<String, CachedFile>()

    override fun handle(request: Request, next: () -> Unit) {
        val cached = cache[request.requestURI]
        var bytes: ByteArray

        if (cached == null) {
            val inputStream = resolver(request.requestURI)

            if (inputStream == null) {
                next()
                return
            }

            bytes = inputStream.toByteArray()
            val md = MessageDigest.getInstance("MD5")

            md.update(bytes)

            val digest = md.digest()
            val etag = "1-" + digest.toHexString()

            cache[request.requestURI] = CachedFile(bytes, etag)

            request.response.setHeader("Cache-Control", "public, must-revalidate")
            request.response.setHeader("ETag", etag)
        } else {
            if (request.getHeader("If-None-Match")?.equals(cached.etag) ?: false) {
                request.response.setStatusWithReason(304, "Not Modified")
                return
            }

            bytes = cached.bytes
        }

        addMimeTypeHeaderByUrl(request)

        request.response.outputStream.write(bytes)

        request.response.closeOutput()
    }
}