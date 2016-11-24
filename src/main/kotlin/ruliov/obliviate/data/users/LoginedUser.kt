package ruliov.obliviate.data.users

data class LoginedUser(val id: Long, val token: String, val expiresAt: Long)