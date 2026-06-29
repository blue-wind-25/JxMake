/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <stdio.h>

// C comment edge cases: // and /* */ in uncommon positions

// Trailing comments on declarations (alignment group)
static int alpha = 1; // Alpha value
static int beta  = 2; // Beta value
static int gamma = 3; // Gamma value

// Block comment between declarations (breaks alignment group)
static int x = 10;

/* Separator */

static int y = 20;

// Comment after struct opening brace
typedef struct {

    /* Fields below */
    int a; // First field
    int b; /* Second field */
    int c; // Third field

} Trio; // struct Trio

// Comment between function params
void multiParam(
    int a, /* First */
    int b, // Second
    int c  /* Third */
)
{
    // Comment at top of function body
    int tmp  = a; /* Save a */
    tmp     += b; // Add b
    tmp     += c; /* Add c */

    // Comment inside if condition
    if /* Check */ (tmp > 0) printf("%d\n", tmp);

    // Comment between else keyword and brace
    if(a < 0) {
        printf("neg\n");
    }
    /* Non-negative */
    else {
        printf("pos\n");
    }

    // Comment inside for header
    for(int i = 0 /* Start */; i < 10 /* Limit */; ++i /* Step */) printf("%d\n", i);

    // Trailing comment on closing brace of for (user-written, not from formatter)
    for(int j = 0; j < 5; ++j) printf("%d\n", j);

    /*
     * Multi-line block comment.
     * Inside a function body.
     * Third line.
     * Fourth line.
     * Fifth line.
     * Sixth line.
     */

    // Comment before return
    return; // Done
}

// Comment inside switch
int switchy(int v)
{
    switch(v) {
        // Before case 1
        case 1: /* Inline on case */ return 1;
        /* Before default */
        default: return 0; // Default case
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
#define MACRO_A 1 // Macro a
#define MACRO_B 2 // Macro b
#define MACRO_C 3 // Macro c
