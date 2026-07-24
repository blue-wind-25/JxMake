/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class Mini {

    public String keyFor(@NonNull String id)
    {
        int index = id.lastIndexOf('@');

        return index == -1 ? id : id;
    }

    public void checkItem(Object item)
    {
        switch(item) {
            case String s      -> System.out.println(s);
            case null, default -> throw new IllegalStateException("bad item");
        }
    }

} // class Mini
