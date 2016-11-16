@file:JvmName("Main")

package ruliov.obliviate

import org.eclipse.jetty.server.Request
import ruliov.async.bindErrorFuture
import ruliov.async.catch
import ruliov.async.createFuture
import ruliov.jetty.HTTPRouter
import ruliov.jetty.IHTTPController
import ruliov.jetty.IHTTPMiddleware
import ruliov.jetty.JettyServer
import ruliov.jetty.static.StaticFilesServer
import ruliov.obliviate.db.Database
import ruliov.obliviate.json.toJSON
import ruliov.toJDBCUrl
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern

fun main(args: Array<String>) {
    val DATABASE_URL =
            toJDBCUrl(System.getenv("DATABASE_URL") ?:
            "postgres://ruliov:ruliov@localhost:5432/obliviate")

    val database = Database(
        "org.postgresql.Driver",
        "jdbc:" + DATABASE_URL
    )

    database.loadData().bindErrorFuture { catch {
        val router = HTTPRouter()

        router.addRoute("GET", "/words/random", object : IHTTPController {
            override fun handle(request: Request, groups: Array<String>?) {
                val response = request.response

                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                response.setHeader("Pragma", "no-cache")
                response.setHeader("Expires", "0")
                response.setHeader("Content-Type", "application/json; charset=utf-8")

                val word = database.getRandomWordWith4RandomTranslations()

                request.response.writer.write(word.toJSON())
            }
        })
        router.addRoute("POST", Pattern.compile("^/words/check/(\\d+)$"), object : IHTTPController {
            override fun handle(request: Request, groups: Array<String>?) {
                groups ?: throw IllegalStateException()
                val response = request.response
                response.setHeader("Content-Type", "application/json; charset=utf-8")

                val wordId = groups[0].toLong()
                val translationIdJSON = database.getWordTranslationId(wordId)?.let { "\"$it\"" } ?: "null"

                val nextWordJSON = database.getRandomWordWith4RandomTranslations().toJSON()

                request.response.writer.write("{\"correct\":$translationIdJSON,\"word\":$nextWordJSON}")
            }
        })

        val classLoader = ClassLoader.getSystemClassLoader()

        val loader: (String) -> InputStream = if (System.getenv("LOCAL") != null)
            { s -> classLoader.getResourceAsStream("static" + s) } else
            { s -> File("/home/ruliov/projects/obliviate/frontend/static" + s).inputStream() }

        val staticFilesServer = StaticFilesServer({ s ->
            // TODO: Load whole static frontend into the memory

            var filename = s

            if (filename.equals("/")) filename = "/index.html"

            loader(filename)
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

        createFuture<Any?>(null)
    } }.run {
        if (it == null) {
            println("Graceful shutdown")
        } else {
            if (it is Throwable) {
                it.printStackTrace()
            } else {
                System.err.println(it)
                System.exit(1)
            }
        }
    }
}