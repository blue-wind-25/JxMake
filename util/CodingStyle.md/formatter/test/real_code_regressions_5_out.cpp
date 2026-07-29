/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Distilled from martinus/nanobench and blake-madden/tinyexpr-plusplus: a scope's own
// closing `}` used to keep whatever indentation the original source happened to have,
// even when it was wrong (indented to match the body instead of the frame that opened
// it), because no pass ever re-derived that one gap's indentation from depth

int list(int ret)
{
    while(ret == 1) {
        ret = 2;
        ret = combine( TE_PURE, level1(ret) );
    } // while

    return ret;
}

// A `case` label sharing a span with the construct that follows it (case labels don't
// end a span the way `;`/`}` do) used to make the anchor-finding walk land on the case
// label itself instead of the `if`, corrupting the `if`/`else` closing-brace indent with
// text pulled from the wrong line
void generateResult(int n)
{
    switch(n) {

    case 1:
        if(n == 2) {
            for(size_t i = 0; i < 10; ++i) generateResultMeasurement(1, i, 2, 3);
        }
        else {
            throw std::runtime_error("got a section inside result");
        }
        break;

    case 2:
        break;

    } // switch
}

// A comment sitting directly between a block's `}` and a following `else` (or any other
// following construct) must not be disturbed by the closing-brace reindent fix
void withComment(int result)
{
    if(result > 0) {
        result = 1;
    }
    /* Call otherwise */
    else {
        result = 0;
    }
}

// A bare compound block (`{ ... }` with no preceding keyword) has no earlier keyword to
// anchor on -- the `{` itself is the first significant token in its own span.
void bareBlock()
{
    {
        int x = 1;
            x = 2;
    }
}

// Empty named-construct bodies must stay collapsed, not get expanded into `{\n}` by the
// closing-brace reindent fix
struct Empty {};
