/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Ranges {

    companion object {
        private fun fromRange(
            left: Int, right: Int
        ): Ranges = when( min(
            left, 2
        ) to min(
            right, 3
        ) ) {

            0 to 0 -> ZERO
            0 to 1 -> ONE
            0 to 2 -> TWO
            0 to 3 -> THREE
            else   -> OTHER

        } // when min(left, 2) to min(right, 3)
    } // companion object

} // class Ranges
