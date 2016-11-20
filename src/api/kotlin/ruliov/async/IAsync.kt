package ruliov.async

import ruliov.data.EitherLeft
import ruliov.data.EitherRight
import ruliov.data.IEither

interface IAsync<R, E> {
    fun run(handler:(IEither<E, R>) -> Unit)
}

fun <R, E> createAsync(run:((IEither<E, R>) -> Unit) -> Unit): IAsync<R, E> {
    return object : IAsync<R, E> {
        override fun run(handler: (IEither<E, R>) -> Unit) = run(handler)
    }
}

fun <R, E> asyncResult(result: R): IAsync<R, E> = createAsync { it(EitherRight(result)) }
fun <R, E> asyncError(error: E): IAsync<R, E> = createAsync { it(EitherLeft(error)) }

fun <R, E, NR> IAsync<R, E>.success(success: (R) -> IAsync<NR, E>): IAsync<NR, E> {
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

fun <R, E> IAsync<R, E>.bindToErrorFuture(success: (R) -> IFuture<E?>): IFuture<E?> {
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

fun <R> catchAsync(block: () -> IAsync<R, Any>): IAsync<R, Any> {
    return createAsync {
        var async: IAsync<R, Any>

        try {
            async = block()
        } catch (e: Throwable) {
            it(EitherLeft(e))
            return@createAsync
        }

        async.run(it)
    }
}