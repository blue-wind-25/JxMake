/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class M {

    void f()
    {
        Thread t = new Thread(
            null,
            ()-> { AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>() ; int depth = 5 ; StringBuilder sb = new StringBuilder(depth) ; for(int i = 1 ; i <= depth ; ++i) { sb.append('a') ; tree.insert( sb.toString(), i ) ; } assertThat( tree.search( sb.toString() ) ).isEqualTo(depth) ; },
            "name",
            128* 1024
        );
    }

} // class M
