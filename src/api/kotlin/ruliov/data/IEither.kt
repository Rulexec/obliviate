package ruliov.data

interface IEither<out L, out R> {
    fun isLeft(): Boolean
    fun left(): L

    fun isRight(): Boolean
    fun right(): R
}