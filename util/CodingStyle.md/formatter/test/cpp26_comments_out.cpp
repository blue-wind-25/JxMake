/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Pack indexing examples
template<typename... T>
// Comment between template<> and the using-declaration
using Nth = T...[N];
template<typename... T>
using Selected = T...[ computeIndex() ];  // Call inside index
using Skipped  = T...[0];  // Zero-based

// Deprecated API marker
void oldApi() = delete(
    "use newApi() instead" // Reason, trailing on the arg itself
);

/* Placeholder examples */
auto [_, count] = getResult(); // Structured binding, trailing
if( auto _ = acquireLock(); true ) {
    // Comment as the sole content before real work starts
    doWork();
    // Trailing comment right before close, no blank line
}

// Contract clauses
int divide(int a, int b)
    // Pre-condition: divisor nonzero
    pre(b != 0)
    post(r: r * b == a)  // Post-condition: result matches, moved inline
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

void process(int x)
{
    // Runtime assertion
    contract_assert(x >= 0);

    // Trailing note with a blank line above it, still inside the block
}
