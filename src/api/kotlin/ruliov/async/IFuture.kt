package ruliov.async

interface IFuture<T> {
    fun run(handler: (T) -> Unit)
}

fun <T> createFuture(run:((T) -> Unit) -> Unit): IFuture<T> {
    return object : IFuture<T> {
        override fun run(handler: (T) -> Unit) = run(handler)
    }
}

fun <T> createFuture(value: T): IFuture<T> {
    return createFuture { it(value) }
}

fun <T> IFuture<T>.bindErrorFuture(noError: () -> IFuture<T?>): IFuture<T?> {
    return createFuture {
        val callback = it

        this.run {
            if (it == null) {
                noError().run(callback)
            } else {
                callback(it)
            }
        }
    }
}

fun catch(block: () -> IFuture<Any?>): IFuture<Any?> {
    return createFuture {
        var future: IFuture<Any?>

        try {
            future = block()
        } catch (e: Throwable) {
            it(e)
            return@createFuture
        }

        future.run(it)
    }
}