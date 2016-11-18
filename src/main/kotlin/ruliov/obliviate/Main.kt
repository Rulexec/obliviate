@file:JvmName("Main")

package ruliov.obliviate

import org.eclipse.jetty.server.Request
import ruliov.async.bindErrorFuture
import ruliov.async.catch
import ruliov.async.createFuture
import ruliov.jetty.*
import ruliov.jetty.static.DevelopmentStaticFilesServer
import ruliov.jetty.static.StaticFilesServer
import ruliov.obliviate.controllers.*
import ruliov.obliviate.db.Database
import ruliov.obliviate.json.toCompactJSON
import ruliov.obliviate.json.toJSON
import ruliov.toJDBCUrl
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern

val LOCAL: Boolean = System.getenv("LOCAL") != null
val PRODUCTION: Boolean = !LOCAL

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

        router.addRoute("GET", "/words/", getAllWordsController(database))
        router.addRoute("DELETE",
                Pattern.compile("^/words/(\\d+)$"),
                deleteWordController(database))
        router.addRoute("POST", Pattern.compile("^/words/(\\d+)$"), updateWordController(database));

        router.addRoute("GET", "/words/random", getRandomWordController(database))
        router.addRoute("POST",
                Pattern.compile("^/words/check/(\\d+)$"),
                checkAndGetNextWordController(database))


        router.addRoute("GET", "/admin/resetdb", resetDbController(database))

        val classLoader = ClassLoader.getSystemClassLoader()

        val staticFilesServer = if (PRODUCTION)
            StaticFilesServer({ s ->
                classLoader.getResourceAsStream("static" + (if (s == "/") "/index.html" else s))
            }) else
            DevelopmentStaticFilesServer({ s ->
                File("/home/ruliov/projects/obliviate/frontend/static" +
                        (if (s == "/") "/index.html" else s)).inputStream()
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