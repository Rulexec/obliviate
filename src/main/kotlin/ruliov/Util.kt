package ruliov

fun ByteArray.toHexString(): String {
    val sb = StringBuilder(this.size * 2)

    this.forEach { sb.append(java.lang.Integer.toHexString(it.toInt() and 0xff)) }

    return sb.toString()
}