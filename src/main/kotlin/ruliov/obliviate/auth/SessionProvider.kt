package ruliov.obliviate.auth

import ruliov.async.IAsync
import ruliov.async.asyncResult
import ruliov.async.success
import ruliov.obliviate.data.sessions.Session
import ruliov.obliviate.db.Database

class SessionProvider(private val database: Database) {
    fun getSession(token: String): IAsync<Session?, Any> =
        database.getSession(token).success {
            if (it == null) return@success asyncResult<Session?, Any>(null)

            asyncResult<Session?, Any>(Session(database, it.token, it.id, it.expiresAt))
        }
}