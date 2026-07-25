/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Widget {

public:
    Widget(Widget&& other)
        : first_member(
            _STD move(other._Outer)
        ), second_member(
            _STD move(other._Inner)
        ), third_member(other.third_member) {}

private:
    int first_member;
    int second_member;
    int third_member;

}; // class Widget
