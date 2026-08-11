/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public final class Repro {
    public static void main(String[] args) {
        while (true) {
            final Lang lang;
            try { lang = new Lang(langName); } catch(IllegalArgumentException e) { continue; }
        }
    }
}
