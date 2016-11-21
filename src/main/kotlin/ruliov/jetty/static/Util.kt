package ruliov.jetty.static

import java.io.InputStream
import java.util.*

fun InputStream.toByteArray(): ByteArray {
    val list = LinkedList<ByteArray>()
    var totalSize = 0

    var n: Int
    val BUFFER_SIZE = 16 * 1024
    var buffer = ByteArray(BUFFER_SIZE)
    var bufferPos = 0

    while (true) {
        n = this.read(buffer, bufferPos, BUFFER_SIZE - bufferPos)
        if (n <= -1) break

        totalSize += n
        bufferPos += n

        if (bufferPos == BUFFER_SIZE) {
            list.add(buffer)
            buffer = ByteArray(BUFFER_SIZE)

            bufferPos = 0
        }
    }

    val totalFile = ByteArray(totalSize)
    var chunksCount = 0

    list.forEachIndexed { i, bytes ->
        System.arraycopy(
            bytes, 0,
            totalFile, i * BUFFER_SIZE,
            BUFFER_SIZE)

        chunksCount++
    }

    if (bufferPos != 0) {
        System.arraycopy(
            buffer, 0,
            totalFile, chunksCount * BUFFER_SIZE,
            bufferPos)
    }

    return totalFile
}