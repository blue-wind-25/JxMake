/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#ifndef AUDIO_ENGINE_H
#define AUDIO_ENGINE_H


#include <stdint.h>
#include <stdbool.h>
#include "platform.h"
#include "types.h"

// Combined .h test: header guard, extern C, declarations, structs, macros.

#define ENGINE_VERSION_MAJOR 2
#define ENGINE_VERSION_MINOR 1
#define ENGINE_MAX_CH        8
#define ENGINE_RATE          48000
#define ENGINE_FRAMES        512

typedef enum EngineState {

    ENGINE_STATE_IDLE,
    ENGINE_STATE_RUNNING,
    ENGINE_STATE_ERROR

} EngineState; // enum EngineState

typedef struct {
    uint32_t sampleRate;
    uint8_t  channels;
    uint16_t frameSize;
    float    gain;
    bool     loopback;
} EngineConfig;

typedef struct Engine Engine;

#ifdef __cplusplus
extern "C" {
#endif

Engine* engine_create(const EngineConfig* cfg);
void    engine_destroy(Engine* eng);
bool    engine_start(Engine* eng);
bool    engine_stop(Engine* eng);
bool    engine_process(Engine* eng, float* buf, uint32_t frames);

EngineState engine_get_state(const Engine* eng);
float       engine_get_gain(const Engine* eng);
void        engine_set_gain(Engine* eng, float gain);
bool        engine_is_muted(const Engine* eng);
void        engine_set_muted(Engine* eng, bool muted);
uint32_t    engine_get_frames(const Engine* eng);

const char* engine_version(void);

#ifdef __cplusplus
} // extern "C"
#endif


#endif // AUDIO_ENGINE_H
