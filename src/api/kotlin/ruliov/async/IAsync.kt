package ruliov.async

import ruliov.data.IEither

interface IAsync<R, E> {
    fun run(handler:(IEither<E, R>) -> Unit)
}

fun <R, E> createAsync(run:((IEither<E, R>) -> Unit) -> Unit): IAsync<R, E> {
    return object : IAsync<R, E> {
        override fun run(handler: (IEither<E, R>) -> Unit) = run(handler)
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