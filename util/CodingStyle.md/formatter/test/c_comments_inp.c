/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <stdio.h>

// C comment edge cases: // and /* */ in uncommon positions.

// Trailing comments on declarations (alignment group)
static int alpha = 1;   // alpha value
static int beta  = 2;  // beta value
static int gamma = 3; // gamma value

// Block comment between declarations (breaks alignment group)
static int x = 10;
/* separator */
static int y = 20;

// Comment after struct opening brace
typedef struct {
    /* fields below */
    int a; // first field
    int b; /* second field */
    int c; // third field
} Trio;

// Comment between function params
void multiParam(
    int a,   /* first */
    int b,   // second
    int c    /* third */
) {
    // Comment at top of function body
    int tmp = a; /* save a */
    tmp += b;    // add b
    tmp += c;    /* add c */

    // Comment inside if condition
    if /* check */ (tmp > 0) {
        printf("%d\n", tmp);
    }

    // Comment between else keyword and brace
    if (a < 0) {
        printf("neg\n");
    } /* non-negative */ else {
        printf("pos\n");
    }

    // Comment inside for header
    for (int i = 0 /* start */; i < 10 /* limit */; ++i /* step */) {
        printf("%d\n", i);
    }

    // Trailing comment on closing brace of for (user-written, not from formatter)
    for (int j = 0; j < 5; ++j) {
        printf("%d\n", j);
    } // end for j

    /*
     * Multi-line block comment.
     * Inside a function body.
     * Third line.
     * Fourth line.
     * Fifth line.
     * Sixth line.
     */

    // Comment before return
    return; // done
}

// Comment inside switch
int switchy(int v) {
    switch (v) {
        // before case 1
        case 1: /* inline on case */ return 1;
        /* before default */
        default: return 0; // default case
    }
}

// Divider comments of various widths
// Short divider:
////////////////////
// Already correct:
////////////////////////////////////////////////////////////////////////////////////////////////////
// Triple divider:
////////////////////
////////////////////
////////////////////

// Comment on same line as preprocessor
#define MACRO_A 1 // macro a
#define MACRO_B 2 // macro b
#define MACRO_C 3 // macro c
