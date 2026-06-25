package com.example.combined;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.core.Base;
import com.example.util.Helper;

import org.example.Framework;

import static java.util.Collections.unmodifiableList;

// Combined Java test: core + Java 17+ constructs in one realistic file.
// Exercises: imports, declarations, getters/setters, closng comments,
// switch expressions, records, sealed classes, text blocks, var, pattern matching.

public sealed class AudioEngine permits AudioEngine.LocalEngine, AudioEngine.RemoteEngine {

    // ── Constants ──────────────────────────────────────────────────────────────────

    private static final int    MAX_CHANNELS   = 8;
    private static final int    DEFAULT_RATE   = 48000;
    private static final String ENGINE_NAME    = "AudioEngine";
    private static final String ENGINE_VERSION = "2.0";

    // ── Types ──────────────────────────────────────────────────────────────────────

    public record ChannelConfig(int channels, int sampleRate, boolean stereo) {}

    public record ProcessResult(boolean success, int framesProcessed, String error)
    {

        public ProcessResult
        {
            if(framesProcessed < 0) throw new IllegalArgumentException("negative frames");
        }

        public boolean hasError() { return error != null && !error.isEmpty(); }

    } // record ProcessResult

    public enum State
    {

        IDLE,
        RUNNING,
        PAUSED,
        ERROR;

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

    public State         getState()                   { return state; }
    public ChannelConfig getConfig()                  { return config; }
    public int           getFrameCount()              { return frameCount; }
    public boolean       isMuted()                    { return muted; }
    public void          setMuted(boolean muted)      { this.muted = muted; }
    public float         getGain()                    { return gain; }
    public void          setGain(float gain)          { this.gain = gain; }
    public String        getLabel()                   { return label; }
    public void          setLabel(String label)       { this.label = label; }

    // ── Core methods ───────────────────────────────────────────────────────────────

    public ProcessResult process(float[] buffer, int frames) throws IOException
    {
        if(buffer == null || frames <= 0) {
            return new ProcessResult(false, 0, "invalid input");
        }
        if(state != State.RUNNING) {
            return new ProcessResult(false, 0, "not running");
        }
        if(muted) {
            java.util.Arrays.fill(buffer, 0, frames, 0.0f);
            return new ProcessResult(true, frames, null);
        }
        for(int i = 0; i < frames; ++i) {
            buffer[i] *= gain;
        }
        frameCount += frames;

        return new ProcessResult(true, frames, null);
    }

    // State machine -- uses switch expression
    public void transition(String event)
    {
        state = switch (event) {
            case "start"  -> State.RUNNING;
            case "pause"  -> State.PAUSED;
            case "stop"   -> State.IDLE;
            case "error"  -> State.ERROR;
            default       -> state; // no change
        };
    }

    // Pattern matching
    public String describe(Object obj)
    {
        if(obj instanceof ChannelConfig cc) {
            return "config: " + cc.channels() + "ch @ " + cc.sampleRate() + "Hz";
        }
        else if(obj instanceof ProcessResult pr) {
            return pr.success() ? "ok:" + pr.framesProcessed() : "err:" + pr.error();
        }
        else if(obj instanceof String s && !s.isEmpty()) {
            return "label: " + s;
        }
        
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
        for (var i = 0; i < cfg.channels(); i++) {
            var name = "ch" + i;
            result.add(name);
        }
        return result;
    }

    // ── Inner implementations ──────────────────────────────────────────────────────

    public static final class LocalEngine extends AudioEngine
    {

        private final int bufferSize;

        public LocalEngine(ChannelConfig config, int bufferSize)
        {
            super(config);
            this.bufferSize = bufferSize;
        }

        public int getBufferSize() { return bufferSize; }

    } // class LocalEngine

    public static final class RemoteEngine extends AudioEngine
    {

        private final String endpoint;
        private final int    port;

        public RemoteEngine(ChannelConfig config, String endpoint, int port)
        {
            super(config);
            this.endpoint = endpoint;
            this.port     = port;
        }

        public String getEndpoint() { return endpoint; }
        public int    getPort()     { return port; }

    } // class RemoteEngine

} // class AudioEngine
