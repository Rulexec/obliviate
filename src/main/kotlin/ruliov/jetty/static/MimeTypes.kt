package ruliov.jetty.static

import org.eclipse.jetty.server.Request

fun addMimeTypeHeaderByUrl(request: Request): Unit {
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
}