/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class MixedChainAndWrappedTernary
{
    int scan(List<Token> tokens, int i, int depth)
    {
        for(; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isPunct(t, ")") || isPunct(t, "]") ) ++depth;
            else if( isPunct(t, "(") || isPunct(t, "[") ) {
                if(depth == 0) break;
                --depth;
            }
            else if( depth == 0 && isPunct(t, ",") ) break;
            slot.add(t);
        }
        return depth;
    }

    void pick(Member first)
    {
        if(ready) {
            if(enabled) {
                final int firstSig = first.templatePrefixFrom != first.templatePrefixTo
                                   ? first.templatePrefixFrom : first.returnTypeFrom;
                use(firstSig);
            }
        }
    }
}
