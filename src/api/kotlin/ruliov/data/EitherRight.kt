package ruliov.data

class EitherRight<L, R>(val value: R) : IEither<L, R> {
    override fun isLeft(): Boolean = false

    override fun left(): L = throw UnsupportedOperationException("It is right!")

    override fun isRight(): Boolean = true

    override fun right(): R = value

    override fun toString(): String = this.right().toString()
}