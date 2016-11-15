@file:JvmName("Main")

package ruliov.obliviate

import org.eclipse.jetty.server.Request
import ruliov.jetty.HTTPRouter
import ruliov.jetty.IHTTPController
import ruliov.jetty.IHTTPMiddleware
import ruliov.jetty.JettyServer
import ruliov.jetty.static.StaticFilesServer
import ruliov.obliviate.json.toJSON

fun main(args: Array<String>) {
    val router = HTTPRouter()

    router.addRoute("GET", "/words/random", object : IHTTPController {
        override fun handle(request: Request, groups: Array<String>?) {
            val response = request.response

            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            response.setHeader("Pragma", "no-cache")
            response.setHeader("Expires", "0")
            response.setHeader("Content-Type", "application/json; charset=utf-8")

            val word = Words.getRandomWordWith4RandomTranslations()

            request.response.writer.write(word.toJSON())
        }
    })

    val classLoader = ClassLoader.getSystemClassLoader()

    val staticFilesServer = StaticFilesServer({ s ->
        // TODO: How insecure is it?

        var filename = s

        if (filename.equals("/")) filename = "/index.html"

        classLoader.getResourceAsStream("static" + filename)
    })

    val handler = object : IHTTPMiddleware {
        override fun handle(request: Request, next: () -> Unit) {
            val isRouted = router.route(request)
            if (!isRouted) staticFilesServer.handle(request, next)
        }
    }

    val PORT = System.getenv("PORT")?.toInt() ?: 5000

    val server = JettyServer(handler, PORT)

    server.start()
    server.join()
}