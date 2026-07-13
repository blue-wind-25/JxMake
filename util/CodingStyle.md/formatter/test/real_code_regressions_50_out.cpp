/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#pragma once


namespace detail {

    struct indexed_element_fn;

    template(typename I, typename S, typename O)(
        requires (!sized_sentinel_for<S, I>) ) //
    O uninitialized_copy(I first, S last, O out)
    {
        for(; first != last; ++first, ++out)
            ::new( (void *)std::addressof(*out) ) iter_value_t<O>(*first);

        return out;
    }

    template(typename I, typename S, typename O)(
        requires sized_sentinel_for<S, I>) O uninitialized_copy(I first, S last, O out)
    {
        return std::uninitialized_copy_n( first, (last - first), out );
    }

    template<typename I, typename O>
    O uninitialized_copy(I first, I last, O out)
    {
        return std::uninitialized_copy(first, last, out);
    }

} // namespace detail

namespace ranges {

    template<typename From>
    struct cursor {

        CPP_member
        auto prev() //
            -> CPP_ret(void)(
                requires detail::decrementable_<From>)
        {
            if(done_)
                done_ = false;
            else
                --from_;
        }

    }; // struct cursor

} // namespace ranges
