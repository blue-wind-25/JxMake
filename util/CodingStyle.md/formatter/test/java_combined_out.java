/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.example.combined;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.Framework;

import com.example.core.Base;
import com.example.util.Helper;

import static java.util.Collections.unmodifiableList;

// Combined Java test: core + Java 17+ constructs in one realistic file.
// Exercises: imports, declarations, getters/setters, closing comments,
// switch expressions, records, sealed classes, text blocks, var, pattern matching.

public sealed class AudioEngine permits AudioEngine.LocalEngine, AudioEngine.RemoteEngine {

    // ── Constants ──────────────────────────────────────────────────────────────────

    private static final int    MAX_CHANNELS   = 8;
    private static final int    DEFAULT_RATE   = 48000;
    private static final String ENGINE_NAME    = "AudioEngine";
    private static final String ENGINE_VERSION = "2.0";

    // ── Types ──────────────────────────────────────────────────────────────────────

    public record ChannelConfig(int channels, int sampleRate, boolean stereo) {}

    public record ProcessResult(boolean success, int framesProcessed, String error) {

        public ProcessResult
        {
            if(framesProcessed < 0) throw new IllegalArgumentException("negative frames");
        }
        public boolean hasError() { return error != null && !error.isEmpty(); }

    } // record ProcessResult

    public enum State {

        IDLE, RUNNING, PAUSED, ERROR

        ;

        public boolean isActive() { return this == RUNNING; }

    } // enum State

    // ── Fields ─────────────────────────────────────────────────────────────────────

    private State         state;
    private ChannelConfig config;
    private int           frameCount;
    private boolean       muted;
    private float         gain;
    private String        label;

    // ── Constructor ────────────────────────────────────────────────────────────────

    protected AudioEngine(ChannelConfig config)
    {
        this.state      = State.IDLE;
        this.config     = config;
        this.frameCount = 0;
        this.muted      = false;
        this.gain       = 1.0f;
        this.label      = ENGINE_NAME;
    }

    // ── Getters / setters ──────────────────────────────────────────────────────────

    public State         getState     (             ) { return state;       }
    public ChannelConfig getConfig    (             ) { return config;      }
    public int           getFrameCount(             ) { return frameCount;  }
    public boolean       isMuted      (             ) { return muted;       }
    public void          setMuted     (boolean muted) { this.muted = muted; }
    public float         getGain      (             ) { return gain;        }
    public void          setGain      (float   gain ) { this.gain = gain;   }
    public String        getLabel     (             ) { return label;       }
    public void          setLabel     (String  label) { this.label = label; }

    // ── Core methods ───────────────────────────────────────────────────────────────

    public ProcessResult process(float[] buffer, int frames) throws IOException
    {
        if(buffer == null || frames <= 0) return new ProcessResult(false, 0, "invalid input");
        if(state != State.RUNNING) return new ProcessResult(false, 0, "not running");
        if(muted) {
            java.util.Arrays.fill(buffer, 0, frames, 0.0f);
            return new ProcessResult(true, frames, null);
        }
        for(int i = 0; i < frames; ++i) buffer[i] *= gain;
        frameCount += frames;

        return new ProcessResult(true, frames, null);
    }

    // State machine -- uses switch expression
    public void transition(String event)
    {
        state = switch(event) {
            case "start" -> State.RUNNING;
            case "pause" -> State.PAUSED;
            case "stop"  -> State.IDLE;
            case "error" -> State.ERROR;
            default      -> state; // No change
        }; // switch
    }

    // Pattern matching
    public String describe(Object obj)
    {
        if(obj instanceof ChannelConfig cc)                return "config: " + cc.channels() + "ch @ " + cc.sampleRate() + "Hz";
        else if(obj instanceof ProcessResult pr)           return pr.success() ? "ok:" + pr.framesProcessed() : "err:" + pr.error();
        else if( obj instanceof String s && !s.isEmpty() ) return "label: " + s;

        return "unknown";
    }

    // Text block usage
    private static final String DEBUG_TEMPLATE = """
            Engine: %s v%s
            State:  %s
            Frames: %d
            """;

    public String debugInfo()
    {
        return DEBUG_TEMPLATE.formatted(ENGINE_NAME, ENGINE_VERSION, state, frameCount);
    }

    // var usage
    public List<String> listChannels()
    {
        var result = new ArrayList<String>();
        var cfg    = config;
        for( var i = 0; i < cfg.channels(); ++i ) {
            var name = "ch" + i;
            result.add(name);
        }

        return result;
    }

    // ── Inner implementations ──────────────────────────────────────────────────────

    public static final class LocalEngine extends AudioEngine {

        private final int bufferSize;

        public LocalEngine(ChannelConfig config, int bufferSize)
        {
            super(config);
            this.bufferSize = bufferSize;
        }

        public int getBufferSize() { return bufferSize; }

    } // class LocalEngine

    public static final class RemoteEngine extends AudioEngine {

        private final String endpoint;
        private final int    port;

        public RemoteEngine(ChannelConfig config, String endpoint, int port)
        {
            super(config);
            this.endpoint = endpoint;
            this.port     = port;
        }

        public String getEndpoint() { return endpoint; }
        public int    getPort    () { return port;     }

    } // class RemoteEngine

    private static void evaluateAt(List<Scored> scored, double threshold)
    {
        int yesCorrect = 0, yesIncorrect = 0, noCorrect = 0, noIncorrect = 0, abstain = 0;
        int total      = scored.size();
        for(Scored s : scored) {
            CommentDecision verdict = s.probabilities == null ? CommentDecision.ABSTAIN : GruClassifier.decide(
                s.probabilities, threshold
            );
            if(verdict == CommentDecision.ABSTAIN) {
                ++abstain;
                continue;
            }
        } // for

        // A `%`-prefixed marker/directive comment such as `<!--% JXM_CFMT_CFG indent-size=2;line-length=80 -->`
        // must survive byte-for-byte -- normal comment

        int aaa   = +1;
        int b     = -1;
        int ccccc = +10;
    }

    private static void evaluateAt2(List<Scored> scored, double threshold)
    {
              int aaa   = +1;
        final int b     = -1;
              int ccccc = +10;

        String s = "";
        s = applyKeywordParenSpacing(s); // §3.5 (also benefits if/while/... examples in §3.1)
        s = applyOperatorSpacing(s);     // §3.2 spacing

        s = applyBraceSpacing(s); // §3.6 (before indent/align so later passes see spaced braces)

        s = applyBraceIndent(s);   // §3.1 (also multi-line hashtable bodies -- §3.4)
        s = applyPipelineSplit(s); // §3.3 (after indent so continuation uses base+1 level)

        s = applyAssignAlignment(s); // §3.2 alignment (also multi-line hashtable entries -- §3.4)

        s = applySwitchArmAlignment(s); // §3.5 arm `{` alignment (after indent)
    }

} // class AudioEngine
