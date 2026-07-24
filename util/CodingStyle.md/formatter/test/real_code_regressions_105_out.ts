// Regression: nested-brace object-type argument inside a tracked generic
// Clause must not lose the outer generic's ANGLE_BRACKET tracking, even
// When a member name inside the nested braces is a KEYWORD not in
// GENERIC_SAFE_KEYWORDS, or when the member separator ';' is seen
type Foo = Record<
    string,
    { local: string; default?: Expression }
>;

// Regression: a mapped-type object as a generic type argument
type Bar = UnionToIntersection<
    { [key in Event]: (...args: EmitFn<Event, key>) => void }[Event]
>;

// Regression: ternary inside a parenthesized grouping expression must not
// Have its ':' treated as a return-type colon
type Baz<T, K> = T extends object ? (keyof T extends K ? true : false) : false;

// Regression: 'typeof x =>' in a type-predicate position must not have
// The identifier wrapped in parens by arrow-parameter-paren enforcement
declare function isVal(val: unknown): val is keyof typeof globalThis extends infer R ? R : never;

// Regression: a trailing type-annotation colon wrapping to the next line
// Must not have a bogus semicolon inserted right after it
declare function longSig(a: number, b: string):
    void;

// Regression: a standalone TS function-type parameter list must not be
// Padded/tightened like an arbitrary grouping paren
type Handler = (...args : any[]) => void;
