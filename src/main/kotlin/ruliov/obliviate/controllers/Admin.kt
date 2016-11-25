package ruliov.obliviate.controllers

import ruliov.jetty.createController
import ruliov.obliviate.LOG
import ruliov.obliviate.db.Database

fun resetDbController(database: Database) = createController { request, groups ->
    val asyncContext = request.startAsync()

    database.resetDb().run {
        if (it == null) {
            request.response.writer.write("ok")
        } else {
            LOG.error("Reset db:", it)
            request.response.writer.write(it.toString())
        }

        asyncContext.complete()
    }
}