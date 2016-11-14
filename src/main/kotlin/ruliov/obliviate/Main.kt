package ruliov.obliviate

import org.eclipse.jetty.server.Request
import ruliov.obliviate.json.toJSON
import java.util.*

fun main(args: Array<String>) {
    val router = HTTPRouter()
    val random = Random()

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

    val server = JettyServer(router, 5000)

    server.start()
    server.join()
}