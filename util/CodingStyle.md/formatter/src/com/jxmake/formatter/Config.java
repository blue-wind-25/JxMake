/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Config {

    private static final String CONFIG_DIR          = ".config/jxmake-code-formatter";
    private static final String CONFIG_FILE         = "config";
    private static final String STYLE_FMT_FILE_NAME = ".jxmake-code-formatter";
    private static final String ENV_PREFIX          = "JXMAKE_CODE_FORMATTER_";

    private static final String[] ALL_KEYS = {
        "line-length", "indent-size", "indent-style", "server-port",
        "closing-comment-min-lines", "format-macros", "line-endings",
        "normalize-comment-start-case", "normalize-comment-end-period",
        "comment-normalization-classifier",
        "header-guard-rename",
        "java-import-order", "java-import-sort", "java-import-depth",
        "java-import-blank-lines",
        "kotlin-import-order", "kotlin-import-sort", "kotlin-import-depth",
        "kotlin-import-blank-lines",
        "js-import-order", "js-import-sort", "js-import-blank-lines",
        "gru-classifier", "gru-weights-path",
        "curly-general-scope-reindent",
        "curly-general-scope-reindent-multipass",
        "html5-tc-gap-level"
    };

    private static final java.util.Set<String> ALL_KEYS_SET =
            Collections.unmodifiableSet(
                new java.util.LinkedHashSet<String>( Arrays.asList(ALL_KEYS) )
            );

    private static final String[] INDENT_STYLE_CHOICES = { "spaces", "tabs", "auto" };
    private static final String[] LINE_ENDINGS_CHOICES = { "lf", "crlf", "preserve" };

    public static final String DEFAULT_INDENT_STYLE = "spaces";
    public static final int    DEFAULT_INDENT_SIZE  = 4;
    public static final int    DEFAULT_LINE_LENGTH  = 100;

    /**
     * Default {@code gru-weights-path} value for the Step 3 GRU classifier (STATE_AI.md): empty,
     *  meaning "not explicitly configured". {@code GruAbstainResolver} treats an empty value as an
     *  instruction to derive the weights file location from the running program's own directory
     *  (the jar's parent directory when run via {@code -jar}, or the classes directory itself for
     *  a dev/test run against {@code $(CLASS_DIR)}) instead of a fixed path -- see
     *  {@code GruAbstainResolver.WEIGHTS_FILENAME}/{@code resolveWeightsPath}. Harmless if the
     *  derived file is absent, since a missing/unreadable/corrupt weights file is already a
     *  fail-safe ABSTAIN path.
     */
    public static final String DEFAULT_GRU_WEIGHTS_PATH = "";

    private int     lineLength                = DEFAULT_LINE_LENGTH;
    private int     indentSize                = DEFAULT_INDENT_SIZE;
    private String  indentStyle               = DEFAULT_INDENT_STYLE;
    private int     serverPort                = 17173;
    private int     closingCommentMinLines    = 5;
    private boolean formatMacros              = false;
    private String  lineEndings               = "lf";
    private boolean normalizeCommentStartCase = true;
    private boolean normalizeCommentEndPeriod = true;
    // Default on: this gates the rule-based comment-grammar classifier path (which the GRU stage
    // sits behind on ABSTAIN, see gruClassifier below) instead of the purely-deterministic
    // isCommentNoCapitalizeWord/dot-count heuristics. First tried on 2026-07-29 alongside
    // gruClassifier; make test regressed 9 fixtures (c/cpp/java _comments/_core/_combined +
    // real_code_regressions_22/54) because KeywordAmbiguityGate's stage-2 weights, trained on a
    // small hand-authored tools/classifier_weights/ example set with no zero-feature NO examples, defaulted to YES
    // (capitalize) for common zero-signal keyword-led comments (consteval/static/while/var/this/
    // const/explicit/public/switch, etc.) that are actually code references. Fixed 2026-07-30 by
    // adding real regression-derived and hand-authored zero-feature NO examples to
    // tools/classifier_weights/examples_{c,cpp,java,kotlin}.md and re-deriving the weights -- see STATE_AI.md's
    // 2026-07-30 section and tools/classifier_weights/weights.md.
    private boolean      commentNormalizationClassifier = true;
    private boolean      headerGuardRename              = false;
    private List<String> javaImportOrder                = Arrays.asList(
        "java", "com", "org", "other", "local", "static"
    );
    private boolean      javaImportSort                 = true;
    private int          javaImportDepth                = 2;
    private int          javaImportBlankLines           = 1;

    private List<String> kotlinImportOrder      = Arrays.asList(
        "kotlin", "java", "android", "com", "org", "other", "local"
    );
    private boolean      kotlinImportSort       = true;
    private int          kotlinImportDepth      = 2;
    private int          kotlinImportBlankLines = 1;

    private List<String> jsImportOrder      = Arrays.asList("builtin", "third-party", "local");
    private boolean      jsImportSort       = true;
    private int          jsImportBlankLines = 1;

    /**
     * Step 3 GRU classifier gate (STATE_AI.md) -- default off: evaluated against the 62
     *  hand-labeled keyword-ambiguity examples in {@code tools/classifier_weights/examples_*.md} (the genuinely-ambiguous
     *  cases the linear {@code KeywordAmbiguityGate} is tuned for) on 2026-07-30 and found to predict
     *  YES on every single example (0/43 NO correct, 30.6% overall precision) -- worse than the
     *  linear classifier's own 67.7% on the same set. Root cause: {@code sample_default.txt} (the
     *  GRU's training corpus) is auto-labeled *by the linear classifier itself* via
     *  {@code GenerateSampleDefault}, which only keeps its own high-confidence YES/NO decisions, so
     *  the GRU never saw the hard ambiguous-keyword NO cases and learned to default to YES instead.
     *  {@code GruAbstainResolver} reads this to decide whether to even attempt loading a weights
     *  file; when off (or when the file is missing/unreadable), no filesystem access is attempted /
     *  the resolver fails safe to the rule-based result alone.
     */
    private boolean gruClassifier  = true;
    private String  gruWeightsPath = DEFAULT_GRU_WEIGHTS_PATH;

    /**
     * {@code curly-general-scope-reindent} -- gates the isolated GDR (general scope-depth
     *  reindentation) pre-pass, see {@code STATE_CURLY_GDR.md}. Default off: the pre-pass never
     *  runs unless a caller explicitly opts in, so the existing pipeline's output is byte-for-byte
     *  unchanged on the default path.
     */
    private boolean curlyGeneralScopeReindent = false;

    /**
     * {@code curly-general-scope-reindent-multipass} -- see {@code STATE_CURLY_GDR.md}'s "Open
     *  design proposal: bounded multi-pass remediation for RDD_KEY_229" section and
     *  {@code RDD_KEY_233}/{@code RDD_KEY_234}. Only takes effect when
     *  {@code curly-general-scope-reindent} is also on; runs a fixed 4-stage sequence (GDR,
     *  pipeline, GDR, pipeline) instead of the base feature's single pre-pass-then-pipeline order.
     *  Default off. Per {@code RDD_KEY_234}, turning this on while the base flag is off is a
     *  silent no-op, not an error -- lets a project stage this flag ahead of flipping the base one.
     */
    private boolean curlyGeneralScopeReindentMultipass = false;

    /**
     * {@code html5-tc-gap-level} -- gates the "tc gap" job's HTML5 deep tree-construction-gap
     *  fixes (see {@code STATE_HTML5_TCG.md}, {@code RDD_KEY_230}). Cumulative levels 0-4: 0 =
     *  off (default, current behavior unchanged), 1 = implicit {@code <body>} start-tag
     *  insertion, 2 = + foster-parenting tree reshaping, 3 = + misnested {@code <form>}
     *  reconstruction, 4 = + adoption agency algorithm. Only levels 0-1 have landed real effect
     *  so far.
     */
    private int html5TcGapLevel = 0;

    private Config()
    {
    }

    public int lineLength()
    {
        return lineLength;
    }

    public int indentSize()
    {
        return indentSize;
    }

    public String indentStyle()
    {
        return indentStyle;
    }

    public int serverPort()
    {
        return serverPort;
    }

    public int closingCommentMinLines()
    {
        return closingCommentMinLines;
    }

    public boolean isFormatMacros()
    {
        return formatMacros;
    }

    public String lineEndings()
    {
        return lineEndings;
    }

    public boolean isNormalizeCommentStartCase()
    {
        return normalizeCommentStartCase;
    }

    public boolean isNormalizeCommentEndPeriod()
    {
        return normalizeCommentEndPeriod;
    }

    public boolean isCommentNormalizationClassifier()
    {
        return commentNormalizationClassifier;
    }

    public boolean isHeaderGuardRename()
    {
        return headerGuardRename;
    }

    public List<String> javaImportOrder()
    {
        return javaImportOrder;
    }

    public boolean isJavaImportSort()
    {
        return javaImportSort;
    }

    public int javaImportDepth()
    {
        return javaImportDepth;
    }

    public int javaImportBlankLines()
    {
        return javaImportBlankLines;
    }

    public List<String> kotlinImportOrder()
    {
        return kotlinImportOrder;
    }

    public boolean isKotlinImportSort()
    {
        return kotlinImportSort;
    }

    public int kotlinImportDepth()
    {
        return kotlinImportDepth;
    }

    public int kotlinImportBlankLines()
    {
        return kotlinImportBlankLines;
    }

    public List<String> jsImportOrder()
    {
        return jsImportOrder;
    }

    public boolean isJsImportSort()
    {
        return jsImportSort;
    }

    public int jsImportBlankLines()
    {
        return jsImportBlankLines;
    }

    public boolean isGruClassifier()
    {
        return gruClassifier;
    }

    public String gruWeightsPath()
    {
        return gruWeightsPath;
    }

    public boolean isCurlyGeneralScopeReindent()
    {
        return curlyGeneralScopeReindent;
    }

    public boolean isCurlyGeneralScopeReindentMultipass()
    {
        return curlyGeneralScopeReindentMultipass;
    }

    public int html5TcGapLevel()
    {
        return html5TcGapLevel;
    }

    /**
     * Returns {@code true} if {@code key} is one of the recognized config properties (the same
     * set documented in {@code STATE_C_CPP_JAVA.md}'s Config Keys and Defaults table). Used by
     * the server's inline-query-param validation to reject typo'd keys with a 400.
     */
    public static boolean isKnownKey(final String key)
    {
        return ALL_KEYS_SET.contains(key);
    }

    private static final String[] ON_OFF_CHOICES = { "on", "off" };

    /**
     * Group name -> ordered key list, mirroring README.md's {@code ### Config file format}
     * section headings and in-section key order exactly (not alphabetical -- same presentation
     * order a human reads there). Used only to order/group {@link #describeAll}'s output; every
     * key in {@link #ALL_KEYS} must appear in exactly one group here, and vice versa --
     * {@link #describeAll} asserts this exhaustiveness so the two lists can't silently drift.
     * Python 3's `python-import-sort`/`python-import-blank-lines` are documented in README.md but
     * intentionally absent here too, since they are likewise absent from {@link #ALL_KEYS} (a
     * pre-existing gap, not introduced by this grouping -- see STATE_COMMON.md).
     */
    private static final Map<String, String[]> GROUPS = buildGroups();

    private static Map<String, String[]> buildGroups()
    {
        final Map<String, String[]> groups = new LinkedHashMap<String, String[]>();
        groups.put(
            "Structural constants",
            new String[] { "server-port", "line-length", "indent-size", "indent-style" }
        );
        groups.put(
            "Behavior",
            new String[] {
                "line-endings", "normalize-comment-start-case", "normalize-comment-end-period",
                "comment-normalization-classifier", "closing-comment-min-lines",
                "curly-general-scope-reindent", "curly-general-scope-reindent-multipass"
            }
        );
        groups.put( "C/C++", new String[] { "header-guard-rename", "format-macros" } );
        groups.put(
            "Java",
            new String[] {
                "java-import-order", "java-import-sort", "java-import-depth",
                "java-import-blank-lines"
            }
        );
        groups.put(
            "Kotlin",
            new String[] {
                "kotlin-import-order", "kotlin-import-sort", "kotlin-import-depth",
                "kotlin-import-blank-lines"
            }
        );
        groups.put(
            "JS/TS",
            new String[] { "js-import-order", "js-import-sort", "js-import-blank-lines" }
        );
        groups.put( "HTML5", new String[] { "html5-tc-gap-level" } );
        groups.put( "AI-assist (GRU)", new String[] { "gru-classifier", "gru-weights-path" } );

        return groups;
    }

    /**
     * Describes one config property: its group (README.md section heading), key, its default
     * value (as the raw string form used in a config file/env var/query param), and, where the
     * value is restricted to a fixed set ({@code on}/{@code off} booleans or one of
     * {@link #INDENT_STYLE_CHOICES}/{@link #LINE_ENDINGS_CHOICES}), the allowed values --
     * {@code null} for a free-form value (integers, paths, comma-separated import-order lists).
     */
    public static final class ConfigProperty {

        public final String   group;
        public final String   key;
        public final String   defaultValue;
        public final String[] allowedValues;

        ConfigProperty(
            final String group, final String key, final String defaultValue, final String[] allowedValues
        )
        {
            this.group         = group;
            this.key           = key;
            this.defaultValue  = defaultValue;
            this.allowedValues = allowedValues;
        }

    } // class ConfigProperty

    /**
     * Zips {@link #GROUPS} (README.md's own section grouping/order) with each key's default value
     * and (where applicable) allowed-values array into one {@link ConfigProperty} per key, grouped
     * and ordered exactly like README.md's {@code ### Config file format} section. Used by the
     * server's {@code /properties} endpoint to let tooling introspect the config surface without
     * parsing README.md -- {@code Config.java} is already the runtime source of truth.
     */
    public static List<ConfigProperty> describeAll()
    {
        final Config               defaults = new Config();
        final List<ConfigProperty> result   = new ArrayList<ConfigProperty>();
        final java.util.Set<String> seen    = new java.util.LinkedHashSet<String>();
        for( final Map.Entry<String, String[]> groupEntry : GROUPS.entrySet() ) {
            final String   group = groupEntry.getKey();
            for(final String key : groupEntry.getValue()) {
                seen.add(key);
                result.add( describeOne(group, key, defaults) );
            } // for
        } // for
        if( !seen.equals(ALL_KEYS_SET) ) {
            throw new IllegalStateException(
                "Config.GROUPS and Config.ALL_KEYS have drifted apart -- keys in ALL_KEYS but not GROUPS: "
                    + minus(ALL_KEYS_SET, seen) + "; keys in GROUPS but not ALL_KEYS: " + minus(seen, ALL_KEYS_SET)
            );
        } // if

        return result;
    }

    private static java.util.Set<String> minus(
        final java.util.Set<String> a, final java.util.Set<String> b
    )
    {
        final java.util.Set<String> result = new java.util.LinkedHashSet<String>(a);
        result.removeAll(b);

        return result;
    }

    private static ConfigProperty describeOne(final String group, final String key, final Config defaults)
    {
        final String   defaultValue;
        final String[] allowedValues;
        switch(key) {
            case "line-length":
                defaultValue  = String.valueOf(defaults.lineLength);
                allowedValues = null;
                break;
            case "indent-size":
                defaultValue  = String.valueOf(defaults.indentSize);
                allowedValues = null;
                break;
            case "indent-style":
                defaultValue  = defaults.indentStyle;
                allowedValues = INDENT_STYLE_CHOICES;
                break;
            case "server-port":
                defaultValue  = String.valueOf(defaults.serverPort);
                allowedValues = null;
                break;
            case "closing-comment-min-lines":
                defaultValue  = String.valueOf(defaults.closingCommentMinLines);
                allowedValues = null;
                break;
            case "format-macros":
                defaultValue  = defaults.formatMacros ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "line-endings":
                defaultValue  = defaults.lineEndings;
                allowedValues = LINE_ENDINGS_CHOICES;
                break;
            case "normalize-comment-start-case":
                defaultValue  = defaults.normalizeCommentStartCase ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "normalize-comment-end-period":
                defaultValue  = defaults.normalizeCommentEndPeriod ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "comment-normalization-classifier":
                defaultValue  = defaults.commentNormalizationClassifier ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "header-guard-rename":
                defaultValue  = defaults.headerGuardRename ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "java-import-order":
                defaultValue  = String.join(", ", defaults.javaImportOrder);
                allowedValues = null;
                break;
            case "java-import-sort":
                defaultValue  = defaults.javaImportSort ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "java-import-depth":
                defaultValue  = String.valueOf(defaults.javaImportDepth);
                allowedValues = null;
                break;
            case "java-import-blank-lines":
                defaultValue  = String.valueOf(defaults.javaImportBlankLines);
                allowedValues = null;
                break;
            case "kotlin-import-order":
                defaultValue  = String.join(", ", defaults.kotlinImportOrder);
                allowedValues = null;
                break;
            case "kotlin-import-sort":
                defaultValue  = defaults.kotlinImportSort ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "kotlin-import-depth":
                defaultValue  = String.valueOf(defaults.kotlinImportDepth);
                allowedValues = null;
                break;
            case "kotlin-import-blank-lines":
                defaultValue  = String.valueOf(defaults.kotlinImportBlankLines);
                allowedValues = null;
                break;
            case "js-import-order":
                defaultValue  = String.join(", ", defaults.jsImportOrder);
                allowedValues = null;
                break;
            case "js-import-sort":
                defaultValue  = defaults.jsImportSort ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "js-import-blank-lines":
                defaultValue  = String.valueOf(defaults.jsImportBlankLines);
                allowedValues = null;
                break;
            case "gru-classifier":
                defaultValue  = defaults.gruClassifier ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "gru-weights-path":
                defaultValue  = defaults.gruWeightsPath;
                allowedValues = null;
                break;
            case "curly-general-scope-reindent":
                defaultValue  = defaults.curlyGeneralScopeReindent ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "curly-general-scope-reindent-multipass":
                defaultValue  = defaults.curlyGeneralScopeReindentMultipass ? "on" : "off";
                allowedValues = ON_OFF_CHOICES;
                break;
            case "html5-tc-gap-level":
                defaultValue  = String.valueOf(defaults.html5TcGapLevel);
                allowedValues = null;
                break;
            default:
                throw new IllegalStateException("describeAll: no case for key '" + key + "'");
        } // switch

        return new ConfigProperty(group, key, defaultValue, allowedValues);
    }

    /**
     * Returns the calling process's own {@code JXMAKE_CODE_FORMATTER_*} environment-variable
     * overrides (tier 3 of the precedence chain, see README.md's "Configuration" section).
     * Exposed publicly so a CLI client delegating a format request to a long-running server
     * (see {@code Main.delegateToServer}) can forward its own, currently-live env snapshot as
     * inline query-param overrides. Without this, the server would resolve tier 3 purely from
     * its own process environment -- frozen at whenever the server was originally started --
     * which can silently go stale relative to the CLI-invoking user's current shell environment
     * for a persistent, long-running server.
     */
    public static Map<String, String> clientEnvOverrides()
    {
        return collectEnvVars();
    }

    public static Config resolve(final Path targetFile, final Map<String, String> cliOverrides)
    {
        return resolve(targetFile, cliOverrides, null);
    }

    /**
     * Same as {@link #resolve(Path, Map)}, plus an optional {@code inFileOverrides} layer (the
     * {@code JXM_CFMT_CFG} directive, see {@link InFileConfig}) applied with higher priority than
     * everything else, including {@code cliOverrides} -- it is the highest-priority layer, full
     * stop (RDD_KEY_167 and the "In-file Config Support" design notes in STATE_COMMON.md).
     */
    public static Config resolve(
        final Path                targetFile,
        final Map<String, String> cliOverrides,
        final Map<String, String> inFileOverrides
    )
    {
        final Map<String, String> merged = new LinkedHashMap<String, String>();

        final Path globalConfigPath = Paths.get(
            System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE
        );
        merged.putAll( parseConfigFile(globalConfigPath) );

        merged.putAll( collectEnvVars() );

        if(targetFile != null) {
            for( final Map<String, String> layer : collectStyleFmtLayers(
                targetFile
            ) ) merged.putAll(
                layer
            );
        } // if

        if(cliOverrides != null) merged.putAll(cliOverrides);

        if(inFileOverrides != null) merged.putAll(inFileOverrides);

        return fromRawMap(merged);
    }

    private static List<Map<String, String>> collectStyleFmtLayers(final Path targetFile)
    {
        final List<Map<String, String>> layers = new ArrayList<Map<String, String>>();
              Path                      dir    = targetFile.toAbsolutePath().getParent();
        while(dir != null) {
            final Path candidate = dir.resolve(STYLE_FMT_FILE_NAME);
            if( Files.isRegularFile(candidate) ) layers.add( parseConfigFile(candidate) );
            dir = dir.getParent();
        }
        Collections.reverse(layers);

        return layers;
    }

    private static Map<String, String> collectEnvVars()
    {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        for(final String key : ALL_KEYS) {
            final String envName = ENV_PREFIX + key.toUpperCase(Locale.ROOT).replace('-', '_');
            final String value   = System.getenv(envName);
            if(value != null) result.put(key, value);
        }

        return result;
    }

    private static Map<String, String> parseConfigFile(final Path path)
    {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        if( !Files.isRegularFile(path) ) return result;
        final List<String> lines;
        try {
            lines = Files.readAllLines(path);
        }
        catch(final IOException e) {
            System.err.println(
                "jxmake-code-formatter: warning: could not read config file " + path + ": " + e.getMessage()
            );
            return result;
        }
        for(final String rawLine : lines) {
            final String line = rawLine.trim();
            if( line.isEmpty() || line.charAt(0) == '#' ) continue;
            final int eq = line.indexOf('=');
            if(eq < 0) continue;
            final String key   = line.substring(0, eq).trim();
            final String value = line.substring(eq + 1).trim();
            if( !key.isEmpty() ) result.put(key, value);
        } // for

        return result;
    }

    private static Config fromRawMap(final Map<String, String> raw)
    {
        final Config config = new Config();
        config.lineLength                         = parseInt(raw, "line-length", config.lineLength);
        config.indentSize                         = parseInt(raw, "indent-size", config.indentSize);
        config.indentStyle                        = parseChoice(
            raw, "indent-style", config.indentStyle, INDENT_STYLE_CHOICES
        );
        config.serverPort                         = parseInt(raw, "server-port", config.serverPort);
        config.closingCommentMinLines             = parseInt(
            raw, "closing-comment-min-lines", config.closingCommentMinLines
        );
        config.formatMacros                       = parseBoolean(
            raw, "format-macros", config.formatMacros
        );
        config.lineEndings                        = parseChoice(
            raw, "line-endings", config.lineEndings, LINE_ENDINGS_CHOICES
        );
        config.normalizeCommentStartCase          = parseBoolean(
            raw, "normalize-comment-start-case",
            config.normalizeCommentStartCase
        );
        config.normalizeCommentEndPeriod          = parseBoolean(
            raw, "normalize-comment-end-period",
            config.normalizeCommentEndPeriod
        );
        config.commentNormalizationClassifier     = parseBoolean(
            raw, "comment-normalization-classifier",
            config.commentNormalizationClassifier
        );
        config.headerGuardRename                  = parseBoolean(
            raw, "header-guard-rename", config.headerGuardRename
        );
        config.javaImportOrder                    = parseStringList(
            raw, "java-import-order", config.javaImportOrder
        );
        config.javaImportSort                     = parseBoolean(
            raw, "java-import-sort", config.javaImportSort
        );
        config.javaImportDepth                    = parseInt(
            raw, "java-import-depth", config.javaImportDepth
        );
        config.javaImportBlankLines               = parseInt(
            raw, "java-import-blank-lines", config.javaImportBlankLines
        );
        config.kotlinImportOrder                  = parseStringList(
            raw, "kotlin-import-order", config.kotlinImportOrder
        );
        config.kotlinImportSort                   = parseBoolean(
            raw, "kotlin-import-sort", config.kotlinImportSort
        );
        config.kotlinImportDepth                  = parseInt(
            raw, "kotlin-import-depth", config.kotlinImportDepth
        );
        config.kotlinImportBlankLines             = parseInt(
            raw, "kotlin-import-blank-lines", config.kotlinImportBlankLines
        );
        config.jsImportOrder                      = parseStringList(
            raw, "js-import-order", config.jsImportOrder
        );
        config.jsImportSort                       = parseBoolean(
            raw, "js-import-sort", config.jsImportSort
        );
        config.jsImportBlankLines                 = parseInt(
            raw, "js-import-blank-lines", config.jsImportBlankLines
        );
        config.gruClassifier                      = parseBoolean(
            raw, "gru-classifier", config.gruClassifier
        );
        config.gruWeightsPath                     = parseString(
            raw, "gru-weights-path", config.gruWeightsPath
        );
        config.curlyGeneralScopeReindent          = parseBoolean(
            raw, "curly-general-scope-reindent", config.curlyGeneralScopeReindent
        );
        config.curlyGeneralScopeReindentMultipass = parseBoolean(
            raw, "curly-general-scope-reindent-multipass", config.curlyGeneralScopeReindentMultipass
        );
        config.html5TcGapLevel                    = parseInt(
            raw, "html5-tc-gap-level", config.html5TcGapLevel
        );

        return config;
    }

    private static int parseInt(final Map<String, String> raw, final String key, final int fallback)
    {
        final String value = raw.get(key);
        if(value == null) return fallback;
        try {
            return Integer.parseInt( value.trim() );
        }
        catch(final NumberFormatException e) {
            warnInvalid( key, value, String.valueOf(fallback) );
            return fallback;
        }
    }

    private static boolean parseBoolean(
        final Map<String, String> raw,
        final String              key,
        final boolean             fallback
    )
    {
        final String value = raw.get(key);
        if(value == null) return fallback;
        final String trimmed = value.trim();
        if( "on".equals(trimmed) ) return true;
        if( "off".equals(trimmed) ) return false;
        warnInvalid(key, value, fallback ? "on" : "off");

        return fallback;
    }

    /**
     * Plain free-form string value, no choice-list validation -- used for the one config key
     *  (Step 3 GRU's {@code gru-weights-path}) whose value is a filesystem path rather than a
     *  constrained enum-like choice or a string list
     */
    private static String parseString(
        final Map<String, String> raw,
        final String              key,
        final String              fallback
    )
    {
        final String value = raw.get(key);
        if(value == null) return fallback;

        return value.trim();
    }

    private static String parseChoice(
        final Map<String, String> raw,
        final String              key,
        final String              fallback,
        final String[]            choices
    )
    {
        final String value = raw.get(key);
        if(value == null) return fallback;
        final String trimmed = value.trim();
        for(final String choice : choices) {
            if( choice.equals(trimmed) ) return trimmed;
        }
        warnInvalid(key, value, fallback);

        return fallback;
    }

    private static List<String> parseStringList(
        final Map<String, String> raw,
        final String              key,
        final List<String>        fallback
    )
    {
        final String value = raw.get(key);
        if(value == null) return fallback;
        final List<String> result = new ArrayList<String>();
        for( final String part : value.split(",") ) {
            final String trimmed = part.trim();
            if( !trimmed.isEmpty() ) result.add(trimmed);
        }
        if( result.isEmpty() ) {
            warnInvalid( key, value, fallback.toString() );
            return fallback;
        }

        return result;
    }

    private static void warnInvalid(final String key, final String value, final String fallback)
    {
        System.err.println("jxmake-code-formatter: warning: invalid value for '" + key + "': \"" + value
                + "\" -- using default \"" + fallback + "\"");
    }

} // class Config
