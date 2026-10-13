/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public enum Mode {
    ALPHA, BETA,
    /** Only produced by the importer, never by the trunk. */
    GAMMA, DELTA;

    /** Maps an index back to its constant. */
    public static Mode fromIndex(int i) {
        return values()[i];
    }

    enum Level { LOW, HIGH }  // two levels only

    public static final java.util.Set<String> KEYS = java.util.Set.of(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            // the second batch lives here
            "alpha_one", "alpha_two", "alpha_three", "alpha_four", "alpha_five", "alpha_six");
}
