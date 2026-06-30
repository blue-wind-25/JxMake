/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <concepts>
#include <compare>
#include <coroutine>
#include <ranges>
#include <span>

// Lone comment

// C++17/20/23 constructs: structured bindings, concepts/requires,
// consteval/constinit, <=>, coroutines, init-statement if/switch.

namespace modern {

////////////////////////////////////////////////////////////////////////////////////
// C++17: structured bindings

struct Pair { int first; int second; };

struct Triple {
  float x;
    float y;
  float z;
};


void useBindings() {
    auto [a, b] = Pair{1, 2};
    auto [x, y, z] = Triple{1.0f, 2.0f, 3.0f};

    // In a group with plain declarations
    int   count = 10;
    auto [lo, hi] = Pair{0, count};
    bool   active = true;

    // Range-based for with structured binding
    std::vector<Pair> pairs = {{1,2},{3,4}};
    for (auto& [f, s] : pairs) {
        (void)f;
        (void)s;
    }
}

////////////////////////////////////////////////////////////////////////////////////
// C++17: init-statement if and switch

void initStatements(int raw) {
    if (int v = raw * 2; v > 100) {
        // 'v' in scope here
    } else {
        (void)v;
    }

    switch (int code = raw % 4; code) {
        case 0: break;
        case 1: break;
        default: break;
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
T square(T x) {
    return x * x;
}

// Trailing requires clause
template<typename T>
T cube(T x) requires Numeric<T> {
    return x * x * x;
}

// Concept in class template
template<Drawable T>
class Canvas {
public:
    void add(T item) { items_.push_back(std::move(item)); }
    void render() {
        int dummy1 = 10;
        auto dummy2 = dummy1;
        for (auto& it : items_) {
            it.draw();
        }
    }

private:
    std::vector<T> items_;
};

////////////////////////////////////////////////////////////////////////////////////
// C++20: consteval and constinit

consteval int factorial(int n) {
    return n <= 1 ? 1 : n * factorial(n - 1);
}

constinit int globalCounter = 0;
static constinit float defaultGain = 1.0f;

////////////////////////////////////////////////////////////////////////////////////
// C++20: three-way comparison operator <=>

struct Version {
    int major;
    int minor;
    int patch;

    auto operator<=>(const Version&) const = default;
};

struct Weight {
    double value;

    auto operator<=>(const Weight& other) const {
        return value <=> other.value;
    }
    bool operator==(const Weight&) const = default;
};

////////////////////////////////////////////////////////////////////////////////////
// C++20: coroutines (basic shape, not full impl)

struct Generator {
    struct promise_type {
        int value;
        Generator get_return_object() { return {}; }
        std::suspend_always initial_suspend() { return {}; }
        std::suspend_always final_suspend() noexcept { return {}; }
        std::suspend_always yield_value(int v) { value = v; return {}; }
        void return_void() {}
        void unhandled_exception() {}
    };
};

Generator makeGenerator() {
    co_yield 1;
    co_yield 2;
    co_return;
}

////////////////////////////////////////////////////////////////////////////////////
// C++20: others

// Nested namespace definition
namespace alpha::beta::gamma {

struct Point {
    int x;
    int y;
};

} // namespace alpha::beta::gamma

// Designated initializers
void designatedInitializers() {
    alpha::beta::gamma::Point p{
        .x = 10,
        .y = 20
    };
    (void)p;
}

// Requires-expression with nested requirement
template<typename T>
concept LargeIntegral = requires {
    requires std::integral<T>;
    requires sizeof(T) >= 4;
};

// Lambda template parameters
auto genericLambda = []<typename T>(T value) {
    return value;
};

// Lambda template with requires clause
auto integralLambda = []<typename T>(T value)
    requires std::integral<T>
{
    return value;
};

// Qualified concept in template parameter list
template<std::ranges::range R>
void consume(R&& r) {
    for (auto&& e : r) {
        (void)e;
    }
}

// Fold expression
template<typename... Ts>
auto sum(Ts... xs) {
    return (... + xs);
}

// Attributes in common locations
[[nodiscard]]
int getValue() {
    return 42;
}

void attributeExamples(int x) {
    [[maybe_unused]]
    int temp = x;

    if (x > 0) [[likely]] {
        ++x;
    }
    else [[unlikely]] {
        --x;
    }

    if (x > 0) {
        [[likely]] ++x;
    }
    else {
        [[unlikely]] --x;
    }
}

////////////////////////////////////////////////////////////////////////////////////
// C++23: others
////////////////////////////////////////////////////////////////////////////////////
// Additional C++23 syntax

// Alias declaration in init-statement
void aliasInitStatement() {
    if (using Int = int; true) {
        Int x = 42;
        (void)x;
    }

    switch (using Size = std::size_t; Size{1}) {
        case 1:
            break;
        default:
            break;
    }
}

// Explicit object parameter using auto
struct Counter {
    int value{};

    void increment(this auto& self) {
        ++self.value;
    }

    int get(this const auto& self) {
        return self.value;
    }

    void reset(this auto&& self) {
        self.value = 0;
    }
};

// Explicit object parameter with templates
struct Wrapper {
    template<typename T>
    void assign(this Wrapper& self, T&& value) {
        // These ones intentionally have spaces
        (void) self;
        (void) value;
    }
};

struct Vec2 {

    int x{};
    int y{};

    // Explicit object parameter (deducing this)
    int lengthSquared(this const Vec2& self)
    {
        return self.x * self.x + self.y * self.y;
    }

    // static operator()
    static void operator()() {}

    // Multidimensional operator[]
    int operator[](std::size_t row, std::size_t col) const
    { return static_cast<int>(row * 100 + col); }

}; // struct Vec2

consteval int always42()
{
    return 42;
}

constexpr int compute()
{
    // if consteval (no condition)
    if consteval {
        return always42();
    }
    else {
        return 0;
    }
}

template <typename T>
constexpr int value(T v)
{
    // if constexpr (with condition)
    if constexpr(std::is_integral_v<T>) {
        return static_cast<int>(v);
    }
    else {
        return 0;
    }
}

int main()
{
    Vec2 v{3, 4};

    // Multidimensional operator[]
    int a = v[1, 2];

    // static operator()
    Vec2::operator()();

    // Attribute introducer
    [[ assume(a >= 0) ]];

    if( int v = 10; myfunc(v) ) return -1;

    const void* a = nullptr;
    char* b = static_cast<char*>(const_cast<void*>(a));
    int* c = reinterpret_cast<int*>(b);

    return compute() + value(5) + v.lengthSquared();
}

} // namespace modern
