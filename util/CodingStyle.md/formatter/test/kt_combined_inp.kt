/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.mycompany.myproject

import org.junit.Test
import kotlin.math.max
import com.mycompany.myproject.util.Helper
import java.util.List as JList

enum class Status(val code: Int) {
    OK(200),
    NOT_FOUND(404);

    fun isOk(): Boolean = this == OK
}

sealed class Shape
data class Circle(val radius: Double) : Shape()
data class Square(val side: Double) : Shape()

typealias ShapeList = List<Shape>

class Box<out T>(val value: T)

class Repository<T> where T : Comparable<T> {
    fun <U> convert(input: T): U? {
        return null
    }
}

infix fun Int.combineWith(other: Int): Int {
    return this + other
}

fun Int.double(): Int = this * 2

fun sumAll(vararg numbers: Int): Int {
    var total = 0
    for (n in numbers) {
        total += n
    }
    return total
}

class Widget(val name: String, val id: Int) {
    var count: Int = 0
        get() = field
        set(value) {
            field = value
        }

    fun describe(status: Status, x: Int?): String {
        val label = when (status) {
            Status.OK -> "ok"
            Status.NOT_FOUND -> "missing"
        };
        val safe = x?.let { it + 1 } ?: 0
        val (a, b) = Pair(1, 2)
        return "$name has id $id, label=$label, safe=$safe, pair=$a/$b"
    }

    fun withReceiver(block: Widget.() -> Unit) {
        this.block()
    }

    fun findFirst(items: List<Int>): Int {
        for (i in 1..10) {
            if (i > 5) return@findFirst i
        }
        return -1
    }
}
