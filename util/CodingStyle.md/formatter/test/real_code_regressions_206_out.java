/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class SwitchArrowBracelessIfElse {

    static String js(String s)
    {
        StringBuilder b = new StringBuilder("\"");
        for( int i = 0; i < s.length(); ++i ) {
            char c = s.charAt(i);
            switch(c) {
                case '"' -> b.append("\\\"");
                default -> { if(c < 0x20) b.append( String.format( "\\u%04x", (int) c ) );
                else b.append(c);
                }
            } // switch
        } // for

        return b.append('"').toString();
    }

} // class SwitchArrowBracelessIfElse
