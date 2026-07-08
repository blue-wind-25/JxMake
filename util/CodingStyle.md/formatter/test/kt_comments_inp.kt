/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.mycompany.myproject

import org.junit.Test
import kotlin.math.max
import com.mycompany.myproject.util.Helper
import java.util.List as JList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

// Comment before enum declaration
enum class Status(val code: Int) {
    OK(200),          // success
    NOT_FOUND(404);   // missing

    // Comment before method
    fun isOk(): Boolean = this == OK
}

// Comment before sealed class
sealed class Shape

// Comment before data class
data class Circle(val radius: Double) : Shape()

/* Block comment before another data class */
data class Square(val side: Double) : Shape()

// Trailing comment on typealias
typealias ShapeList = List<Shape> // list of shapes

class Box<out T>(val value: T)

class Repository<T> where T : Comparable<T> {

    // Comment before generic method
    fun <U> convert(input: T): U? {
        return null // always null
    }
}

infix fun Int.combineWith(other: Int): Int {
    return this + other // add values
}

fun Int.double(): Int = this * 2

fun sumAll(
    vararg numbers: Int // values
): Int {
    var total = 0

    for (n in numbers) {
        total += n // accumulate
    } // end for

    return total
}

class Widget(
    val name: String, // widget name
    /* identifier */ val id: Int
) {

    // Comment before property
    var count: Int = 0 // current count
        get() = field
        set(value) {
            field = value
        }

    fun describe(
        status: Status, // current status
        /* nullable */ x: Int?
    ): String {

        val label = when (status) {
            // success case
            Status.OK -> "ok"
            /* failure case */
            Status.NOT_FOUND -> "missing"
        };

        val safe = x?.let { it + 1 } /* nullable */ ?: 0

        val (a, b) = Pair(
            1, // first
            2  // second
        )

        return "$name has id $id, label=$label, safe=$safe, pair=$a/$b"
    }

    fun withReceiver(block: Widget.() -> Unit) {
        this.block()
    }

    fun test(): int {
        val result = "123".let parse@{
            // Comment before early return
            if (it.isEmpty())
                return@parse 0

            it.toInt() // parsed value
        }

        val result = "123".let {
            if (it.isEmpty())
                0
            else
                it.toInt() // parsed value
        }

        val value = runCatching outer@{
            if /* time check */ (System.currentTimeMillis() < 0)
                return@outer 42

            100
        }.getOrThrow()

        // Comment before return
        return 0 // dummy value
    }

    fun findFirst(items: List<Int>): Int {

        items.forEach { i ->
            if /* small value */ (i <= 5)
                return@forEach

            return i
        } // end forEach

        for (i in 1..10) {
            if (i > 5)
                return i
        } // end for

        return -1
    }

    fun findFirstX(items: List<Int>): Int {

        return run search@{

            for (i in items) {
                if (i > 5)
                    return@search i
            } // end for

            /*
             * Multi-line block comment inside lambda.
             * Formatter should normalize this.
             */
            -1
        }
    }
}
