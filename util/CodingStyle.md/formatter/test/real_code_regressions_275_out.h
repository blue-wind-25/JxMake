/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct ExtractorModel {

    static int legacy_heads(void)       { return 1;                                      }
    int        image_floats(    ) const { return kInC * in_hw * in_hw;                   }
    int        arch_id     (    ) const { return in_hw == kHW64 ? kArchId64 : kArchId96; }

}; // struct ExtractorModel
