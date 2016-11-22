package ruliov.obliviate.db.migrations

import ruliov.async.bindToErrorFuture
import ruliov.async.catchFuture
import ruliov.javadb.DBConnectionPool

fun executeSQL(sql: String) {
    Class.forName("org.postgresql.Driver")
    val pool = DBConnectionPool(1, JDBC_DATABASE_URL)

    pool.getConnection().bindToErrorFuture { catchFuture { it.use {
        val connection = it.getConnection()

        val statement = connection.createStatement()

        statement.execute(sql)

        throw Exception()
    } } }.run {
        if (it != null) {
            System.err.println(it)
        }
    }
}