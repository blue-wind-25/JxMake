/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <string>

class MyClass {
};

std::string compute();
MyClass&    getRef();

void m(bool cond)
{
    if(cond) {
        std::string s = compute();
    }
    if(cond) {
        MyClass* obj = nullptr;
    }
    if(cond) {
        MyClass& ref = getRef();
    }
}
