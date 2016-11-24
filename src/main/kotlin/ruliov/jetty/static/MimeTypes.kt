package ruliov.jetty.static

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response

fun addMimeTypeHeaderByFileName(fileName: String, response: Response): Unit {
    val lastDotPos = fileName.lastIndexOf('.')
    if (lastDotPos != -1) {
        val extension = fileName.substring(lastDotPos + 1)

        when (extension) {
            "png" -> response.setHeader("Content-Type", "image/png")
            "ico" -> response.setHeader("Content-Type", "image/x-icon")
            "css","less" -> response.setHeader("Content-Type", "text/css; charset=UTF-8")
            "html" -> response.setHeader("Content-Type", "text/html; charset=UTF-8")
            "js" -> response.setHeader("Content-Type", "text/javascript; charset=UTF-8")
        }
    }
}