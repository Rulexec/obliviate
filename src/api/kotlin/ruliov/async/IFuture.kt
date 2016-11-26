package ruliov.async

interface IFuture<out T> {
    fun run(handler: (T) -> Unit)
}

inline fun <reified T> createFuture(crossinline run:((T) -> Unit) -> Unit): IFuture<T> {
    return object : IFuture<T> {
        override fun run(handler: (T) -> Unit) {
            if (T::class == Any::class) {
                var exceptionInHandler = false

                try {
                    return run({
                        try {
                            handler(it)
                        } catch (e: Throwable) {
                            exceptionInHandler = true
                            throw e
                        }
                    })
                } catch (e: Throwable) {
                    if (!exceptionInHandler) {
                        handler(e as T)
                    } else {
                        throw e
                    }
                }
            } else {
                return run(handler)
            }
        }
    }
}

inline fun <T> createFuture(value: T): IFuture<T> {
    return object : IFuture<T> {
        override fun run(handler: (T) -> Unit) = handler(value)
    }
}

inline fun <reified T> IFuture<T?>.bind(crossinline noError: () -> IFuture<T?>): IFuture<T?> {
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