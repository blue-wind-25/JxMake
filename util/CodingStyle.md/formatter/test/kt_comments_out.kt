/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.mycompany.myproject

import kotlin.math.max

import java.util.List as JList

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import org.junit.Test

import com.mycompany.myproject.util.Helper

// Comment before enum declaration
enum class Status(val code: Int) {

    OK(200),          // Success
    NOT_FOUND(404)    // Missing

    ;

    // Comment before method
    fun isOk(): Boolean = this == OK

} // enum class Status

// Comment before sealed class
sealed class Shape

// Comment before data class
data class Circle(val radius: Double) : Shape()

/* Block comment before another data class */
data class Square(val side: Double) : Shape()

// Trailing comment on typealias
typealias ShapeList = List<Shape> // List of shapes

class Box<out T>(val value: T)

class Repository<T> where T : Comparable<T> {

    // Comment before generic method
    fun <U> convert(input: T): U?
    {
        return null // Always null
    }

} // class Repository

infix fun Int.combineWith(other: Int): Int
{
    return this + other // Add values
}

fun Int.double(): Int = this * 2

fun sumAll(
    vararg numbers : Int // Values
): Int
{
    var total = 0

    for(n in numbers) {
        total += n // Accumulate
    }

    return total
}

class Widget(
    val name : String, // Widget name
    /* Identifier */ val id : Int
) {

    // Comment before property
    var count : Int = 0 // Current count
        get() = field
        set(value) {
            field = value
        }

    fun describe(
        status : Status, // Current status
        /* Nullable */ x : Int?
    ): String
    {

        val label = when(status) {

            // Success case
            Status.OK        -> "ok"
            /* Failure case */
            Status.NOT_FOUND -> "missing"

        } // when status

        val safe   = x?.let { it + 1 } /* Nullable */ ?: 0
        val (a, b) = Pair(
            1, // First
            2  // Second
        )

        return "$name has id $id, label=$label, safe=$safe, pair=$a/$b"
    }

    fun withReceiver(block: Widget.() -> Unit)
    {
        this.block()
    }

    fun test(): Int
    {
        val result = "123".let parse@ {
            // Comment before early return
            if( it.isEmpty() ) return@parse 0

            it.toInt() // Parsed value
        }

        val result = "123".let {
            if( it.isEmpty() ) 0
            else               it.toInt() // Parsed value
        }

        val classified = "123".let {
                 if( it.isEmpty() )     0
            else if(it.length > 2)      1
            else if(it.length > 100000) 2
            else                        it.toInt() // Parsed value
        }

        val value = runCatching outer@ {
            if /* Time check */ ( System.currentTimeMillis() < 0 ) return@outer 42

            100
        }.getOrThrow()

        // Comment before return

        return 0 // Dummy value
    }

    fun findFirst(items: List<Int>): Int
    {
        items.forEach { i ->
            if /* Small value */ (i <= 5) return@forEach

            return i
        }

        for(i in 1..10) {
            if(i > 5) return i
        }

        return -1
    }

    fun findFirstX(items: List<Int>): Int
    {
        if( it.isEmpty() ) return@parse 1

        //% JXM_CFMT_DIS
        if (it.isEmpty())
            return@parse 2
        //% JXM_CFMT_ENA

        if( it.isEmpty() ) return@parse 3

        /*% JXM_CFMT_DIS */
        if (it.isEmpty())
            return@parse 4
        /*% JXM_CFMT_ENA */

        if( it.isEmpty() ) return@parse 5

        return run search@ {
            for(i in items) {
                if(i > 5) return@search i
            }

            /*
             * Multi-line block comment inside lambda.
             * Formatter should normalize this.
             */
            -1
        }
    }

} // class Widget
