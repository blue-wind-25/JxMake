/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#pragma once

namespace ranges
{
    namespace views
    {
        template<typename ViewFn>
        struct view_closure
        {
#ifndef RANGES_WORKAROUND_CLANG_43400
            // This overload is deleted because when piping a range into a
            // View, it must be moved in
            template<typename Rng, typename ViewFn>         // **************************
            friend constexpr auto                           // **************************
            operator|(Rng &&, view_closure<ViewFn> const &) // ******* READ THIS ********
                                                              // **** IF YOUR COMPILE *****
                -> CPP_broken_friend_ret(Rng)(               // ****** BREAKS HERE *******
                    requires range<Rng> &&                   // **************************
                    (!viewable_range<Rng>) ) = delete;       // **************************
#endif
        };
    } // namespace views
} // namespace ranges
