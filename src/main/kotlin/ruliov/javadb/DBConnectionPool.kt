package ruliov.javadb

import ruliov.async.IAsync
import ruliov.async.createAsync
import ruliov.data.EitherRight
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class DBConnectionPool(slots: Int, private val url: String) : IDBConnectionPool {
    private val connections: Array<Connection>
    private val freeSlots: BlockingQueue<Int> = ArrayBlockingQueue(slots)

    init {
        this.connections = Array(slots, { DriverManager.getConnection(url) })

        for (i in 0..(slots - 1)) {
            this.freeSlots.put(i)
        }
    }

    override fun getConnection(): IAsync<IDBConnectionFromPool, Any> {
        return createAsync {
            val freeSlot = this.freeSlots.take()

            var freed = false

            it(EitherRight(object : IDBConnectionFromPool {
                override fun getConnection(): Connection {
                    if (freed) throw UnsupportedOperationException("Closed")

                    return this@DBConnectionPool.connections[freeSlot]
                }

                override fun close() {
                    if (freed) return

                    freed = true

                    this@DBConnectionPool.freeSlots.put(freeSlot)
                }
            }))
        }
    }
}