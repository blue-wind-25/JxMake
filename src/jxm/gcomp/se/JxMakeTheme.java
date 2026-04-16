/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.util.Map;
import java.util.HashMap;

import java.awt.Color;
import java.awt.Font;

import java.awt.font.TextAttribute;

import javax.swing.UIManager;

import org.fife.ui.rtextarea.RTextScrollPane;

import org.fife.ui.rsyntaxtextarea.MatchedBracketPopup;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

import org.fife.ui.rsyntaxtextarea.spell.SpellingParser;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
class JxMakeTheme {

    public static class TType {

        public static final int Null           = TokenTypes.NULL;
        public static final int Whitespace     = TokenTypes.WHITESPACE;
        public static final int NormalText     = TokenTypes.IDENTIFIER;
        public static final int ErrorText      = TokenTypes.ERROR_IDENTIFIER;
        public static final int CombineNL      = TokenTypes.COMMENT_MARKUP;
        public static final int ForceNL        = TokenTypes.ERROR_CHAR;
        public static final int Comment        = TokenTypes.COMMENT_MULTILINE;
        public static final int Keyword        = TokenTypes.RESERVED_WORD;
        public static final int SKeyword       = TokenTypes.RESERVED_WORD_2;
        public static final int MacroDef       = TokenTypes.DATA_TYPE;
        public static final int MacroUseMark   = TokenTypes.LITERAL_BACKQUOTE;
        public static final int MacroUse       = TokenTypes.COMMENT_KEYWORD;
        public static final int IncLibName     = TokenTypes.COMMENT_DOCUMENTATION;
        public static final int Number         = TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
        public static final int String         = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
        public static final int StringMLMarker = TokenTypes.MARKUP_DTD;
        public static final int StringDQF      = TokenTypes.MARKUP_PROCESSING_INSTRUCTION;
        public static final int ODString       = TokenTypes.MARKUP_ENTITY_REFERENCE;
        public static final int EscapeSeq      = TokenTypes.LITERAL_CHAR;
        public static final int PROperEval     = TokenTypes.PREPROCESSOR;
        public static final int RefVarMark     = TokenTypes.SEPARATOR;
        public static final int RVariableEval  = TokenTypes.VARIABLE;
        public static final int SVariableEval  = TokenTypes.LITERAL_NUMBER_FLOAT;
        public static final int SVarShortcut   = TokenTypes.LITERAL_NUMBER_HEXADECIMAL;
        public static final int VariableEvalN  = TokenTypes.LITERAL_BOOLEAN;
        public static final int ANSIEscapeCode = TokenTypes.MARKUP_COMMENT;
        public static final int SuppressError  = TokenTypes.COMMENT_EOL;
        public static final int BFunctionCall  = TokenTypes.FUNCTION;
        public static final int UFunctionName  = TokenTypes.ANNOTATION;
        public static final int ROperator      = TokenTypes.OPERATOR;
        public static final int SOperator      = TokenTypes.MARKUP_CDATA;
        public static final int SOperatorIF    = TokenTypes.MARKUP_CDATA_DELIMITER;
        public static final int Command        = TokenTypes.MARKUP_TAG_ATTRIBUTE;
        public static final int CommandSpcXDir = TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE;
        public static final int RegExprM       = TokenTypes.MARKUP_TAG_DELIMITER;
        public static final int RegExpr1       = TokenTypes.REGEX;
        public static final int RegExpr2       = TokenTypes.MARKUP_TAG_NAME;

        /*
        ERROR_STRING_DOUBLE
        ERROR_NUMBER_FORMAT
        */

    } // class TType

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _clamp(final float v)
    { return Math.min( 255, Math.max( 0, Math.round(v * 255) ) ); }

    private static float _hueToRGB(final float p, final float q, float t)
    {
        if(t < 0.0f) t += 1.0f;
        if(t > 1.0f) t -= 1.0f;

        if(t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
        if(t < 1.0f / 2.0f) return q;
        if(t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
                            return p;
    }

    private static Color _hslToRGB(final float[] hsl)
    {
        float h = hsl[0];
        float s = hsl[1];
        float l = hsl[2];

        float r, g, b;

        if(s == 0.0f) {
            r = g = b = l;
        }
        else {
            final float q = (l < 0.5f) ? l * (1f + s) : l + s - l * s;
            final float p = 2.0f * l - q;
            r = _hueToRGB(p, q, h + 1.0f / 3.0f);
            g = _hueToRGB(p, q, h              );
            b = _hueToRGB(p, q, h - 1.0f / 3.0f);
        }

        return new Color( _clamp(r), _clamp(g), _clamp(b) );
    }

    private static float[] _rgbToHSL(final Color color)
    {
        final float r = color.getRed  () / 255.0f;
        final float g = color.getGreen() / 255.0f;
        final float b = color.getBlue () / 255.0f;

        final float max = Math.max( r, Math.max(g, b) );
        final float min = Math.min( r, Math.min(g, b) );

        float h;
        float s;
        float l = (max + min) / 2.0f;

        if(max == min) {
            h = s = 0.0f;
        }
        else {
            final float d = max - min;
            s = (l > 0.5f) ? d / (2f - max - min) : d / (max + min);
                 if(max == r) h = (g - b) / d + (g < b ? 6.0f : 0.0f);
            else if(max == g) h = (b - r) / d + 2.0f;
            else              h = (r - g) / d + 4.0f;
            h /= 6.0f;
        }

        return new float[] { h, s, l };
    }

    private static Color _genDarkModeFGColor(final Color color)
    {
        final float[] hsl = _rgbToHSL(color);

        hsl[1] = Math.min(1.00f,         hsl[1] + 0.10f);
        hsl[2] = Math.min(1.00f, 1.00f - hsl[2] + 0.05f);

        return _hslToRGB(hsl);
    }

    private static Color _genDarkModeBGColor(final Color color)
    {
        final float[] hsl = _rgbToHSL(color);

        hsl[2] = Math.max(0.05f, 1.00f - hsl[2]);

        return _hslToRGB(hsl);
    }

    private static Color _genDarkModeHLColor(final Color color)
    {
        final float[] hsl = _rgbToHSL(color);

        hsl[2] = Math.max(0.40f,         hsl[2] - 0.10f);
        hsl[2] = Math.max(0.25f, 1.00f - hsl[2] - 0.10f);

        return _hslToRGB(hsl);
    }

    private static Color _decodeFGCol(final boolean darkMode, final String htmlColor)
    {
        final Color color = Color.decode(htmlColor);

        return darkMode ? _genDarkModeFGColor(color) : color;
    }

    private static Color _decodeBGCol(final boolean darkMode, final String htmlColor)
    {
        final Color color = Color.decode(htmlColor);

        return darkMode ? _genDarkModeBGColor(color) : color;
    }

    private static Color _decodeHLCol(final boolean darkMode, final String htmlColor)
    {
        final Color color = Color.decode(htmlColor);

        return darkMode ? _genDarkModeHLColor(color) : color;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Font _monoFont = null;

    public static Font getMonoFont()
    {
        // Create the font as needed
        if(_monoFont == null) {
            final XCom.Pair<String, Integer> fnz = SysUtil.getTextAreaMonospacedFontSpec(
                                                       "JXMAKE_SEDITOR_FONT_NAME",
                                                       "JXMAKE_SEDITOR_FONT_SIZE"
                                                   );

            final String fontName = fnz.first ();
            final int    fontSize = fnz.second();

            _monoFont = new Font(fontName, Font.PLAIN, fontSize + 2);
        }

        // Return the font
        return _monoFont;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Color _bg_LightMode = _decodeBGCol(false, "#FFFFFF");
    private static Color _bg_DarkMode  = _decodeBGCol(true , "#FFFFFF");

    private static void _setStyle(final SyntaxScheme scheme, final int tokenType, final Color fgColor, final Color bgColor, final boolean b, final boolean i, final boolean u, final boolean s)
    {
        // Set the style
        final Style style = scheme.getStyle(tokenType);

        style.foreground = fgColor;
        style.background = bgColor;

        style.font = getMonoFont().deriveFont(
              (b && i) ? (Font.BOLD | Font.ITALIC)
            : (b     ) ? (Font.BOLD              )
            : (     i) ? (            Font.ITALIC)
            :            (Font.PLAIN             )
        );

        style.underline = u;

        if(/*u ||*/ s) {
            final HashMap<TextAttribute, Object> attributes = new HashMap<>( style.font.getAttributes() );
          //if(u) attributes.put(TextAttribute.UNDERLINE    , TextAttribute.UNDERLINE_ON    );
            if(s) attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            style.font = Font.getFont(attributes);
        }
    }

    private static void _setStyle(final SyntaxScheme scheme, final int tokenType, final boolean darkMode, final String fgColorHTML, final boolean b, final boolean i, final boolean u, final boolean s)
    {
        _setStyle(
            scheme, tokenType, _decodeFGCol(darkMode, fgColorHTML), darkMode ? _bg_DarkMode : _bg_LightMode, b, i, u, s
        );
    }

    public static void apply(final JxMakeRootPane rootPane, final boolean darkMode)
    {
        final RSyntaxTextArea textArea   = rootPane.textArea          ();
        final RTextScrollPane scrollPane = rootPane.textAreaScrollPane();

        System.setProperty(MatchedBracketPopup.PROPERTY_CONSIDER_TEXTAREA_BACKGROUND, "true");

        //*
        textArea.setBackground                 ( darkMode ? _bg_DarkMode : _bg_LightMode  );
        textArea.setCaretColor                 (        _decodeFGCol(darkMode, "#FF0000") );

        textArea.setUseSelectedTextColor       ( false                                    );
        textArea.setSelectionColor             (        _decodeHLCol(darkMode, "#C8C8FF") );

        textArea.setFadeCurrentLineHighlight   ( false                                    );
        textArea.setCurrentLineHighlightColor  (        _decodeHLCol(darkMode, "#FFFFAA") );

        textArea.setMarginLineEnabled          ( false                                     );
        textArea.setMarginLineColor            (        _decodeFGCol(darkMode, "#B0B4B9") );

        textArea.setMarkAllHighlightColor      (        _decodeHLCol(darkMode, "#FFC800") );

        textArea.setMarkOccurrences            ( false                                    );
        textArea.setMarkOccurrencesColor       (        _decodeHLCol(darkMode, "#D4D4D4") );

        textArea.setHighlightSecondaryLanguages( false                                    );
        textArea.setAnimateBracketMatching     ( true                                     );
        textArea.setMatchedBracketBorderColor  (        _decodeFGCol(darkMode, "#000080") );
        textArea.setMatchedBracketBGColor      (        _decodeFGCol(darkMode, "#EAEAFF") );

        textArea.setHyperlinkForeground        (        _decodeFGCol(darkMode, "#0000FF") );

        textArea.setSecondaryLanguageBackground( 1    , _decodeBGCol(darkMode, "#FFF0CC") );
        textArea.setSecondaryLanguageBackground( 2    , _decodeBGCol(darkMode, "#DAFEDA") );
        textArea.setSecondaryLanguageBackground( 3    , _decodeBGCol(darkMode, "#FFE0F0") );

        //*
        scrollPane.getGutter().setBorderColor                 ( _decodeFGCol(false, "#DDDDDD") );
        scrollPane.getGutter().setLineNumberColor             ( _decodeFGCol(false, "#787878") );
        scrollPane.getGutter().setCurrentLineNumberColor      ( _decodeFGCol(false, "#06176B") );

        scrollPane.getGutter().setFoldIndicatorForeground     ( _decodeFGCol(false, "#808080") );
        scrollPane.getGutter().setFoldIndicatorArmedForeground( _decodeFGCol(false, "#585858") );
        scrollPane.getGutter().setFoldBackground              ( _decodeFGCol(false, "#FFFFFF") );

        scrollPane.getGutter().setActiveLineRangeColor        ( _decodeFGCol(false, "#3399FF") );
        //*/

        //*
        final SyntaxScheme scheme = textArea.getSyntaxScheme();

        _setStyle(scheme, TType.Whitespace    , darkMode, "#000000", false, false, false, false);
        _setStyle(scheme, TType.NormalText    , darkMode, "#000000", false, false, false, false);
        _setStyle(scheme, TType.ErrorText     , darkMode, "#FF0000", true , true , false, true );
        _setStyle(scheme, TType.CombineNL     , darkMode, "#808080", false, false, false, false);
        _setStyle(scheme, TType.ForceNL       , darkMode, "#FF1F1F", true , true , false, false);
        _setStyle(scheme, TType.Comment       , darkMode, "#808080", false, false, false, false);
        _setStyle(scheme, TType.Keyword       , darkMode, "#000000", true , false, false, false);
        _setStyle(scheme, TType.SKeyword      , darkMode, "#000000", true , false, true , false);
        _setStyle(scheme, TType.MacroDef      , darkMode, "#000000", true , false, true , false);
        _setStyle(scheme, TType.MacroUseMark  , darkMode, "#FF7FAF", true , true , true , false);
        _setStyle(scheme, TType.MacroUse      , darkMode, "#000000", false, true , false, false);
        _setStyle(scheme, TType.IncLibName    , darkMode, "#000000", false, true , false, false);
        _setStyle(scheme, TType.Number        , darkMode, "#AAAA00", false, false, false, false);
        _setStyle(scheme, TType.String        , darkMode, "#BF3F00", false, false, false, false);
        _setStyle(scheme, TType.StringMLMarker, darkMode, "#FF8000", true , false, false, false);
        _setStyle(scheme, TType.StringDQF     , darkMode, "#007F7F", true , true , false, false);
        _setStyle(scheme, TType.ODString      , darkMode, "#007F7F", false, false, false, false);
        _setStyle(scheme, TType.EscapeSeq     , darkMode, "#005AB3", true , false, false, false);
        _setStyle(scheme, TType.PROperEval    , darkMode, "#D08290", true , false, false, false);
        _setStyle(scheme, TType.RefVarMark    , darkMode, "#C300B3", true , false, false, false);
        _setStyle(scheme, TType.RVariableEval , darkMode, "#006A00", false, false, false, false);
        _setStyle(scheme, TType.SVariableEval , darkMode, "#5A00B3", false, false, false, false);
        _setStyle(scheme, TType.SVarShortcut  , darkMode, "#000000", true , true , false, false);
        _setStyle(scheme, TType.VariableEvalN , darkMode, "#FF3F3F", true , false, false, false);
        _setStyle(scheme, TType.ANSIEscapeCode, darkMode, "#404040", true , true , false, false);
        _setStyle(scheme, TType.SuppressError , darkMode, "#0000BF", true , true , true , false);
        _setStyle(scheme, TType.BFunctionCall , darkMode, "#0000BF", false, true , false, false);
        _setStyle(scheme, TType.UFunctionName , darkMode, "#007F7F", false, false, false, false);
        _setStyle(scheme, TType.ROperator     , darkMode, "#5300B3", false, false, false, false);
        _setStyle(scheme, TType.SOperator     , darkMode, "#C300B3", false, false, false, false);
        _setStyle(scheme, TType.SOperatorIF   , darkMode, "#C300B3", false, true , false, false);
        _setStyle(scheme, TType.Command       , darkMode, "#C8A4A2", true , false, false, false);
        _setStyle(scheme, TType.CommandSpcXDir, darkMode, "#C8A4A2", true , false, true , false);
        _setStyle(scheme, TType.RegExprM      , darkMode, "#50C0C0", true , true , false, false);
        _setStyle(scheme, TType.RegExpr1      , darkMode, "#5F5FBF", false, true , false, false);
        _setStyle(scheme, TType.RegExpr2      , darkMode, "#BF3FBF", false, true , false, false);
        //*/

        applySpellingParser(rootPane, darkMode);

        //*
        UIManager.put( "ToolTip.background", _decodeBGCol(false, "#CDE6FF") );
        UIManager.put( "ToolTip.foreground", _decodeFGCol(false, "#000000") );
        //*/
    }

    public static void applySpellingParser(final JxMakeRootPane rootPane, final boolean darkMode)
    {
        final SpellingParser spellingParser = rootPane.spellingParser();

        //*
        if(spellingParser != null) spellingParser.setSquiggleUnderlineColor( _decodeFGCol(darkMode, "#FF0000") );
        //*/
    }

} // class JxMakeTheme
