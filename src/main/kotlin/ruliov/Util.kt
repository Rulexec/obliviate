package ruliov

import java.util.regex.Pattern

fun ByteArray.toHexString(): String {
    val sb = StringBuilder(this.size * 2)

    this.forEach { sb.append(java.lang.Integer.toHexString(it.toInt() and 0xff)) }

    return sb.toString()
}

private val pattern = Pattern.compile("^postgres://(.+):(.+)@(.+):(\\d+)/(.+)$")
fun toJDBCUrl(url: String): String {
    //"postgres://ruliov:ruliov@localhost:5432/obliviate"

    val matcher = pattern.matcher(url)
    if (!matcher.matches()) throw Exception("Bad url: $url")

    val user = matcher.group(1)
    val password = matcher.group(2)
    val host = matcher.group(3)
    val port = matcher.group(4)
    val database = matcher.group(5)

    return "postgresql://$host:$port/$database?user=$user&password=$password"
}