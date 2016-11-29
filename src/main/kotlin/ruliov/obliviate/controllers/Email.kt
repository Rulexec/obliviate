package ruliov.obliviate.controllers

import ruliov.UTF8
import ruliov.jetty.createController
import ruliov.jetty.respondsJSON
import ruliov.jetty.static.toByteArray
import ruliov.obliviate.LOG
import ruliov.obliviate.db.Database
import java.util.regex.Pattern

private val emailRegex = Pattern.compile("^[^@]+@[^@]+$")

fun saveEmailVerbsController(database: Database) = createController { request, groups ->
    respondsJSON(request)

    val email = request.inputStream.toByteArray().toString(UTF8)

    LOG.msg("EMAIL $email")

    if (emailRegex.matcher(email).matches()) {
        database.saveEmail(email).run {
            if (it != null) LOG.error("Email save:", it)
        }
    }

    request.response.writer.write("""{"error":null}""")
    request.response.closeOutput()
}