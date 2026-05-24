// Translated by Claude Sonnet 4.6
package com.pcpp.pcmd;

import com.pcpp.parser.*;
import com.pcpp.ply.LexToken;
import com.pcpp.preprocessor.FileInclusionTime;
import com.pcpp.preprocessor.Preprocessor;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Command-line C preprocessor entry point.
 * Mirrors pcmd.py's CmdPreprocessor class and main() function.
 *
 * Usage:
 *   pcpp [options] [input files...]
 *
 * Equivalent to: python -m pcpp [options]
 */
public class CmdPreprocessor extends Preprocessor {

    private static final String VERSION = "1.30";

    // Instance state from pcmd.py
    private boolean bypass_ifpassthru;
    private String potential_include_guard;
    private Args args;

    // Parsed arguments
    private static class Args {
        List<String> inputPaths = new ArrayList<>();
        List<String> outputPaths = new ArrayList<>();
        List<String> defines = new ArrayList<>();
        List<String> undefines = new ArrayList<>();
        List<String> nevers = new ArrayList<>();
        List<String> includes = new ArrayList<>();
        boolean passthru_defines = false;
        boolean passthru_unfound_includes = false;
        boolean passthru_undefined_exprs = false;
        boolean passthru_comments = false;
        boolean passthru_magic_macros = false;
        String passthru_includes = null;
        boolean auto_pragma_once_disabled = false;
        String line_directive = "#line";
        boolean debug = false;
        boolean time_report = false;
        String filetimes = null;
        boolean compress = false;
        String assume_input_encoding = null;
        String output_encoding = null;
        boolean write_bom = false;
    }

    /** Creates a single-pair Args copy for the given index. */
    private static Args singlePairArgs(Args base, int idx) {
        Args a = new Args();
        a.inputPaths = Collections.singletonList(base.inputPaths.get(idx));
        a.outputPaths = Collections.singletonList(base.outputPaths.get(idx));
        a.defines = base.defines;
        a.undefines = base.undefines;
        a.nevers = base.nevers;
        a.includes = base.includes;
        a.passthru_defines = base.passthru_defines;
        a.passthru_unfound_includes = base.passthru_unfound_includes;
        a.passthru_undefined_exprs = base.passthru_undefined_exprs;
        a.passthru_comments = base.passthru_comments;
        a.passthru_magic_macros = base.passthru_magic_macros;
        a.passthru_includes = base.passthru_includes;
        a.auto_pragma_once_disabled = base.auto_pragma_once_disabled;
        a.line_directive = base.line_directive;
        a.debug = base.debug;
        a.time_report = base.time_report;
        a.filetimes = base.filetimes;
        a.compress = base.compress;
        a.assume_input_encoding = base.assume_input_encoding;
        a.output_encoding = base.output_encoding;
        a.write_bom = base.write_bom;
        return a;
    }

    /**
     * Expands brace-list arguments of the form {@code {a,b,...}} into separate arguments.
     * This handles the case where the shell did not expand them (e.g. when called from Java).
     */
    private static String[] expandBraces(String[] argv) {
        List<String> expanded = new ArrayList<>();
        for (String arg : argv) {
            if (arg.startsWith("{") && arg.endsWith("}") && arg.indexOf(',') >= 0) {
                String inner = arg.substring(1, arg.length() - 1);
                for (String part : inner.split(",", -1)) {
                    expanded.add(part);
                }
            } else {
                expanded.add(arg);
            }
        }
        return expanded.toArray(new String[0]);
    }

    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }

    public CmdPreprocessor(String[] argv) {
        args = parseArgs(expandBraces(argv));

        // Multi-file mode: equal number of inputs and outputs, both >= 2
        if (args.inputPaths.size() >= 2 && args.outputPaths.size() == args.inputPaths.size()) {
            for (int idx = 0; idx < args.inputPaths.size(); idx++) {
                Args single = singlePairArgs(args, idx);
                CmdPreprocessor pair = new CmdPreprocessor(single);
                if (pair.return_code != 0) this.return_code = pair.return_code;
            }
            return;
        }

        // Validate mismatch
        if (args.outputPaths.size() > 1 && args.outputPaths.size() != args.inputPaths.size()) {
            System.err.println("pcpp: error: number of output files (" + args.outputPaths.size() +
                    ") does not match number of input files (" + args.inputPaths.size() + ")");
            System.exit(1);
        }

        // Single-file mode (original behaviour)
        setupAndProcess();
    }

    /** Private constructor used by multi-file mode for each file pair. */
    private CmdPreprocessor(Args singleArgs) {
        this.args = singleArgs;
        setupAndProcess();
    }

    /** Shared setup and processing logic (single input/output pair). */
    private void setupAndProcess() {
        // Override Preprocessor instance variables (mirrors pcmd.py __init__)
        define("__PCPP_VERSION__ " + VERSION);
        define("__PCPP_ALWAYS_FALSE__ 0");
        define("__PCPP_ALWAYS_TRUE__ 1");

        if (args.debug) {
            try {
                debugout = new PrintWriter(new FileWriter("pcpp_debug.log"));
            } catch (IOException e) {
                System.err.println("Could not open pcpp_debug.log: " + e.getMessage());
            }
        }

        auto_pragma_once_enabled = !args.auto_pragma_once_disabled;

        line_directive = args.line_directive;
        if (line_directive != null && (line_directive.equalsIgnoreCase("nothing") ||
                line_directive.equalsIgnoreCase("none") || line_directive.isEmpty())) {
            line_directive = null;
        }

        if (args.passthru_includes != null) {
            passthru_includes = Pattern.compile(args.passthru_includes);
        }

        compress = args.compress ? 2 : 0;

        if (args.passthru_magic_macros) {
            undef("__DATE__");
            undef("__TIME__");
            expand_linemacro = false;
            expand_filemacro = false;
            expand_countermacro = false;
        }

        if (args.assume_input_encoding != null) {
            assume_encoding = args.assume_input_encoding;
        }

        // My own instance variables
        bypass_ifpassthru = false;
        potential_include_guard = null;

        // Process -D defines
        for (String d : args.defines) {
            String def = d;
            if (!def.contains("=")) def += "=1";
            def = def.replaceFirst("=", " ");
            define(def);
        }

        // Process -U undefines
        for (String d : args.undefines) {
            undef(d);
        }

        // Process -I includes
        for (String d : args.includes) {
            add_path(d);
        }

        String outputPath = args.outputPaths.isEmpty() ? null : args.outputPaths.get(0);

        // Parse and write
        Writer output;
        try {
            if (outputPath != null) {
                if (args.output_encoding != null) {
                    output = new OutputStreamWriter(new FileOutputStream(outputPath), args.output_encoding);
                } else {
                    output = new FileWriter(outputPath);
                }
                if (args.write_bom) {
                    output.write('﻿');
                }
            } else {
                if (args.output_encoding != null) {
                    output = new OutputStreamWriter(System.out, args.output_encoding);
                } else {
                    output = new OutputStreamWriter(System.out);
                }
                if (args.write_bom) output.write('﻿');
            }

            if (args.inputPaths.isEmpty() || args.inputPaths.equals(Collections.singletonList("-"))) {
                // Read from stdin
                try (Reader stdin = new InputStreamReader(System.in)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[8192];
                    int n;
                    while ((n = stdin.read(buf)) != -1) sb.append(buf, 0, n);
                    parse(sb.toString(), "<stdin>");
                }
            } else if (args.inputPaths.size() == 1) {
                // Single input file
                String inputPath = args.inputPaths.get(0);
                try (Reader rdr = args.assume_input_encoding != null ?
                        new InputStreamReader(new FileInputStream(inputPath), args.assume_input_encoding) :
                        new FileReader(inputPath)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[8192];
                    int n;
                    while ((n = rdr.read(buf)) != -1) sb.append(buf, 0, n);
                    parse(sb.toString(), inputPath);
                }
            } else {
                // Multiple input files: generate virtual #include chain
                StringBuilder sb = new StringBuilder();
                for (String p : args.inputPaths) {
                    sb.append("#include \"").append(p).append("\"\n");
                }
                parse(sb.toString(), null);
            }

            write(new PrintWriter(output));

            if (!(output instanceof OutputStreamWriter && ((OutputStreamWriter) output).getEncoding() != null &&
                    System.out.equals(output))) {
                output.close();
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (lastdirective != null) {
                System.err.printf("%nINTERNAL PREPROCESSOR ERROR AT AROUND %s:%d, FATALLY EXITING NOW%n",
                        lastdirective.source, lastdirective.lineno);
            }
            System.exit(-99);
        }

        if (args.time_report) {
            System.out.println("\nTime report:");
            System.out.println("============");
            for (int n = 0; n < include_times.size(); n++) {
                FileInclusionTime it = include_times.get(n);
                if (n == 0) {
                    System.out.printf("top level: %f seconds%n", it.elapsed);
                } else if (it.depth == 1) {
                    System.out.printf("\n %s: %f seconds (%.0f%%)%n",
                            it.included_path, it.elapsed,
                            100.0 * it.elapsed / include_times.get(0).elapsed);
                } else {
                    System.out.printf("%s%s: %f seconds%n",
                            spaces(it.depth), it.included_path, it.elapsed);
                }
            }
        }

        if (args.filetimes != null) {
            try (PrintWriter fw = new PrintWriter(new FileWriter(args.filetimes))) {
                fw.println("\"Total seconds\",\"Self seconds\",\"File size\",\"File path\"");
                Map<String, double[]> filetimesMap = new LinkedHashMap<>();
                List<String> currentfiles = new ArrayList<>();
                for (FileInclusionTime it : include_times) {
                    while (it.depth < currentfiles.size()) currentfiles.remove(currentfiles.size() - 1);
                    if (it.depth > currentfiles.size() - 1) currentfiles.add(it.included_abspath);
                    String path2 = currentfiles.isEmpty() ? null : currentfiles.get(currentfiles.size() - 1);
                    if (path2 != null) {
                        filetimesMap.computeIfAbsent(path2, k -> new double[]{0, 0});
                        filetimesMap.get(path2)[0] += it.elapsed;
                        filetimesMap.get(path2)[1] += it.elapsed;
                        if (it.elapsed > 0 && currentfiles.size() > 1) {
                            String parent = currentfiles.get(currentfiles.size() - 2);
                            if (filetimesMap.containsKey(parent)) {
                                filetimesMap.get(parent)[1] -= it.elapsed;
                            }
                        }
                    }
                }
                List<double[]> sorted = new ArrayList<>();
                for (Map.Entry<String, double[]> entry : filetimesMap.entrySet()) {
                    double[] v = {entry.getValue()[0], entry.getValue()[1], 0};
                    sorted.add(v);
                }
                sorted.sort((a, b) -> Double.compare(b[0], a[0]));
                for (String fp : filetimesMap.keySet()) {
                    double[] v = filetimesMap.get(fp);
                    long sz = new java.io.File(fp).length();
                    fw.printf("%f,%f,%d,\"%s\"%n", v[0], v[1], sz, fp);
                }
            } catch (IOException e) {
                System.err.println("Could not write filetimes: " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Hook overrides (mirrors pcmd.py's CmdPreprocessor methods)
    // -----------------------------------------------------------------------

    @Override
    public String on_include_not_found(boolean is_malformed, boolean is_system_include,
                                       String curdir, String includepath) {
        if (args.passthru_unfound_includes) {
            throw new OutputDirective(Action.IgnoreAndPassThrough);
        }
        return super.on_include_not_found(is_malformed, is_system_include, curdir, includepath);
    }

    @Override
    public Boolean on_unknown_macro_in_defined_expr(LexToken tok) {
        if (!args.undefines.isEmpty() && args.undefines.contains(tok.value)) {
            return Boolean.FALSE;
        }
        if (args.passthru_undefined_exprs) {
            return null; // Pass through as expanded as possible
        }
        return super.on_unknown_macro_in_defined_expr(tok);
    }

    @Override
    public Object on_unknown_macro_in_expr(String ident) {
        if (!args.undefines.isEmpty() && args.undefines.contains(ident)) {
            return super.on_unknown_macro_in_expr(ident);
        }
        if (args.passthru_undefined_exprs) {
            return null; // Pass through as expanded as possible
        }
        return super.on_unknown_macro_in_expr(ident);
    }

    @Override
    public java.util.function.Function<com.pcpp.evaluator.Value, com.pcpp.evaluator.Value>
            on_unknown_macro_function_in_expr(String ident) {
        if (!args.undefines.isEmpty() && args.undefines.contains(ident)) {
            return super.on_unknown_macro_function_in_expr(ident);
        }
        if (args.passthru_undefined_exprs) {
            return null;
        }
        return super.on_unknown_macro_function_in_expr(ident);
    }

    @Override
    public Boolean on_directive_handle(LexToken directive, List<LexToken> toks,
                                       boolean ifpassthru, List<LexToken> precedingtoks) {
        String dval = (String) directive.value;
        if (ifpassthru) {
            if ("if".equals(dval) || "elif".equals(dval) || "else".equals(dval) || "endif".equals(dval)) {
                bypass_ifpassthru = toks.stream().anyMatch(t ->
                        "__PCPP_ALWAYS_FALSE__".equals(t.value) || "__PCPP_ALWAYS_TRUE__".equals(t.value));
            }
            if (!bypass_ifpassthru && ("define".equals(dval) || "undef".equals(dval))) {
                if (!toks.isEmpty() && !toks.get(0).value.equals(potential_include_guard)) {
                    throw new OutputDirective(Action.IgnoreAndPassThrough);
                }
            }
        }
        if (("define".equals(dval) || "undef".equals(dval)) && !args.nevers.isEmpty()) {
            if (!toks.isEmpty() && args.nevers.contains(toks.get(0).value)) {
                throw new OutputDirective(Action.IgnoreAndPassThrough);
            }
        }
        if (args.passthru_defines) {
            super.on_directive_handle(directive, toks, ifpassthru, precedingtoks);
            return null; // Pass through where possible
        }
        return super.on_directive_handle(directive, toks, ifpassthru, precedingtoks);
    }

    @Override
    public Boolean on_directive_unknown(LexToken directive, List<LexToken> toks,
                                        boolean ifpassthru, List<LexToken> precedingtoks) {
        if (ifpassthru) {
            return null; // Pass through
        }
        return super.on_directive_unknown(directive, toks, ifpassthru, precedingtoks);
    }

    @Override
    public void on_potential_include_guard(String macro) {
        potential_include_guard = macro;
        super.on_potential_include_guard(macro);
    }

    @Override
    public Boolean on_comment(LexToken tok) {
        if (args.passthru_comments) {
            return Boolean.TRUE; // Pass through
        }
        return super.on_comment(tok);
    }

    // -----------------------------------------------------------------------
    // Argument parsing (mirrors argparse setup in pcmd.py)
    // -----------------------------------------------------------------------

    private static Args parseArgs(String[] argv) {
        Args a = new Args();
        if (argv.length == 0) {
            printHelp();
            System.exit(0);
        }
        int i = 0;
        while (i < argv.length) {
            String arg = argv[i];
            switch (arg) {
                case "--help": case "-h": printHelp(); System.exit(0); break;
                case "--version": System.out.println("pcpp " + VERSION); System.exit(0); break;
                case "-o":
                    // Accept one or more consecutive non-flag arguments as output paths
                    if (i + 1 < argv.length) {
                        a.outputPaths.add(argv[++i]);
                        while (i + 1 < argv.length && !argv[i + 1].startsWith("-")) {
                            a.outputPaths.add(argv[++i]);
                        }
                    }
                    break;
                case "-D":
                    if (++i < argv.length) a.defines.add(argv[i]);
                    break;
                case "-U":
                    if (++i < argv.length) a.undefines.add(argv[i]);
                    break;
                case "-N":
                    if (++i < argv.length) a.nevers.add(argv[i]);
                    break;
                case "-I":
                    if (++i < argv.length) a.includes.add(argv[i]);
                    break;
                case "--passthru-defines":           a.passthru_defines = true; break;
                case "--passthru-unfound-includes":  a.passthru_unfound_includes = true; break;
                case "--passthru-unknown-exprs":     a.passthru_undefined_exprs = true; break;
                case "--passthru-comments":          a.passthru_comments = true; break;
                case "--passthru-magic-macros":      a.passthru_magic_macros = true; break;
                case "--passthru-includes":
                    if (++i < argv.length) a.passthru_includes = argv[i];
                    break;
                case "--disable-auto-pragma-once":   a.auto_pragma_once_disabled = true; break;
                case "--line-directive":
                    if (++i < argv.length) a.line_directive = argv[i];
                    else a.line_directive = null;
                    break;
                case "--debug":    a.debug = true; break;
                case "--time":     a.time_report = true; break;
                case "--filetimes":
                    if (++i < argv.length) a.filetimes = argv[i];
                    break;
                case "--compress": a.compress = true; break;
                case "--assume-input-encoding":
                    if (++i < argv.length) a.assume_input_encoding = argv[i];
                    break;
                case "--output-encoding":
                    if (++i < argv.length) a.output_encoding = argv[i];
                    break;
                case "--write-bom": a.write_bom = true; break;
                default:
                    if (arg.startsWith("-")) {
                        // Unknown flag – ignore (matches pcpp's known_args behaviour)
                        System.err.println("NOTE: Argument " + arg + " not known, ignoring!");
                    } else {
                        a.inputPaths.add(arg);
                    }
                    break;
            }
            i++;
        }
        return a;
    }

    private static void printHelp() {
        System.out.println(
                "usage: pcpp [-h] [-o [path [path ...]]] [-D macro[=val]] [-U macro] [-N macro] [-I path]\n" +
                "            [--passthru-defines] [--passthru-unfound-includes]\n" +
                "            [--passthru-unknown-exprs] [--passthru-comments]\n" +
                "            [--passthru-magic-macros] [--passthru-includes <regex>]\n" +
                "            [--disable-auto-pragma-once] [--line-directive [form]]\n" +
                "            [--debug] [--time] [--filetimes path] [--compress]\n" +
                "            [--assume-input-encoding <encoding>] [--output-encoding <encoding>]\n" +
                "            [--write-bom] [--version]\n" +
                "            [input ...]\n\n" +
                "A pure universal Java C (pre-)preprocessor implementation.\n\n" +
                "Multi-file mode: supply equal numbers of input and output files.\n" +
                "  Brace expansion is supported: {a,b,c} is expanded to separate arguments."
        );
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static int main(String[] argv) {
        CmdPreprocessor p = new CmdPreprocessor(argv);
        return p.return_code;
    }

    public static void main(String[] args) {
        System.exit(main(args));
    }
}
