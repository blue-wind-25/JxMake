/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace tao::pegtl::internal
{
   template< typename Guard, typename Input, bool = has_eol_rule< Input > >
   struct rematch_input;
}
