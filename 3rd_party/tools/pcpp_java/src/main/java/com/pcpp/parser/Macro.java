// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

import java.util.*;

import com.pcpp.ply.LexToken;

/**
 * Holds information about a preprocessor macro definition.
 * Mirrors the Macro class in parser.py.
 *
 *   name      - Macro name
 *   value     - Macro value (list of tokens)
 *   arglist   - List of argument names (null for object-like macros)
 *   variadic  - True for variadic macros
 *   vararg    - Name of the variadic parameter
 *   source    - Source file where defined
 *   lineno    - Line number where defined
 *
 * Patch lists are populated by macro_prescan() in Preprocessor:
 *   patch         - Standard argument patches [(type, argnum, i)]
 *   str_patch     - String conversion patches [(argnum, i)]
 *   var_comma_patch - Variadic comma patches [i]
 */
public class Macro {

    public final String         name;
    public       List<LexToken> value;
    public final List<String>   arglist;
    public final boolean        variadic;
    public       String         vararg;
    public       String         source;
    public       int            lineno;

    // Filled in by macro_prescan()
    public List<Object[]> patch;           // Each entry: {String type, Integer argnum, Integer i}
    public List<int[]>    str_patch;       // Each entry: {argnum, i}
    public List<Integer>  var_comma_patch;

    public Macro(String name, List<LexToken> value)
    {
        this(name, value, null, false);
    }

    public Macro(String name, List<LexToken> value, List<String> arglist, boolean variadic)
    {
        this.name     = name;
        this.value    = value;
        this.arglist  = arglist;
        this.variadic = variadic;
        if( variadic && arglist != null && !arglist.isEmpty() ) this.vararg = arglist.get(
            arglist.size() - 1
        );
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)=%s", name, arglist, value);
    }

} // class Macro
