/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include "mymodule.hpp"

// C++11/14 core formatting test

namespace audio {

////////////////////////////////////////////////////////////////////////////////////
// Constants and types

static constexpr int   MAX_CHANNELS = 8;
static constexpr int   SAMPLE_RATE  = 44100;
static constexpr float DEFAULT_GAIN = 1.0f;

enum class SampleFormat {

    PCM_S16,
    PCM_S24,
    PCM_F32

}; // enum class SampleFormat

struct alignas(16) AudioBuffer {

    float*   data;
    uint32_t frames;
    uint32_t channels;
    uint32_t sampleRate;

}; // struct AudioBuffer

// Class with multiple access specifiers
class Processor {

public:
    Processor() = default;
    explicit Processor(int channels, float gain);
    ~Processor() = default;

    Processor(const Processor&)            = delete;
    Processor& operator=(const Processor&) = delete;
    Processor(Processor&&)                 = default;
    Processor& operator=(Processor&&)      = default;

    void  process    (AudioBuffer& buf);
    float getGain    (                ) const;
    void  setGain    (float gain      );
    int   getChannels(                ) const;
    void  setChannels(int ch          );

protected:
    virtual void onProcess(AudioBuffer& buf) = 0;

private:
    float gain_     = DEFAULT_GAIN;
    int   channels_ = MAX_CHANNELS;
    bool  active_   = false;

}; // class Processor

// Template class
template<typename T, int N>
class RingBuffer {

public:
    void push (const T& val);
    T    pop  (            );
    bool empty(            ) const { return head_ == tail_;          }
    int  size (            ) const { return (tail_ - head_ + N) % N; }

private:
    T   buf_[N];
    int head_ = 0;
    int tail_ = 0;

}; // class RingBuffer

////////////////////////////////////////////////////////////////////////////////////
// Free functions

// Lambda in function
void applyGain(AudioBuffer& buf, float gain)
{
    auto transform = [&buf, gain]() {
        for(uint32_t i = 0; i < buf.frames * buf.channels; ++i) buf.data[i] *= gain;
    };
    transform();
}

// Auto return type
auto makeBuffer(uint32_t frames, uint32_t channels) -> std::unique_ptr<AudioBuffer>
{
    auto buf      = std::make_unique<AudioBuffer>();
    buf->frames   = frames;
    buf->channels = channels;
    buf->data     = new float[frames * channels]();
    return buf;
}

// Function with initializer list
Processor::Processor(int channels, float gain)
    : gain_(gain), channels_(channels), active_(false)
{
}

void Processor::process(AudioBuffer& buf)
{
    if(!active_) return;
    if(buf.channels > 8) {
        return;
    }
    else if(buf.channels > 4) {
        buf.channels = 4;
    }
    else if(buf.channels > 2) {
        // Do nothing
    }
    else {
        // Do nothing
    }
    for(uint32_t i = 0; i < buf.channels; ++i) {
        // Process each channel
        float* ch = buf.data + i * buf.frames;
        for(uint32_t j = 0; j < buf.frames; ++j) ch[j] *= gain_;
    }
    onProcess(buf);
}

// Getters and setters
float Processor::getGain    (          ) const { return gain_;     }
void  Processor::setGain    (float gain)       { gain_ = gain;     }
int   Processor::getChannels(          ) const { return channels_; }
void  Processor::setChannels(int   ch  )       { channels_ = ch;   }

extern "C" {

    void audio_init();
    void audio_shutdown();
    int  audio_process(float* buf, int frames);

} // extern "C"

// Namespace closing
} // namespace audio
