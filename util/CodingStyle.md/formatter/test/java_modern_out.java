/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.example.modern;

import java.util.List;
import java.util.Optional;

// Java 17+ constructs: records, sealed classes, switch expressions, text blocks, var, pattern matching

// --- Records ---

public record Point(int x, int y) {}

public record NamedPoint(String name, int x, int y) implements Comparable<NamedPoint> {

    // Compact constructor
    public NamedPoint
    {
        if(name == null) throw new IllegalArgumentException("name must not be null");
    }
    public double distance() { return Math.sqrt(x * x + y * y); }

} // record NamedPoint

// --- Sealed classes ---

public abstract sealed class Shape permits Circle, Rectangle, Triangle {

    public abstract double area();

} // class Shape

public final class Circle extends Shape {

    private final double radius;
    public Circle(double radius) { this.radius = radius; }
    public double area() { return Math.PI * radius * radius; }

} // class Circle

public final class Rectangle extends Shape {

    private final double width;
    private final double height;
    public Rectangle(double width, double height) { this.width = width; this.height = height; }
    public double area() { return width * height; }

} // class Rectangle

public non-sealed class Triangle extends Shape {

    private final double base;
    private final double height;
    public Triangle(double base, double height) { this.base = base; this.height = height; }
    public double area() { return 0.5 * base * height; }

} // class Triangle

// Sealed interface with many permits -- should wrap
public sealed interface Expr
    permits Expr.Num,
            Expr.Add,
            Expr.Mul,
            Expr.Neg,
            Expr.Var,
            Expr.Lit,
            Expr.XXX,
            Expr.YYY {

    record Num(int value) implements Expr {}
    record Add(Expr left, Expr right) implements Expr {}
    record Mul(Expr left, Expr right) implements Expr {}
    record Neg(Expr operand) implements Expr {}
    record Var(String name) implements Expr {}
    record Lit(String text) implements Expr {}

} // interface Expr

// --- Switch expressions ---

public class Switcher {

    public String dayType(Day day)
    {
        return switch(day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
            case SATURDAY, SUNDAY                             -> "Weekend";
        };
    }

    // Switch expression with block body
    public int compute(int x)
    {
        return switch(x) {
            case 0 -> 0;
            case 1 -> {
                System.out.println("one");
                yield 1;
            }
            default -> x * 2;
        }; // switch
    }

    // Switch expression with aligned arrows
    public String describe(Status s)
    {
        return switch(s) {
            case ACTIVE   -> "active";
            case INACTIVE -> "inactive";
            case PENDING  -> "pending";
        };
    }

} // class Switcher

// --- Text blocks ---

public class Templates {

    private static final String JSON = """
            {
                "key": "value",
                "list": [1, 2, 3]
            }
            """;

    private static final String HTML = """
            <html>
                <body>
                    <p>Hello</p>
                </body>
            </html>
            """;

} // class Templates

// --- var ---

public class VarUsage {

    public void demo(List<String> items)
    {
        var result = new java.util.ArrayList<String>();
        for(var item : items) {
            var trimmed = item.trim();
            result.add(trimmed);
        }
        var map   = new java.util.HashMap<String, Integer>();
        var count = map.size();
    }

} // class VarUsage

// --- Pattern matching instanceof ---

public class Patterns {

    public String describe(Object obj)
    {
             if(obj instanceof String s)     return "string: " + s.toUpperCase();
        else if(obj instanceof Integer i)    return "int: " + i;
        else if(obj instanceof List<?> list) return "list of " + list.size();

        return "unknown";
    }

} // class Patterns

enum Day {

    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

} // enum Day

enum Status {

    ACTIVE, INACTIVE, PENDING

} // enum Status
