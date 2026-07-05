/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <concepts>
#include <compare>
#include "audio.hpp"
#include "platform.hpp"

// Combined C++ test: C++11/14/17/20/23 constructs together

namespace audio {

////////////////////////////////////////////////////////////////////////////////////
// C++20 concepts

template<typename T>
concept AudioProcessor = requires(T t, float* buf, uint32_t frames) {

    { t.process(buf, frames) } -> std::same_as<bool>;
    { t.reset() } -> std::same_as<void>;
    t.gain;

}; // concept AudioProcessor

template<typename T>
concept Configurable = requires(T t) {

    typename T::Config;
    { T::defaultConfig() } -> std::same_as<typename T::Config>;

}; // concept Configurable

////////////////////////////////////////////////////////////////////////////////////
// Types

enum class SampleFormat : uint8_t {

    S16 = 0,
    S24 = 1,
    F32 = 2

}; // enum class SampleFormat

// Structured binding support
struct FrameRange {

    uint32_t start;
    uint32_t end;
    auto operator<=>(const FrameRange&) const = default;

}; // struct FrameRange

////////////////////////////////////////////////////////////////////////////////////
// Main engine class (C++11/14 base + C++20 features)

template<AudioProcessor Impl>
class Engine {

public:
    struct Config {

        uint32_t     sampleRate = 48000;
        uint8_t      channels   = 2;
        SampleFormat format     = SampleFormat::F32;
        uint32_t     frameSize  = 512;
        float        gain       = 1.0f;
        bool         loopback   = false;

    }; // struct Config

    explicit Engine(Config cfg);
    ~Engine();

    Engine(const Engine&)            = delete;
    Engine& operator=(const Engine&) = delete;
    Engine(Engine&&)                 = default;
    Engine& operator=(Engine&&)      = default;

    // Core API
    bool start();
    bool stop();
    bool process(float* buf, uint32_t frames);

    // Getters/setters
    float         getGain() const;
    void          setGain(float g);
    bool          isMuted() const;
    void          setMuted(bool m);
    uint32_t      getFrameCount() const;
    const Config& getConfig() const;

    // C++17 structured bindings helper
    FrameRange getActiveRange() const;

    // Static factory
    static Config defaultConfig();

    // consteval utility
    static consteval uint32_t maxChannels() { return 8; }

private:
    Config   cfg_;
    Impl     impl_;
    float    gain_       = 1.0f;
    bool     muted_      = false;
    uint32_t frameCount_ = 0;
    bool     running_    = false;

}; // class Engine

////////////////////////////////////////////////////////////////////////////////////
// Implementation

template<AudioProcessor Impl>
Engine<Impl>::Engine(Config cfg) : cfg_( std::move(cfg) ), impl_(), gain_(cfg_.gain)
{
}

template<AudioProcessor Impl>
bool Engine<Impl>::process(float* buf, uint32_t frames)
{
    if(!running_ || buf == nullptr || frames == 0) return false;
    if(muted_) {
        std::fill(buf, buf + frames, 0.0f);
    }
    else {
        if( !impl_.process(buf, frames) ) return false;
        for(uint32_t i = 0; i < frames; ++i) buf[i] *= gain_;
    }
    frameCount_ += frames;

    return true;
}

// Getters/setters (one-liners)
template<AudioProcessor Impl> float    Engine<Impl>::getGain      (       ) const { return gain_;       }
template<AudioProcessor Impl> void     Engine<Impl>::setGain      (float g)       { gain_ = g;          }
template<AudioProcessor Impl> bool     Engine<Impl>::isMuted      (       ) const { return muted_;      }
template<AudioProcessor Impl> void     Engine<Impl>::setMuted     (bool  m)       { muted_ = m;         }
template<AudioProcessor Impl> uint32_t Engine<Impl>::getFrameCount(       ) const { return frameCount_; }

template<AudioProcessor Impl>
FrameRange Engine<Impl>::getActiveRange() const
{
    return { 0, frameCount_ };
}

////////////////////////////////////////////////////////////////////////////////////
// C++17 structured bindings usage

void useBindings(const FrameRange& range)
{
    auto     [start, end] = range;
    uint32_t  length      = end - start;
    (void)length;

    // In a declaration group
    uint32_t  offset  = 0;
    auto     [lo, hi] = FrameRange{ offset, offset + 100 };
    bool      active  = lo < hi;
    (void)active;
}

////////////////////////////////////////////////////////////////////////////////////
// C++17 init-statement

void withInitStatement(int raw)
{
    if(auto v = raw * 2; v > 100) printf("big: %d\n", v);
    switch(int code = raw & 3; code) {
        case 0  : break;
        case 1  : break;
        default : break;
    }
}

////////////////////////////////////////////////////////////////////////////////////
// consteval and constinit

consteval int sampleCount(int channels, int frames)
{
    return channels * frames;
}

       constinit float    globalGain       = 1.0f;
static constinit uint32_t globalSampleRate = 48000;

////////////////////////////////////////////////////////////////////////////////////
// Three-way comparison

struct AudioVersion {

    int major;
    int minor;
    int patch;
    auto operator<=>(const AudioVersion&) const = default;

}; // struct AudioVersion

////////////////////////////////////////////////////////////////////////////////////
// Lambda, auto return, extern "C"

auto makeProcessor(float gain) -> std::function<bool(float*, uint32_t)>
{
    return [gain](float* buf, uint32_t frames) mutable {
        for(uint32_t i = 0; i < frames; ++i) buf[i] *= gain;
        return true;
    };
}

extern "C" {

    void audio_c_init();
    void audio_c_shutdown();
    int  audio_c_process(float* buf, int frames, float gain);

} // extern "C"

////////////////////////////////////////////////////////////////////////////////////
// Comment edge cases (combined)

// Trailing comments on declarations
static constinit float gainA = 1.0f;  // Channel A
static constinit float gainB = 0.5f;  // Channel B
static constinit float gainC = 0.25f; // Channel C

// Comment inside requires expression
template<typename T>
concept HasProcess = requires(T t, float* /* Buffer */ buf, uint32_t /* Count */ n) {

    { t.process(buf /* Data */, n /* Len */) } -> std::same_as<bool>;

}; // concept HasProcess

} // namespace audio
