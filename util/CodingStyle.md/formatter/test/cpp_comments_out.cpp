/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <iostream>

// C++ comment edge cases: // and /* */ in uncommon positions

namespace cppcomments {

// Trailing comments on declarations (alignment group)
static int alpha = 1; // Alpha value
static int beta  = 2; // Beta value
static int gamma = 3; // Gamma value

// Block comment inside template parameter list
template<typename /* Key type */ K, typename /* Value type */ V>
class Map {

public:
    void insert(const K& /* Key */ k, const V& /* Value */ v);
    V    get(const K& k) const; // May throw

private:
    // Internal storage
    struct Entry {

        K key;   // The key
        V value; // The value

    }; // struct Entry

}; // class Map

// Comment inside concept requires expression
template<typename T>
concept HasDraw = requires(T t /* The object */) {

    /* Must have draw */ t.draw();
    { t.area() /* Area method */ } -> std::convertible_to<double>;

}; // concept HasDraw

// Comment between class specifier and base
class /* Derived */ Derived /* From */ : /* public */ public Base {

public:
    // Comment before constructor
    Derived(); // default ctor
    /* explicit */ explicit Derived(int v); // Value ctor
    ~Derived(); // Dtor

    // Inline methods with trailing comments
    int  getValue(     ) const { return v_;     } // Getter
    void setValue(int v)       { v_ = v;        } // Setter
    bool isValid (     ) const { return v_ > 0; } // Validator

private:
    int v_ = 0; /* Initial value */

}; // class Derived

// Comment inside function definition with complex params
void complexFunction(
    int                      a, // Plain int
    const std::string&       b, /* Const ref string */
    std::vector<int>         c, // Vector by value
    std::function<void(int)> d  /* Callback */
)
{
    // Comment at top
    auto result = a; /* Start with a */
    result += static_cast<int>( b.size() ); // Add string length

    // Comment inside if with init-statement
    if(/* Check */ auto it = c.begin(); it != c.end() /* Valid */) {
        result += *it;
    }

    // Comment between else and brace in C++
    if(result > 0) {
        d(result);
    }
    /* Call otherwise */
    else {
        d(0);
    }

    // Comment inside for range header
    for(/* Elem */ int x : c /* Range */) result += x;

    /*
     * Multi-line block comment in C++ function.
     * Second line.
     * Third line.
     * Fourth line.
     * Fifth line.
     * Sixth line.
     */

    // Structured binding with comment
    auto [lo /* Low */, hi /* High */] = std::pair<int, int>{0, result};
    (void)lo;
    (void)hi;
}

// Comment inside requires clause
template<typename T> requires /* Numeric */ std::integral<T> /* Constraint */ T doubled(T x)
{
    return x * 2;
}

} // namespace cppcomments
