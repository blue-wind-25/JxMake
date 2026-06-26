/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <concepts>
#include <compare>
#include <coroutine>
#include <ranges>
#include <span>

// C++17/20/23 constructs: structured bindings, concepts/requires,
// consteval/constinit, <=>, coroutines, init-statement if/switch.

namespace modern {

////////////////////////////////////////////////////////////////////////////////////
// C++17: structured bindings

struct Pair   { int first; int second; };
struct Triple { float x; float y; float z; };

void useBindings()
{
    auto [a, b]    = Pair{ 1, 2 };
    auto [x, y, z] = Triple{ 1.0f, 2.0f, 3.0f };

    // In a group with plain declarations
    int   count   = 10;
    auto [lo, hi] = Pair{0, count};
    bool  active  = true;

    // Range-based for with structured binding
    std::vector<Pair> pairs = { {1, 2}, {3, 4} };
    for(auto& [f, s] : pairs) {
        (void) f;
        (void) s;
    }
}

////////////////////////////////////////////////////////////////////////////////////
// C++17: init-statement if and switch

void initStatements(int raw)
{
    if(int v = raw * 2; v > 100) {
        // v in scope here
    }
    else {
        (void) v;
    }

    switch(int code = raw % 4; code) {
        case 0  : break;
        case 1  : break;
        default : break;
    }
}

////////////////////////////////////////////////////////////////////////////////////
// C++20: concepts and requires

template<typename T>
concept Numeric = std::integral<T> || std::floating_point<T>;

template<typename T>
concept Drawable = requires(T t) {
    t.draw();
    { t.area() } -> std::convertible_to<double>;
};

template<typename T>
concept Serializable = requires(T t, std::ostream& os) {
    { t.serialize(os) } -> std::same_as<void>;
    T::version;
};

// Requires clause on function
template<Numeric T>
T square(T x)
{
    return x * x;
}

// Trailing requires clause
template<typename T>
T cube(T x) requires Numeric<T>
{
    return x * x * x;
}

// Concept in class template
template<Drawable T>
class Canvas {

public:

    void add(T item) { items_.push_back( std::move(item) ); }

    void render()
    {
        for(auto& it : items_) {
            it.draw();
        }
    }

private:

    std::vector<T> items_;

}; // class Canvas

////////////////////////////////////////////////////////////////////////////////////
// C++20: consteval and constinit

consteval int factorial(int n)
{
    return n <= 1 ? 1 : n * factorial(n - 1);
}

constinit        int   globalCounter = 0;
static constinit float defaultGain   = 1.0f;

////////////////////////////////////////////////////////////////////////////////////
// C++20: three-way comparison operator <=>

struct Version {

    int major;
    int minor;
    int patch;

    auto operator<=>(const Version&) const = default;

}; // struct Version

struct Weight {

    double value;

    auto operator<=>(const Weight& other) const
    {
        return value <=> other.value;
    }

    bool operator==(const Weight&) const = default;

}; // struct Weight

////////////////////////////////////////////////////////////////////////////////////
// C++20: coroutines (basic shape, not full impl)

struct Generator {

    struct promise_type {

        int value;
        Generator           get_return_object  (     )          { return {};            }
        std::suspend_always initial_suspend    (     )          { return {};            }
        std::suspend_always final_suspend      (     ) noexcept { return {};            }
        std::suspend_always yield_value        (int v)          { value = v; return {}; }
        void                return_void        (     )          {                       }
        void                unhandled_exception(     )          {                       }

    }; // struct promise_type

}; // struct Generator

Generator makeGenerator()
{
    co_yield 1;
    co_yield 2;
    co_return;
}

} // namespace modern
