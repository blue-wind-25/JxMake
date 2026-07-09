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

enum class Status(val code: Int) {

    OK(200),
    NOT_FOUND(404)

    ;

    fun isOk(): Boolean = this == OK

} // enum class Status

sealed class Shape
data class Circle(val radius: Double) : Shape()
data class Square(val side: Double) : Shape()

typealias ShapeList = List<Shape>

class Box<out T>(val value: T)

class Repository<T> where T : Comparable<T> {

    fun <U> convert(input: T): U?
    {
        return null
    }

} // class Repository

infix fun Int.combineWith(other: Int): Int
{
    return this + other
}

fun Int.double(): Int = this * 2

fun sumAll(vararg numbers: Int): Int
{
    var total = 0
    for(n in numbers) total += n

    return total
}

class Widget(val name: String, val id: Int) {

    var count : Int = 0
        get() = field
        set(value) {
            field = value
        }

    fun describe(status: Status, x: Int?): String
    {
        val label = when(status) {

            Status.OK        -> "ok"
            Status.NOT_FOUND -> "missing"

        } // when status
        val safe   = x?.let { it + 1 } ?: 0
        val (a, b) = Pair(1, 2)

        return "$name has id $id, label=$label, safe=$safe, pair=$a/$b"
    }

    fun withReceiver(block: Widget.() -> Unit)
    {
        this.block()
    }

    fun test(): Int
    {
        val result1 = "123".let parse@ {
            if( it.isEmpty() ) return@parse 0

            it.toInt()
        }
        val result2 = "123".let {
            if( it.isEmpty() ) 0
            else               it.toInt()
        }
        val value = runCatching outer@ {
            if( System.currentTimeMillis() < 0 ) return@outer 42

            100
        }.getOrThrow()

        return 0
    }

    fun findFirst(items: List<Int>): Int
    {
        items.forEach { i ->
            if(i <= 5) return@forEach
            return i
        }
        for(i in 1..10) {
            if(i > 5) return i
        }

        return -1
    }

    fun findFirstX(items: List<Int>): Int
    {
        return run search@ {
            for(i in items) {
                if(i > 5) return@search i
            }

            -1
        }
    }

} // class Widget

class Accessors {

    fun getX()        : Int    = 111
    fun getLongName() : Int    = 222
    fun getZ()        : String = "abcxyz"

} // class Accessors
