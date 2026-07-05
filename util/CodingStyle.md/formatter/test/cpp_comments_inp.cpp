/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <iostream>

// C++ comment edge cases: // and /* */ in uncommon positions.

namespace cppcomments {

// Trailing comments on declarations (alignment group)
static int alpha = 1;   // alpha value
static int beta  = 2;  // beta value
static int gamma = 3; // gamma value

// Block comment inside template parameter list
template<typename /* key type */ K, typename /* value type */ V>
class Map {
public:
    void insert(const K& /* key */ k, const V& /* value */ v);
    V    get(const K& k) const; // may throw

private:
    // internal storage
    struct Entry {
        K key;   // the key
        V value; // the value
    };
};

// Comment inside concept requires expression
template<typename T>
concept HasDraw = requires(T t /* the object */) {
    /* must have draw */ t.draw();
    { t.area() /* area method */ } -> std::convertible_to<double>;
};

// Comment between class specifier and base
class /* derived */ Derived /* from */ : /* public */ public Base {
public:
    // Comment before constructor
    Derived(); // default ctor
    /* explicit */ explicit Derived(int v); // value ctor
    ~Derived(); // dtor

    // Inline methods with trailing comments
    int  getValue() const { return v_; }   // getter
    void setValue(int v)  { v_ = v; }      // setter
    bool isValid() const  { return v_ > 0; } // validator

private:
    int v_ = 0; /* initial value */
};

// Comment inside function definition with complex params
void complexFunction(
    int                        a, // plain int
    const std::string&         b, /* const ref string */
    std::vector<int>           c, // vector by value
    std::function<void(int)>   d  /* callback */
) {
    // Comment at top
    auto result = a; /* start with a */
    result += static_cast<int>(b.size()); // add string length

    // Comment inside if with init-statement
    if (/* check */ auto it = c.begin(); it != c.end() /* valid */) {
        result += *it;
    }

    // Comment between else and brace in C++
    if (result > 0) {
        d(result);
    } /* call otherwise */ else {
        d(0);
    }

    // Comment inside for range header
    for (/* elem */ int x : c /* range */) {
        result += x;
    }

    /*
     * Multi-line block comment in C++ function.
     * Second line.
     * Third line.
     * Fourth line.
     * Fifth line.
     * Sixth line.
     */

    // Structured binding with comment
    auto [lo /* low */, hi /* high */] = std::pair<int,int>{0, result};
    (void)lo;
    (void)hi;
}

// Comment inside requires clause
template<typename T>
    requires /* numeric */ std::integral<T> /* constraint */
T doubled(T x) {
    return x * 2;
}

} // namespace cppcomments
