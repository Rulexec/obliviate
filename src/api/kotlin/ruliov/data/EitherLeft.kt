package ruliov.data

class EitherLeft<L, R>(val value: L) : IEither<L, R> {
    override fun isLeft(): Boolean = true

    override fun left(): L = value

    override fun isRight(): Boolean = false

    override fun right(): R = throw UnsupportedOperationException("It is left!")
}