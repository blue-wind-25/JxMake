/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#pragma once


struct __base_scope {

  bool try_join()
  {
    do {
      if( __count(__old_bits) == 0ul ) {
        if( __bits_.compare_exchange_weak(
              __old_bits,
              __new_bits,
              // On success, we need to publish to future callers of try_associate
              // That the scope is closed
              __std::memory_order_acq_rel,
              // On failure, relaxed is fine because we'll loop back and try again
              __std::memory_order_relaxed) ) {
          return true;
        }
      } // if
    } while(true);
  }

}; // struct __base_scope
