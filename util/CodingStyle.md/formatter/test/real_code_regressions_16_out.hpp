/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace Outer {

    namespace Detail {

        inline std::string getAnnotation(Class cls, SEL sel)
        {
            return "";
        }

    } // namespace Detail

    LazyExpression::LazyExpression(bool isNegated)
    :   m_isNegated( isNegated )
    {}

    LazyExpression::LazyExpression(
        LazyExpression const& other
    ) : m_isNegated( other.m_isNegated ) {}

    TagAlias::TagAlias(
        std::string const& _tag,
        SourceLineInfo     _lineInfo
    ): tag(_tag), lineInfo( _lineInfo ) {}

    void installSignalHandler()
    {
        struct sigaction sa = {};

        sa.sa_handler = handleSignal;
    }

} // namespace Outer
