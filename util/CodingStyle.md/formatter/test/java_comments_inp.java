package com.example.comments;

// Test file: uncommon // and /* */ comment locations in Java.

public class CommentEdgeCases {

    // Comment between annotations and declaration
    @SuppressWarnings("unchecked")
    // this comment is between annotation and field
    private int annotatedField = 0;

    /* Block comment between annotation and field */
    @Deprecated
    /* another block comment */
    private String deprecatedField = "x";

    // Trailing line comment on a field declaration
    private int alpha = 1; // alpha value
    private int beta  = 2; // beta value
    private int gamma = 3; // gamma value

    // Trailing block comment on a field declaration
    private int delta = 4; /* delta */ private int epsilon = 5; /* epsilon */

    // Comment inside a method signature (between params)
    public void method(
        int a,         // first param
        /* second */ int b,
        int c          // third param
    ) {
        // Comment at top of method body
        int x = a + b; // inline comment on assignment
        /* block comment inline on next line */ int y = b + c;

        // Comment inside if condition context (before brace)
        if /* condition check */ (x > y) {
            System.out.println("greater");
        }

        // Comment between else and brace
        if (x < 0) {
            System.out.println("negative");
        } /* else block */ else {
            System.out.println("non-negative");
        }

        // Comment inside for loop header
        for (int i = 0 /* start */; i < 10 /* end */; ++i /* step */) {
            System.out.println(i);
        }

        // Comment after closing brace of for (not a named construct)
        for (int j = 0; j < 5; j++) {
            System.out.println(j);
        } // end for

        // Block comment on its own line inside a block
        /*
         * Multi-line block comment inside method body.
         * Should be reformatted to standard style.
         */
        int z = x + y;

        // Line comment before return
        // return value is z
        return; // this is a void method so this is wrong but tests the comment
    }

    // Comment between method declarations
    /* ---- separator ---- */

    public void another() {
        // empty
    }

    // Comment inside switch
    public int switchy(int v) {
        switch (v) {
            // comment before case
            case 1: // comment after case label
                return 1;
            /* block comment before default */
            default:
                return 0;
        }
    }

    // Comment inside ternary expression (tricky for tokenizer)
    public int ternaryWithComment(boolean cond) {
        return cond /* is true? */ ? 1 : 0;
    }

    // Comment inside array initializer
    private static final int[] VALUES = {
        1, // one
        2, // two
        /* three */ 3
    };

} // end CommentEdgeCases
