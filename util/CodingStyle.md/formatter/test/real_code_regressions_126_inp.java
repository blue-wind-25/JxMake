/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class Mini {
    public boolean tryHardToDelete(java.io.File f) {
        if (!f.delete()) {
            if (!f.canWrite()) {
                final boolean ignored = f.setWritable(true);
            }
            if (f.exists()) {
                return false;
            }
        }
        return true;
    }
}
