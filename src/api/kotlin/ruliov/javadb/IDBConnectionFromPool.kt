package ruliov.javadb

import java.io.Closeable
import java.sql.Connection

interface IDBConnectionFromPool : Closeable {
    fun getConnection(): Connection
}