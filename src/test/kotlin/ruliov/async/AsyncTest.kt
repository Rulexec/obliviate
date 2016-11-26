package ruliov.async

import io.kotlintest.specs.FeatureSpec
import ruliov.data.EitherLeft
import ruliov.data.EitherRight
import ruliov.data.IEither

class AsyncTest : FeatureSpec() {
    init {
        feature("success") {
            scenario("chains at EitherRight") {
                var result: IEither<Any, Int>? = null

                createAsync<Int, Any> {
                    it(EitherRight(1))
                }.success {
                    asyncResult<Int, Any>(it + 2)
                }.success {
                    asyncResult<Int, Any>(it + 4)
                }.run {
                    result = it
                }

                (result != null) shouldBe true
                result!!.isRight() shouldBe true
                result!!.right() shouldBe 1 + 2 + 4
            }

            scenario("stop chaining at EitherLeft") {
                var result: IEither<Any, Int>? = null

                createAsync<Int, Int> {
                    it(EitherRight(1))
                }.success {
                    asyncError<Int, Int>(13)
                }.success {
                    asyncResult<Int, Int>(it + 4)
                }.run {
                    result = it
                }

                (result != null) shouldBe true
                result!!.isLeft() shouldBe true
                result!!.left() shouldBe 13
            }

            scenario("with exception in success") {
                class MyException(val data: Int) : Throwable()

                var result: Any? = null

                createAsync<Int, Any> {
                    it(EitherRight(12))
                }.success<Int, Any, Int> {
                    throw MyException(42)
                }.run {
                    result = it
                }

                (result is EitherLeft<*, *>) shouldBe true
                @Suppress("UNCHECKED_CAST")
                val eitherLeft = result as EitherLeft<Any, Int>
                (eitherLeft.value is MyException) shouldBe true
                (eitherLeft.value as MyException).data shouldBe 42
            }

            scenario("with exception in IAsync.run") {
                class MyException(val data: Int) : Throwable()

                var result: Any? = null

                createAsync<Int, Any?> {
                    it(EitherRight(12))
                }.success<Int, Any?, Int> {
                    createAsync { throw MyException(42) }
                }.run {
                    result = it
                }

                (result is EitherLeft<*, *>) shouldBe true
                @Suppress("UNCHECKED_CAST")
                val eitherLeft = result as EitherLeft<Any?, Int>
                (eitherLeft.value is MyException) shouldBe true
                (eitherLeft.value as MyException).data shouldBe 42
            }
        }
    }
}