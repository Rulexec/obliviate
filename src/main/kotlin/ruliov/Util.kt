package ruliov

import java.nio.charset.Charset
import java.util.regex.Pattern

val UTF8 = Charset.forName("UTF-8")

fun ByteArray.toHexString(): String {
    val sb = StringBuilder(this.size * 2)

    this.forEach { sb.append(java.lang.Integer.toHexString(it.toInt() and 0xff)) }

    return sb.toString()
}
private inline fun Char.hexToInt(): Int {
    if (this <= '9') return this - '0'
    else return this - 'a' + 10
}
fun String.hexToByteArray(): ByteArray {
    val array = ByteArray(this.length / 2)

    for (i in 0..(this.length / 2 - 1)) {
        val a = this[2 * i]
        val b = this[2 * i + 1]

        array[i] = (a.hexToInt() * 16 + b.hexToInt()).toByte()
    }

    return array
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