package ruliov.obliviate

import org.eclipse.jetty.server.Request
import java.util.*
import java.util.regex.Pattern

class HTTPRouter {
    private data class DynamicRoute(
            val method: String, val pattern: Pattern, val controller: IHTTPController)

    private val staticRoutes = HashMap<String, MutableMap<String, IHTTPController>>()
    private val dynamicRoutes = HashMap<String, MutableList<DynamicRoute>>()

    fun route(request: Request): Boolean {
        val method = request.method
        val path = request.requestURI

        val pathToControllerMap = this.staticRoutes.get(method)
        val controller = pathToControllerMap?.get(path)

        if (controller != null) {
            controller.handle(request, null)

            return true
        }

        val dynamicRoutes = this.dynamicRoutes.get(method) ?: return false

        for ((routeMethod, pattern, routeController) in dynamicRoutes) {
            if (!routeMethod.equals(method)) continue

            val matcher = pattern.matcher(path)
            if (!matcher.matches()) continue

            val groupCount = matcher.groupCount()
            val groups = Array<String>(groupCount, { i -> matcher.group(i + 1) })

            routeController.handle(request, groups)

            return true
        }

        return false
    }

    fun addRoute(method: String, staticPath: String, controller: IHTTPController) {
        this.staticRoutes.compute(method, { k, pathToControllerMap ->
            (pathToControllerMap ?: HashMap()).apply { put(staticPath, controller) }
        })
    }

    fun addRoute(method: String, regexPath: Pattern, controller: IHTTPController) {
        this.dynamicRoutes.compute(method, { k, dynamicRoutesList ->
            (dynamicRoutesList ?: LinkedList()).apply { add(DynamicRoute(method, regexPath, controller)) }
        })
    }
}