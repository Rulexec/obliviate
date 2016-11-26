package ruliov.async

import io.kotlintest.specs.FeatureSpec
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

class FutureTest : FeatureSpec() {
    init {
        feature("createFuture {...}") {
            scenario("pure") {
                var result: Int = 0

                createFuture<Int> {
                    it(42)
                }.run {
                    result = it
                }

                result shouldBe 42
            }

            scenario("async") {
                var result: Int = 0

                val finished = Semaphore(1)
                finished.acquire()

                createFuture<Int> {
                    thread(block = {
                        it(42)
                    })
                }.run {
                    result = it
                    finished.release()
                }

                finished.acquire()

                result shouldBe 42
            }

            scenario("with exception") {
                class MyException(val data: Int) : Throwable()

                var result: Int = 0

                createFuture<Any?> {
                    throw MyException(42)
                }.run {
                    if (it is MyException) {
                        result = it.data
                    }
                }

                result shouldBe 42
            }

            scenario("handler called only once") {
                var callCount = 0
                var exceptionCatched = false

                try {
                    createFuture<Any?> {
                        it(42)
                    }.run {
                        callCount++
                        throw Exception()
                    }
                } catch (e: Throwable) {
                    exceptionCatched = true
                }

                callCount shouldBe 1
                exceptionCatched shouldBe true
            }
        }

        feature("bind") {
            scenario("chains nulls") {
                var passed = 0
                var result: Any? = Unit

                createFuture<Any?> {
                    passed++
                    it(null)
                }.bind {
                    passed++
                    createFuture<Any?> { it(null) }
                }.bind {
                    passed++
                    createFuture<Any?> { it(null) }
                }.run {
                    result = it
                }

                passed shouldBe 3
                result shouldBe null
            }

            scenario("skip binds after non-null") {
                var passed = 0
                var result: Any? = null

                createFuture<Any?> {
                    passed++
                    it(null)
                }.bind {
                    passed++
                    createFuture<Any?> { it(Unit) }
                }.bind {
                    passed++
                    createFuture<Any?> { it(null) }
                }.run {
                    result = it
                }

                passed shouldBe 2
                result shouldBe Unit
            }

            scenario("with exception in IFuture.run") {
                class MyException(val data: Int) : Throwable()

                var result: Any? = null

                createFuture<Any?> {
                    it(null)
                }.bind {
                    createFuture<Any?> { throw MyException(42) }
                }.run {
                    result = it
                }

                (result is MyException) shouldBe true
                (result as MyException).data shouldBe 42
            }

            scenario("with exception in bind") {
                class MyException(val data: Int) : Throwable()

                var result: Any? = null

                createFuture<Any?> {
                    it(null)
                }.bind {
                    throw MyException(42)
                }.run {
                    result = it
                }

                (result is MyException) shouldBe true
                (result as MyException).data shouldBe 42
            }
        }
    }
}