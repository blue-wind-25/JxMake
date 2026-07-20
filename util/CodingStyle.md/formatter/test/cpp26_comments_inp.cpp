/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Pack indexing examples
template<typename... T>
// comment between template<> and the using-declaration
using Nth = T...[N];
template<typename... T>
using Selected = T...[computeIndex()];  // call inside index
using Skipped = T...[0];  // zero-based

// Deprecated API marker
void oldApi() = delete(
    "use newApi() instead" // reason, trailing on the arg itself
);

/* Placeholder examples */
auto [_, count] = getResult();  // structured binding, trailing
if(auto _ = acquireLock(); true) {
    // comment as the sole content before real work starts
    doWork();
    // trailing comment right before close, no blank line
}

// Contract clauses
int divide(int a, int b)
// pre-condition: divisor nonzero
pre(b != 0)
post(r: r * b == a)  // post-condition: result matches, moved inline
{
    return a / b;
}

int clamp(int x, int lo, int hi)
pre(lo <= hi)
/*
 * Multi-sentence rationale for this contract.
 * Kept as a block comment between two contract clauses.
 */
pre(x >= lo && x <= hi)
{
    return x;
}

void process(int x) {
    // runtime assertion
    contract_assert(x >= 0);

    // trailing note with a blank line above it, still inside the block
}
