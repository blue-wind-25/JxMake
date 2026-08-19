/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class AssignmentCommentGap
{
    void m()
    {
        if(a) {
            text = 1;
            // this comment sits between two grouped assignment statements
            text = 2;
        }
    }
}
