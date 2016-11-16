package ruliov.javadb

import ruliov.async.IAsync

interface IDBConnectionPool {
    fun getConnection(): IAsync<IDBConnectionFromPool, Any>
}