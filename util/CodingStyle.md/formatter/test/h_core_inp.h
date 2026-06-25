/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
#ifndef AUDIO_CODEC_H
#define AUDIO_CODEC_H

#include <stdint.h>
#include <stdbool.h>
#include "platform.h"

// Audio codec public API.

#define CODEC_VERSION_MAJOR 2
#define CODEC_VERSION_MINOR 0
#define CODEC_MAX_CHANNELS  8
#define CODEC_SAMPLE_RATE   48000

typedef enum {
    CODEC_FORMAT_PCM_S16,
    CODEC_FORMAT_PCM_S24,
    CODEC_FORMAT_PCM_F32
} CodecFormat;

typedef struct {
    uint32_t   sampleRate;
    uint8_t   channels;
    CodecFormat   format;
    uint16_t   frameSize;
} CodecConfig;

typedef struct Codec Codec;

#ifdef __cplusplus
extern "C" {
#endif

Codec*   codec_create(const CodecConfig* cfg);
void   codec_destroy(Codec* codec);
int   codec_process(Codec* codec, const float* in, float* out, uint32_t frames);
bool   codec_is_valid(const Codec* codec);

const char*   codec_version(void);
const char*   codec_last_error(void);

#ifdef __cplusplus
}
#endif

#endif
