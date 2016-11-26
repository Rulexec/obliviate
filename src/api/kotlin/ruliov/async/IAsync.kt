package ruliov.async

import ruliov.data.EitherLeft
import ruliov.data.EitherRight
import ruliov.data.IEither

interface IAsync<out R, out E> {
    fun run(handler:(IEither<E, R>) -> Unit)
}

inline fun <R, reified E> createAsync(crossinline run:((IEither<E, R>) -> Unit) -> Unit): IAsync<R, E> {
    return object : IAsync<R, E> {
        override fun run(handler: (IEither<E, R>) -> Unit) {
            if (E::class == Any::class) {
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
                        handler(EitherLeft(e as E))
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

inline fun <R, E> asyncResult(result: R): IAsync<R, E> {
    return object : IAsync<R, E> {
        override fun run(handler: (IEither<E, R>) -> Unit) = handler(EitherRight(result))
    }
}
inline fun <R, E> asyncError(error: E): IAsync<R, E> {
    return object : IAsync<R, E> {
        override fun run(handler: (IEither<E, R>) -> Unit) = handler(EitherLeft(error))
    }
}

inline fun <R, reified E, NR> IAsync<R, E>.success(crossinline success: (R) -> IAsync<NR, E>): IAsync<NR, E> {
    return createAsync {
        val callback = it

        this.run {
            if (it.isRight()) {
                success(it.right()).run(callback)
            } else {
                callback(EitherLeft(it.left()))
            }
        }
    }
}

inline fun <R, reified E> IAsync<R, E>.bindToFuture(crossinline success: (R) -> IFuture<E?>): IFuture<E?> {
    return createFuture {
        val callback = it

        this.run {
            if (it.isRight()) {
                success(it.right()).run(callback)
            } else {
                callback(it.left())
            }
        }
    }
}