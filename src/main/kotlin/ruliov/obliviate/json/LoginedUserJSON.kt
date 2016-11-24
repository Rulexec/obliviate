package ruliov.obliviate.json

import ruliov.obliviate.data.users.LoginedUser

fun LoginedUser.toJSON(): String {
    return """{"id":${this.id},"token":"${this.token}","expiresAt":${this.expiresAt}}"""
}