// Translated using Claude Sonnet 4.6
package com.pcpp.evaluator;

import java.math.BigInteger;

/**
 * A signed or unsigned integer within a preprocessor expression, bounded
 * to within INT_MIN and INT_MAX, or 0 and UINT_MAX. Signed overflow is handled
 * like a two's complement CPU, as GCC and clang do.
 *
 * Mirrors the Value class in evaluator.py.
 *
 * All arithmetic operations return a new Value and propagate unsigned-ness:
 * if either operand is unsigned the result is unsigned.
 */
public final class Value {
    // 64-bit integer range constants
    public static final long        INT_MIN  = Long.MIN_VALUE; // -(1L << 63)
    public static final long        INT_MAX  = Long.MAX_VALUE; //  (1L << 63) - 1
    public static final long        UINT_MIN = 0L;
    // UINT_MAX cannot be represented in a signed long; we use BigInteger helpers
    private static final BigInteger BIG_MASK = BigInteger.ONE.shiftLeft( 64 ).subtract( BigInteger.ONE );

    private final long              raw;       // the integer value
    public final boolean            unsigned;  // whether this is an unsigned value
    public final Exception          exception; // propagated exception (lazy evaluation)

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public Value( long raw, boolean unsigned )
    {
        this.raw       = unsigned ? uclamp( raw ) : sclamp( raw );
        this.unsigned  = unsigned;
        this.exception = null;
    }

    public Value( long raw )
    {
        this(raw, false);
    }

    /** Wrap a boolean (0 or 1, signed). */
    public Value( boolean b )
    {
        this(b ? 1L : 0L, false);
    }

    /** Error-carrying value; all operations on it propagate the exception. */
    public Value( long raw, Exception exception )
    {
        this.raw       = raw;
        this.unsigned  = false;
        this.exception = exception;
    }

    /** Copy with new unsigned flag (used for ternary result type promotion). */
    public Value withUnsigned( boolean u )
    {
        if( exception != null ) return this;
        return new Value( raw, u );
    }

    /**
     * Parse a C integer literal or character constant.
     * Mirrors the string-parsing branch of Value.__new__() in evaluator.py.
     */
    public static Value parse( String input )
    {
        boolean unsigned = false;
        String  s        = input.trim();

        // Character literal: 'c' or L'c'
        if( (s.startsWith( "L'" ) || s.startsWith( "'" ) ) && s.endsWith( "'" ) ) {
            int    start = s.startsWith( "L'" ) ? 2 : 1;
            String inner = s.substring( start, s.length() - 1 );
            if( inner.isEmpty() ) throw new SyntaxException( "Empty character escape sequence" );
            inner = expandEscapes( inner );
            long v       = inner.codePointAt( 0 );
            return new Value( v, false );
        }

        // Hex
        if( s.startsWith( "0x" ) || s.startsWith( "0X" ) ) {
            s = stripSuffixes( s );
            if( s.endsWith( "u" ) || s.endsWith( "U" ) ) unsigned = true;
            s = stripSuffixes( s ); // may still have L after U
            // re-strip
            while( !s.isEmpty() ) {
                char last = s.charAt( s.length() - 1 );
                if( last == 'u' || last == 'U' ) { unsigned = true; s = s.substring( 0, s.length() - 1 ); }
                else if( last == 'l' || last == 'L' ) { s = s.substring( 0, s.length() - 1 ); }
                else {break;}
            }
            long v = Long.parseUnsignedLong( s.substring( 2 ), 16 );
            return new Value( v, unsigned );
        }

        // Octal
        if( s.startsWith( "0" ) && s.length() > 1 ) {
            while( !s.isEmpty() ) {
                char last = s.charAt( s.length() - 1 );
                if( last == 'u' || last == 'U' ) { unsigned = true; s = s.substring( 0, s.length() - 1 ); }
                else if( last == 'l' || last == 'L' ) { s = s.substring( 0, s.length() - 1 ); }
                else {break;}
            }
            long v = Long.parseUnsignedLong( s, 8 );
            return new Value( v, unsigned );
        }

        // Decimal
        while( !s.isEmpty() ) {
            char last = s.charAt( s.length() - 1 );
            if( last == 'u' || last == 'U' ) { unsigned = true; s = s.substring( 0, s.length() - 1 ); }
            else if( last == 'l' || last == 'L' ) { s = s.substring( 0, s.length() - 1 ); }
            else {break;}
        }
        if( s.isEmpty() ) s = "0";
        // Try to parse as BigInteger first to handle out-of-range literals correctly
        BigInteger big = new BigInteger( s );
        long       v   = big.longValue(); // may wrap -- we then clamp
        return new Value( v, unsigned );
    }

    private static String stripSuffixes( String s )
    {
        while( !s.isEmpty() ) {
            char last = s.charAt( s.length() - 1 );
            if( last == 'u' || last == 'U' || last == 'l' || last == 'L' ) {
                s = s.substring( 0, s.length() - 1 );
            }
            else { break;}
        }
        return s;
    }

    // -----------------------------------------------------------------------
    // Clamping helpers (mirrors __sclamp / __uclamp in evaluator.py)
    // -----------------------------------------------------------------------

    private static long sclamp( long v )
    {
        // Two's complement wrap: same as (long)(v & 0xFFFFF...F) then sign-extend
        return v; // Java longs are already 64-bit two's complement -- no extra work needed
    }

    private static long uclamp( long v )
    {
        return v; // Java bit pattern is correct; we just treat it as unsigned semantically
    }

    // -----------------------------------------------------------------------
    // Value access
    // -----------------------------------------------------------------------

    /**
     * Return the raw long value, or throw the propagated exception.
     * Mirrors Value.value() in evaluator.py.
     */
    public long value()
    {
        if( exception != null ) {
            if( exception instanceof RuntimeException ) throw (RuntimeException) exception;
            throw new RuntimeException( exception );
        }
        return raw;
    }

    /** Raw long (without exception check). Used for arithmetic computations. */
    public long raw() { return raw; }

    // -----------------------------------------------------------------------
    // Arithmetic (mirrors the operator overloads in evaluator.py)
    // -----------------------------------------------------------------------

    private Value propagate( Value other )
    {
        if( exception != null ) return this;
        if( other.exception != null ) return other;
        return null; // no exception; caller should continue
    }

    public Value add( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw + other.raw, u );
    }

    public Value sub( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw - other.raw, u );
    }

    public Value mul( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw * other.raw, u );
    }

    public Value div( Value other )
    {
        Value e = propagate( other ); if( e != null ) return e;
        try {
            boolean u = unsigned || other.unsigned;
            if( u ) {
                long result = Long.divideUnsigned( raw, other.raw );
                return new Value( result, true );
            }
            else {
                return new Value( raw / other.raw, false );
            }
        } catch( ArithmeticException ex ) {
            return new Value( 0, ex );
        }
    }

    public Value mod( Value other )
    {
        Value e = propagate( other ); if( e != null ) return e;
        try {
            boolean u = unsigned || other.unsigned;
            if( u ) {
                long result = Long.remainderUnsigned( raw, other.raw );
                return new Value( result, true );
            }
            else {
                return new Value( raw % other.raw, false );
            }
        } catch( ArithmeticException ex ) {
            return new Value( 0, ex );
        }
    }

    public Value neg()
    {
        if( exception != null ) return this;
        return new Value( -raw, unsigned );
    }

    public Value pos()
    {
        if( exception != null ) return this;
        return new Value( raw, unsigned );
    }

    public Value invert()
    {
        if( exception != null ) return this;
        return new Value( ~raw, unsigned );
    }

    public Value and( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw & other.raw, u );
    }

    public Value or( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw | other.raw, u );
    }

    public Value xor( Value other )
    {
        Value   e = propagate( other ); if( e != null ) return e;
        boolean u = unsigned || other.unsigned;
        return new Value( raw ^ other.raw, u );
    }

    public Value lshift( Value other )
    {
        Value e     = propagate( other ); if( e != null ) return e;
        int   shift = (int) (other.raw & 63);
        if( unsigned ) return new Value( raw << shift, true );
        return new Value( raw << shift, false );
    }

    public Value rshift( Value other )
    {
        Value e     = propagate( other ); if( e != null ) return e;
        int   shift = (int) (other.raw & 63);
        if( unsigned ) return new Value( raw >>> shift, true );
        return new Value( raw >> shift, false );
    }

    // Comparison (returns Value(0) or Value(1))
    public Value lt( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = u ? Long.compareUnsigned( raw, other.raw ) < 0 : raw < other.raw;
        return new Value( result ? 1L : 0L, u );
    }

    public Value le( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = u ? Long.compareUnsigned( raw, other.raw ) <= 0 : raw <= other.raw;
        return new Value( result ? 1L : 0L, u );
    }

    public Value gt( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = u ? Long.compareUnsigned( raw, other.raw ) > 0 : raw > other.raw;
        return new Value( result ? 1L : 0L, u );
    }

    public Value ge( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = u ? Long.compareUnsigned( raw, other.raw ) >= 0 : raw >= other.raw;
        return new Value( result ? 1L : 0L, u );
    }

    public Value eq( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = raw == other.raw; // same bit pattern -> equal for both signed and unsigned
        return new Value( result ? 1L : 0L, u );
    }

    public Value ne( Value other )
    {
        Value   e      = propagate( other ); if( e != null ) return e;
        boolean u      = unsigned || other.unsigned;
        boolean result = raw != other.raw;
        return new Value( result ? 1L : 0L, u );
    }

    /** Logical AND: returns Value(1) if both non-zero, else Value(0). Short-circuits. */
    public Value logicalAnd( Value other )
    {
        Value e = propagate( other ); if( e != null ) return e;
        return new Value( (raw != 0 && other.raw != 0) ? 1L : 0L, false );
    }

    /** Logical OR: returns Value(1) if either non-zero. */
    public Value logicalOr( Value other )
    {
        Value e = propagate( other ); if( e != null ) return e;
        return new Value( (raw != 0 || other.raw != 0) ? 1L : 0L, false );
    }

    /** Logical NOT: 0 if non-zero, 1 if zero. */
    public Value logicalNot()
    {
        if( exception != null ) return this;
        return new Value( raw == 0 ? 1L : 0L, false );
    }

    /** Comma operator: returns the right-hand value. */
    public Value comma( Value other )
    {
        Value e = propagate( other ); if( e != null ) return e;
        return other;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    public boolean isTrue() { return raw != 0; }

    @Override
    public String toString()
    {
        if( exception != null ) return "Exception(" + exception + ")";
        if( unsigned ) return "Value(" + Long.toUnsignedString( raw ) + "U)";
        return "Value(" + raw + ")";
    }

    // -----------------------------------------------------------------------
    // Unicode escape expansion (mirrors _expand_escape_sequences_pat in evaluator.py)
    // -----------------------------------------------------------------------

    private static String expandEscapes( String s )
    {
        if( !s.contains( "\\" ) ) return s;
        StringBuilder out = new StringBuilder();
        int           i   = 0;
        while( i < s.length() ) {
            char c = s.charAt( i );
            if( c != '\\' || i + 1 >= s.length() ) {
                out.append( c );
                i++;
                continue;
            }
            char next = s.charAt( i + 1 );
            switch( next ) {
                case 'n':
                    out.append( '\n' ); i     += 2; break;

                case 'r':
                    out.append( '\r' ); i     += 2; break;

                case 't':
                    out.append( '\t' ); i     += 2; break;

                case 'a':
                    out.append( '\u0007' ); i += 2; break;

                case 'b':
                    out.append( '\b' ); i     += 2; break;

                case 'f':
                    out.append( '\f' ); i     += 2; break;

                case 'v':
                    out.append( '\u000B' ); i += 2; break;

                case '\\':
                    out.append( '\\' ); i     += 2; break;

                case '\'':
                    out.append( '\'' ); i     += 2; break;

                case '"':
                    out.append( '"' );  i     += 2; break;

                case 'x': {
                    // \ xHH
                    String hex = s.substring( i + 2, Math.min( i + 4, s.length() ) );
                    out.append( (char) Integer.parseInt( hex, 16 ) );
                    i += 2 + hex.length();
                    break;
                }

                case 'u': {
                    // \ uHHHH
                    String hex = s.substring( i + 2, Math.min( i + 6, s.length() ) );
                    out.append( (char) Integer.parseInt( hex, 16 ) );
                    i += 2 + hex.length();
                    break;
                }

                case 'U': {
                    // \ UHHHHHHHH
                    String hex = s.substring( i + 2, Math.min( i + 10, s.length() ) );
                    int    cp  = Integer.parseUnsignedInt( hex, 16 );
                    out.appendCodePoint( cp );
                    i += 2 + hex.length();
                    break;
                }

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    // Octal \ NNN
                    int    end = i + 1;
                    while( end < s.length() && end < i + 4 && s.charAt( end ) >= '0' && s.charAt( end ) <= '7' ) end++;
                    String oct = s.substring( i + 1, end );
                    out.append( (char) Integer.parseInt( oct, 8 ) );
                    i = end;
                    break;
                }

                default:
                    out.append( '\\' );
                    out.append( next );
                    i += 2;
                    break;
            } // switch
        }
        return out.toString();
    }

    // -----------------------------------------------------------------------
    // SyntaxException (mirrors Python SyntaxError)
    // -----------------------------------------------------------------------

    public static class SyntaxException extends RuntimeException {
        public SyntaxException( String msg ) {
            super(msg);
        }
    }
} // class Value
