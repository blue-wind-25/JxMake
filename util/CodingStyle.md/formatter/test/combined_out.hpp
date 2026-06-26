/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#pragma once


#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#include <functional>
#include <concepts>

#include "platform.hpp"
#include "types.hpp"

// Combined .hpp test: pragma once, concepts, templates, classes, extern C.

namespace audio {

////////////////////////////////////////////////////////////////////////////////////
// Concepts

template<typename T>
concept Processor = requires(T t, float* buf, uint32_t n) {

    { t.process(buf, n) } -> std::same_as<bool>;
    { t.reset() } -> std::same_as<void>;

}; // concept Processor

template<typename T>
concept Configurable = requires {

    typename T::Config;

}; // concept Configurable

////////////////////////////////////////////////////////////////////////////////////
// Enums and structs

enum class Format : uint8_t {

    S16 = 0,
    F32 = 1

}; // enum class Format

struct EngineConfig {

    uint32_t sampleRate = 48000;
    uint8_t  channels   = 2;
    Format   format     = Format::F32;
    float    gain       = 1.0f;

}; // struct EngineConfig

struct FrameRange {

    uint32_t start;
    uint32_t end;

    auto operator<=>(const FrameRange&) const = default;

}; // struct FrameRange

////////////////////////////////////////////////////////////////////////////////////
// Engine interface

template<Processor Impl>
class EngineBase {

public:
    using ConfigType = EngineConfig;

    explicit EngineBase(EngineConfig cfg);
    virtual ~EngineBase() = default;

    EngineBase(const EngineBase&)            = delete;
    EngineBase& operator=(const EngineBase&) = delete;

    virtual bool process(float* buf, uint32_t frames) = 0;
    virtual void reset()                              = 0;

    float    getGain      (       ) const;
    void     setGain      (float g);
    bool     isMuted      (       ) const;
    void     setMuted     (bool m );
    uint32_t getFrameCount(       ) const;

    FrameRange getActiveRange() const;

    static consteval uint32_t maxChannels() { return 8; }

protected:
    EngineConfig cfg_;
    Impl         impl_;
    float        gain_       = 1.0f;
    bool         muted_      = false;
    uint32_t     frameCount_ = 0;

}; // class EngineBase

////////////////////////////////////////////////////////////////////////////////////
// Concrete engine

template<Processor Impl>
class DefaultEngine : public EngineBase<Impl> {

public:
    explicit DefaultEngine(EngineConfig cfg);

    bool process(float* buf, uint32_t frames) override;
    void reset() override;

private:
    bool running_ = false;

}; // class DefaultEngine

////////////////////////////////////////////////////////////////////////////////////
// Factory

template<Processor Impl>
std::unique_ptr< EngineBase<Impl> > makeEngine(EngineConfig cfg);

////////////////////////////////////////////////////////////////////////////////////
// C interop

extern "C" {
    void audio_global_init();
    void audio_global_shutdown();
} // extern "C"

} // namespace audio
