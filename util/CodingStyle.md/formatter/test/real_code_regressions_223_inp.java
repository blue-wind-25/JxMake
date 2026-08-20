/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class AnonClassMultiMember
{
    void doIt()
    {
        run(new Comparator() { public int compare(Object a, Object b) {
        return 0;
        }
        public boolean equals(Object o) {
        return false;
        } });
    }

    private void run(Comparator r) {}
    interface Comparator { int compare(Object a, Object b); boolean equals(Object o); }
}
