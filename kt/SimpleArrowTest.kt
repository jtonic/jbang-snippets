///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21
//KOTLIN 2.4.21

//DEPS org.jetbrains.kotlin:kotlin-test-junit5:2.4.21
//DEPS org.junit.jupiter:junit-jupiter-engine:5.10.1
//DEPS io.arrow-kt:arrow-functions:2.2.1
//SOURCES jb/JBangJunit5Test.kt


import arrow.core.andThen
import arrow.core.partially1
import arrow.core.partially2
import kt.jb.JBangJunit5Test
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals

class SimpleArrowTest : JBangJunit5Test() {

    @Test
    fun `andThen and partially`() {
        val add: (Int, Int) -> Int = { a, b -> a + b }
        val double: (Int) -> Int = { it * 2 }

        // partially1 fixes the first argument: (Int, Int) -> Int becomes (Int) -> Int
        val addTen: (Int) -> Int = add.partially1(10)

        // andThen composes left-to-right: first addTen, then double
        val addTenThenDouble: (Int) -> Int = addTen andThen double

        // print takes three parameters; partially2 fixes the 2nd one each time:
        // (Int, String, String) -> String  --partially2("Math")-->  (Int, String) -> String
        // (Int, String) -> String          --partially2("sum")--->  (Int) -> String
        val print: (Int, String, String) -> String = { value, domain, operation ->
            "[$domain] $operation = $value".also(::println)
        }
        val printMathSum: (Int) -> String = print.partially2("Math").partially2("sum")

        val addTenThenDoubleThenPrint: (Int) -> String = addTenThenDouble andThen printMathSum

        assertEquals(15, addTen(5))
        assertEquals(30, addTenThenDouble(5))
        assertEquals("[Math] sum = 30", addTenThenDoubleThenPrint(5))
    }
}

fun main() = JBangJunit5Test.run(SimpleArrowTest::class)
