package ruliov.data

interface IEither<out L, out R> {
    fun isLeft(): Boolean
    fun left(): L

    fun isRight(): Boolean
    fun right(): R
}

fun <L, R, T> IEither<L, R>.map(left: (L) -> T, right: (R) -> T): T {
    if (this.isLeft()) {
        return left(this.left())
    } else {
        return right(this.right())
    }
}
fun <L, R, T> IEither<L, R>.mapR(right: (R) -> T, left: (L) -> T): T = this.map(left, right)