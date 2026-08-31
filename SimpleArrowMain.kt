///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21+
//COMPILE_OPTIONS -Xcontext-parameters
//KOTLIN 2.4.10

//DEPS io.arrow-kt:arrow-core:2.2.3

@file:Suppress("KotlinPrintToLogpoint")

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import arrow.core.raise.either

fun main() {
    val result: String = either<String, String> {
        validateNumber(100)
    }.fold(ifLeft = { it }, ifRight = { it })
    println("result: $result")
}

context(_: Raise<String>)
fun validateNumber(number: Int): String {
    ensure(number > 0) { "The input number must be greater than 0" }
    return number.toString()
}
