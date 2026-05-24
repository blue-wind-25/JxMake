// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Holds a non-terminal grammar symbol during parsing.
 *
 * Mirrors PLY's YaccSymbol from yacc.py.
 * Fields:
 *   type      - Grammar symbol type (non-terminal name or token type)
 *   value     - Symbol value (set by grammar action functions)
 *   lineno    - Starting line number
 *   endlineno - Ending line number
 *   lexpos    - Starting lex position
 *   endlexpos - Ending lex position
 */
public class YaccSymbol {
    public String  type;
    public Object  value;
    public int     lineno;
    public int     endlineno;
    public int     lexpos;
    public int     endlexpos;

    /** Whether lineno has been set (Python uses hasattr, we track explicitly). */
    public boolean hasLineno;
    /** Whether lexpos has been set (Python uses hasattr, we track explicitly). */
    public boolean hasLexpos;

    public YaccSymbol() {}

    public YaccSymbol( String type, Object value )
    {
        this.type  = type;
        this.value = value;
    }

    @Override
    public String toString()
    {
        return type != null ? type : "<null>";
    }
} // class YaccSymbol
