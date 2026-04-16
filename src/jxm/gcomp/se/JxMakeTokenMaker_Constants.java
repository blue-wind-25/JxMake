/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jxm.annotation.*;

import static jxm.gcomp.se.JxMakeTheme.TType;


@package_private
class JxMakeTokenMaker_Constants {

    private static Map<String, Integer> toUnmodifiableMap(final Object[][] entries)
    {
        return Collections.unmodifiableMap(
            Stream.of(entries).collect( Collectors.toMap(
                e -> (String ) e[0],
                e -> (Integer) e[1]
            ) )
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Standard operators
    public static final Map<String, Integer> _ROperator;
    public static final Map<String, Integer> _SOperator;

    static {

        final Object[][] _entryROperator = {
            {   "=", TType.ROperator },
            {  "+=", TType.ROperator },
            {  "?=", TType.ROperator },
            {  ":=", TType.ROperator },
            { "+:=", TType.ROperator },
            { "?:=", TType.ROperator },
            { ":+=", TType.ROperator },
            { ":?=", TType.ROperator },
            {   "(", TType.ROperator },
            {   ")", TType.ROperator },
            {   ",", TType.ROperator },
            {  "<" , TType.ROperator },
            {  "<=", TType.ROperator },
            {  ">" , TType.ROperator },
            {  ">=", TType.ROperator },
            {  "==", TType.ROperator },
            {  "!=", TType.ROperator },
            { "&==", TType.ROperator },
            { "&!=", TType.ROperator },
        };

        final Object[][] _entrySOperator = {
            {  ":", TType.SOperator },
            {  "?", TType.SOperator },
            {  "%", TType.SOperator },
            {  "!", TType.SOperator },
            {  "&", TType.SOperator },
            { "!&", TType.SOperator },
        };

        _ROperator = toUnmodifiableMap(_entryROperator);
        _SOperator = toUnmodifiableMap(_entrySOperator);

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Standard operators
    public static final Set<String> _EvalOper;
    public static final Set<String> _EvalFunc;

    static {

        _EvalOper = Collections.unmodifiableSet( Stream.of(
              "(",  ")", "[",  "]",
              "+",  "-", "@",
              "~",  "!",
             "**",  "!",
              "*",  "/", "%",
             "<<", ">>",
            "<=>",
              "<", "<=", ">", ">=",
             "==", "!=",
              "&",  "^", "|",
             "&&", "||",
              "?",  ":",
            ""
        ).collect( Collectors.toSet() ) );

        _EvalFunc = Collections.unmodifiableSet( Stream.of(
            "sgn",
            "min",
            "max",
            ""
        ).collect( Collectors.toSet() ) );

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Built-in function names
    public static final Set<String> _BIFName;

    static {

        _BIFName = Collections.unmodifiableSet( Stream.of(
            "call"              ,
            "exec"              ,
            "add_target"        ,
            "add_extradep"      ,
            "has_var"           ,
            "nop"               ,
            "alt_glibc_for"     ,
            "sh_delay"          ,
            "sh_restore"        ,
            "cmd_echo_off"      ,
            "cmd_echo_on"       ,
            "cmd_streaming_off" ,
            "cmd_streaming_on"  ,
            "cmd_stderr_chk_off",
            "cmd_stderr_chk_on" ,
            "cmd_stdout_chk_off",
            "cmd_stdout_chk_on" ,
            "cmd_clear_state"   ,
            "silent_sem"        ,
            "restore_sem"       ,
            "micros"            ,
            "millis"            ,
            "datetime"          ,
            "sleep"             ,
            "getenv"            ,
            "clear_project"     ,
            "exit"              ,
            "cwd"               ,
            "ptd"               ,
            "uhd"               ,
            "jdd"               ,
            "jtd"               ,
            "jxd"               ,
            "set_rwx3"          ,
            "cat_paths"         ,
            "cat_path"          ,
            "abs_path"          ,
            "rel_path"          ,
            "valid_path"        ,
            "uptodate_path"     ,
            "newer_path"        ,
            "same_path"         ,
            "path_is_abs"       ,
            "path_is_rel"       ,
            "path_is_file"      ,
            "path_is_directory" ,
            "path_is_symlink"   ,
            "path_is_readable"  ,
            "path_is_writable"  ,
            "path_is_executable",
            "path_last_part"    ,
            "path_rm_last_part" ,
            "path_ndsep"        ,
            "symlink_target"    ,
            "symlink_real_apath",
            "symlink_resolve"   ,
            "dir_name"          ,
            "file_name"         ,
            "file_ext"          ,
            "file_mime_type"    ,
            "touch"             ,
            "rmfiles_rec"       ,
            "rmfiles"           ,
            "rmfile"            ,
            "cpfile"            ,
            "mvfile"            ,
            "srfile_rec"        ,
            "mkdir"             ,
            "rmdir_rec"         ,
            "rmdir"             ,
            "cpdir_rec"         ,
            "lsdir_rec"         ,
            "lsdir"             ,
            "srdir_rec"         ,
            "txzdir_rec"        ,
            "untxz_rec"         ,
            "tbz2dir_rec"       ,
            "untbz2_rec"        ,
            "tgzdir_rec"        ,
            "untgz_rec"         ,
            "tzipdir_rec"       ,
            "untzip_rec"        ,
            "untar_rec"         ,
            "unzip_rec"         ,
            "unxz"              ,
            "xz"                ,
            "bzip2"             ,
            "bunzip2"           ,
            "gzip"              ,
            "gunzip"            ,
            "get_file_nel"      ,
            "get_file"          ,
            "put_file"          ,
            "md2_sum_file"      ,
            "md5_sum_file"      ,
            "sha1_sum_file"     ,
            "sha2_224sum_file"  ,
            "sha2_256sum_file"  ,
            "sha2_384sum_file"  ,
            "sha2_512sum_file"  ,
            "sha3_224sum_file"  ,
            "sha3_256sum_file"  ,
            "sha3_384sum_file"  ,
            "sha3_512sum_file"  ,
            "to_cp"             ,
            "to_ch"             ,
            "ucase"             ,
            "lcase"             ,
            "tcase"             ,
            "ltrim"             ,
            "rtrim"             ,
            "strim"             ,
            "strlen"            ,
            "substr"            ,
            "str_replace"       ,
            "str_fidx"          ,
            "str_lidx"          ,
            "str_begwith"       ,
            "str_endwith"       ,
            "str_rmfchr"        ,
            "str_rmlchr"        ,
            "re_from_glob"      ,
            "re_match"          ,
            "re_split"          ,
            "re_replace"        ,
            "re_quote_repval"   ,
            "re_quote"          ,
            "color_udiff"       ,
            "explode"           ,
            "sfchars"           ,
            "sfspaces"          ,
            "sfdots"            ,
            "sfpipes"           ,
            "sflines"           ,
            "sfdseps"           ,
            "sfpseps"           ,
            "implode"           ,
            "flatten"           ,
            "ftchars"           ,
            "ftspaces"          ,
            "ftdots"            ,
            "ftpipes"           ,
            "ftlines"           ,
            "ftdseps"           ,
            "ftpseps"           ,
            "part_count"        ,
            "partnm"            ,
            "partn"             ,
            "part_fidx"         ,
            "part_lidx"         ,
            "part_remove"       ,
            "part_insert"       ,
            "part_replace"      ,
            "contains"          ,
            "lookup"            ,
            "unique"            ,
            "erase_ifempty"     ,
            "erase_ifnot"       ,
            "erase_if"          ,
            "repeat"            ,
            "stack"             ,
            "series"            ,
            "interleave"        ,
            "ftt_stack"         ,
            "sort_ascending"    ,
            "sort_descending"   ,
            "map_new_from"      ,
            "map_new"           ,
            "map_delete"        ,
            "map_clear"         ,
            "map_put"           ,
            "map_add"           ,
            "map_remove"        ,
            "map_get"           ,
            "map_keys"          ,
            "map_num_keys"      ,
            "map_num_vals"      ,
            "map_to_edata"      ,
            "map_from_edata"    ,
            "map_valid_handle"  ,
            "nmap_from_json"    ,
            "nmap_delete"       ,
            "stk_new"           ,
            "stk_delete"        ,
            "stk_clear"         ,
            "stk_push"          ,
            "stk_peek"          ,
            "stk_pop"           ,
            "stk_num_elems"     ,
            "stk_to_edata"      ,
            "stk_from_edata"    ,
            "stk_valid_handle"  ,
            "bse_new"           ,
            "bse_delete"        ,
            "bse_size"          ,
            "bse_cursor"        ,
            "bse_seek_abs"      ,
            "bse_seek_beg"      ,
            "bse_seek_end"      ,
            "bse_seek_cur"      ,
            "bse_truncate"      ,
            "bse_save_file"     ,
            "bse_load_file"     ,
            "bse_save_base64str",
            "bse_load_base64str",
            "bse_set_be"        ,
            "bse_set_le"        ,
            "bse_wr_byte"       ,
            "bse_rd_byte"       ,
            "bse_wr_uint08"     ,
            "bse_wr_sint08"     ,
            "bse_rd_uint08"     ,
            "bse_rd_sint08"     ,
            "bse_wr_uint16"     ,
            "bse_wr_sint16"     ,
            "bse_rd_uint16"     ,
            "bse_rd_sint16"     ,
            "bse_wr_uint32"     ,
            "bse_wr_sint32"     ,
            "bse_rd_uint32"     ,
            "bse_rd_sint32"     ,
            "bse_wr_uint64"     ,
            "bse_wr_sint64"     ,
            "bse_rd_uint64"     ,
            "bse_rd_sint64"     ,
            "bse_wr_flt32"      ,
            "bse_rd_flt32"      ,
            "bse_wr_dbl64"      ,
            "bse_rd_dbl64"      ,
            "bse_wr_utf8"       ,
            "bse_rd_utf8"       ,
            "bse_wr_utf16"      ,
            "bse_rd_utf16"      ,
            "bse_wr_utf32"      ,
            "bse_rd_utf32"      ,
            "bse_range_clr"     ,
            "bse_range_cpy"     ,
            "bse_range_cut"     ,
            "bse_range_pst"     ,
            "bse_range_ovr"     ,
            "bse_valid_handle"  ,
            "fwc_new"           ,
            "fwc_delete"        ,
            "fwc_load_raw_bin"  ,
            "fwc_load_elf_bin"  ,
            "fwc_load_intel_hex",
            "fwc_load_moto_srec",
            "fwc_load_tektx_hex",
            "fwc_load_mos_tech" ,
            "fwc_load_titxt_hex",
            "fwc_load_ascii_hex",
            "fwc_load_vl_vmem"  ,
            "fwc_save_raw_bin"  ,
            "fwc_save_intel_hex",
            "fwc_save_moto_srec",
            "fwc_save_tektx_hex",
            "fwc_save_mos_tech" ,
            "fwc_save_titxt_hex",
            "fwc_clear"         ,
            "fwc_min_start_addr",
            "fwc_max_final_addr",
            "fwc_equals"        ,
            "fwc_compose"       ,
            "cmp"               ,
            "not"               ,
            "and"               ,
            "or"                ,
            "csel"              ,
            "printf"            ,
            "sprintf"           ,
            "vprintf"           ,
            "vsprintf"          ,
            "read_line"         ,
            "read_pswd"         ,
            "resolve_ip"        ,
            "http_head"         ,
            "http_get"          ,
            "http_post"         ,
            "http_download"     ,
            "http_set_auth"     ,
            "http_clr_auth"     ,
            "ssl_trust_all"     ,
            "gh_get_tags"       ,
            "gh_get_assets"     ,
            "ls_serial_ports"   ,
            "mcu_reset"         ,
            "mcu_bootloader"    ,
            "mcu_prog_cfg_json" ,
            "mcu_prog_cfg_ini"  ,
            "mcu_prog_exec"     ,
            "serial_console"    ,
            "tcp_serial_console",
            "serial_plotter"    ,
            "tcp_serial_plotter",
            "xmlframe_file"     ,
            "xmlframe_string"   ,
            "xml_escape"        ,
            "aboard_from_file"  ,
            "aboard_selector"   ,
            "aboard_getselconf" ,
            "esp_st_decoder"    ,
            "gdl_cpp_include"   ,
            "gdl_cpp_module"    ,
            "gdl_java_import"   ,
            "pp_java_scf"       ,
            ""
        ).collect( Collectors.toSet() ) );

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Keywords
    public static final Map<String, Integer> _Keywords;

    static {

        final Object[][] _entryKeywords = {
            { "jxmake"     , TType.Keyword },
            { "jxwait"     , TType.Keyword },
            { "depload"    , TType.Keyword },
            { "sdepload"   , TType.Keyword },
            { "echo"       , TType.Keyword },
            { "echoln"     , TType.Keyword },
            { "local"      , TType.Keyword },
            { "const"      , TType.Keyword },
            { "unset"      , TType.Keyword },
            { "function"   , TType.Keyword },
            { "endfunction", TType.Keyword },
            { "supersede"  , TType.Keyword },
            { "__origin__" , TType.Keyword },
            { "target"     , TType.Keyword },
            { "endtarget"  , TType.Keyword },
            { "extradep"   , TType.Keyword },
            { "label"      , TType.Keyword },
            { "goto"       , TType.Keyword },
            { "sgoto"      , TType.Keyword },
            { "if"         , TType.Keyword },
            { "elif"       , TType.Keyword },
            { "else"       , TType.Keyword },
            { "endif"      , TType.Keyword },
            { "for"        , TType.Keyword },
            { "to"         , TType.Keyword },
            { "step"       , TType.Keyword },
            { "endfor"     , TType.Keyword },
            { "foreach"    , TType.Keyword },
            { "in"         , TType.Keyword },
            { "skip"       , TType.Keyword },
            { "endforeach" , TType.Keyword },
            { "while"      , TType.Keyword },
            { "endwhile"   , TType.Keyword },
            { "do"         , TType.Keyword },
            { "whilst"     , TType.Keyword },
            { "repeat"     , TType.Keyword },
            { "until"      , TType.Keyword },
            { "loop"       , TType.Keyword },
            { "endloop"    , TType.Keyword },
            { "continue"   , TType.Keyword },
            { "break"      , TType.Keyword },
            { "return"     , TType.Keyword },
            { "inc"        , TType.Keyword },
            { "dec"        , TType.Keyword },
            { "add"        , TType.Keyword },
            { "sub"        , TType.Keyword },
            { "mul"        , TType.Keyword },
            { "div"        , TType.Keyword },
            { "mod"        , TType.Keyword },
            { "abs"        , TType.Keyword },
            { "neg"        , TType.Keyword },
            { "shl"        , TType.Keyword },
            { "shr"        , TType.Keyword },
            { "min"        , TType.Keyword },
            { "max"        , TType.Keyword },
            { "not"        , TType.Keyword },
            { "and"        , TType.Keyword },
            { "or"         , TType.Keyword },
            { "xor"        , TType.Keyword },
            { "eval"       , TType.Keyword },
            { "deprecate"  , TType.Keyword },
            { "deprecated" , TType.Keyword },
            { "by"         , TType.Keyword },
        };

        _Keywords = toUnmodifiableMap(_entryKeywords);

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Special shell/operating system commands
    public static final Set<String> _SpcCmds;

    static {

        _SpcCmds = Collections.unmodifiableSet( Stream.of(
            "clrenv",
            "delenv",
            "setenv",
            "addenv",
            "sstdin",
            "jxmake",
            ""
        ).collect( Collectors.toSet() ) );

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Special variables
    public static final Map<String, Integer> _SVars;

    static {

        final Object[][] _entrySVars = {
            { "cmdecho"     , TType.SVariableEval },
            { "cmdstreaming", TType.SVariableEval },
            { "lserr"       , TType.SVariableEval },
            { "jxmakefile"  , TType.SVariableEval },
            { "function"    , TType.SVariableEval },
            { "cmdtargets"  , TType.SVariableEval },
            { "usn"         , TType.SVariableEval },
            { "target"      , TType.SVariableEval },
            { "excode"      , TType.SVariableEval },
            { "stderr"      , TType.SVariableEval },
            { "stdout"      , TType.SVariableEval },
        };

        _SVars = toUnmodifiableMap(_entrySVars);

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Special variable shortcuts
    public static final Map<String, Integer> _SVarSCuts;

    static {

        final Object[][] _entrySVarSCuts = {
            { "__jxm_ver_major__"  , TType.SVarShortcut },
            { "__jxm_ver_minor__"  , TType.SVarShortcut },
            { "__jxm_ver_patch__"  , TType.SVarShortcut },
            { "__jxm_ver_value__"  , TType.SVarShortcut },
            { "__jxm_ver_devel__"  , TType.SVarShortcut },
            { "__os_name__"        , TType.SVarShortcut },
            { "__os_name_actual__" , TType.SVarShortcut },
            { "__os_windows__"     , TType.SVarShortcut },
            { "__os_posix__"       , TType.SVarShortcut },
            { "__os_linux__"       , TType.SVarShortcut },
            { "__os_bsd__"         , TType.SVarShortcut },
            { "__os_mac__"         , TType.SVarShortcut },
            { "__os_cygwin__"      , TType.SVarShortcut },
            { "__os_mingw__"       , TType.SVarShortcut },
            { "__os_msys__"        , TType.SVarShortcut },
            { "__os_posix_compat__", TType.SVarShortcut },
            { "__os_arch__"        , TType.SVarShortcut },
            { "__os_bit_count__"   , TType.SVarShortcut },
            { "__os_32bit__"       , TType.SVarShortcut },
            { "__os_64bit__"       , TType.SVarShortcut },
            { "__os_be__"          , TType.SVarShortcut },
            { "__os_le__"          , TType.SVarShortcut },
            { "__os_dsep_char__"   , TType.SVarShortcut },
            { "__os_psep_char__"   , TType.SVarShortcut },
            { "__re_all__"         , TType.SVarShortcut },
            { "__include_paths__"  , TType.SVarShortcut },
            { "__class_paths__"    , TType.SVarShortcut },
        };

        _SVarSCuts = toUnmodifiableMap(_entrySVarSCuts);

    } // static

    // ANSI escape codes
    public static final Map<String, Integer> _ANSIEscapeCodes;

    static {

        final Object[][] _entryANSIEscapeCodes = {
            { "__c_use_dark__" , TType.ANSIEscapeCode },
            { "__c_use_light__", TType.ANSIEscapeCode },
            { "__c_clrscr__"   , TType.ANSIEscapeCode },
            { "__c_black__"    , TType.ANSIEscapeCode },
            { "__c_dgray__"    , TType.ANSIEscapeCode },
            { "__c_lgray__"    , TType.ANSIEscapeCode },
            { "__c_red__"      , TType.ANSIEscapeCode },
            { "__c_green__"    , TType.ANSIEscapeCode },
            { "__c_yellow__"   , TType.ANSIEscapeCode },
            { "__c_blue__"     , TType.ANSIEscapeCode },
            { "__c_magenta__"  , TType.ANSIEscapeCode },
            { "__c_cyan__"     , TType.ANSIEscapeCode },
            { "__c_white__"    , TType.ANSIEscapeCode },
            { "__c_reset__"    , TType.ANSIEscapeCode },
        };

        _ANSIEscapeCodes = toUnmodifiableMap(_entryANSIEscapeCodes);

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Options
    public static final Map<String, Integer> _OptionsKey1;
    public static final Map<String, Integer> _OptionsKey2;
    public static final Map<String, Integer> _OptionsKey2PC;

    static {

        final Object[][] _entryOptionsKey1 = {
            { "warning", TType.Keyword },
        };

        final Object[][] _entryOptionsKey2 = {
            { "push"   , TType.Keyword },
            { "pop"    , TType.Keyword },
            { "disable", TType.Keyword },
            { "enable" , TType.Keyword },
        };

        final Object[][] _entryOptionsKey2PC = {
            { "disable", 1 },
            { "enable" , 1 },
        };

        _OptionsKey1   = toUnmodifiableMap(_entryOptionsKey1  );
        _OptionsKey2   = toUnmodifiableMap(_entryOptionsKey2  );
        _OptionsKey2PC = toUnmodifiableMap(_entryOptionsKey2PC);

    } // static

} // class JxMakeTokenMaker_Constants
