/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-by-operator-priority=on

public class T {
    public static <E extends Enum<E>> boolean isWildcard(Class<E> type) {
        return type != null;
    }

    static boolean isEmpty(Object value) {
        if(value instanceof java.util.Optional) return !( (java.util.Optional<?>) value ).isPresentXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX();
        return false;
    }
}
