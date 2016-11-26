package ruliov.obliviate.data.sessions

import ruliov.async.IFuture
import ruliov.obliviate.db.Database

class Session internal constructor(
    private val database: Database,
    val token: String, val userId: Long, val expiresAt: Long)
{
    fun terminate(): IFuture<Any?> = database.logout(this.token)
}