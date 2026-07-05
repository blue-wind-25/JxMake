/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */


#pragma once


#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#include "platform.hpp"
#include "types.hpp"

// C++ audio processor header -- pragma once form

namespace audio {

////////////////////////////////////////////////////////////////////////////////////
// Forward declarations

class  Processor;
class  Buffer;
struct Config;

////////////////////////////////////////////////////////////////////////////////////
// Concepts

template<typename T>
concept Processable = requires(T t, Buffer& buf) {

    { t.process(buf) } -> std::same_as<void>;
    t.reset();

}; // concept Processable

////////////////////////////////////////////////////////////////////////////////////
// Types

enum class SampleRate : uint32_t {

    SR_44100 = 44100,
    SR_48000 = 48000,
    SR_96000 = 96000

}; // enum class SampleRate

struct Config {

    SampleRate rate      = SampleRate::SR_48000;
    uint8_t    channels  = 2;
    uint32_t   frameSize = 512;
    bool       loopback  = false;

}; // struct Config

////////////////////////////////////////////////////////////////////////////////////
// Processor interface

class Processor {

public:
    virtual ~Processor() = default;

    virtual void process(Buffer& buf) = 0;
    virtual void reset()              = 0;
    virtual bool isReady() const      = 0;

    std::string getName() const;
    void        setName(std::string name);

    static std::unique_ptr<Processor> create(const Config& cfg);

protected:
    explicit Processor(Config cfg);

private:
    std::string name_;
    Config      cfg_;

}; // class Processor

////////////////////////////////////////////////////////////////////////////////////
// Concrete processors

class GainProcessor : public Processor {

public:
    explicit GainProcessor(Config cfg, float gain = 1.0f);

    void process(Buffer& buf) override;
    void reset() override;
    bool isReady() const override;

    float getGain() const;
    void  setGain(float gain);

    float getDebug  (        ) const          { return dbg_;     }
    void  setDebug  (bool dbg) const          { dbg_ = dbg;      }
    int   getChannel(        ) const          { return channel_; }
    void  setChannel(int  ch )                { channel_ = ch;   }
    int   getMode   (        ) const noexcept { return channel_; }
    void  seMode    (int  ch ) noexcept       { channel_ = ch;   }

    float getDebug2(        ) const { return dbg_; } // Comment A  : 10
    void  setDebug2(bool dbg) const { dbg_ = dbg;  } // Comment BB : 20

private:
    float gain_;
    bool  dbg_;
    int   ch_;

}; // class GainProcessor

class MixerProcessor : public Processor {

public:
    explicit MixerProcessor(Config cfg);

    void process(Buffer& buf) override;
    void reset() override;
    bool isReady() const override;

    void addSource(std::shared_ptr<Processor> src);
    void removeSource(std::shared_ptr<Processor> src);

private:
    std::vector< std::shared_ptr<Processor> > sources_;

}; // class MixerProcessor

} // namespace audio
