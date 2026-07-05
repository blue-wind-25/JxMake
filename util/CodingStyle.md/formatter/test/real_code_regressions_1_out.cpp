/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Regression coverage for three formatter bugs found by dogfood-testing against
// Real-world C++ (github.com/blake-madden/tinyexpr-plusplus): (1) a multi-line call whose
// Sole argument is itself a nested call wrapping onto a second line got a duplicated comma;
// (2) two sibling arguments legitimately sharing one source line got wrongly split onto
// Separate lines; (3) a borderline-length line's "fits?" measurement ran before later
// Spacing-padding was applied, so first-format and re-format disagreed on whether to break it,
// And a while-loop body's closing-comment line-count threshold was evaluated before a
// Line-count-expanding pass ran, so it only fired on a second format pass.

#include <bit>
#include <cstdint>

struct DiffRun {

    char type;
    int  aStart;
    int  aCount;
    int  bStart;
    int  bCount;

}; // struct DiffRun

class Rotator {

    static uint16_t rotr(uint16_t val1, int val2)
    {
        return static_cast<uint16_t>( std::rotr(
            static_cast<uint16_t>(val1), static_cast<int>(val2)
        ) );
    }

    static void mergeRuns(std::vector<DiffRun>& runs, const DiffRun& prev, const DiffRun& step)
    {
        runs.push_back( new DiffRun(step.type, prev.aStart, prev.aCount + step.aCount, prev.bStart,
            prev.bCount + step.bCount) );
    }

}; // class Rotator

int list(int ret)
{
    while(ret == 1) {
        ret = 2;
        ret = combine(
            TE_PURE, some_variant(te_builtins::te_comma),
            { ret, level1(ret) }
        );
    } // while

    return ret;
}
