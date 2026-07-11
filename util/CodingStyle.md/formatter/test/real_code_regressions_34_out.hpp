/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#pragma once


namespace ns {

  template <class _Token>
  concept unstoppable_token = stoppable_token<_Token> && requires {

      {

      _Token::stop_possible()

  } -> boolean_testable;

  } && ( !_Token::stop_possible() );

  MACRO_PUSH()
  MACRO_IGNORE_GNU("-Warray-bounds")
#if defined(_MSC_VER)
  extern "C" void _mm_pause();
#endif
  MACRO_POP()

} // namespace ns
