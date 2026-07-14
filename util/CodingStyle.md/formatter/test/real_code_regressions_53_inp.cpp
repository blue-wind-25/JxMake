/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

template <facade F, class T, class Alloc, class U, class... Args>
constexpr proxy<F> allocate_proxy(const Alloc& alloc,
                                  std::initializer_list<U> il, Args&&... args)
  requires(std::is_constructible_v<T, std::initializer_list<U>&, Args...>)
{
  return details::allocate_proxy_impl<F, T>(alloc, il,
                                            std::forward<Args>(args)...);
}

template <class P, class U, class... Args>
constexpr explicit proxy(
    std::in_place_type_t<P>, std::initializer_list<U> il,
    Args&&... args) noexcept(std::is_nothrow_constructible_v<
                                       P, std::initializer_list<U>&, Args...>)
    requires (details::ptr_traits<P>::applicable && std::is_constructible_v<P, std::initializer_list<U>&, Args...>)
{
  initialize<P>(il, std::forward<Args>(args)...);
}

template <class T, class CharT, class FormatContext>
auto format_impl(const T& self, std::basic_string_view<CharT> spec, FormatContext& fc)
    requires(
#if __cpp_lib_format_ranges >= 202207L
        std::formattable<T, CharT>
#else
        std::is_default_constructible_v<std::formatter<T, CharT>>
#endif // __cpp_lib_format_ranges >= 202207L
    )
{
  return fc.out();
}
