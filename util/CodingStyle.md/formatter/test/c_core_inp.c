/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include "mymodule.h"
#include "util.h"

// C11 core formatting test: declarations, functions, structs, enums, macros.

////////////////////////////////////////////////////////////////////////////////////
// Declarations and alignment

#define MAX_ITEMS 64
#define TIMEOUT_MS 500
#define PREFIX "item_"

typedef enum {
    STATE_IDLE,
    STATE_RUNNING,
    STATE_DONE,
    STATE_ERROR
} State;

typedef struct {
    volatile uint8_t   buffer[64];
    uint16_t timeout;
    uint8_t flags : 4;
    uint8_t mode : 2;
    uint8_t reserved : 2;
    char label[32];
} DeviceState;

// Struct with closing comment (should get one -- it is a named construct)
typedef struct Point {
    float x;
    float y;
    float z;
} Point;

// Enum with closing comment
typedef enum Color {
    COLOR_RED,
    COLOR_GREEN,
    COLOR_BLUE
} Color;

////////////////////////////////////////////////////////////////////////////////////
// Functions -- Allman brace style for definitions, K&R for control flow

// Short function (no closing comment needed on the brace)
int add(int a, int b) {
    return a + b;
}

// Longer function
void process(DeviceState* state, uint8_t* data, size_t len) {
    if (state == NULL || data == NULL) {
        return;
    }
    for (size_t i = 0; i < len; ++i) {
        if (data[i] == 0) {
            continue;
        }
        state->buffer[i % 64] = data[i];
    }
    state->flags = 0x0F;
    state->mode  = 0x01;
    state->timeout = TIMEOUT_MS;
}

// Function with void parameter (C style)
void reset(void) {
    // nothing
}

// Pointer declarations
void   transform(uint8_t* const src, uint8_t* dst, size_t n);
const char*   describe(State s);
uint8_t*   alloc_buffer(size_t sz);

// Declaration alignment group
static volatile uint8_t   buf[64];
static uint16_t   timeout_val;
static bool   running;
static int   count;

// Mixed static and non-static
int   alpha;
static int   beta;
int   gamma;
static int   delta;

// Switch
const char* describe(State s) {
    switch (s) {
        case STATE_IDLE:    return "idle";
        case STATE_RUNNING: return "running";
        case STATE_DONE:    return "done";
        case STATE_ERROR:   return "error";
        default:            return "unknown";
    }
}

// Pre-increment enforcement
void count_up(int* arr, int n) {
    for (int i = 0; i < n; i++) {
        arr[i]++;
    }
}

// Do-while
void drain(int* q, int* len) {
    do {
        (*len)--;
    } while (*len > 0);
}

// Extern C block (C file -- not applicable, but test that it doesn't break)

// Ternary
int clamp(int v, int lo, int hi) {
    return v < lo ? lo : v > hi ? hi : v;
}

// Assignment alignment
void assignments(void) {
    int x         = 1;
    long bigNum   = 123456L;
    bool flag     = true;
    double ratio  = 3.14;

    x       = 10;
    bigNum |= 0xFF;
    flag   &= true;
}
