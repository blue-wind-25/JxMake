/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jxm.*;
import jxm.gcomp.*;
import jxm.xb.*;


/*
 * The Unicode(R) Standard Version 17.0 - Core Specification
 * https://www.unicode.org/versions/Unicode17.0.0/core-spec
 * https://www.unicode.org/versions/Unicode17.0.0/UnicodeStandard-17.0.pdf
 *
 * Unicode(R) Standard Annex #11 - East Asian Width
 * https://www.unicode.org/reports/tr11
 *
 * Unicode(R) Standard Annex #15 - Unicode Normalization Forms
 * https://www.unicode.org/reports/tr15
 *
 * Unicode(R) Standard Annex #24 - Unicode Script Property
 * https://www.unicode.org/reports/tr24
 *
 * Unicode(R) Standard Annex #29 - Unicode Text Segmentation
 * https://www.unicode.org/reports/tr29
 *
 * Unicode(R) Standard Annex #44 - Unicode Character Database
 * https://www.unicode.org/reports/tr44
 *
 * Unicode(R) Standard Annex #51 - Unicode Emoji
 * https://www.unicode.org/reports/tr51
 *
 * Unicode Character Database
 * https://www.unicode.org/Public/UCD/latest/ucd
 * https://www.unicode.org/Public/UCD/latest/ucd/Blocks.txt
 * https://www.unicode.org/Public/UCD/latest/ucd/DerivedNormalizationProps.txt
 * https://www.unicode.org/Public/UCD/latest/ucd/ScriptExtensions.txt
 * https://www.unicode.org/Public/UCD/latest/ucd/Scripts.txt
 * https://www.unicode.org/Public/UCD/latest/ucd/UnicodeData.txt
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Hangul Syllable Composition and Decomposition (Section 3.12, Conjoining Jamo Behavior)
 * https://www.unicode.org/versions/Unicode17.0.0/core-spec/chapter-3/#G24646
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * The code in this class was created with the help of Microsoft(R) Copilot,  especially in sections
 * where numerical constants are used extensively.
 */
public class UnicodeUtil {

    public static enum CharType {
        BMP_BASE,            // BMP (Basic Multilingual Plane) character   : non-surrogate, non-combining
        HIGH_SURROGATE,      // Leading surrogate                          : first  half of a UTF-16 pair
        LOW_SURROGATE,       // Trailing surrogate                         : second half of a UTF-16 pair
        COMBINING_MARK,      // Any combining mark                         : diacritic, vowel sign, tone mark, emoji modifier, etc.
        ZWJ,                 // Zero Width Joiner                          : used to form ligatures and emoji sequences
        REGIONAL_INDICATOR,  // Regional indicator symbol                  : used in pairs to form flag emoji
        OTHER                // Fallback/unknown or unclassified character
    }

    public static CharType charType(final char ch)
    {
        /*
         * NOTE : This should be correct per Unicode 17.0 ranges and combining mark classification (simplified).
         *
         * "simplified" means:
         *      # Only BMP ranges are checked (non-BMP combining marks like Cyrillic Extended-D, Glagolitic,
         *        Indic Siyaq are omitted).
         *      # Regional Indicator detection is simplified to a high-surrogate check (U+D83C) without
         *        validating the low surrogate.
         *      # BMP_BASE lumps together controls, private use, and unassigned code points.
         *      # Practical coverage for everyday text streams, but not exhaustive spec-level classification.
         */

        // Surrogate ranges (immutable forever)
        if(ch >= '\uD800' && ch <= '\uDBFF') return CharType.HIGH_SURROGATE;
        if(ch >= '\uDC00' && ch <= '\uDFFF') return CharType.LOW_SURROGATE;

        // Combining mark ranges (stable, extendable if Unicode adds new ones)
        if(
            (ch >=  '\u064B' && ch <=  '\u065F') || // Arabic vowel signs
            (ch ==  '\u0670'                   ) || // Arabic superscript alef
            (ch >=  '\u06D6' && ch <=  '\u06ED') || // Arabic small high marks
            (ch >=  '\u0300' && ch <=  '\u036F') || // Combining Diacritical Marks (Latin, Greek)
            (ch >=  '\u1AB0' && ch <=  '\u1AFF') || // Combining Diacritical Marks Extended
            (ch >=  '\u1DC0' && ch <=  '\u1DFF') || // Combining Diacritical Marks Supplement
            (ch >=  '\u0591' && ch <=  '\u05C7') || // Hebrew vowel/cantillation marks
            (ch >=  '\u2DE0' && ch <=  '\u2DFF') || // Cyrillic combining marks
            (ch >=  '\uA670' && ch <=  '\uA67F') || // Combining Cyrillic marks
            (ch >=  '\u20D0' && ch <=  '\u20FF') || // Combining Marks for Symbols
            (ch >=  '\uFE20' && ch <=  '\uFE2F')    // Combining Half Marks
            /*
            // NOTE : Handled by HIGH_SURROGATE and LOW_SURROGATE
            (ch >= '\u1E030' && ch <= '\u1E08F') || // Cyrillic Extended-D combining marks
            (ch >= '\u1E000' && ch <= '\u1E02F') || // Glagolitic Combining Marks
            (ch >= '\u1CF00' && ch <= '\u1CFCF')    // Indic Siyaq Numbers marks
            */
        ) {
            return CharType.COMBINING_MARK;
        }

        // Zero Width Joiner
        if(ch == '\u200D') return CharType.ZWJ;

        /* Regional Indicator symbols (U+1F1E6 to U+1F1FF) are outside BMP.
         * In UTF-16 they appear as surrogate pairs starting with U+D83C.
         * Detecting the high surrogate lets us handle flag emoji correctly.
         */
        if(ch == '\uD83C') return CharType.REGIONAL_INDICATOR;

        // BMP base characters
        if(ch <= '\uD7FF') return CharType.BMP_BASE;

        // Fallback
        return CharType.OTHER;
    }

    public static int stepSize(final CharType type)
    {
        switch(type) {
            case BMP_BASE           : return 1; // Normal
            case HIGH_SURROGATE     : return 2; // Surrogate pair start
            case LOW_SURROGATE      : return 0; // Consumed with high surrogate
            case COMBINING_MARK     : return 0; // Attaches to base
            case ZWJ                : return 0; // Joins with previous grapheme
            case REGIONAL_INDICATOR : return 2; // Flags are pairs
            default                 : return 1; // Fallback/unknown
        }
    }

    public static int stepSize(final char ch)
    { return stepSize( charType(ch) ); }

    public static boolean isBoundaryCharType(final CharType type)
    {
        return (    type == UnicodeUtil.CharType.BMP_BASE
                 || type == UnicodeUtil.CharType.HIGH_SURROGATE
                 || type == UnicodeUtil.CharType.REGIONAL_INDICATOR );
    }

    public static boolean isBoundaryCharType(final char ch)
    { return isBoundaryCharType( charType(ch) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final char REPLACEMENT_CHAR = '\uFFFD'; // "�" black diamond with question mark

    public static int toCodePoint(final char high, final char low)
    {
        if(high < 0xD800 || high > 0xDBFF) return REPLACEMENT_CHAR;
        if(low  < 0xDC00 || low  > 0xDFFF) return REPLACEMENT_CHAR;

        return ( (high - 0xD800) << 10 ) + (low - 0xDC00) + 0x10000;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int graphemeCount(final String text, final int len_)
    {
        if( text == null || text.isEmpty() ) return 0;

        final int len   = (len_ >= 0) ? len_ : text.length();
              int count = 0;
              int i     = 0;

        while(i < len) {
            final char ch   = text.charAt(i);
            final int  step = stepSize( charType(ch) );

            if(step > 0) {
                // Start of a new grapheme cluster
                ++count;
                i += step;
            }
            else {
                // Combining mark, etc.
                ++i;
            }
        }

        return count;
    }

    public static int graphemeCount(final String text)
    { return graphemeCount(text, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean isDoubleWidth(final int codePoint)
    {
        /*
         * NOTE : This should be correct per Unicode 17.0 ranges (simplified plus Arabic).
         *
         * "simplified" means:
         *      # CJK ranges are collapsed (Extension A + Basic, Extensions B-J grouped).
         *      # Kana ranges are grouped (Hiragana, Katakana, Supplement, Extended-A).
         *      # Hangul ranges are grouped (Jamo, Compatibility Jamo, Extended-A/B, Syllables).
         *      # Emoji ranges are approximated for Unicode 17.0.
         *
         * "plus Arabic" means:
         *      # Arabic script blocks (core, supplements, extended A/B/C, presentation forms)
         *        are explicitly treated as double-width.
         *      # This is a deliberate override, since Unicode East Asian Width does not classify
         *        Arabic as wide.
         *      # Ensures console alignment matches real-world rendering in many fonts.
         */

        // CJK Unified Ideographs (Basic + Extensions)
            /*
            (codePoint >= 0x03400 && codePoint <= 0x04DBF) || // Extension A
            (codePoint >= 0x04E00 && codePoint <= 0x09FFF) || // Basic
            (codePoint >= 0x20000 && codePoint <= 0x2FA1F) || // Extensions B → Extensions H
            (codePoint >= 0x2FA20 && codePoint <= 0x3FFFD)    // Extensions I + Extensions J (future-proof)
            */
        if( (codePoint >= 0x03400 && codePoint <= 0x09FFF) || // Extension  A + Basic
            (codePoint >= 0x20000 && codePoint <= 0x3FFFD)    // Extensions B → Extensions J
        ) return true;

        // Hiragana + Katakana
            /*
            (codePoint >= 0x1B000 && codePoint <= 0x1B0FF) || // Kana Supplement
            (codePoint >= 0x1B100 && codePoint <= 0x1B12F)    // Kana Extended-A
            */
        if( (codePoint >= 0x03040 && codePoint <= 0x030FF) || // Hiragana + Katakana
            (codePoint >= 0x031F0 && codePoint <= 0x031FF) || // Katakana Phonetic Extensions
            (codePoint >= 0x1B000 && codePoint <= 0x1B12F)    // Kana Supplement + Extended-A
        ) return true;

        // Hangul
            /*
            (codePoint >= 0xAC00 && codePoint <= 0xD7AF) || // Hangul Syllables
            (codePoint >= 0xD7B0 && codePoint <= 0xD7FF)    // Hangul Jamo Extended-B
            */
        if( (codePoint >= 0x1100 && codePoint <= 0x11FF) || // Hangul Jamo
            (codePoint >= 0x3130 && codePoint <= 0x318F) || // Hangul Compatibility Jamo
            (codePoint >= 0xA960 && codePoint <= 0xA97F) || // Hangul Jamo Extended-A
            (codePoint >= 0xAC00 && codePoint <= 0xD7FF)    // Hangul Syllables + Jamo Extended-B
        ) return true;

        // Arabic
        if( (codePoint >= 0x00600 && codePoint <= 0x006FF) || // Arabic
            (codePoint >= 0x00750 && codePoint <= 0x0077F) || // Arabic Supplement
            /*
            (codePoint >= 0x00870 && codePoint <= 0x0089F) || // Arabic Extended-B
            (codePoint >= 0x008A0 && codePoint <= 0x008FF) || // Arabic Extended-A
            */
            (codePoint >= 0x00870 && codePoint <= 0x008FF) || // Arabic Extended-B + Extended-A
            (codePoint >= 0x10EC0 && codePoint <= 0x10EFF) || // Arabic Extended-C
            (codePoint >= 0x0FB50 && codePoint <= 0x0FDFF) || // Arabic Presentation Forms-A
            (codePoint >= 0x0FE70 && codePoint <= 0x0FEFF)    // Arabic Presentation Forms-B
        ) return true;

        // Fullwidth + Halfwidth forms
        if( (codePoint >= 0xFF01 && codePoint <= 0xFF60) || // Fullwidth punctuation
            (codePoint >= 0xFF61 && codePoint <= 0xFF64) || // Halfwidth punctuation
            (codePoint >= 0xFF65 && codePoint <= 0xFF9F) || // Halfwidth Katakana
            (codePoint >= 0xFFE0 && codePoint <= 0xFFE6)    // Fullwidth currency signs
        ) return true;

        // Emoji & symbols (Unicode 17.0 approximate)
        if( (codePoint >= 0x1F000 && codePoint <= 0x1FAFF) || // Misc Symbols + Emoji
            (codePoint >= 0x1FB00 && codePoint <= 0x1FBFF) || // Symbols for Legacy Computing
            (codePoint >= 0x1FC00 && codePoint <= 0x1FCFF)    // Additional emoji ranges
        ) return true;

        // It is single width
        return false;
    }

    public static boolean isDoubleWidth(final char high, final char low)
    { return isDoubleWidth( toCodePoint(high, low) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean isCTL(final int codePoint)
    {
        /*
         * NOTE : This should be correct per Unicode 17.0 ranges and CTL classification (simplified).
         *
         * "simplified" means:
         *      # Major CTL scripts (Arabic, Indic, Thai, Lao, Myanmar, Khmer, Hebrew, Tibetan) are included.
         *      # Ranges are collapsed for readability (e.g., Devanagari → Sinhala grouped as U+0900 to U+0DFF).
         *      # Some rarer scripts and non-BMP CTL ranges are omitted.
         *      # Combining marks (Mn, Mc, Me) are included, but vowel sign ranges are broadened for simplicity.
         *      # Practical for most real-world text, but not exhaustive for every historical or niche script.
         */

        // Arabic
        if( (codePoint >= 0x00600 && codePoint <= 0x006FF) || // Arabic
            (codePoint >= 0x00750 && codePoint <= 0x0077F) || // Arabic Supplement
            /*
            (codePoint >= 0x00870 && codePoint <= 0x0089F) || // Arabic Extended-B
            (codePoint >= 0x008A0 && codePoint <= 0x008FF) || // Arabic Extended-A
            */
            (codePoint >= 0x00870 && codePoint <= 0x008FF) || // Arabic Extended-B + Extended-A
            (codePoint >= 0x10EC0 && codePoint <= 0x10EFF) || // Arabic Extended-C
            (codePoint >= 0x0FB50 && codePoint <= 0x0FDFF) || // Arabic Presentation Forms-A
            (codePoint >= 0x0FE70 && codePoint <= 0x0FEFF)    // Arabic Presentation Forms-B
        ) return true;

        // Indic scripts
            /*
            (codePoint >= 0x00900 && codePoint <= 0x0097F) || // Devanagari
            (codePoint >= 0x00980 && codePoint <= 0x009FF) || // Bengali
            (codePoint >= 0x00A00 && codePoint <= 0x00A7F) || // Gurmukhi
            (codePoint >= 0x00A80 && codePoint <= 0x00AFF) || // Gujarati
            (codePoint >= 0x00B00 && codePoint <= 0x00B7F) || // Oriya
            (codePoint >= 0x00B80 && codePoint <= 0x00BFF) || // Tamil
            (codePoint >= 0x00C00 && codePoint <= 0x00C7F) || // Telugu
            (codePoint >= 0x00C80 && codePoint <= 0x00CFF) || // Kannada
            (codePoint >= 0x00D00 && codePoint <= 0x00D7F) || // Malayalam
            (codePoint >= 0x00D80 && codePoint <= 0x00DFF) || // Sinhala
            */
        if( (codePoint >= 0x00900 && codePoint <= 0x00DFF) || // Devanagari → Sinhala
            /*
            (codePoint >= 0x11000 && codePoint <= 0x1107F) || // Brahmi
            (codePoint >= 0x11100 && codePoint <= 0x1114F) || // Chakma
            */
            (codePoint >= 0x11000 && codePoint <= 0x1114F) || // Brahmi     + Chakma
            /*
            (codePoint >= 0x11300 && codePoint <= 0x1137F) || // Grantha
            (codePoint >= 0x11400 && codePoint <= 0x1147F) || // Newa
            */
            (codePoint >= 0x11300 && codePoint <= 0x1147F) || // Grantha    + Newa
            /*
            (codePoint >= 0x11580 && codePoint <= 0x115FF) || // Siddham
            (codePoint >= 0x11600 && codePoint <= 0x1165F) || // Modi
            */
            (codePoint >= 0x11580 && codePoint <= 0x1165F) || // Siddham    + Modi
            /*
            (codePoint >= 0x11C00 && codePoint <= 0x11C6F) || // Bhaiksuki
            (codePoint >= 0x11D00 && codePoint <= 0x11D5F)    // Masaram Gondi
            */
            (codePoint >= 0x11C00 && codePoint <= 0x11D5F)    // Bhaiksuki  + Masaram Gondi
        ) return true;

        // Thai + Lao
            /*
            (codePoint >= 0x0E00 && codePoint <= 0x0E7F) || // Thai
            (codePoint >= 0x0E80 && codePoint <= 0x0EFF)    // Lao
            */
        if( (codePoint >= 0x0E00 && codePoint <= 0x0EFF) ) return true; // Thai + Lao

        // Myanmar
        if( (codePoint >= 0x1000 && codePoint <= 0x109F) || // Myanmar
            (codePoint >= 0xAA60 && codePoint <= 0xAA7F) || // Myanmar Extended-A
            (codePoint >= 0xA9E0 && codePoint <= 0xA9FF)    // Myanmar Extended-B
        ) return true;

        // Khmer
        if(codePoint >= 0x1780 && codePoint <= 0x17FF) return true;

        // Hebrew
        if(codePoint >= 0x0590 && codePoint <= 0x05FF) return true;

        // Tibetan
        if(codePoint >= 0x0F00 && codePoint <= 0x0FFF) return true;

        // Non-spacing marks (Mn)
        if( (codePoint >= 0x0300 && codePoint <= 0x036F) || // Combining Diacritical Marks
            (codePoint >= 0x1AB0 && codePoint <= 0x1AFF) || // Combining Diacritical Marks Extended
            (codePoint >= 0x1DC0 && codePoint <= 0x1DFF) || // Combining Diacritical Marks Supplement
            (codePoint >= 0x20D0 && codePoint <= 0x20FF) || // Combining Diacritical Marks for Symbols
            (codePoint >= 0xFE20 && codePoint <= 0xFE2F)    // Combining Half Marks
         ) return true;

        // Spacing combining marks (Mc)
        if( (codePoint >= 0x0903 && codePoint <= 0x0939) || // Devanagari vowel signs
            (codePoint >= 0x0A3E && codePoint <= 0x0A4C) || // Gurmukhi vowel signs
            (codePoint >= 0x0ABE && codePoint <= 0x0AC5) || // Gujarati vowel signs
            (codePoint >= 0x0B3E && codePoint <= 0x0B44) || // Oriya vowel signs
            (codePoint >= 0x0C3E && codePoint <= 0x0C56) || // Telugu vowel signs
            (codePoint >= 0x0D3E && codePoint <= 0x0D44) || // Malayalam vowel signs
            (codePoint >= 0x0E31 && codePoint <= 0x0E3A) || // Thai marks
            (codePoint >= 0x0EB1 && codePoint <= 0x0EB9)    // Lao marks
        ) return true;

        // Enclosing marks (Me)
        if( (codePoint >= 0x20DD && codePoint <= 0x20E0) || // Enclosing circle, square, etc.
            (codePoint >= 0x20E1 && codePoint <= 0x20E1) || // Enclosing circle backslash
            (codePoint >= 0x20E2 && codePoint <= 0x20E4)    // Enclosing marks
        ) return true;

        // Not CTL
        return false;
    }

    public static boolean containsCTL(final String text)
    {
        // Iterate over code points instead of chars
        for( int i = 0; i < text.length(); ) {

            final int codePoint = text.codePointAt(i);

            if( isCTL(codePoint) ) return true;

            i += Character.charCount(codePoint);

        } // for

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static interface CharAccessor {

        int length();
        char charAt(final int index);

    } // interface CharAccessor

    private static int _stepCount_impl(final CharAccessor text, final int index, final boolean forward)
    {
        if(forward) {
            final int start    = index;
                  int consumed = 1;

            final CharType baseType = charType( text.charAt(start) );
            if( baseType == CharType.HIGH_SURROGATE && start + 1 < text.length() ) {
                consumed = 2; // Surrogate pair -> consume both
            }

            // Consume following combining marks, ZWJ sequences, and regional indicators
            int next = start + consumed;

            while( next < text.length() ) {
                final CharType nextType = charType( text.charAt(next) );

                if(nextType == CharType.COMBINING_MARK) {
                    ++consumed; // Consume COMBINING_MARK
                    ++next;
                    continue;
                }

                if( nextType == CharType.ZWJ && next + 1 < text.length() ) {
                    ++consumed; // Consume ZWJ
                    ++next;
                    ++consumed; // Consume the next base glyph
                    ++next;
                    continue;
                }

                if(nextType == CharType.REGIONAL_INDICATOR) {
                    int count = 0;
                    while( next < text.length() && charType( text.charAt(next) ) == CharType.REGIONAL_INDICATOR && count < 2 ) {
                        ++consumed; // Consume REGIONAL_INDICATOR
                        ++next;
                        ++count;
                    } // while
                    continue;
                }

                break;
            } // while

            return consumed;
        }

        else {
            int end      = index;
            int consumed = 0;

            // Step back over combining marks
            while(end > 0) {
                final CharType prevType = charType( text.charAt(end - 1) );
                if(prevType != CharType.COMBINING_MARK) break;
                ++consumed; // Consume COMBINING_MARK
                --end;
            } // while

            // Step back over ZWJ sequences
            while(end > 1) {
                final CharType prevType = charType( text.charAt(end - 1) );
                if(prevType != CharType.ZWJ) break;
                consumed += 2; // Consume ZWJ and the preceding base glyph
                end      -= 2;
            } // while

            // Step back over regional indicator pairs
            int riCount = 0;
            while( end > 0 && charType( text.charAt(end - 1) ) == CharType.REGIONAL_INDICATOR && riCount < 2 ) {
                ++consumed; // Consume REGIONAL_INDICATOR
                --end;
                ++riCount;
            } // while

            // Step back over base glyph (BMP or surrogate pair)
            if(end > 0) {
                final CharType baseType = charType( text.charAt(end - 1) );
                if( baseType == CharType.LOW_SURROGATE && end - 2 >= 0 ) {
                    consumed += 2; // Surrogate pair backward - consume both LOW_SURROGATE and HIGH_SURROGATE
                    end      -= 2;
                }
                else {
                    ++consumed;
                    --end;
                }
            }

            return -consumed;
        }
    }

    public static int stepCount(final String text, final int index, final boolean forward)
    {
        return _stepCount_impl(
            new CharAccessor() {
                public int  length(           ) { return text.length( ); }
                public char charAt(final int i) { return text.charAt(i); }
            },
            index,
            forward
        );
    }

    public static int stepCountFW(final String text, final int index) { return stepCount(text, index, true ); }
    public static int stepCountBW(final String text, final int index) { return stepCount(text, index, false); }

    public static int stepCount(final ANSIScreenBuffer.Char[] text, final int index, final boolean forward)
    {
        return _stepCount_impl(
            new CharAccessor() {
                public int  length(           ) { return text.length; }
                public char charAt(final int i) { return text[i].ch ; }
            },
            index,
            forward
        );
    }

    public static int stepCountFW(final ANSIScreenBuffer.Char[] text, final int index) { return stepCount(text, index, true ); }
    public static int stepCountBW(final ANSIScreenBuffer.Char[] text, final int index) { return stepCount(text, index, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _stepCountRange_impl(final CharAccessor text, final int index, final int count, final boolean forward)
    {
        int consumedTotal = 0;
        int pos           = index;

        if(forward) {
            for( int i = 0; i < count && pos < text.length(); ++i) {
                final int consumed = _stepCount_impl(text, pos, true );
                consumedTotal += consumed;
                pos           += consumed;
            }
        }
        else {
            for(int i = 0; i < count && pos > 0; ++i) {
                final int consumed = _stepCount_impl(text, pos, false);
                consumedTotal += consumed;
                pos           += consumed;
            }
        }

        return consumedTotal;
    }

    public static int stepCountRange(final String text, final int index, final int count, final boolean forward)
    {
        return _stepCountRange_impl(
            new CharAccessor() {
                public int  length(           ) { return text.length( ); }
                public char charAt(final int i) { return text.charAt(i); }
            },
            index,
            count,
            forward
        );
    }

    public static int stepCountRangeFW(final String text, final int index, final int count) { return stepCountRange(text, index, count, true ); }
    public static int stepCountRangeBW(final String text, final int index, final int count) { return stepCountRange(text, index, count, false); }

    public static int stepCountRange(final ANSIScreenBuffer.Char[] text, final int index, final int count, final boolean forward)
    {
        return _stepCountRange_impl(
            new CharAccessor() {
                public int  length(           ) { return text.length; }
                public char charAt(final int i) { return text[i].ch ; }
            },
            index,
            count,
            forward
        );
    }

    public static int stepCountRangeFW(final ANSIScreenBuffer.Char[] text, final int index, final int count) { return stepCountRange(text, index, count, true ); }
    public static int stepCountRangeBW(final ANSIScreenBuffer.Char[] text, final int index, final int count) { return stepCountRange(text, index, count, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final static HashMap<Integer, int[]  > _decompositionMap  = new HashMap<>();
    private final static HashSet<Integer         > _exclusionsSet     = new HashSet<>();

    private final static HashMap<Long   , Integer> _compositionMap    = new HashMap<>();
    private final static HashMap<Integer, Integer> _combiningClassMap = new HashMap<>();

    private       static boolean                   _uniDataLoaded     = false;
    private       static boolean                   _uniDataLoadError  = false;

    private static String _skipEmptyOrComment(final String line_)
    {
        String line = line_.trim();
        if( line.isEmpty() || line.startsWith("#") ) return null;

        final int hashIndex = line.indexOf('#');
        if(hashIndex >= 0) line = line.substring(0, hashIndex).trim();

        return line.isEmpty() ? null : line;
    }

    private static void _loadUnicodeData_impl(final URL url) throws IOException
    {
        try(
            BufferedReader reader = new BufferedReader( new InputStreamReader( url.openStream(), SysUtil._CharEncoding) )
        ) {

            String line;

            while( ( line = reader.readLine() ) != null ) {

                line = _skipEmptyOrComment(line);
                if(line == null) continue;

                final String[] fields = line.split(";");
                if(fields.length <= 5) continue;

                final int    codePoint      = Integer.parseInt( fields[0], 16 );
                final int    canonCombClass = Integer.parseInt( fields[3]     );
                final String decomposition  =                   fields[5]      ;

                _combiningClassMap.put(codePoint, canonCombClass);

                if( !decomposition.isEmpty() && !decomposition.startsWith("<") ) {
                    final String[] parts = decomposition.split(" ");
                    final int   [] seq   = new int[parts.length];
                    for(int i = 0; i < parts.length; ++i) seq[i] = Integer.parseInt( parts[i], 16 );
                    _decompositionMap.put(codePoint, seq);
                }

            } // while

        }
    }

    private static void _loadExclusions_impl(final URL url) throws IOException
    {
        try(
            BufferedReader reader = new BufferedReader( new InputStreamReader( url.openStream(), SysUtil._CharEncoding) )
        ) {

            String line;

            while( ( line = reader.readLine() ) != null ) {

                line = _skipEmptyOrComment(line);
                if(line == null) continue;

                final int codePoint = Integer.parseInt(line, 16);
                _exclusionsSet.add(codePoint);

            } // while

        }
    }

    private static void _buildCompositionMap_impl()
    {
        for( final Map.Entry<Integer, int[]> entry : _decompositionMap.entrySet() ) {

            final int composed = entry.getKey();
            if( _exclusionsSet.contains(composed) ) continue;

            final int[] parts = entry.getValue();
            if(parts.length == 2) {
                final int  starter   = parts[0];
                final int  combining = parts[1];
                final long key       = ( ( (long) starter ) << 32 ) | combining;
                _compositionMap.put(key, composed);
            }

        } // for
    }

    private synchronized static boolean _initializeUnicodeNormalization()
    {
        if(_uniDataLoaded   ) return true ;
        if(_uniDataLoadError) return false;

        try {
            _loadUnicodeData_impl( SysUtil.getUnicodeTextURL_defLoc("UnicodeData"          ) );
            _loadExclusions_impl ( SysUtil.getUnicodeTextURL_defLoc("CompositionExclusions") );

            _buildCompositionMap_impl();

            _uniDataLoaded = true;
            return true;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set flag
            _uniDataLoadError = true;
        }

        return false;
    }

    private static String _cpToString(final int[] codePoints)
    {
        final StringBuilder sb = new StringBuilder(codePoints.length);
        for(final int cp : codePoints) sb.appendCodePoint(cp);

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Remove '_decomposeNFD_impl_single()' later !!! #####

    private static int[] _reorderCanonical(final int[] codePoints)
    {
        final int[] result = Arrays.copyOf(codePoints, codePoints.length);

        int start = 0;

        while(start < result.length) {

            // Find next starter (class 0)
            int i = start + 1;
            while( i < result.length && _combiningClassMap.getOrDefault( result[i], 0 ) != 0 ) ++i;

            // Sort [start+1, i) by combining class using insertion sort
            for(int j = start + 1; j < i; ++j) {

                final int key      = result[j];
                final int keyClass = _combiningClassMap.getOrDefault(key, 0);
                      int k        = j - 1;

                while( k >= start + 1 && _combiningClassMap.getOrDefault(result[k], 0) > keyClass ) {
                    result[k + 1] = result[k];
                    --k;
                }

                result[k + 1] = key;

            } // for

            start = i;

        } // while

        return result;
    }

    private static void _decomposeNFD_impl_codePointRecursive(final int cp, final ArrayList<Integer> out)
    {
        // Hangul syllable decomposition
        if(cp >= 0xAC00 && cp <= 0xD7A3) {
            final int SIndex = cp - 0xAC00;
            final int LIndex =   SIndex / (21 * 28)        ;
            final int VIndex = ( SIndex % (21 * 28) ) / 28 ;
            final int TIndex =   SIndex %  28              ;
            final int LPart  = 0x1100 + LIndex;
            final int VPart  = 0x1161 + VIndex;
            out.add(LPart);
            out.add(VPart);
            if(TIndex > 0) {
                final int TPart = 0x11A7 + TIndex;
                out.add(TPart);
            }
            return;
        }

        // Lookup in decomposition map
        final int[] decomposition = _decompositionMap.get(cp);

        if(decomposition != null) {
            for(final int part : decomposition) {
                // Recurse on each part
                _decomposeNFD_impl_codePointRecursive(part, out);
            }
        }
        else {
            // No decomposition
            out.add(cp);
        }
    }

    private static int[] _decomposeNFD_impl_recursive(final int[] codePoints)
    {
        if( !_initializeUnicodeNormalization() ) return codePoints;

        final ArrayList<Integer> out = new ArrayList<>();

        for(final int cp : codePoints) {
            _decomposeNFD_impl_codePointRecursive(cp, out);
        }

        // Convert ArrayList<Integer> → int[]
        final int[] result = new int[ out.size() ];

        for( int i = 0; i < out.size(); ++i ) result[i] = out.get(i);

        // Canonical reordering
        return _reorderCanonical(result);
    }

    private static int[] _decomposeNFD_impl_single(final int[] codePoints)
    {
        if( !_initializeUnicodeNormalization() ) return codePoints;

        int[] result = new int[codePoints.length * 3]; // Allocate x3 for safe upper bound
        int   size   = 0;

        for(final int cp : codePoints) {

            final int[] decomposition;

            // Check for Hangul syllable decomposition first
            if(cp >= 0xAC00 && cp <= 0xD7A3) {
                final int SIndex = cp - 0xAC00;
                final int LIndex =   SIndex / (21 * 28)        ;
                final int VIndex = ( SIndex % (21 * 28) ) / 28 ;
                final int TIndex =   SIndex %  28              ;
                final int LPart  = 0x1100 + LIndex;
                final int VPart  = 0x1161 + VIndex;
                if(TIndex > 0) {
                    final int TPart = 0x11A7 + TIndex;
                    decomposition = new int[] { LPart, VPart, TPart };
                }
                else {
                    decomposition = new int[] { LPart, VPart        };
                }
            }
            // Use decomposition map
            else {
                decomposition = _decompositionMap.get(cp);
            }

            final boolean canDecompose = (decomposition != null);

            if( ( size + (canDecompose ? decomposition.length : 1) ) > result.length ) {
                result = Arrays.copyOf(result, result.length * 2);
            }

            if(canDecompose) {
                for(final int part : decomposition) result[size++] = part;
            }
            else {
                result[size++] = cp;
            }

        } // for

        return _reorderCanonical( Arrays.copyOf(result, size) );
    }

    private static int[] _decomposeNFD_impl(final int[] codePoints)
    { return (true) ? _decomposeNFD_impl_recursive(codePoints) : _decomposeNFD_impl_single(codePoints); }

    public static String decomposeNFD(final String string)
    {
        if( !_initializeUnicodeNormalization() ) return string;

        final int[] result = _decomposeNFD_impl( string.codePoints().toArray() );

        return _cpToString(result);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _tryCompose(final int starter, final int combining)
    {
        final long key = ( ( (long) starter ) << 32 ) | combining;

        // Check for Hangul syllable composition first
        if(starter >= 0x1100 && starter <= 0x1112 && combining >= 0x1161 && combining <= 0x1175) {
            // L + V → LV syllable
            final int LIndex = starter   - 0x1100;
            final int VIndex = combining - 0x1161;
            final int S      = 0xAC00 + (LIndex * 21 + VIndex) * 28;
            return S;
        }

        if(starter >= 0xAC00 && starter <= 0xD7A3 && combining >= 0x11A8 && combining <= 0x11C2) {
            // LV + T → LVT syllable
            final int TIndex = combining - 0x11A7;
            final int S      = starter + TIndex;
            return S;
        }

        // Use composition map
        return _compositionMap.getOrDefault(key, -1);
    }

    private static int[] _composeNFC_impl(final int[] codePoints)
    {
        if( !_initializeUnicodeNormalization() ) return codePoints;

        final int[] result  = new int[codePoints.length];
              int   size    = 0;
              int   starter = -1;

        for(final int cp : codePoints) {

            if (starter == -1) {
                starter = cp;
                result[size++] = cp;
            }
            else {
                final int composed = _tryCompose(starter, cp);
                if(composed != -1) {
                    result[size - 1] = composed;
                    starter = composed;
                }
                else {
                    result[size++] = cp;
                    starter = cp;
                }
            }

        } // for

        return Arrays.copyOf(result, size);
    }

    public static String composeNFC(final String string)
    {
        if( !_initializeUnicodeNormalization() ) return string;

        final int[] result = _composeNFC_impl(
                                 _decomposeNFD_impl( string.codePoints().toArray() )
                             );

        return _cpToString(result);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static abstract class StreamingNormalizerBase {

        private static final char[] EMPTY_CHAR_ARRAY = new char[0];

        private int[] _buffer      = new int[8];
        private int   _length      = 0;
        private char  _pendingHigh = 0;

        private void _reset()
        {
            _length      = 0;
            _pendingHigh = 0;
        }

        private void _addCodePoint(final int cp)
        {
            if(_length >= _buffer.length) _buffer = Arrays.copyOf(_buffer, _buffer.length * 2);
            _buffer[_length++] = cp;
        }

        protected abstract int[] _processCodePoints(final int[] codePoints);

        protected char[] _flushBuffer()
        {
            if(_length == 0) return EMPTY_CHAR_ARRAY;

            final int[] codePoints = Arrays.copyOf(_buffer, _length);
            final int[] processed  = _processCodePoints(codePoints);

            _reset();

            if(processed == null || processed.length == 0) return EMPTY_CHAR_ARRAY;

            return ( new String(processed, 0, processed.length) ).toCharArray();
        }

        public char[] processChar(final char chr)
        {
            switch( charType(chr) ) {

                case BMP_BASE: /* FALLTHROUGH */
                case OTHER   :
                    // Flush before adding a standalone character
                    final char[] res = _flushBuffer();
                    _addCodePoint(chr);
                    return res;

                case HIGH_SURROGATE:
                    _pendingHigh = chr;
                    break;

                case LOW_SURROGATE:
                    if(_pendingHigh != 0) {
                        _addCodePoint( toCodePoint(_pendingHigh, chr) );
                        _pendingHigh = 0;
                    }
                    else {
                        _addCodePoint(chr);
                    }
                    break;

                case COMBINING_MARK:
                    _addCodePoint(chr);
                    break;

                case ZWJ:
                    _addCodePoint(chr);
                    break;

                case REGIONAL_INDICATOR:
                    _addCodePoint(chr);
                    break;

            } // switch

            return EMPTY_CHAR_ARRAY;
        }

    } // class StreamingNormalizerBase

    private static class StreamingNFDNormalizer extends StreamingNormalizerBase
    {
        @Override
        protected int[] _processCodePoints(final int[] codePoints)
        { return _decomposeNFD_impl(codePoints); }

    } // class StreamingNFDNormalizer

    private static class StreamingNFCNormalizer extends StreamingNormalizerBase {

        @Override
        protected int[] _processCodePoints(final int[] codePoints)
        { return _composeNFC_impl( _decomposeNFD_impl(codePoints) ); }

    } // class StreamingNFCNormalizer

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final StreamingNFCNormalizer _streamingNFC = new StreamingNFCNormalizer();

    public synchronized static char[] composeNFC(final char chr)
    { return _streamingNFC.processChar(chr); }

    public synchronized static char[] flushNFC()
    { return _streamingNFC._flushBuffer(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final StreamingNFDNormalizer _streamingNFD = new StreamingNFDNormalizer();

    public synchronized static char[] decomposeNFD(final char chr)
    { return _streamingNFD.processChar(chr); }

    public synchronized static char[] flushNFD()
    { return _streamingNFD._flushBuffer(); }

    /*
    // ##### !!! TODO : REMOVE THIS LATER !!! #####

    private static String _dumpUNI(final String str)
    { return '"' + str + '"'; }

    private static String _dumpUNP(final String str)
    {
        final StringBuilder sb = new StringBuilder();

        for( int i = 0; i < str.length(); ) {

            final int cp  = str.codePointAt(i);

            sb.append('"').append( Character.toChars(cp) ).append('"');

            i += Character.charCount(cp);

            if( i < str.length() ) sb.append(" + ");

        } // for

        return sb.toString();
    }

    private static String _dumpNFD(final String str)
    {
        final StringBuilder sb = new StringBuilder();

        for( int i = 0; i < str.length(); ) {

            final int cp  = str.codePointAt(i);

            if(i == 0) sb.append('"').append( Character.toChars(cp)        ).append('"');
            else       sb.append('"').append( String.format("\\u%04X", cp) ).append('"');

            i += Character.charCount(cp);

            if( i < str.length() ) sb.append(" + ");

        } // for

        return sb.toString();
    }

    static {

        SysUtil.stdDbg().println();

        // Example 1: Precomposed character "é" (U+00E9)
        final String s1 = "é";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s1  ) ); // → "é"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpNFD( decomposeNFD(s1) ) ); // → "e" + "\u0301"
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s1) ) ); // → "é"
        SysUtil.stdDbg().println();

        // Example 2: Precomposed "Å" (U+00C5)
        final String s2 = "Å";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s2  ) ); // → "Å"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpNFD( decomposeNFD(s2) ) ); // → "A" + "\u030A"
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s2) ) ); // → "Å"
        SysUtil.stdDbg().println();

        // Example 3: Multiple combining marks
        //     "o" + COMBINING DIAERESIS (U+0308) + COMBINING MACRON (U+0304)
        //     NFD ensures canonical order: diaeresis (class 230) before macron (class 230, stable sort preserves input order)
        String s3 = "o\u0308\u0304";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s3  ) ); // → "ȫ"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpNFD( decomposeNFD(s3) ) ); // → "o" + "\u0308" + "\u0304"
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s3) ) ); // → "ȫ"
        SysUtil.stdDbg().println();

        // Example 4: String with combining marks out of order
        //     "a" + COMBINING ACUTE (U+0301) + COMBINING CEDILLA (U+0327)
        //     NFD will reorder to "a" + "\u0327 + "\u0301 (cedilla before acute)
        String s4 = "a\u0301\u0327";
        SysUtil.stdDbg().println( "Original (unordered) : " + _dumpUNI(              s4  ) ); // → "á̧"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpNFD( decomposeNFD(s4) ) ); // → "a" + "\u0327" + "\u0301"
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s4) ) ); // → "á̧"
        SysUtil.stdDbg().println();

        // Example 4: Hangul syllable "가" (U+AC00)
        final String s5 = "가";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s5  ) ); // → "가"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpUNP( decomposeNFD(s5) ) ); // → "ᄀ" + "ᅡ" (L + V Jamo)
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s5) ) ); // → "가"
        SysUtil.stdDbg().println();

        // Example 6: Hangul syllable "한" (U+D55C)
        final String s6 = "한";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s6  ) ); // → "한"
        SysUtil.stdDbg().println( "NFD                  : " + _dumpUNP( decomposeNFD(s6) ) ); // → "ᄒ" + "ᅡ" + "ᆫ" (L + V + T Jamo)
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s6) ) ); // → "한"
        SysUtil.stdDbg().println();

        // Example 7: Angstrom Sign "Å" (U+212B) (recursive)
        final String s7 = "Å";
        SysUtil.stdDbg().println( "Original             : " + _dumpUNI(              s7  ) ); // → "Å"
        SysUtil.stdDbg().println( "NFD (recursive)      : " + _dumpNFD( decomposeNFD(s7) ) ); // → "A" + "\u030A"
        SysUtil.stdDbg().println( "NFC                  : " + _dumpUNI( composeNFC  (s7) ) ); // → "Å"
        SysUtil.stdDbg().println();

        SysUtil.stdDbg().println();

        final String str = "echo \"Cursor moved to row 12, col 10 😀😀 This is col 50\"";
        for(int i = 0; i < str.length(); ++i) {

            for( final char c : composeNFC( str.charAt(i) ) )  SysUtil.stdDbg().print(c);
            for( final char c : flushNFC()                  )  SysUtil.stdDbg().print(c);

        }
        SysUtil.stdDbg().println();

        SysUtil.stdDbg().println();

    } // static
    //*/

} // class UnicodeUtil
