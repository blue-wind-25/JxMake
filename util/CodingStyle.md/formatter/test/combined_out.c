/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "audio.h"
#include "platform.h"
#include "util.h"

// Combined C test: declarations, structs, enums, functions, comments, macros

////////////////////////////////////////////////////////////////////////////////////
// Macros

#define AUDIO_VERSION_MAJOR 2
#define AUDIO_VERSION_MINOR 0
#define AUDIO_MAX_CHANNELS  8
#define AUDIO_SAMPLE_RATE   48000
#define AUDIO_FRAME_SIZE    512

////////////////////////////////////////////////////////////////////////////////////
// Types

typedef enum AudioState {

    AUDIO_STATE_IDLE,
    AUDIO_STATE_RUNNING,
    AUDIO_STATE_PAUSED,
    AUDIO_STATE_ERROR

} AudioState; // enum AudioState

typedef enum AudioFormat {

    AUDIO_FORMAT_S16,
    AUDIO_FORMAT_S24,
    AUDIO_FORMAT_F32

} AudioFormat; // enum AudioFormat

typedef struct {
    uint32_t    sampleRate;
    uint8_t     channels;
    AudioFormat format;
    uint16_t    frameSize;
    bool        loopback;
} AudioConfig;

typedef struct {
    volatile uint8_t  buffer[512];
             uint32_t writePos;
             uint32_t readPos;
             uint16_t count;
             bool     overflow;
             char     label[32];
} AudioRingBuf;

typedef struct {
          bool  success;
          int   frames;
    const char* error;
} ProcessResult;

////////////////////////////////////////////////////////////////////////////////////
// Forward declarations

static void audio_apply_gain(float* buf, uint32_t frames, float gain);
static bool audio_validate(const AudioConfig* cfg);

////////////////////////////////////////////////////////////////////////////////////
// Global state

static AudioState  g_state       = AUDIO_STATE_IDLE;
static AudioConfig g_config      = { AUDIO_SAMPLE_RATE, 2, AUDIO_FORMAT_F32, AUDIO_FRAME_SIZE, false };
static float       g_gain        = 1.0f;
static bool        g_muted       = false;
static uint32_t    g_frame_count = 0;

////////////////////////////////////////////////////////////////////////////////////
// Public API

void audio_init(const AudioConfig* cfg)
{
    if(cfg != NULL) g_config = *cfg;
    g_state       = AUDIO_STATE_IDLE;
    g_frame_count = 0;
}

void audio_start(void)
{
    if(g_state == AUDIO_STATE_IDLE || g_state == AUDIO_STATE_PAUSED) g_state = AUDIO_STATE_RUNNING;
}

void audio_stop(void)
{
    g_state = AUDIO_STATE_IDLE;
}

ProcessResult audio_process(float* buf, uint32_t frames)
{
    if(buf == NULL || frames == 0) {
        ProcessResult r = { false, 0, "invalid input" };
        return r;
    }
    if(g_state != AUDIO_STATE_RUNNING) {
        ProcessResult r = { false, 0, "not running" };
        return r;
    }
    if(g_muted) {
        memset( buf, 0, frames * sizeof(float) );
    }
    else {
        audio_apply_gain(buf, frames, g_gain);
    }
    g_frame_count += frames;

    ProcessResult r = { true, (int)frames, NULL };

    return r;
}

// Getters / setters
AudioState audio_get_state      (void      ) { return g_state;       }
float      audio_get_gain       (void      ) { return g_gain;        }
void       audio_set_gain       (float gain) { g_gain = gain;        }
bool       audio_is_muted       (void      ) { return g_muted;       }
void       audio_set_muted      (bool muted) { g_muted = muted;      }
uint32_t   audio_get_frame_count(void      ) { return g_frame_count; }

////////////////////////////////////////////////////////////////////////////////////
// Internal

static void audio_apply_gain(float* buf, uint32_t frames, float gain)
{
    for(uint32_t i = 0; i < frames; ++i) buf[i] *= gain;
}

static bool audio_validate(const AudioConfig* cfg)
{
    if(cfg == NULL) return false;
    if(cfg->channels == 0 || cfg->channels > AUDIO_MAX_CHANNELS) return false;
    switch(cfg->format) {
        case AUDIO_FORMAT_S16 : /* FALL-THROUGH */
        case AUDIO_FORMAT_S24 : /* FALL-THROUGH */
        case AUDIO_FORMAT_F32 : break;
        default               : return false;
    } // switch
    return true;
}

// Comment in uncommon locations test (combined)
void audio_debug(const AudioConfig* cfg /* Config */, char* out /* Output */, size_t len /* Buffer size */)
{
    if /* Sanity check */ (cfg == NULL || out == NULL) return;
    // Format: "ch=%u rate=%u"
    snprintf(out /* Dest */, len /* Max */, "ch=%u rate=%u" /* Fmt */, cfg->channels, cfg->sampleRate);
}
