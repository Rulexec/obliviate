package ruliov.obliviate.controllers

import ruliov.jetty.createController
import ruliov.obliviate.db.Database

fun resetDbController(database: Database) = createController { request, groups ->
    database.resetDb().run {
        if (it == null) {
            request.response.writer.write("ok")
        } else {
            request.response.writer.write(it.toString())
        }
    }
}