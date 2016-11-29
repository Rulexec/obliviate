package ruliov.logs

import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

sealed class LogLevel(val level: Int) {
    object FATAL : LogLevel(1)
    object ERROR : LogLevel(2)
    object WARNING : LogLevel(3)
    object INFO : LogLevel(4)
    object MESSAGE : LogLevel(5)
    object TRACE : LogLevel(6)
}

/*
 * <level of log> <microseconds from start padded to left width=14> <log message>
 */

class Logger {
    private data class Log(val level: LogLevel, val passed: Long, val messages: Array<out Any?>)

    private val start: Long = System.nanoTime()

    private val thread = Thread({ threadRun() })

    private val queue = ConcurrentLinkedQueue<Log>()
    private val semaphore = Semaphore(1)
    private val finishedSemaphore = Semaphore(1)

    private var waitingForFinish = AtomicInteger(0)

    init {
        this.write(LogLevel.INFO, (System.nanoTime() - start) / 1000, "Logging started at ${System.currentTimeMillis()}")

        this.semaphore.acquire()

        this.thread.isDaemon = true
        this.thread.start()

        this.finishedSemaphore.acquire()

        Runtime.getRuntime().addShutdownHook(Thread({ this.sync() }))
    }

    private fun threadRun() {
        var clearWaitForFinish = false

        while (true) {
            while (true) {
                val log = this.queue.poll() ?: break

                this.write(log.level, log.passed, *log.messages)
            }

            if (waitingForFinish.get() > 0) {
                if (clearWaitForFinish) {
                    val n = waitingForFinish.decrementAndGet()
                    clearWaitForFinish = false

                    this.finishedSemaphore.release()

                    if (n > 0) continue
                } else {
                    clearWaitForFinish = true
                    continue
                }
            }

            this.semaphore.acquire()
        }
    }

    fun sync() {
        this.waitingForFinish.incrementAndGet()

        this.semaphore.release()
        this.finishedSemaphore.acquire()
    }

    fun log(level: LogLevel, vararg messages: Any?) {
        this.queue.add(Log(level, (System.nanoTime() - start) / 1000, messages))
        this.semaphore.release()
    }
    fun fatal(vararg messages: Any?) = this.log(LogLevel.FATAL, *messages)
    fun error(vararg messages: Any?) = this.log(LogLevel.ERROR, *messages)
    fun warn(vararg messages: Any?) = this.log(LogLevel.WARNING, *messages)
    fun info(vararg messages: Any?) = this.log(LogLevel.INFO, *messages)
    fun msg(vararg messages: Any?) = this.log(LogLevel.MESSAGE, *messages)
    fun trace(vararg messages: Any?) = this.log(LogLevel.TRACE, *messages)

    private fun write(level: LogLevel, passed: Long, vararg messages: Any?) {
        val padded = String.format("%-14s", passed)

        val sb = StringBuilder()

        var isFirst = true
        for (message in messages) {
            when (message) {
            is Throwable -> {
                val sw = StringWriter()
                message.printStackTrace(PrintWriter(sw))
                sb.append(sw.toString())
            }
            null -> sb.append("null")
            else -> sb.append(message.toString())
            }

            if (isFirst) isFirst = false
            else sb.append(' ')
        }

        val lines = sb.toString().split('\n')

        val iterator = lines.iterator()
        System.out.println("${level.level} $padded ${iterator.next()}")

        for (x in iterator) {
            System.out.println(">                $x")
        }
    }
}