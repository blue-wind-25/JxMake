// Translated using Claude Sonnet 4.6
package com.pcpp.evaluator;

import com.pcpp.parser.CppTokens;
import com.pcpp.ply.LexToken;

import java.util.*;
import java.util.function.*;

/**
 * Evaluates fully-macro-expanded C preprocessor #if/#elif expressions.
 *
 * Mirrors evaluator.py.  The Python version used PLY's yacc; this translation
 * uses a hand-written Pratt / recursive-descent parser for the same grammar,
 * producing identical results.
 *
 * Skipped token types (CPP_WS, CPP_LINECONT, CPP_COMMENT1, CPP_COMMENT2)
 * are filtered out before parsing, as in the original.
 */
public class Evaluator {

    private static final Set<String> SKIP_TYPES = new HashSet<>( Arrays.asList(
        CppTokens.CPP_WS, CppTokens.CPP_LINECONT,
        CppTokens.CPP_COMMENT1, CppTokens.CPP_COMMENT2 ) );

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Evaluate a list of already-macro-expanded tokens.
     * Mirrors Evaluator.__call__(input, functions, identifiers) in evaluator.py.
     *
     * @param tokens     list of LexToken (whitespace tokens are filtered internally)
     * @param functions  map of function-name → Function&lt;Value, Value&gt;
     * @param identifiers map of identifier-name → long (or Value)
     * @return the result Value
     */
    public Value evaluate( List<LexToken>    tokens,
        Map<String, Function<Value, Value> > functions,
        Map<String, Object>                  identifiers )
    {
        // Filter skipped types (mirrors _EVALUATOR_SKIP_TYPES in evaluator.py)
        List<LexToken> filtered = new ArrayList<>();
        for( LexToken t : tokens )
            if( !SKIP_TYPES.contains( t.type ) ) filtered.add( t );
        Parser p                = new Parser( filtered, functions, identifiers );
        return p.parseExpression( 0 );
    }

    /** Convenience: evaluate a list with empty maps. */
    public Value evaluate( List<LexToken> tokens )
    {
        return evaluate( tokens, Collections.emptyMap(), Collections.emptyMap() );
    }

    // -----------------------------------------------------------------------
    // Pratt parser
    // -----------------------------------------------------------------------
    // Precedence levels matching evaluator.py's precedence tuple (lowest first):
    //  COMMA=1, TERNARY=2, LOR=3, LAND=4, BITOR=5, XOR=6, BITAND=7,
    //  EQ=8, CMP=9, SHIFT=10, ADDITIVE=11, MULTIPLICATIVE=12, UNARY=13

    private static final int PREC_COMMA    = 1;
    private static final int PREC_TERNARY  = 2;
    private static final int PREC_LOR      = 3;
    private static final int PREC_LAND     = 4;
    private static final int PREC_BITOR    = 5;
    private static final int PREC_XOR      = 6;
    private static final int PREC_BITAND   = 7;
    private static final int PREC_EQ       = 8;
    private static final int PREC_CMP      = 9;
    private static final int PREC_SHIFT    = 10;
    private static final int PREC_ADDITIVE = 11;
    private static final int PREC_MUL      = 12;
    private static final int PREC_UNARY    = 13;

    private static class Parser {
        private final List<LexToken>                       toks;
        private int                                        pos;
        private final Map<String, Function<Value, Value> > functions;
        private final Map<String, Object>                  identifiers;

        Parser( List<LexToken>                   toks,
            Map<String, Function<Value, Value> > functions,
            Map<String, Object>                  identifiers )
        {
            this.toks        = toks;
            this.pos         = 0;
            this.functions   = functions;
            this.identifiers = identifiers;
        }

        private LexToken peek()
        {
            if( pos >= toks.size() ) return null;
            return toks.get( pos );
        }

        private LexToken consume()
        {
            return toks.get( pos++ );
        }

        private boolean check( String type )
        {
            LexToken t = peek();
            return t != null && type.equals( t.type );
        }

        private boolean checkValue( String val )
        {
            LexToken t = peek();
            return t != null && val.equals( t.value );
        }

        /** Parse an expression at the given minimum precedence (Pratt). */
        Value parseExpression( int minPrec )
        {
            Value left = parseUnary();
            while( true ) {
                LexToken op   = peek();
                if( op == null ) break;
                int      prec = infixPrec( op );
                if( prec < 0 || prec <= minPrec ) break;

                consume(); // eat the operator

                // Ternary ?:
                if( CppTokens.CPP_QUESTION.equals( op.type ) ) {
                    Value then = parseExpression( 0 );
                    if( !check( CppTokens.CPP_COLON ) ) throw new SyntaxException( "Missing ':' in ternary operator" );
                    consume(); // eat ':'
                    Value els  = parseExpression( PREC_TERNARY - 1 );
                    if( left.exception != null ) { left = left; }
                    else {
                        try {
                            Value   chosen = left.value() != 0 ? then : els;
                            // Output type: if either branch is unsigned, result is unsigned
                            boolean u      = (then.unsigned || els.unsigned);
                            left = chosen.withUnsigned( u );
                        } catch( Exception e ) {
                            left = new Value( 0, e );
                        }
                    }
                    continue;
                }

                // Short-circuit &&
                if( CppTokens.CPP_LOGICALAND.equals( op.type ) ) {
                    // If left is false, don't evaluate right (but still parse it)
                    if( left.exception == null && left.raw() == 0 ) {
                        // Short-circuit: consume the right side but don't evaluate
                        Value right = parseExpression( PREC_LAND );
                        left = new Value( 0L, false );
                    }
                    else {
                        Value right = parseExpression( PREC_LAND );
                        left = left.logicalAnd( right );
                    }
                    continue;
                }

                // Short-circuit ||
                if( CppTokens.CPP_LOGICALOR.equals( op.type ) ) {
                    if( left.exception == null && left.raw() != 0 ) {
                        Value right = parseExpression( PREC_LOR );
                        left = new Value( 1L, false );
                    }
                    else {
                        Value right = parseExpression( PREC_LOR );
                        left = left.logicalOr( right );
                    }
                    continue;
                }

                // Regular binary op (left-associative)
                Value right = parseExpression( prec );
                left = applyBinop( op, left, right );
            }
            return left;
        }

        private Value parseUnary()
        {
            LexToken t = peek();
            if( t == null ) throw new SyntaxException( "Unexpected end of expression" );

            // Unary prefix operators
            if( CppTokens.CPP_PLUS.equals( t.type ) ) {
                consume();
                Value v = parseExpression( PREC_UNARY - 1 );
                return v.pos();
            }
            if( CppTokens.CPP_MINUS.equals( t.type ) ) {
                consume();
                Value v = parseExpression( PREC_UNARY - 1 );
                return v.neg();
            }
            if( CppTokens.CPP_EXCLAMATION.equals( t.type ) ) {
                consume();
                Value v = parseExpression( PREC_UNARY - 1 );
                try {
                    return v.logicalNot();
                } catch( Exception e ) {
                    return new Value( 0, e );
                }
            }
            if( CppTokens.CPP_TILDE.equals( t.type ) ) {
                consume();
                Value v = parseExpression( PREC_UNARY - 1 );
                return v.invert();
            }

            return parsePrimary();
        }

        private Value parsePrimary()
        {
            LexToken t = peek();
            if( t == null ) throw new SyntaxException( "at EOF" );

            // Integer literal
            if( CppTokens.CPP_INTEGER.equals( t.type ) ) {
                consume();
                try {
                    return Value.parse( (String) t.value );
                }
                catch( Exception e ) {
                    return new Value( 0, e );
                }
            }

            // Character literal
            if( CppTokens.CPP_CHAR.equals( t.type ) ) {
                consume();
                try {
                    return Value.parse( (String) t.value );
                }
                catch( Exception e ) {
                    return new Value( 0, e );
                }
            }

            // String literal (rare in #if, but grammar allows it)
            if( CppTokens.CPP_STRING.equals( t.type ) ) {
                consume();
                // Strings in #if are non-standard; return the raw string value wrapped
                return new Value( 0, new SyntaxException( "String literal in #if expression: " + t.value ) );
            }

            // Parenthesised sub-expression
            if( CppTokens.CPP_LPAREN.equals( t.type ) ) {
                consume();
                Value v = parseExpression( 0 );
                if( !check( CppTokens.CPP_RPAREN ) ) throw new SyntaxException( "Missing ')' in expression" );
                consume();
                return v;
            }

            // __has_include(<variant>) – eat the angle-bracketed form
            if( CppTokens.CPP_LESS.equals( t.type ) ) {
                // <expr> form (used inside __has_include)
                consume();
                parseExpression( 0 );
                if( check( CppTokens.CPP_GREATER ) ) consume();
                return new Value( 0, new SyntaxException( "Unexpected '<' in expression" ) );
            }

            // Identifier: function call or identifier lookup
            if( CppTokens.CPP_ID.equals( t.type ) ) {
                String name = (String) t.value;
                consume();
                // Function call?
                if( check( CppTokens.CPP_LPAREN ) ) {
                    consume(); // eat '('
                    Value arg                 = parseExpression( 0 );
                    if( !check( CppTokens.CPP_RPAREN ) ) throw new SyntaxException( "Missing ')' in function call to " + name );
                    consume(); // eat ')'
                    Function<Value, Value> fn = functions.get( name );
                    if( fn == null ) return new Value( 0, new SyntaxException( "Unknown function " + name ) );
                    try {
                        return fn.apply( arg );
                    }
                    catch( Exception e ) {
                        return new Value( 0, e );
                    }
                }
                // Identifier lookup
                Object val = identifiers.get( name );
                if( val == null && !identifiers.containsKey( name ) ) return new Value( 0, new SyntaxException( "Unknown identifier " + name ) );
                if( val instanceof Value ) return (Value) val;
                if( val instanceof Long ) return new Value( (Long) val );
                if( val instanceof Integer ) return new Value( (Integer) val );
                if( val instanceof Number ) return new Value( ( (Number) val).longValue() );
                return new Value( 0, new SyntaxException( "Unknown identifier value type for " + name ) );
            }

            throw new SyntaxException( "around token '" + t.value + "' type " + t.type );
        }

        /** Return the infix precedence of the given operator token, or -1 if not an infix op. */
        private int infixPrec( LexToken t )
        {
            switch( t.type ) {
                case CppTokens.CPP_COMMA:
                    return PREC_COMMA;

                case CppTokens.CPP_QUESTION:
                    return PREC_TERNARY;

                case CppTokens.CPP_LOGICALOR:
                    return PREC_LOR;

                case CppTokens.CPP_LOGICALAND:
                    return PREC_LAND;

                case CppTokens.CPP_BAR:
                    return PREC_BITOR;

                case CppTokens.CPP_HAT:
                    return PREC_XOR;

                case CppTokens.CPP_AMPERSAND:
                    return PREC_BITAND;

                case CppTokens.CPP_EQUALITY:
                case CppTokens.CPP_INEQUALITY:
                    return PREC_EQ;

                case CppTokens.CPP_LESS:
                case CppTokens.CPP_LESSEQUAL:
                case CppTokens.CPP_GREATER:
                case CppTokens.CPP_GREATEREQUAL:
                    return PREC_CMP;

                case CppTokens.CPP_LSHIFT:
                case CppTokens.CPP_RSHIFT:
                    return PREC_SHIFT;

                case CppTokens.CPP_PLUS:
                case CppTokens.CPP_MINUS:
                    return PREC_ADDITIVE;

                case CppTokens.CPP_STAR:
                case CppTokens.CPP_FSLASH:
                case CppTokens.CPP_PERCENT:
                    return PREC_MUL;

                default:
                    return -1;
            } // switch
        }

        private Value applyBinop( LexToken op, Value left, Value right )
        {
            try {
                switch( op.type ) {
                    case CppTokens.CPP_STAR:
                        return left.mul( right );

                    case CppTokens.CPP_FSLASH:
                        return left.div( right );

                    case CppTokens.CPP_PERCENT:
                        return left.mod( right );

                    case CppTokens.CPP_PLUS:
                        return left.add( right );

                    case CppTokens.CPP_MINUS:
                        return left.sub( right );

                    case CppTokens.CPP_LSHIFT:
                        return left.lshift( right );

                    case CppTokens.CPP_RSHIFT:
                        return left.rshift( right );

                    case CppTokens.CPP_LESS:
                        return left.lt( right );

                    case CppTokens.CPP_LESSEQUAL:
                        return left.le( right );

                    case CppTokens.CPP_GREATER:
                        return left.gt( right );

                    case CppTokens.CPP_GREATEREQUAL:
                        return left.ge( right );

                    case CppTokens.CPP_EQUALITY:
                        return left.eq( right );

                    case CppTokens.CPP_INEQUALITY:
                        return left.ne( right );

                    case CppTokens.CPP_AMPERSAND:
                        return left.and( right );

                    case CppTokens.CPP_HAT:
                        return left.xor( right );

                    case CppTokens.CPP_BAR:
                        return left.or( right );

                    case CppTokens.CPP_LOGICALAND:
                        return left.logicalAnd( right );

                    case CppTokens.CPP_LOGICALOR:
                        return left.logicalOr( right );

                    case CppTokens.CPP_COMMA:
                        return left.comma( right );

                    default:
                        return new Value( 0, new SyntaxException( "Unknown operator " + op.value ) );
                } // switch
            } catch( Exception e ) {
                return new Value( 0, e );
            }
        }
    } // class Parser

    // -----------------------------------------------------------------------
    // SyntaxException
    // -----------------------------------------------------------------------

    public static class SyntaxException extends RuntimeException {
        public SyntaxException( String msg ) {
            super(msg);
        }
    }
} // class Evaluator
