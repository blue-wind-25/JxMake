/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.gcomp.*;
import jxm.xb.*;


public class CommandSpecifier {

    public static enum AssignmentStyle {

        NONE  , // <opt>
        VALUE , //       <value>
        SPACE , // <opt> <value>
        EQUALS  // <opt>=<value>

    } // enum enum

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String Text_Option_NA = Texts.ScrEdt_CCNotUsed;

    public static class OptionConfig {

        public final boolean         repeatable;
        public final AssignmentStyle style;
        public final boolean         requiresValue;
        public final String[]        possibleValues;
        public final String[]        descriptions;

        public OptionConfig(final boolean repeatable_, final AssignmentStyle style_, final String[] possibleValues_, final String[] descriptions_)
        {
            final String[] descs = (descriptions_ != null) ? new String[descriptions_.length] : null;
            if(descs != null) {
                for(int i = 0; i < descriptions_.length; ++i) {
                    descs[i] = (descriptions_[i] != null) ? descriptions_[i].trim() : null;
                }
            }

            repeatable     = repeatable_;
            style          = style_;
            requiresValue  = style_ != AssignmentStyle.NONE;
            possibleValues = requiresValue  ? possibleValues_ : null;
            descriptions   = descs;
        }

        public OptionConfig(final boolean repeatable_, final AssignmentStyle style_, final String[] possibleValues_, final String description_)
        { this( repeatable_, style_, possibleValues_, new String[] { description_ } ); }

        public String getDescription(final int idx)
        {
            if(descriptions == null) return null;

            // Overall description
            if(idx < 0 || descriptions.length == 1) return descriptions[0];

            // Bounds check +1 because value descriptions start at index 1
            if(idx + 1 >= descriptions.length) return null;

            return descriptions[idx + 1];
        }

        public String getDescriptionHTML(final int idx)
        {
            final String desc = getDescription(idx);
            if(desc == null) return null;

            return CommandRegistry._genHTMLDesc(desc);
        }

    } // class OptionConfig

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class CommandConfig extends LinkedHashMap<String, OptionConfig> {

        public final String             optNADescription;
        public final boolean            optAlwaysMandatory;

        public final String             commandName;
        public final String             description;
        public final ArrayList<Integer> preselectOptions;
        public final ArrayList<String > preselectValues;

        public CommandConfig(final boolean optAlwaysMandatory_, final String commandName_, final String description_, final String optNADescription_)
        {
            final String desc = (description_ != null) ? description_.trim() : "";

            optNADescription   = optNADescription_;
            optAlwaysMandatory = optAlwaysMandatory_;

            commandName        = commandName_;
            description        = desc.isEmpty() ? null : desc;

            preselectOptions   = new ArrayList<>();
            preselectValues    = new ArrayList<>();
        }

        public CommandConfig(final boolean optAlwaysMandatory_, final String commandName_, final String description_)
        { this(optAlwaysMandatory_, commandName_, description_, null             ); }

        public CommandConfig(final String commandName_, final String description_, final String optNADescription_)
        { this(false              , commandName_, description_, optNADescription_); }

        public CommandConfig(final String commandName_, final String description_)
        { this(false              , commandName_, description_, null             ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public String getDescription()
        { return description; }

        public String getDescriptionHTML()
        { return (description == null) ? null : CommandRegistry._genHTMLDesc(description); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final ArrayList<String> _usageExample = new ArrayList<>();

        public void addUsageExample(final String msg)
        { _usageExample.add(msg); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void addOption(final String option_, final OptionConfig optionConfig_)
        { put(option_, optionConfig_); }

        public void addOption(final String option_, final boolean repeatable_, final AssignmentStyle style_, final String[] possibleValues_, final String[] descriptions_)
        { put( option_, new OptionConfig(repeatable_, style_              , possibleValues_, descriptions_) ); }

        public void addOption(final String option_, final boolean repeatable_, final AssignmentStyle style_, final String[] possibleValues_, final String   description_ )
        { put( option_, new OptionConfig(repeatable_, style_              , possibleValues_, description_) ); }

        public void addOption(final String option_,                            final AssignmentStyle style_, final String[] possibleValues_, final String[] descriptions_)
        { put( option_, new OptionConfig(false      , style_              , possibleValues_, descriptions_) ); }

        public void addOption(final String option_,                            final AssignmentStyle style_, final String[] possibleValues_, final String   description_ )
        { put( option_, new OptionConfig(false      , style_              , possibleValues_, description_) ); }

        public void addOption(final String option_, final boolean repeatable_, final AssignmentStyle style_,                                 final String   description_ )
        { put( option_, new OptionConfig(repeatable_, style_              , null           , description_) ); }

        public void addOption(final String option_,                            final AssignmentStyle style_,                                 final String   description_ )
        { put( option_, new OptionConfig(false      , style_              , null           , description_) ); }

        public void addOption(final String option_,                                                                                          final String   description_ )
        { put( option_, new OptionConfig(false      , AssignmentStyle.NONE, null           , description_) ); }

        public OptionConfig getOption(final String name)
        { return get(name); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void preselectOptions(final int... opts)
        {
            preselectOptions.clear();

            for(final int i : opts) preselectOptions.add(i);
        }

        public void preselectValues(final String... vals)
        {
            preselectValues.clear();

            for(final String i : vals) preselectValues.add(i);
        }

        public void preselectValues(final int... vals)
        {
            preselectValues.clear();

            for(final int i : vals) preselectValues.add( String.valueOf(i) );
        }

        /*
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public List<String> sortedKeySet()
        {
            final ArrayList<String> keys = new ArrayList<>( keySet() );

            Collections.sort(keys);

            return keys;
        }
        */

    } // class CommandConfig

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class CommandRegistry extends LinkedHashMap<String, CommandConfig> {

        public void registerCommand(final CommandConfig config)
        { put(config.commandName, config); }

        public CommandConfig getCommand(final String name)
        { return get(name); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static String ATTR_COLOR_RESET    = null;

        public static String ATTR_COLOR_CAPTION1 = null;
        public static String ATTR_COLOR_CAPTION2 = null;

        public static String ATTR_COLOR_COMMAND  = null;
        public static String ATTR_COLOR_OPTION   = null;
        public static String ATTR_COLOR_VALUE    = null;
        public static String ATTR_COLOR_OPERATOR = null;

        public static void resetHelpTextColors()
        {
            ATTR_COLOR_RESET    = ANSIScreenBuffer.ASeq_Attr_RstAll;

            ATTR_COLOR_CAPTION1 = ANSIScreenBuffer.ASeq_Attr_SetBrightWhite + ANSIScreenBuffer.ASeq_Attr_SetBold;
            ATTR_COLOR_CAPTION2 = ANSIScreenBuffer.ASeq_Attr_SetBrightBlue  + ANSIScreenBuffer.ASeq_Attr_SetBold;

            ATTR_COLOR_COMMAND  = ANSIScreenBuffer.ASeq_Attr_SetBrightYellow;
            ATTR_COLOR_OPTION   = ANSIScreenBuffer.ASeq_Attr_SetBrightGreen;
            ATTR_COLOR_VALUE    = ANSIScreenBuffer.ASeq_Attr_SetBrightCyan;
            ATTR_COLOR_OPERATOR = ANSIScreenBuffer.ASeq_Attr_SetBrightMagenta;
        }

        static {
            resetHelpTextColors();
        }

        private void _putOverallDesc(final StringBuilder sb, final String desc)
        {
            if(desc == null) return;

            boolean insideBraces = false;

            for(int i = 0; i < desc.length(); ++i) {

                final char c = desc.charAt(i);

                switch (c) {

                    case '{':
                        sb.append(ATTR_COLOR_OPERATOR)
                          .append(c                  )
                          .append(ATTR_COLOR_VALUE  );
                        insideBraces = true;
                        break;

                    case '}':
                        sb.append(ATTR_COLOR_OPERATOR)
                          .append(c                  )
                          .append(ATTR_COLOR_RESET   );
                        insideBraces = false;
                        break;

                    case '|':
                        if(insideBraces) {
                            sb.append(ATTR_COLOR_OPERATOR)
                              .append(c                  )
                              .append(ATTR_COLOR_VALUE   );
                        }
                        else {
                            sb.append(c);
                        }
                        break;

                    default:
                        sb.append(c);
                        break;

                } // switch

            } // for

            if(insideBraces) sb.append(ATTR_COLOR_RESET); // Ensure reset at end
        }

        public List<String> sortedKeySet()
        {
            final ArrayList<String> keys = new ArrayList<>( keySet() );

            Collections.sort(keys);

            return keys;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static String _spaces(final int count)
        {
            final char[] arr = new char[count];

            Arrays.fill(arr, ' ');

            return new String(arr);
        }

        private static String _genDesc_impl(final String str, final boolean html)
        {
            if( str == null || str.isEmpty() ) return "";

            final StringBuilder sb     = new StringBuilder();
            final String[]      tokens = ( html ? XCom.escapeHTML(str) : str ).split(" ");

            if(html) sb.append("<html><b>");

            String deffered = null;

            for(int i = 0; i < tokens.length;) {

                String t = deffered;
                if(t == null) {
                    t = tokens[i++];
                }
                deffered = null;

                     if( t.startsWith("\\@") ) {                                                sb.append( t.substring(1) );                                              }
                else if( t.startsWith("\\:") ) {                                                sb.append( t.substring(1) );                                              }
                else if( t.startsWith("\\=") ) {                                                sb.append( t.substring(1) );                                              }
                else if( t.startsWith("\\+") ) {                                                sb.append( t.substring(1) );                                              }
                else if( t.startsWith("\\`") ) {                                                sb.append( t.substring(1) );                                              }
                else if( t.startsWith("@"  ) ) { sb.append(html ? "<i>" : ATTR_COLOR_COMMAND ); sb.append( t.substring(1) ); sb.append(html ? "</i>" : ATTR_COLOR_RESET); }
                else if( t.startsWith(":"  ) ) { sb.append(html ? "<i>" : ATTR_COLOR_OPTION  ); sb.append( t.substring(1) ); sb.append(html ? "</i>" : ATTR_COLOR_RESET); }
                else if( t.startsWith("="  ) ) { sb.append(html ? "<i>" : ATTR_COLOR_VALUE   ); sb.append( t.substring(1) ); sb.append(html ? "</i>" : ATTR_COLOR_RESET); }
                else if( t.startsWith("+"  ) ) { sb.append(html ? "<i>" : ATTR_COLOR_OPERATOR); sb.append( t.substring(1) ); sb.append(html ? "</i>" : ATTR_COLOR_RESET); }
                else if( t.startsWith("`"  ) ) {                                                deffered = t.substring(1)  ;                                              }
                else                           {                                                sb.append( t              );                                              }

                if(deffered != null) {
                    final int sbl = sb.length();
                    if( sbl > 0 && sb.charAt(sbl - 1) == ' ' ) sb.deleteCharAt(sbl - 1);
                    continue;
                }

                if( sb.length() > 0 ) sb.append(' ');

            } // for

            if(deffered != null) sb.append(deffered);

            if(html) sb.append("</b></html>");

            return sb.toString();
        }

        private static String _colorizeDesc(final String str)
        { return _genDesc_impl(str, false); }

        private static String _genHTMLDesc(final String str)
        { return _genDesc_impl(str, true); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static String _generateHelpString_cache = null;

        private String _generateHelpString_impl()
        {
            final String        asgnEquals = "=<arg>";
            final String        asgnSpace  = " <arg>";
            final String        repeatable = "...";

            final StringBuilder sb         = new StringBuilder();

            sb.append(ATTR_COLOR_RESET);

            for( CommandConfig cmd : values() ) {

                // Command header
                final int indentWidth = cmd.commandName.length() + 3; // 3 for " - "

                sb.append(ATTR_COLOR_COMMAND).append(cmd.commandName).append(ATTR_COLOR_RESET);
                if( cmd.description != null && !cmd.description.isEmpty() ) {
                    final String indentCmd = "\n" + _spaces(indentWidth);
                    sb.append(" - ").append( _colorizeDesc(cmd.description).replace("\n", indentCmd) );
                }
                sb.append('\n');

                if( cmd.isEmpty() ) {
                    sb.append('\n');
                    continue;
                }

                // Find maximum option name length for alignment
                int maxOptNameLen = 0;

                for( final Map.Entry<String, OptionConfig> entry : cmd.entrySet() ) {

                    final String          optName = entry.getKey  ();
                    final OptionConfig    opt     = entry.getValue();
                          int             optNLen = optName.length();

                    if(opt.style == AssignmentStyle.EQUALS || opt.style == AssignmentStyle.SPACE) optNLen += asgnEquals.length();
                    if(opt.repeatable) optNLen += repeatable.length();

                    if(optNLen > maxOptNameLen) maxOptNameLen = optNLen;

                } // for

                // Options
                for( final Map.Entry<String, OptionConfig> entry : cmd.entrySet() ) {

                    final String       optName = entry.getKey  ();
                    final OptionConfig opt     = entry.getValue();
                          String       optAsgn = "";

                    // Render option name according to style
                    switch(opt.style) {

                        case EQUALS:
                            optAsgn = asgnEquals;
                            break;

                        case SPACE:
                            optAsgn = asgnSpace;
                            break;

                        default:
                            break;

                    } // switch

                    // Option name padded
                    sb.append("    ") .append( String.format(
                        "%s%-" + maxOptNameLen + "s%s",
                        ATTR_COLOR_OPTION, optName + optAsgn + (opt.repeatable ? repeatable : ""), ATTR_COLOR_RESET
                    ) );

                    // Overall description
                    final String indentOpt   = "\n    " + _spaces(maxOptNameLen + 3); // 3 for " : "
                    final String overallDesc = _colorizeDesc( opt.getDescription(-1).replace("\n", indentOpt) );

                    if(overallDesc != null) {
                        sb.append(" : ");
                        _putOverallDesc(sb, overallDesc);
                    }
                    sb.append('\n');

                    // Possible values with descriptions
                    if(opt.possibleValues != null) {

                        // Find maximum option value length for alignment
                        int maxOptValLen = 0;

                        for(final String pv : opt.possibleValues) {

                            final int valNLen = pv.length();

                            if(valNLen > maxOptValLen) maxOptValLen = valNLen;

                        } // for

                        final String indentVal = "\n        " + _spaces(maxOptValLen + 3); // 3 for " = ";

                        for(int i = 0; i < opt.possibleValues.length; ++i) {

                            final String val     = opt.possibleValues[i];
                            final String valDesc = _colorizeDesc( opt.getDescription(i).replace("\n", indentVal) );

                            sb.append("        ").append( String.format(
                                                              "%s%-" + maxOptValLen + "s%s",
                                                              ATTR_COLOR_VALUE, val, ATTR_COLOR_RESET
                                                        ) );

                            if(valDesc != null) sb.append(" = ").append(valDesc);

                            sb.append('\n');

                        } // for
                    }

                } // for

                if( !cmd._usageExample.isEmpty() ) {
                    sb.append('\n').append(ATTR_COLOR_RESET).append("    ").append(Texts.ScrEdt_Examples).append('\n');
                    for(final String msg : cmd._usageExample) sb.append("        ").append(msg).append('\n');
                    sb.append('\n');
                }
                else {
                    sb.append('\n').append(ATTR_COLOR_RESET);
                }

            } // for

            return XCom.stripTrailingNewlines( sb.toString() ) + '\n';
        }

        public String generateHelpString()
        {
            if(_generateHelpString_cache == null) _generateHelpString_cache = _generateHelpString_impl();

            return _generateHelpString_cache;
        }

    } // class CommandRegistry

} // class CommandSpecifier
