/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;


/*
 * Special prefix characters for menu item texts:
 *     "~"         : Creates the menu item as disabled by default
 *     "*" or "*-" : Creates the menu item as a checkbox     (unchecked  by default)
 *            "*+" : Creates the menu item as a checkbox     (checked    by default)
 *     "^" or "^-" : Creates the menu item as a radio button (unselected by default)
 *            "^+" : Creates the menu item as a radio button (selected   by default)
 * NOTE : Radio buttons are grouped using 'MenuSpec.Separator'.
 */
public class MenuSpec {

    public static final int VK_0                         = KeyEvent.VK_0                        ; //    48 (0x0030)
    public static final int VK_1                         = KeyEvent.VK_1                        ; //    49 (0x0031)
    public static final int VK_2                         = KeyEvent.VK_2                        ; //    50 (0x0032)
    public static final int VK_3                         = KeyEvent.VK_3                        ; //    51 (0x0033)
    public static final int VK_4                         = KeyEvent.VK_4                        ; //    52 (0x0034)
    public static final int VK_5                         = KeyEvent.VK_5                        ; //    53 (0x0035)
    public static final int VK_6                         = KeyEvent.VK_6                        ; //    54 (0x0036)
    public static final int VK_7                         = KeyEvent.VK_7                        ; //    55 (0x0037)
    public static final int VK_8                         = KeyEvent.VK_8                        ; //    56 (0x0038)
    public static final int VK_9                         = KeyEvent.VK_9                        ; //    57 (0x0039)
    public static final int VK_A                         = KeyEvent.VK_A                        ; //    65 (0x0041)
    public static final int VK_ACCEPT                    = KeyEvent.VK_ACCEPT                   ; //    30 (0x001E)
    public static final int VK_ADD                       = KeyEvent.VK_ADD                      ; //   107 (0x006B)
    public static final int VK_AGAIN                     = KeyEvent.VK_AGAIN                    ; // 65481 (0xFFC9)
    public static final int VK_ALL_CANDIDATES            = KeyEvent.VK_ALL_CANDIDATES           ; //   256 (0x0100)
    public static final int VK_ALPHANUMERIC              = KeyEvent.VK_ALPHANUMERIC             ; //   240 (0x00F0)
    public static final int VK_ALT                       = KeyEvent.VK_ALT                      ; //    18 (0x0012)
    public static final int VK_ALT_GRAPH                 = KeyEvent.VK_ALT_GRAPH                ; // 65406 (0xFF7E)
    public static final int VK_AMPERSAND                 = KeyEvent.VK_AMPERSAND                ; //   150 (0x0096)
    public static final int VK_ASTERISK                  = KeyEvent.VK_ASTERISK                 ; //   151 (0x0097)
    public static final int VK_AT                        = KeyEvent.VK_AT                       ; //   512 (0x0200)
    public static final int VK_B                         = KeyEvent.VK_B                        ; //    66 (0x0042)
    public static final int VK_BACK_QUOTE                = KeyEvent.VK_BACK_QUOTE               ; //   192 (0x00C0)
    public static final int VK_BACK_SLASH                = KeyEvent.VK_BACK_SLASH               ; //    92 (0x005C)
    public static final int VK_BACK_SPACE                = KeyEvent.VK_BACK_SPACE               ; //     8 (0x0008)
    public static final int VK_BEGIN                     = KeyEvent.VK_BEGIN                    ; // 65368 (0xFF58)
    public static final int VK_BRACELEFT                 = KeyEvent.VK_BRACELEFT                ; //   161 (0x00A1)
    public static final int VK_BRACERIGHT                = KeyEvent.VK_BRACERIGHT               ; //   162 (0x00A2)
    public static final int VK_C                         = KeyEvent.VK_C                        ; //    67 (0x0043)
    public static final int VK_CANCEL                    = KeyEvent.VK_CANCEL                   ; //     3 (0x0003)
    public static final int VK_CAPS_LOCK                 = KeyEvent.VK_CAPS_LOCK                ; //    20 (0x0014)
    public static final int VK_CIRCUMFLEX                = KeyEvent.VK_CIRCUMFLEX               ; //   514 (0x0202)
    public static final int VK_CLEAR                     = KeyEvent.VK_CLEAR                    ; //    12 (0x000C)
    public static final int VK_CLOSE_BRACKET             = KeyEvent.VK_CLOSE_BRACKET            ; //    93 (0x005D)
    public static final int VK_CODE_INPUT                = KeyEvent.VK_CODE_INPUT               ; //   258 (0x0102)
    public static final int VK_COLON                     = KeyEvent.VK_COLON                    ; //   513 (0x0201)
    public static final int VK_COMMA                     = KeyEvent.VK_COMMA                    ; //    44 (0x002C)
    public static final int VK_COMPOSE                   = KeyEvent.VK_COMPOSE                  ; // 65312 (0xFF20)
    public static final int VK_CONTEXT_MENU              = KeyEvent.VK_CONTEXT_MENU             ; //   525 (0x020D)
    public static final int VK_CONTROL                   = KeyEvent.VK_CONTROL                  ; //    17 (0x0011)
    public static final int VK_CONVERT                   = KeyEvent.VK_CONVERT                  ; //    28 (0x001C)
    public static final int VK_COPY                      = KeyEvent.VK_COPY                     ; // 65485 (0xFFCD)
    public static final int VK_CUT                       = KeyEvent.VK_CUT                      ; // 65489 (0xFFD1)
    public static final int VK_D                         = KeyEvent.VK_D                        ; //    68 (0x0044)
    public static final int VK_DEAD_ABOVEDOT             = KeyEvent.VK_DEAD_ABOVEDOT            ; //   134 (0x0086)
    public static final int VK_DEAD_ABOVERING            = KeyEvent.VK_DEAD_ABOVERING           ; //   136 (0x0088)
    public static final int VK_DEAD_ACUTE                = KeyEvent.VK_DEAD_ACUTE               ; //   129 (0x0081)
    public static final int VK_DEAD_BREVE                = KeyEvent.VK_DEAD_BREVE               ; //   133 (0x0085)
    public static final int VK_DEAD_CARON                = KeyEvent.VK_DEAD_CARON               ; //   138 (0x008A)
    public static final int VK_DEAD_CEDILLA              = KeyEvent.VK_DEAD_CEDILLA             ; //   139 (0x008B)
    public static final int VK_DEAD_CIRCUMFLEX           = KeyEvent.VK_DEAD_CIRCUMFLEX          ; //   130 (0x0082)
    public static final int VK_DEAD_DIAERESIS            = KeyEvent.VK_DEAD_DIAERESIS           ; //   135 (0x0087)
    public static final int VK_DEAD_DOUBLEACUTE          = KeyEvent.VK_DEAD_DOUBLEACUTE         ; //   137 (0x0089)
    public static final int VK_DEAD_GRAVE                = KeyEvent.VK_DEAD_GRAVE               ; //   128 (0x0080)
    public static final int VK_DEAD_IOTA                 = KeyEvent.VK_DEAD_IOTA                ; //   141 (0x008D)
    public static final int VK_DEAD_MACRON               = KeyEvent.VK_DEAD_MACRON              ; //   132 (0x0084)
    public static final int VK_DEAD_OGONEK               = KeyEvent.VK_DEAD_OGONEK              ; //   140 (0x008C)
    public static final int VK_DEAD_SEMIVOICED_SOUND     = KeyEvent.VK_DEAD_SEMIVOICED_SOUND    ; //   143 (0x008F)
    public static final int VK_DEAD_TILDE                = KeyEvent.VK_DEAD_TILDE               ; //   131 (0x0083)
    public static final int VK_DEAD_VOICED_SOUND         = KeyEvent.VK_DEAD_VOICED_SOUND        ; //   142 (0x008E)
    public static final int VK_DECIMAL                   = KeyEvent.VK_DECIMAL                  ; //   110 (0x006E)
    public static final int VK_DELETE                    = KeyEvent.VK_DELETE                   ; //   127 (0x007F)
    public static final int VK_DIVIDE                    = KeyEvent.VK_DIVIDE                   ; //   111 (0x006F)
    public static final int VK_DOLLAR                    = KeyEvent.VK_DOLLAR                   ; //   515 (0x0203)
    public static final int VK_DOWN                      = KeyEvent.VK_DOWN                     ; //    40 (0x0028)
    public static final int VK_E                         = KeyEvent.VK_E                        ; //    69 (0x0045)
    public static final int VK_END                       = KeyEvent.VK_END                      ; //    35 (0x0023)
    public static final int VK_ENTER                     = KeyEvent.VK_ENTER                    ; //    10 (0x000A)
    public static final int VK_EQUALS                    = KeyEvent.VK_EQUALS                   ; //    61 (0x003D)
    public static final int VK_ESCAPE                    = KeyEvent.VK_ESCAPE                   ; //    27 (0x001B)
    public static final int VK_EURO_SIGN                 = KeyEvent.VK_EURO_SIGN                ; //   516 (0x0204)
    public static final int VK_EXCLAMATION_MARK          = KeyEvent.VK_EXCLAMATION_MARK         ; //   517 (0x0205)
    public static final int VK_F                         = KeyEvent.VK_F                        ; //    70 (0x0046)
    public static final int VK_F1                        = KeyEvent.VK_F1                       ; //   112 (0x0070)
    public static final int VK_F10                       = KeyEvent.VK_F10                      ; //   121 (0x0079)
    public static final int VK_F11                       = KeyEvent.VK_F11                      ; //   122 (0x007A)
    public static final int VK_F12                       = KeyEvent.VK_F12                      ; //   123 (0x007B)
    public static final int VK_F13                       = KeyEvent.VK_F13                      ; // 61440 (0xF000)
    public static final int VK_F14                       = KeyEvent.VK_F14                      ; // 61441 (0xF001)
    public static final int VK_F15                       = KeyEvent.VK_F15                      ; // 61442 (0xF002)
    public static final int VK_F16                       = KeyEvent.VK_F16                      ; // 61443 (0xF003)
    public static final int VK_F17                       = KeyEvent.VK_F17                      ; // 61444 (0xF004)
    public static final int VK_F18                       = KeyEvent.VK_F18                      ; // 61445 (0xF005)
    public static final int VK_F19                       = KeyEvent.VK_F19                      ; // 61446 (0xF006)
    public static final int VK_F2                        = KeyEvent.VK_F2                       ; //   113 (0x0071)
    public static final int VK_F20                       = KeyEvent.VK_F20                      ; // 61447 (0xF007)
    public static final int VK_F21                       = KeyEvent.VK_F21                      ; // 61448 (0xF008)
    public static final int VK_F22                       = KeyEvent.VK_F22                      ; // 61449 (0xF009)
    public static final int VK_F23                       = KeyEvent.VK_F23                      ; // 61450 (0xF00A)
    public static final int VK_F24                       = KeyEvent.VK_F24                      ; // 61451 (0xF00B)
    public static final int VK_F3                        = KeyEvent.VK_F3                       ; //   114 (0x0072)
    public static final int VK_F4                        = KeyEvent.VK_F4                       ; //   115 (0x0073)
    public static final int VK_F5                        = KeyEvent.VK_F5                       ; //   116 (0x0074)
    public static final int VK_F6                        = KeyEvent.VK_F6                       ; //   117 (0x0075)
    public static final int VK_F7                        = KeyEvent.VK_F7                       ; //   118 (0x0076)
    public static final int VK_F8                        = KeyEvent.VK_F8                       ; //   119 (0x0077)
    public static final int VK_F9                        = KeyEvent.VK_F9                       ; //   120 (0x0078)
    public static final int VK_FINAL                     = KeyEvent.VK_FINAL                    ; //    24 (0x0018)
    public static final int VK_FIND                      = KeyEvent.VK_FIND                     ; // 65488 (0xFFD0)
    public static final int VK_FULL_WIDTH                = KeyEvent.VK_FULL_WIDTH               ; //   243 (0x00F3)
    public static final int VK_G                         = KeyEvent.VK_G                        ; //    71 (0x0047)
    public static final int VK_GREATER                   = KeyEvent.VK_GREATER                  ; //   160 (0x00A0)
    public static final int VK_H                         = KeyEvent.VK_H                        ; //    72 (0x0048)
    public static final int VK_HALF_WIDTH                = KeyEvent.VK_HALF_WIDTH               ; //   244 (0x00F4)
    public static final int VK_HELP                      = KeyEvent.VK_HELP                     ; //   156 (0x009C)
    public static final int VK_HIRAGANA                  = KeyEvent.VK_HIRAGANA                 ; //   242 (0x00F2)
    public static final int VK_HOME                      = KeyEvent.VK_HOME                     ; //    36 (0x0024)
    public static final int VK_I                         = KeyEvent.VK_I                        ; //    73 (0x0049)
    public static final int VK_INPUT_METHOD_ON_OFF       = KeyEvent.VK_INPUT_METHOD_ON_OFF      ; //   263 (0x0107)
    public static final int VK_INSERT                    = KeyEvent.VK_INSERT                   ; //   155 (0x009B)
    public static final int VK_INVERTED_EXCLAMATION_MARK = KeyEvent.VK_INVERTED_EXCLAMATION_MARK; //   518 (0x0206)
    public static final int VK_J                         = KeyEvent.VK_J                        ; //    74 (0x004A)
    public static final int VK_JAPANESE_HIRAGANA         = KeyEvent.VK_JAPANESE_HIRAGANA        ; //   260 (0x0104)
    public static final int VK_JAPANESE_KATAKANA         = KeyEvent.VK_JAPANESE_KATAKANA        ; //   259 (0x0103)
    public static final int VK_JAPANESE_ROMAN            = KeyEvent.VK_JAPANESE_ROMAN           ; //   261 (0x0105)
    public static final int VK_K                         = KeyEvent.VK_K                        ; //    75 (0x004B)
    public static final int VK_KANA                      = KeyEvent.VK_KANA                     ; //    21 (0x0015)
    public static final int VK_KANA_LOCK                 = KeyEvent.VK_KANA_LOCK                ; //   262 (0x0106)
    public static final int VK_KANJI                     = KeyEvent.VK_KANJI                    ; //    25 (0x0019)
    public static final int VK_KATAKANA                  = KeyEvent.VK_KATAKANA                 ; //   241 (0x00F1)
    public static final int VK_KP_DOWN                   = KeyEvent.VK_KP_DOWN                  ; //   225 (0x00E1)
    public static final int VK_KP_LEFT                   = KeyEvent.VK_KP_LEFT                  ; //   226 (0x00E2)
    public static final int VK_KP_RIGHT                  = KeyEvent.VK_KP_RIGHT                 ; //   227 (0x00E3)
    public static final int VK_KP_UP                     = KeyEvent.VK_KP_UP                    ; //   224 (0x00E0)
    public static final int VK_L                         = KeyEvent.VK_L                        ; //    76 (0x004C)
    public static final int VK_LEFT                      = KeyEvent.VK_LEFT                     ; //    37 (0x0025)
    public static final int VK_LEFT_PARENTHESIS          = KeyEvent.VK_LEFT_PARENTHESIS         ; //   519 (0x0207)
    public static final int VK_LESS                      = KeyEvent.VK_LESS                     ; //   153 (0x0099)
    public static final int VK_M                         = KeyEvent.VK_M                        ; //    77 (0x004D)
    public static final int VK_META                      = KeyEvent.VK_META                     ; //   157 (0x009D)
    public static final int VK_MINUS                     = KeyEvent.VK_MINUS                    ; //    45 (0x002D)
    public static final int VK_MODECHANGE                = KeyEvent.VK_MODECHANGE               ; //    31 (0x001F)
    public static final int VK_MULTIPLY                  = KeyEvent.VK_MULTIPLY                 ; //   106 (0x006A)
    public static final int VK_N                         = KeyEvent.VK_N                        ; //    78 (0x004E)
    public static final int VK_NONCONVERT                = KeyEvent.VK_NONCONVERT               ; //    29 (0x001D)
    public static final int VK_NUMBER_SIGN               = KeyEvent.VK_NUMBER_SIGN              ; //   520 (0x0208)
    public static final int VK_NUMPAD0                   = KeyEvent.VK_NUMPAD0                  ; //    96 (0x0060)
    public static final int VK_NUMPAD1                   = KeyEvent.VK_NUMPAD1                  ; //    97 (0x0061)
    public static final int VK_NUMPAD2                   = KeyEvent.VK_NUMPAD2                  ; //    98 (0x0062)
    public static final int VK_NUMPAD3                   = KeyEvent.VK_NUMPAD3                  ; //    99 (0x0063)
    public static final int VK_NUMPAD4                   = KeyEvent.VK_NUMPAD4                  ; //   100 (0x0064)
    public static final int VK_NUMPAD5                   = KeyEvent.VK_NUMPAD5                  ; //   101 (0x0065)
    public static final int VK_NUMPAD6                   = KeyEvent.VK_NUMPAD6                  ; //   102 (0x0066)
    public static final int VK_NUMPAD7                   = KeyEvent.VK_NUMPAD7                  ; //   103 (0x0067)
    public static final int VK_NUMPAD8                   = KeyEvent.VK_NUMPAD8                  ; //   104 (0x0068)
    public static final int VK_NUMPAD9                   = KeyEvent.VK_NUMPAD9                  ; //   105 (0x0069)
    public static final int VK_NUM_LOCK                  = KeyEvent.VK_NUM_LOCK                 ; //   144 (0x0090)
    public static final int VK_O                         = KeyEvent.VK_O                        ; //    79 (0x004F)
    public static final int VK_OPEN_BRACKET              = KeyEvent.VK_OPEN_BRACKET             ; //    91 (0x005B)
    public static final int VK_P                         = KeyEvent.VK_P                        ; //    80 (0x0050)
    public static final int VK_PAGE_DOWN                 = KeyEvent.VK_PAGE_DOWN                ; //    34 (0x0022)
    public static final int VK_PAGE_UP                   = KeyEvent.VK_PAGE_UP                  ; //    33 (0x0021)
    public static final int VK_PASTE                     = KeyEvent.VK_PASTE                    ; // 65487 (0xFFCF)
    public static final int VK_PAUSE                     = KeyEvent.VK_PAUSE                    ; //    19 (0x0013)
    public static final int VK_PERIOD                    = KeyEvent.VK_PERIOD                   ; //    46 (0x002E)
    public static final int VK_PLUS                      = KeyEvent.VK_PLUS                     ; //   521 (0x0209)
    public static final int VK_PREVIOUS_CANDIDATE        = KeyEvent.VK_PREVIOUS_CANDIDATE       ; //   257 (0x0101)
    public static final int VK_PRINTSCREEN               = KeyEvent.VK_PRINTSCREEN              ; //   154 (0x009A)
    public static final int VK_PROPS                     = KeyEvent.VK_PROPS                    ; // 65482 (0xFFCA)
    public static final int VK_Q                         = KeyEvent.VK_Q                        ; //    81 (0x0051)
    public static final int VK_QUOTE                     = KeyEvent.VK_QUOTE                    ; //   222 (0x00DE)
    public static final int VK_QUOTEDBL                  = KeyEvent.VK_QUOTEDBL                 ; //   152 (0x0098)
    public static final int VK_R                         = KeyEvent.VK_R                        ; //    82 (0x0052)
    public static final int VK_RIGHT                     = KeyEvent.VK_RIGHT                    ; //    39 (0x0027)
    public static final int VK_RIGHT_PARENTHESIS         = KeyEvent.VK_RIGHT_PARENTHESIS        ; //   522 (0x020A)
    public static final int VK_ROMAN_CHARACTERS          = KeyEvent.VK_ROMAN_CHARACTERS         ; //   245 (0x00F5)
    public static final int VK_S                         = KeyEvent.VK_S                        ; //    83 (0x0053)
    public static final int VK_SCROLL_LOCK               = KeyEvent.VK_SCROLL_LOCK              ; //   145 (0x0091)
    public static final int VK_SEMICOLON                 = KeyEvent.VK_SEMICOLON                ; //    59 (0x003B)
    public static final int VK_SEPARATER                 = KeyEvent.VK_SEPARATER                ; //   108 (0x006C)
    public static final int VK_SEPARATOR                 = KeyEvent.VK_SEPARATOR                ; //   108 (0x006C)
    public static final int VK_SHIFT                     = KeyEvent.VK_SHIFT                    ; //    16 (0x0010)
    public static final int VK_SLASH                     = KeyEvent.VK_SLASH                    ; //    47 (0x002F)
    public static final int VK_SPACE                     = KeyEvent.VK_SPACE                    ; //    32 (0x0020)
    public static final int VK_STOP                      = KeyEvent.VK_STOP                     ; // 65480 (0xFFC8)
    public static final int VK_SUBTRACT                  = KeyEvent.VK_SUBTRACT                 ; //   109 (0x006D)
    public static final int VK_T                         = KeyEvent.VK_T                        ; //    84 (0x0054)
    public static final int VK_TAB                       = KeyEvent.VK_TAB                      ; //     9 (0x0009)
    public static final int VK_U                         = KeyEvent.VK_U                        ; //    85 (0x0055)
    public static final int VK_UNDEFINED                 = KeyEvent.VK_UNDEFINED                ; //     0 (0x0000)
    public static final int VK_UNDERSCORE                = KeyEvent.VK_UNDERSCORE               ; //   523 (0x020B)
    public static final int VK_UNDO                      = KeyEvent.VK_UNDO                     ; // 65483 (0xFFCB)
    public static final int VK_UP                        = KeyEvent.VK_UP                       ; //    38 (0x0026)
    public static final int VK_V                         = KeyEvent.VK_V                        ; //    86 (0x0056)
    public static final int VK_W                         = KeyEvent.VK_W                        ; //    87 (0x0057)
    public static final int VK_WINDOWS                   = KeyEvent.VK_WINDOWS                  ; //   524 (0x020C)
    public static final int VK_X                         = KeyEvent.VK_X                        ; //    88 (0x0058)
    public static final int VK_Y                         = KeyEvent.VK_Y                        ; //    89 (0x0059)
    public static final int VK_Z                         = KeyEvent.VK_Z                        ; //    90 (0x005A)

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int KModNone         = 0;
    public static final int KModShift        =                                                 InputEvent.SHIFT_DOWN_MASK;
    public static final int KModCtrl         = SysUtil.osIsMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK ;
    public static final int KModAlt          =                                                 InputEvent.ALT_DOWN_MASK  ;

    public static final int KModCtrlShift    = KModCtrl           | KModShift;
    public static final int KModCtrlAlt      = KModCtrl | KModAlt            ;
    public static final int KModCtrlAltShift = KModCtrl | KModAlt | KModShift;
    public static final int KModAltShift     =            KModAlt | KModShift;

    public static String printKeyCombination(final String key, final int modifiers)
    {
        String kc = "";

        if( (modifiers & KModCtrl ) != 0 ) kc += ( kc.isEmpty() ? "" : "+" ) + ( SysUtil.osIsMac() ? "⌘" : "Ctrl " ); // ⎈
        if( (modifiers & KModAlt  ) != 0 ) kc += ( kc.isEmpty() ? "" : "+" ) + ( SysUtil.osIsMac() ? "⌥" : "Alt"   ); // ⎇
        if( (modifiers & KModShift) != 0 ) kc += ( kc.isEmpty() ? "" : "+" ) + ( SysUtil.osIsMac() ? "⇧" : "Shift" ); // ⇧
                                           kc += ( kc.isEmpty() ? "" : "+" ) + ( key                               );

        return kc;
    }
    public static String printKeyCombination(final String format, final String key, final int modifiers)
    { return String.format( format, printKeyCombination(key, modifiers) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _nullCheck(final String string)
    { return ( string == null || string.trim().isEmpty() ) ? null : string.trim(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public final String    id;
    public final String    icon;
    public final String    text;
    public final int       mnemonic;
    public final KeyStroke accelerator;
    public final String    action;

    public MenuSpec(String id_, final String icon_, final String text_, final int mnemonic_, final KeyStroke accelerator_, final String action_)
    {
        id          = _nullCheck(id_    );
        icon        = _nullCheck(icon_  );
        text        = _nullCheck(text_  );
        mnemonic    = (mnemonic_ <= 0) ? -1 : mnemonic_;
        accelerator = accelerator_;
        action      = _nullCheck(action_);
    }

    public MenuSpec(final String id_, final String icon_, final String text_, final int mnemonic_, final String action_)
    { this(id_ , icon_, text_, mnemonic_, null        , action_); }

    public MenuSpec(final String id_, final String icon_, final String text_, final KeyStroke accelerator_, final String action_)
    { this(id_ , icon_, text_, -1       , accelerator_, action_); }

    public MenuSpec(final String id_, final String icon_, final String text_, final String action_)
    { this(id_ , icon_, text_, -1       , null        , action_); }

    public MenuSpec(final String id_, final String text_, final int mnemonic_, final String action_)
    { this(id_ , null , text_, mnemonic_, null        , action_); }

    public MenuSpec(final String id_, final String text_, final int mnemonic_)
    { this(id_ , null , text_, mnemonic_, null        , null   ); }

    public MenuSpec(final String text_, final int mnemonic_, final String action_)
    { this(null, null , text_, mnemonic_, null        , action_); }

    public MenuSpec(final String text_, final int mnemonic_)
    { this(null, null , text_, mnemonic_, null        , null   ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isMenu()
    { return               icon == null && !"-".equals(text) && mnemonic != -1 && accelerator == null                  ; }

    public boolean isSeparator()
    { return id == null && icon == null &&  "-".equals(text) && mnemonic == -1 && accelerator == null && action == null; }

    public boolean isItem()
    { return               icon != null && !"-".equals(text)                                          && action != null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final MenuSpec Separator = new MenuSpec("-", -1);

} // class MenuSpec
