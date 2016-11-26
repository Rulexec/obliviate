@file:JvmName("Main")

package ruliov.obliviate

import org.eclipse.jetty.server.Request
import ruliov.async.bind
import ruliov.async.createFuture
import ruliov.jetty.HTTPRouter
import ruliov.jetty.IHTTPMiddleware
import ruliov.jetty.JettyServer
import ruliov.jetty.static.DevelopmentStaticFilesServer
import ruliov.jetty.static.StaticFilesServer
import ruliov.logs.JettyLogger
import ruliov.logs.Logger
import ruliov.obliviate.auth.AuthProvider
import ruliov.obliviate.auth.SessionProvider
import ruliov.obliviate.controllers.*
import ruliov.obliviate.db.Database
import ruliov.toJDBCUrl
import java.io.File
import java.util.regex.Pattern

val LOCAL: Boolean = System.getenv("LOCAL") != null
val PRODUCTION: Boolean = !LOCAL

val OUR_URI = if (PRODUCTION)
    "https://obliviate-332.herokuapp.com" else
    "http://localhost:5001"

val JDBC_DATABASE_URL =
    "jdbc:" + toJDBCUrl(System.getenv("DATABASE_URL") ?:
                        "postgres://ruliov:ruliov@localhost:5432/obliviate1")

val JDBC_DRIVER = "org.postgresql.Driver"
fun initJDBCDriver() {
    Class.forName(JDBC_DRIVER)
}

val PORT = System.getenv("PORT")?.toInt() ?: 5000

val VK_SECRET: String = System.getenv("VK_SECRET") ?: throw IllegalStateException("No VK_SECRET env")

val LOG = Logger()

fun main(args: Array<String>) {
    org.eclipse.jetty.util.log.Log.setLog(JettyLogger())

    initJDBCDriver()

    val database = Database(JDBC_DATABASE_URL)

    val authProvider = AuthProvider(database)

    val sessionProvider = SessionProvider(database)

    LOG.trace("Before database.loadData()")

    database.loadData().bind {
        LOG.info("Data is loaded")

        val classLoader = ClassLoader.getSystemClassLoader()

        val staticFilesServer = if (PRODUCTION)
            StaticFilesServer({ s ->
                classLoader.getResourceAsStream("static" + (if (s == "/") "/index.html" else s))
            }) else
            DevelopmentStaticFilesServer({ s ->
                val file = File("/home/ruliov/projects/obliviate/frontend/static" +
                    (if (s == "/") "/index.html" else s))
                if (file.exists()) file.inputStream()
                else null
            })

        val router = HTTPRouter()

        router.addRoute("GET", "/auth/vk", authVkController(staticFilesServer, authProvider))
        router.addRoute("POST", "/log/in/vk", loginVkController(authProvider))
        router.addRoute("POST", "/log/out", logoutController(database))

        router.addRoute("GET", "/words/", getAllWordsController(database, sessionProvider))
        router.addRoute("POST", "/words/", createWordController(database, sessionProvider))

        router.addRoute("DELETE",
                Pattern.compile("^/words/(\\d+)$"),
                deleteWordController(database, sessionProvider))
        router.addRoute("POST",
                Pattern.compile("^/words/(\\d+)$"),
            updateWordController(database, sessionProvider))

        router.addRoute("GET", "/words/random", getRandomWordController(database, sessionProvider))
        router.addRoute("POST",
                Pattern.compile("^/words/check/(\\d+)$"),
                checkAndGetNextWordController(database, sessionProvider))

        val handler = object : IHTTPMiddleware {
            override fun handle(request: Request, next: () -> Unit) {
                LOG.trace("HTTP ${request.method} ${request.requestURI}")

                val isRouted = router.route(request)
                if (!isRouted) staticFilesServer.handle(request) {
                    LOG.info("404 ${request.requestURI}")
                    request.response.status = 404
                    request.response.writer.write("404")
                }
            }
        }

        val server = JettyServer(handler, PORT)

        LOG.info("Starting server at port $PORT")

        server.start()
        server.join()

        createFuture<Any?>(null)
    }.run {
        if (it == null) {
            LOG.info("Graceful shutdown")
        } else {
            LOG.fatal(it)
            LOG.sync()
            System.exit(1)
        }
    }
}