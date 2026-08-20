/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class AnonClassCallArgument
{
    void doIt()
    {
        run(new Runnable() { public void run() {
        int x = 1;
        int y = 2;
        System.out.println(x);
        } });
    }

    private void run(Runnable r) { r.run(); }
}
