package ruliov.logs

import org.eclipse.jetty.util.log.Logger

class JettyLogger : org.eclipse.jetty.util.log.Logger {
    override fun getName(): String = "no"
    override fun warn(msg: String?, vararg args: Any?) {}
    override fun warn(thrown: Throwable?) {}
    override fun warn(msg: String?, thrown: Throwable?) {}
    override fun info(msg: String?, vararg args: Any?) {}
    override fun info(thrown: Throwable?) {}
    override fun info(msg: String?, thrown: Throwable?) {}
    override fun getLogger(name: String?): Logger = this
    override fun setDebugEnabled(enabled: Boolean) {}
    override fun isDebugEnabled(): Boolean = false
    override fun debug(msg: String?, vararg args: Any?) {}
    override fun debug(msg: String?, value: Long) {}
    override fun debug(thrown: Throwable?) {}
    override fun debug(msg: String?, thrown: Throwable?) {}
    override fun ignore(ignored: Throwable?) {}
}