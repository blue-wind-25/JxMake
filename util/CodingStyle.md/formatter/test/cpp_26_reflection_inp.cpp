constexpr auto refl=^^SomeType;
constexpr auto splice=[:refl:];
constexpr auto computed=[:  computeRefl(x)  :];
constexpr auto nested=^^(a + b);

template<typename T>
constexpr auto reflectMember(T&& obj) {
return ^^obj;
}

void useSplice() {
constexpr auto r = ^^int;
auto v = [:r:];
total += [:r:];
}

constexpr auto x1 = ^^Foo;
constexpr auto x2 = ^^Bar;

void checkReflected(int x) {
if(isReflected(x)) return;
}
