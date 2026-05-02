/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import jxm.*;
import jxm.dl.*;
import jxm.tool.*;
import jxm.xb.fci.*;


public class FuncCall extends ExecBlock {

    private final XCom.FuncSpec      _funcSpec;
    private final XCom.ReadVarSpecs  _rvarSpecs;

    private       XCom.VariableValue _retVal;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FuncCall(final String path, final int lNum, final int cNum, final XCom.FuncSpec funcSpec, final XCom.ReadVarSpecs rvarSpecs)
    {
        super(path, lNum, cNum);

        _funcSpec  = funcSpec;
        _rvarSpecs = XCom.substEmptyReadVarSpecs(rvarSpecs);
        _retVal    = _funcSpec.retVal ? new XCom.VariableValue() : null;
    }

    public String funcName()
    { return "$" + _funcSpec.fnName.name(); }

    public XCom.VariableValue getReturnValue()
    { return _retVal; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static XCom.VariableValue _getOptParam(final ArrayList<XCom.VariableValue> evalVals, final int idx) throws JXMException
    {
        //return ( evalVals.size() >= (idx + 1) ) ? evalVals.get(idx) : null;

        if( evalVals.size() < (idx + 1) ) return null;

        final XCom.VariableValue varVal = evalVals.get(idx);

        return ( ( varVal.size() == 1 ) && ( varVal.get(0).value == XCom.Str_NullArgument) ) ? null : varVal;
    }

    public static String _readFlattenOptParam(final ArrayList<XCom.VariableValue> evalVals, final int idx, final String defaultValue) throws JXMException
    {
        //return ( evalVals.size() >= (idx + 1) ) ? XCom.flatten( evalVals.get(idx), "" ) : defaultValue;

        if( evalVals.size() < (idx + 1) ) return defaultValue;

        final XCom.VariableValue varVal = evalVals.get(idx);

        return ( ( varVal.size() == 1 ) && ( varVal.get(0).value == XCom.Str_NullArgument) ) ? defaultValue : XCom.flatten(varVal, "");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Clear the return value first
        if(_retVal != null) _retVal.clear();

        // Evaluate the argument(s)
        final ArrayList<XCom.VariableValue> evalVals = new ArrayList<>();

        for(final XCom.ReadVarSpec item : _rvarSpecs) {
            final XCom.VariableValue varVal = execData.execState.readVar(this, execData, item, true);
            evalVals.add(varVal);
        }

        // Check the number of arguments
        int argCnt = evalVals.size();

        argCnt -= _funcSpec.reqCnt;
        if(argCnt < 0) throw XCom.newJXMFatalLogicError( Texts.EMsg_TooFewNumArg, _funcSpec.functionName(), evalVals.size(), _funcSpec.reqCnt, _funcSpec.optCnt ); // NOTE : This should never got executed!

        if(_funcSpec.optCnt >= 0) {
            argCnt -= _funcSpec.optCnt;
            if(argCnt > 0) throw XCom.newJXMFatalLogicError( Texts.EMsg_TooManyNumArg, _funcSpec.functionName(), evalVals.size(), _funcSpec.reqCnt, _funcSpec.optCnt ); // NOTE : This should never got executed!
        }

        /*
        // Check for partial optional arguments (passing partial optional arguments can only be done when
        // calling user-defined functions)
        if(_funcSpec.fnName != XCom.FuncName.call && _funcSpec.fnName != XCom.FuncName.exec) {
            for(final XCom.VariableValue varVal : evalVals) {
                final boolean varNull = ( varVal.size() == 1 && varVal.get(0).value != null && varVal.get(0).value.equals(XCom.Str_NullArgument) );
                if(varNull) throw XCom.newJXMRuntimeError(Texts.EMsg_PartOptArgNonUFunc);
            }
        }
        //*/

        // Execute the function based on the name
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

        try {

            switch(_funcSpec.fnName) {

                case call               : xres =          _execute_call_exec    (         evalVals,       execData, false                  ); break;
                case exec               : xres =          _execute_call_exec    (         evalVals,       execData, true                   ); break;

                case add_target         :        MixUtil ._execute_add_target   (         evalVals,       execData                         ); break;
                case add_extradep       :        MixUtil ._execute_add_extradep (         evalVals, this, execData                         ); break;

                case has_var            :        MixUtil ._execute_has_var      (_retVal, evalVals,       execData                         ); break;

                case nop                : /* FALLTHROUGH */
                case __dummy_dep_1__    : /* FALLTHROUGH */
                case __dummy_dep_2__    : /* FALLTHROUGH */
                case __dummy_rep_2__    : break;

                case alt_glibc_for      :        SysManip._execute_alt_glibc_for(         evalVals                                         ); break;
                case sh_delay           :        SysManip._execute_sh_delay     (                                                          ); break;
                case sh_restore         :        SysManip._execute_sh_restore   (                                                          ); break;
                case cmd_echo_off       :        SysManip._execute_cmd_echo     (                         execData, false                  ); break;
                case cmd_echo_on        :        SysManip._execute_cmd_echo     (                         execData, true                   ); break;
                case cmd_streaming_off  :        SysManip._execute_cmd_streaming(                         execData, false                  ); break;
                case cmd_streaming_on   :        SysManip._execute_cmd_streaming(                         execData, true                   ); break;
                case cmd_stderr_chk_off :        SysManip._execute_cmd_stderrchk(                         execData, false                  ); break;
                case cmd_stderr_chk_on  :        SysManip._execute_cmd_stderrchk(                         execData, true                   ); break;
                case cmd_stdout_chk_off :        SysManip._execute_cmd_stdoutchk(                         execData, false                  ); break;
                case cmd_stdout_chk_on  :        SysManip._execute_cmd_stdoutchk(                         execData, true                   ); break;

                case cmd_clear_state    :        SysManip._execute_cmd_clr_state(                         execData                         ); break;
                case silent_sem         :        SysManip._execute_silent_SEM   (                                                          ); break;
                case restore_sem        :        SysManip._execute_restore_SEM  (                                                          ); break;
                case micros             :        SysManip._execute_micros       (_retVal                                                   ); break;
                case millis             :        SysManip._execute_millis       (_retVal                                                   ); break;
                case datetime           :        SysManip._execute_datetime     (_retVal, evalVals                                         ); break;
                case sleep              :        SysManip._execute_sleep        (         evalVals, this, execData                         ); break;
                case getenv             :        SysManip._execute_getenv       (_retVal, evalVals                                         ); break;
                case clear_project      :        SysManip._execute_clear_project(         evalVals, this, execData                         ); break;
                case exit               :        SysManip._execute_exit         (         evalVals, this, execData                         );
                                          xres = XCom.ExecuteResult.ProgramExit;                                                              break;

                case cwd                :        FSUtil  ._execute_cwd          (_retVal                                                   ); break;
                case ptd                :        FSUtil  ._execute_ptd          (_retVal                                                   ); break;
                case uhd                :        FSUtil  ._execute_uhd          (_retVal                                                   ); break;
                case jdd                :        FSUtil  ._execute_jdd          (_retVal                                                   ); break;
                case jtd                :        FSUtil  ._execute_jtd          (_retVal                                                   ); break;
                case jxd                :        FSUtil  ._execute_jxd          (_retVal                                                   ); break;
                case set_rwx3           :        FSUtil  ._execute_set_rwx3     (         evalVals                                         ); break;
                case cat_path           :        FSUtil  ._execute_cat_path     (_retVal, evalVals                                         ); break;
                case cat_paths          :        FSUtil  ._execute_cat_paths    (_retVal, evalVals                                         ); break;
                case abs_path           :        FSUtil  ._execute_abs_path     (_retVal, evalVals                                         ); break;
                case rel_path           :        FSUtil  ._execute_rel_path     (_retVal, evalVals                                         ); break;
                case valid_path         :        FSUtil  ._execute_valid_path   (_retVal, evalVals                                         ); break;
                case uptodate_path      :        FSUtil  ._execute_uptodate_path(_retVal, evalVals,       execData                         ); break;
                case newer_path         :        FSUtil  ._execute_newer_path   (_retVal, evalVals                                         ); break;
                case same_path          :        FSUtil  ._execute_same_path    (_retVal, evalVals                                         ); break;
                case path_is_abs        :        FSUtil  ._execute_path_is_abs  (_retVal, evalVals                                               ); break;
                case path_is_rel        :        FSUtil  ._execute_path_is_rel  (_retVal, evalVals                                         ); break;
                case path_is_file       :        FSUtil  ._execute_path_is_file (_retVal, evalVals                                         ); break;
                case path_is_directory  :        FSUtil  ._execute_path_is_dir  (_retVal, evalVals                                         ); break;
                case path_is_symlink    :        FSUtil  ._execute_path_is_syml (_retVal, evalVals                                         ); break;
                case path_is_readable   :        FSUtil  ._execute_path_is_rable(_retVal, evalVals                                         ); break;
                case path_is_writable   :        FSUtil  ._execute_path_is_wable(_retVal, evalVals                                         ); break;
                case path_is_executable :        FSUtil  ._execute_path_is_xable(_retVal, evalVals                                         ); break;
                case path_last_part     :        FSUtil  ._execute_path_lpart   (_retVal, evalVals                                         ); break;
                case path_rm_last_part  :        FSUtil  ._execute_path_rmlpart (_retVal, evalVals                                         ); break;
                case path_ndsep         :        FSUtil  ._execute_path_ndsep   (_retVal, evalVals                                         ); break;
                case symlink_target     :        FSUtil  ._execute_syml_target  (_retVal, evalVals                                         ); break;
                case symlink_real_apath :        FSUtil  ._execute_syml_rapath  (_retVal, evalVals                                         ); break;
                case symlink_resolve    :        FSUtil  ._execute_syml_resolve (_retVal, evalVals                                         ); break;
                case dir_name           :        FSUtil  ._execute_dir_name     (_retVal, evalVals                                         ); break;
                case file_name          :        FSUtil  ._execute_file_name    (_retVal, evalVals, this, execData                         ); break;
                case file_ext           :        FSUtil  ._execute_file_ext     (_retVal, evalVals                                         ); break;
                case file_mime_type     :        FSUtil  ._execute_file_mime_typ(_retVal, evalVals                                         ); break;

                case touch              :        FSManip ._execute_touch        (         evalVals                                         ); break;
                case rmfile             :        FSManip ._execute_rmfile       (         evalVals                                         ); break;
                case rmfiles            :        FSManip ._execute_rmfiles_rnr  (         evalVals,                 false                  ); break;
                case rmfiles_rec        :        FSManip ._execute_rmfiles_rnr  (         evalVals,                 true                   ); break;
                case cpfile             :        FSManip ._execute_cpmvfile     (         evalVals, this, execData, false                  ); break;
                case mvfile             :        FSManip ._execute_cpmvfile     (         evalVals, this, execData, true                   ); break;
                case mkdir              :        FSManip ._execute_mkdir        (         evalVals                                         ); break;
                case rmdir              :        FSManip ._execute_rmdir        (         evalVals                                         ); break;
                case rmdir_rec          :        FSManip ._execute_rmdir_rec    (         evalVals                                         ); break;
                case cpdir_rec          :        FSManip ._execute_cpdir_rec    (         evalVals, this, execData                         ); break;
                case lsdir              :        FSManip ._execute_lsdir_rnr    (_retVal, evalVals, this, execData, false                  ); break;
                case lsdir_rec          :        FSManip ._execute_lsdir_rnr    (_retVal, evalVals, this, execData, true                   ); break;
                case srfile_rec         :        FSManip ._execute_srfd_rec     (_retVal, evalVals, this, execData, false                  ); break;
                case srdir_rec          :        FSManip ._execute_srfd_rec     (_retVal, evalVals, this, execData, true                   ); break;

                case tzstdir_rec        :        FCUtil  ._execute_tzstdir_rec  (_retVal, evalVals, this, execData, false                  ); break;
                case untzst_rec         :        FCUtil  ._execute_tzstdir_rec  (_retVal, evalVals, this, execData, true                   ); break;
                case txzdir_rec         :        FCUtil  ._execute_txzdir_rec   (_retVal, evalVals, this, execData, false                  ); break;
                case untxz_rec          :        FCUtil  ._execute_txzdir_rec   (_retVal, evalVals, this, execData, true                   ); break;
                case tbz2dir_rec        :        FCUtil  ._execute_tbz2dir_rec  (_retVal, evalVals, this, execData, false                  ); break;
                case untbz2_rec         :        FCUtil  ._execute_tbz2dir_rec  (_retVal, evalVals, this, execData, true                   ); break;
                case tgzdir_rec         :        FCUtil  ._execute_tgzdir_rec   (_retVal, evalVals, this, execData, false                  ); break;
                case untgz_rec          :        FCUtil  ._execute_tgzdir_rec   (_retVal, evalVals, this, execData, true                   ); break;
                case tzipdir_rec        :        FCUtil  ._execute_tzipdir_rec  (_retVal, evalVals, this, execData, false                  ); break;
                case untzip_rec         :        FCUtil  ._execute_tzipdir_rec  (_retVal, evalVals, this, execData, true                   ); break;
                case untar_rec          :        FCUtil  ._execute_untardir_rec (_retVal, evalVals, this, execData                         ); break;
                case unzip_rec          :        FCUtil  ._execute_unzipdir_rec (_retVal, evalVals, this, execData                         ); break;
                case zst                :        FCUtil  ._execute_zst          (_retVal, evalVals, this, execData, false                  ); break;
                case unzst              :        FCUtil  ._execute_zst          (_retVal, evalVals, this, execData, true                   ); break;
                case xz                 :        FCUtil  ._execute_xz           (_retVal, evalVals, this, execData, false                  ); break;
                case unxz               :        FCUtil  ._execute_xz           (_retVal, evalVals, this, execData, true                   ); break;
                case bzip2              :        FCUtil  ._execute_bzip2        (_retVal, evalVals, this, execData, false                  ); break;
                case bunzip2            :        FCUtil  ._execute_bzip2        (_retVal, evalVals, this, execData, true                   ); break;
                case gzip               :        FCUtil  ._execute_gzip         (_retVal, evalVals, this, execData, false                  ); break;
                case gunzip             :        FCUtil  ._execute_gzip         (_retVal, evalVals, this, execData, true                   ); break;

                case put_file           :        FCUtil  ._execute_put_file     (_retVal, evalVals                                         ); break;
                case get_file           :        FCUtil  ._execute_get_file     (_retVal, evalVals, this, execData                         ); break;
                case get_file_nel       :        FCUtil  ._execute_get_file_nel (_retVal, evalVals, this, execData                         ); break;
                case md2_sum_file       :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "MD2"                  ); break;
                case md5_sum_file       :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "MD5"                  ); break;
                case sha1_sum_file      :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA-1"                ); break;
                case sha2_224sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA-224"              ); break;
                case sha2_256sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA-256"              ); break;
                case sha2_384sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA-384"              ); break;
                case sha2_512sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA-512"              ); break;
                case sha3_224sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA3-224"             ); break;
                case sha3_256sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA3-256"             ); break;
                case sha3_384sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA3-384"             ); break;
                case sha3_512sum_file   :        FCUtil  ._execute_md_file      (_retVal, evalVals,                 "SHA3-512"             ); break;

                case to_cp              :        StrUtil ._execute_to_cp        (_retVal, evalVals                                         ); break;
                case to_ch              :        StrUtil ._execute_to_ch        (_retVal, evalVals, this, execData                         ); break;
                case ucase              :        StrUtil ._execute_ucase        (_retVal, evalVals                                         ); break;
                case lcase              :        StrUtil ._execute_lcase        (_retVal, evalVals                                         ); break;
                case tcase              :        StrUtil ._execute_tcase        (_retVal, evalVals                                         ); break;
                case ltrim              :        StrUtil ._execute_ltrim        (_retVal, evalVals                                         ); break;
                case rtrim              :        StrUtil ._execute_rtrim        (_retVal, evalVals                                         ); break;
                case strim              :        StrUtil ._execute_strim        (_retVal, evalVals                                         ); break;
                case strlen             :        StrUtil ._execute_strlen       (_retVal, evalVals                                         ); break;
                case substr             :        StrUtil ._execute_substr       (_retVal, evalVals, this, execData                         ); break;
                case str_replace        :        StrUtil ._execute_str_replace  (_retVal, evalVals                                         ); break;
                case str_fidx           :        StrUtil ._execute_str_xidx     (_retVal, evalVals, this, execData, false                  ); break;
                case str_lidx           :        StrUtil ._execute_str_xidx     (_retVal, evalVals, this, execData, true                   ); break;
                case str_begwith        :        StrUtil ._execute_str_xxxwith  (_retVal, evalVals,                 false                  ); break;
                case str_endwith        :        StrUtil ._execute_str_xxxwith  (_retVal, evalVals,                 true                   ); break;
                case str_rmfchr         :        StrUtil ._execute_str_rmxchr   (_retVal, evalVals,                 false                  ); break;
                case str_rmlchr         :        StrUtil ._execute_str_rmxchr   (_retVal, evalVals,                 true                   ); break;
                case re_from_glob       :        StrUtil ._execute_re_from_glob (_retVal, evalVals                                         ); break;
                case re_match           :        StrUtil ._execute_re_match     (_retVal, evalVals                                         ); break;
                case re_split           :        StrUtil ._execute_re_split     (_retVal, evalVals                                         ); break;
                case re_replace         :        StrUtil ._execute_re_replace   (_retVal, evalVals                                         ); break;
                case re_quote           :        StrUtil ._execute_re_quote     (_retVal, evalVals                                         ); break;
                case re_quote_repval    :        StrUtil ._execute_re_quote_repv(_retVal, evalVals                                         ); break;
                case color_udiff        :        StrUtil ._execute_color_udiff  (_retVal, evalVals                                         ); break;

                case explode            :        SetUtil ._execute_explode      (_retVal, evalVals                                         ); break;
                case sfchars            :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 ""                     ); break;
                case sfspaces           :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 " "                    ); break;
                case sfdots             :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 "."                    ); break;
                case sfpipes            :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 "|"                    ); break;
                case sflines            :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 "\n"                   ); break;
                case sfdseps            :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 File.separator         ); break;
                case sfpseps            :        SetUtil ._execute_sfconstants  (_retVal, evalVals,                 File.pathSeparator     ); break;
                case implode            :        SetUtil ._execute_implode      (_retVal, evalVals                                         ); break;
                case flatten            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 ""                     ); break;
                case ftchars            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 ""                     ); break;
                case ftspaces           :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 " "                    ); break;
                case ftdots             :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 "."                    ); break;
                case ftpipes            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 "|"                    ); break;
                case ftlines            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 "\n"                   ); break;
                case ftdseps            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 File.separator         ); break;
                case ftpseps            :        SetUtil ._execute_ftconstants  (_retVal, evalVals,                 File.pathSeparator     ); break;
                case part_count         :        SetUtil ._execute_part_count   (_retVal, evalVals                                         ); break;
                case partn              :        SetUtil ._execute_partn        (_retVal, evalVals, this, execData                         ); break;
                case partnm             :        SetUtil ._execute_partnm       (_retVal, evalVals, this, execData                         ); break;
                case part_fidx          :        SetUtil ._execute_part_xidx    (_retVal, evalVals, this, execData, false                  ); break;
                case part_lidx          :        SetUtil ._execute_part_xidx    (_retVal, evalVals, this, execData, true                   ); break;
                case part_remove        :        SetUtil ._execute_part_remove  (_retVal, evalVals, this, execData                         ); break;
                case part_insert        :        SetUtil ._execute_part_insert  (_retVal, evalVals, this, execData                         ); break;
                case part_replace       :        SetUtil ._execute_part_replace (_retVal, evalVals, this, execData                         ); break;
                case contains           :        SetUtil ._execute_contains     (_retVal, evalVals                                         ); break;
                case lookup             :        SetUtil ._execute_lookup       (_retVal, evalVals                                         ); break;
                case unique             :        SetUtil ._execute_unique       (_retVal, evalVals                                         ); break;
                case erase_if           :        SetUtil ._execute_erase_ifxxx  (_retVal, evalVals, this, execData, false                  ); break;
                case erase_ifnot        :        SetUtil ._execute_erase_ifxxx  (_retVal, evalVals, this, execData, true                   ); break;
                case erase_ifempty      :        SetUtil ._execute_erase_empty  (_retVal, evalVals                                         ); break;
                case repeat             :        SetUtil ._execute_repeat       (_retVal, evalVals, this, execData                         ); break;
                case stack              :        SetUtil ._execute_stack        (_retVal, evalVals                                         ); break;
                case series             :        SetUtil ._execute_series       (_retVal, evalVals                                         ); break;
                case interleave         :        SetUtil ._execute_interleave   (_retVal, evalVals                                         ); break;
                case ftt_stack          :        SetUtil ._execute_ftt_stack    (_retVal, evalVals                                         ); break;
                case sort_ascending     :        SetUtil ._execute_sort         (_retVal, evalVals,                 false                  ); break;
                case sort_descending    :        SetUtil ._execute_sort         (_retVal, evalVals,                 true                   ); break;
                case map_new            :        SetUtil ._execute_map_new      (_retVal                                                   ); break;
                case map_new_from       :        SetUtil ._execute_map_new_from (_retVal, evalVals, this, execData                         ); break;
                case map_delete         :        SetUtil ._execute_map_delete   (         evalVals                                         ); break;
                case map_clear          :        SetUtil ._execute_map_clear    (         evalVals                                         ); break;
                case map_put            :        SetUtil ._execute_map_putadd   (         evalVals, this, execData, false                  ); break;
                case map_add            :        SetUtil ._execute_map_putadd   (         evalVals, this, execData, true                   ); break;
                case map_remove         :        SetUtil ._execute_map_remove   (         evalVals                                         ); break;
                case map_get            :        SetUtil ._execute_map_get      (_retVal, evalVals                                         ); break;
                case map_keys           :        SetUtil ._execute_map_keys     (_retVal, evalVals                                         ); break;
                case map_num_keys       :        SetUtil ._execute_map_num_keys (_retVal, evalVals                                         ); break;
                case map_num_vals       :        SetUtil ._execute_map_num_vals (_retVal, evalVals                                         ); break;
                case map_to_edata       :        SetUtil ._execute_map_to_edata (_retVal, evalVals                                         ); break;
                case map_from_edata     :        SetUtil ._execute_map_fr_edata (_retVal, evalVals                                         ); break;
                case map_valid_handle   :        SetUtil ._execute_map_vl_handle(_retVal, evalVals                                         ); break;
                case nmap_from_json     :        SetUtil ._execute_nmap_frm_json(_retVal, evalVals                                         ); break;
                case nmap_delete        :        SetUtil ._execute_nmap_delete  (         evalVals                                         ); break;
                case stk_new            :        SetUtil ._execute_stk_new      (_retVal                                                   ); break;
                case stk_delete         :        SetUtil ._execute_stk_delete   (         evalVals                                         ); break;
                case stk_clear          :        SetUtil ._execute_stk_clear    (         evalVals                                         ); break;
                case stk_push           :        SetUtil ._execute_stk_push     (         evalVals                                         ); break;
                case stk_peek           :        SetUtil ._execute_stk_peek_pop (_retVal, evalVals,                 false                  ); break;
                case stk_pop            :        SetUtil ._execute_stk_peek_pop (_retVal, evalVals,                 true                   ); break;
                case stk_num_elems      :        SetUtil ._execute_stk_num_elems(_retVal, evalVals                                         ); break;
                case stk_to_edata       :        SetUtil ._execute_stk_to_edata (_retVal, evalVals                                         ); break;
                case stk_from_edata     :        SetUtil ._execute_stk_fr_edata (_retVal, evalVals                                         ); break;
                case stk_valid_handle   :        SetUtil ._execute_stk_vl_handle(_retVal, evalVals                                         ); break;

                case bse_new            :        BSEUtil ._execute_bse_new      (_retVal                                                   ); break;
                case bse_delete         :        BSEUtil ._execute_bse_delete   (         evalVals                                         ); break;
                case bse_size           :        BSEUtil ._execute_bse_size     (_retVal, evalVals                                         ); break;
                case bse_cursor         :        BSEUtil ._execute_bse_cursor   (_retVal, evalVals                                         ); break;
                case bse_seek_abs       :        BSEUtil ._execute_bse_seek_abs (         evalVals, this, execData                         ); break;
                case bse_seek_beg       :        BSEUtil ._execute_bse_seek_beg (         evalVals, this, execData                         ); break;
                case bse_seek_end       :        BSEUtil ._execute_bse_seek_end (         evalVals, this, execData                         ); break;
                case bse_seek_cur       :        BSEUtil ._execute_bse_seek_cur (         evalVals, this, execData                         ); break;
                case bse_truncate       :        BSEUtil ._execute_bse_truncate (         evalVals                                         ); break;
                case bse_save_file      :        BSEUtil ._execute_bse_save_file(         evalVals                                         ); break;
                case bse_load_file      :        BSEUtil ._execute_bse_load_file(         evalVals                                         ); break;
                case bse_save_base64str :        BSEUtil ._execute_bse_save_b64s(_retVal, evalVals                                         ); break;
                case bse_load_base64str :        BSEUtil ._execute_bse_load_b64s(         evalVals                                         ); break;
                case bse_set_be         :        BSEUtil ._execute_bse_set_be   (         evalVals                                         ); break;
                case bse_set_le         :        BSEUtil ._execute_bse_set_le   (         evalVals                                         ); break;
                case bse_wr_byte        :        BSEUtil ._execute_bse_wr_byte  (         evalVals                                         ); break;
                case bse_rd_byte        :        BSEUtil ._execute_bse_rd_byte  (_retVal, evalVals                                         ); break;
                case bse_wr_uint08      :        BSEUtil ._execute_bse_wr_uint08(         evalVals                                         ); break;
                case bse_wr_sint08      :        BSEUtil ._execute_bse_wr_sint08(         evalVals                                         ); break;
                case bse_rd_uint08      :        BSEUtil ._execute_bse_rd_uint08(_retVal, evalVals                                         ); break;
                case bse_rd_sint08      :        BSEUtil ._execute_bse_rd_sint08(_retVal, evalVals                                         ); break;
                case bse_wr_uint16      :        BSEUtil ._execute_bse_wr_uint16(         evalVals                                         ); break;
                case bse_wr_sint16      :        BSEUtil ._execute_bse_wr_sint16(         evalVals                                         ); break;
                case bse_rd_uint16      :        BSEUtil ._execute_bse_rd_uint16(_retVal, evalVals                                         ); break;
                case bse_rd_sint16      :        BSEUtil ._execute_bse_rd_sint16(_retVal, evalVals                                         ); break;
                case bse_wr_uint32      :        BSEUtil ._execute_bse_wr_uint32(         evalVals                                         ); break;
                case bse_wr_sint32      :        BSEUtil ._execute_bse_wr_sint32(         evalVals                                         ); break;
                case bse_rd_uint32      :        BSEUtil ._execute_bse_rd_uint32(_retVal, evalVals                                         ); break;
                case bse_rd_sint32      :        BSEUtil ._execute_bse_rd_sint32(_retVal, evalVals                                         ); break;
                case bse_wr_uint64      :        BSEUtil ._execute_bse_wr_uint64(         evalVals                                         ); break;
                case bse_wr_sint64      :        BSEUtil ._execute_bse_wr_sint64(         evalVals                                         ); break;
                case bse_rd_uint64      :        BSEUtil ._execute_bse_rd_uint64(_retVal, evalVals                                         ); break;
                case bse_rd_sint64      :        BSEUtil ._execute_bse_rd_sint64(_retVal, evalVals                                         ); break;
                case bse_wr_flt32       :        BSEUtil ._execute_bse_wr_flt32 (         evalVals                                         ); break;
                case bse_rd_flt32       :        BSEUtil ._execute_bse_rd_flt32 (_retVal, evalVals                                         ); break;
                case bse_wr_dbl64       :        BSEUtil ._execute_bse_wr_dbl64 (         evalVals                                         ); break;
                case bse_rd_dbl64       :        BSEUtil ._execute_bse_rd_dbl64 (_retVal, evalVals                                         ); break;
                case bse_wr_utf8        :        BSEUtil ._execute_bse_wr_utf8  (         evalVals, this, execData                         ); break;
                case bse_rd_utf8        :        BSEUtil ._execute_bse_rd_utf8  (_retVal, evalVals, this, execData                         ); break;
                case bse_wr_utf16       :        BSEUtil ._execute_bse_wr_utf16 (         evalVals, this, execData                         ); break;
                case bse_rd_utf16       :        BSEUtil ._execute_bse_rd_utf16 (_retVal, evalVals, this, execData                         ); break;
                case bse_wr_utf32       :        BSEUtil ._execute_bse_wr_utf32 (         evalVals, this, execData                         ); break;
                case bse_rd_utf32       :        BSEUtil ._execute_bse_rd_utf32 (_retVal, evalVals, this, execData                         ); break;
                case bse_range_clr      :        BSEUtil ._execute_bse_range_clr(         evalVals, this, execData                         ); break;
                case bse_range_cpy      :        BSEUtil ._execute_bse_range_cpy(_retVal, evalVals, this, execData                         ); break;
                case bse_range_cut      :        BSEUtil ._execute_bse_range_cut(_retVal, evalVals, this, execData                         ); break;
                case bse_range_pst      :        BSEUtil ._execute_bse_range_pst(         evalVals                                         ); break;
                case bse_range_ovr      :        BSEUtil ._execute_bse_range_ovr(         evalVals                                         ); break;
                case bse_valid_handle   :        BSEUtil ._execute_bse_vl_handle(_retVal, evalVals                                         ); break;

                case fwc_new            :        FWCUtil ._execute_fwc_new      (_retVal                                                   ); break;
                case fwc_delete         :        FWCUtil ._execute_fwc_delete   (         evalVals                                         ); break;
                case fwc_load_raw_bin   :        FWCUtil ._execute_fwc_ld_rbin  (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_elf_bin   :        FWCUtil ._execute_fwc_ld_elfb  (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_intel_hex :        FWCUtil ._execute_fwc_ld_ihex  (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_moto_srec :        FWCUtil ._execute_fwc_ld_msrec (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_tektx_hex :        FWCUtil ._execute_fwc_ld_thex  (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_mos_tech  :        FWCUtil ._execute_fwc_ld_mostc (_retVal, evalVals, this, execData                         ); break;
                case fwc_load_titxt_hex :        FWCUtil ._execute_fwc_ld_tixhex(_retVal, evalVals, this, execData                         ); break;
                case fwc_load_ascii_hex :        FWCUtil ._execute_fwc_ld_aschex(_retVal, evalVals, this, execData                         ); break;
                case fwc_load_vl_vmem   :        FWCUtil ._execute_fwc_ld_vlvmem(_retVal, evalVals, this, execData                         ); break;
                case fwc_save_raw_bin   :        FWCUtil ._execute_fwc_sv_rbin  (         evalVals, this, execData                         ); break;
                case fwc_save_intel_hex :        FWCUtil ._execute_fwc_sv_ihex  (         evalVals                                         ); break;
                case fwc_save_moto_srec :        FWCUtil ._execute_fwc_sv_msrec (         evalVals                                         ); break;
                case fwc_save_tektx_hex :        FWCUtil ._execute_fwc_sv_thex  (         evalVals                                         ); break;
                case fwc_save_mos_tech  :        FWCUtil ._execute_fwc_sv_mostc (         evalVals                                         ); break;
                case fwc_save_titxt_hex :        FWCUtil ._execute_fwc_sv_tixhex(         evalVals                                         ); break;
                case fwc_clear          :        FWCUtil ._execute_fwc_clear    (         evalVals                                         ); break;
                case fwc_min_start_addr :        FWCUtil ._execute_fwc_min_saddr(_retVal, evalVals                                         ); break;
                case fwc_max_final_addr :        FWCUtil ._execute_fwc_max_faddr(_retVal, evalVals                                         ); break;
                case fwc_equals         :        FWCUtil ._execute_fwc_equals   (_retVal, evalVals, this, execData                         ); break;
                case fwc_compose        :        FWCUtil ._execute_fwc_compose  (_retVal, evalVals, this, execData                         ); break;

                case cmp                :        SetUtil ._execute_cmp          (_retVal, evalVals, this, execData                         ); break;
                case not                :        SetUtil ._execute_not          (_retVal, evalVals, this, execData                         ); break;
                case and                :        SetUtil ._execute_and_or       (_retVal, evalVals, this, execData, false                  ); break;
                case or                 :        SetUtil ._execute_and_or       (_retVal, evalVals, this, execData, true                   ); break;
                case csel               :        SetUtil ._execute_csel         (_retVal, evalVals, this, execData                         ); break;

                case printf             :        StdIO   ._execute_printf       (_retVal, evalVals, this, execData, false, true            ); break;
                case sprintf            :        StdIO   ._execute_printf       (_retVal, evalVals, this, execData, false, false           ); break;
                case vprintf            :        StdIO   ._execute_printf       (_retVal, evalVals, this, execData, true , true            ); break;
                case vsprintf           :        StdIO   ._execute_printf       (_retVal, evalVals, this, execData, true , false           ); break;
                case read_line          :        StdIO   ._execute_read_line    (_retVal                                                   ); break;
                case read_pswd          :        StdIO   ._execute_read_pswd    (_retVal                                                   ); break;

                case resolve_ip         :        MixUtil ._execute_resolve_ip   (_retVal, evalVals                                         ); break;

                case http_head          :        HTTP    ._execute_http_headget (_retVal, evalVals, this, execData, false                  ); break;
                case http_get           :        HTTP    ._execute_http_headget (_retVal, evalVals, this, execData, true                   ); break;
                case http_post          :        HTTP    ._execute_http_post    (_retVal, evalVals, this, execData                         ); break;
                case http_download      :        HTTP    ._execute_http_download(_retVal, evalVals, this, execData                         ); break;
                case http_set_auth      :        HTTP    ._execute_http_set_auth(         evalVals                                         ); break;
                case http_clr_auth      :        HTTP    ._execute_http_clr_auth(         evalVals                                         ); break;
                case ssl_trust_all      :        HTTP    ._execute_ssl_trust_all(         evalVals, this, execData                         ); break;
                case gh_get_tags        :        HTTP    ._execute_gh_get_tags  (_retVal, evalVals                                         ); break;
                case gh_get_assets      :        HTTP    ._execute_gh_get_assets(_retVal, evalVals                                         ); break;

                case ls_serial_ports    :        SerUtil ._execute_ls_ser_ports (_retVal, evalVals                                         ); break;
              //case set_baudrate       :        SerUtil ._execute_set_baudrate (_retVal, evalVals                                         ); break;
                case mcu_reset          :        SerUtil ._execute_mcu_reset    (         evalVals, this, execData                         ); break;
                case mcu_bootloader     :        SerUtil ._execute_mcu_bootload (         evalVals, this, execData                         ); break;
                case mcu_prog_cfg_json  :        MixUtil ._execute_mcu_prog_json(_retVal, evalVals                                         ); break;
                case mcu_prog_cfg_ini   :        MixUtil ._execute_mcu_prog_ini (_retVal, evalVals                                         ); break;
                case mcu_prog_exec      :        MixUtil ._execute_mcu_prog_exec(_retVal, evalVals, this, execData                         ); break;

                case serial_console     :        SerUtil ._execute_sercon       (_retVal, evalVals,                 false, false           ); break;
                case tcp_serial_console :        SerUtil ._execute_sercon       (_retVal, evalVals,                 true , false           ); break;
                case serial_plotter     :        SerUtil ._execute_sercon       (_retVal, evalVals,                 false, true            ); break;
                case tcp_serial_plotter :        SerUtil ._execute_sercon       (_retVal, evalVals,                 true , true            ); break;

                case xmlframe_file      :        XMLFUtil._execute_xmlframe     (_retVal, evalVals,                 false                  ); break;
                case xmlframe_string    :        XMLFUtil._execute_xmlframe     (_retVal, evalVals,                 true                   ); break;
                case xml_escape         :        XMLFUtil._execute_xml_escape   (_retVal, evalVals                                         ); break;

                case aboard_from_file   :        ABrdUtil._execute_ab_from_file (_retVal, evalVals                                         ); break;
                case aboard_selector    :        ABrdUtil._execute_ab_selector  (_retVal, evalVals                                         ); break;
                case aboard_getselconf  :        ABrdUtil._execute_ab_getselconf(_retVal, evalVals                                         ); break;

                case esp_st_decoder     :        MixUtil ._execute_esp_st_dec   (_retVal, evalVals                                         ); break;

                case gdl_cpp_include    :        GDLUtil ._execute_gen_deplf    (         evalVals, this, execData, GDLUtil.DLFT.CppInclude); break;
                case gdl_cpp_module     :        GDLUtil ._execute_gen_deplf    (         evalVals, this, execData, GDLUtil.DLFT.CppModule ); break;
                case gdl_java_import    :        GDLUtil ._execute_gen_deplf    (         evalVals, this, execData, GDLUtil.DLFT.JavaImport); break;

                case pp_java_scf        :        MixUtil ._execute_pp_java_scf  (         evalVals                                         ); break;

                default                 : throw XCom.newJXMFatalLogicError( Texts.EMsg_InvalidFuncName, '$' + _funcSpec.fnName.name() ); // NOTE : This should never got executed!

            } // switch _funcSpec.fnName

        } // try

        catch(final Exception e) {

            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();

            // Ensure the return value of this function call is set to an empty string
            if(_retVal != null) _retVal.add(XCom.VarStr_EmptyString);

            // Store the error message
            final String errMsg = e.toString();
            setErrorFromString(errMsg);

            // If the 'supErr' flag is not set, raise a normal error
            if(!_funcSpec.supErr) return XCom.ExecuteResult.Error;

            // Otherwise, store the actual error message to the execution state so that it can be read by the program later
            execData.execState.setLastSupError(errMsg);

            // Raise a suppressed error
            return XCom.ExecuteResult.SuppressedError;

        } // catch

        // Done
        return xres;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.ExecuteResult _execute_call_exec(final ArrayList<XCom.VariableValue> evalVals, final XCom.ExecData execData, final boolean enShellOper) throws JXMException
    {
        // Error if shell operation is already enabled
        if(execData.enShellOper) throw XCom.newJXMRuntimeError(Texts.EMsg_UserFuncNoCallOther);

        // Get the user-defined function's name
              String  ufuncName = XCom.flatten( evalVals.get(0), "" );
        final boolean callOrig  = ufuncName.equals("__origin__");

        if(callOrig) ufuncName = execData.execState.activeBlockName();

        // Get the user-defined function's execution-blocks
        ExecBlock execBlock = execData.functionMap.get(ufuncName);

        if(execBlock == null) throw XCom.newJXMRuntimeError(Texts.EMsg_UserFuncDefNotExist, ufuncName);

        if(callOrig) {
            ufuncName = ( (Function) execBlock ).getOriginal();
            if(ufuncName == null) throw XCom.newJXMRuntimeError( Texts.EMsg_UserFuncDefNonSuper, execData.execState.activeBlockName() );
            execBlock = execData.functionMap.get(ufuncName);
        }

        final Function function = execData.execState.cboInCallStack( (ContainerBlock) execBlock )
                                ? (Function) execBlock.deepClone() // If this is a recursive call, clone the execution-blocks
                                : (Function) execBlock;

        // Check if the function is deprecated
        final String depreFor = function.getDepreFor();
        if( depreFor != null ) {
            if( depreFor.isEmpty() ) {
                SysUtil.printfWarning( this.getPath(), this.getLNum(), this.getCNum(), Texts.WMsg_DeprecatedUsrFunc0, ufuncName);
            }
            else {
                SysUtil.printfWarning( this.getPath(), this.getLNum(), this.getCNum(), Texts.WMsg_DeprecatedUsrFunc1, ufuncName, depreFor);
            }
        }

        // Push the program state
        execData.execState.pushProgState(function);

        // Enable shell operation as requested
        execData.enShellOper = enShellOper;

        // Map and store the argument(s)
        final ArrayList<String> parNames = function.parNames();

        int reqCnt = 0;
        int optCnt = 0;

        for(final String name : parNames) {
            final boolean isOpt = ( name.charAt(0) == '?' );
            if(isOpt) ++optCnt;
            else      ++reqCnt;
        }

        final int gotArgCnt = evalVals.size() - 1; // Decrement it by one because the first argument is the function name

        if( gotArgCnt < reqCnt          ) throw XCom.newJXMRuntimeError(Texts.EMsg_UserFuncTooFewNumArg,  ufuncName);
        if( gotArgCnt > reqCnt + optCnt ) throw XCom.newJXMRuntimeError(Texts.EMsg_UserFuncTooManyNumArg, ufuncName);

        int idx = 1;
        for(final String name : parNames) {
            final String             varName = ( idx <= reqCnt          ) ? XCom.genRVarName(name) : XCom.genRVarName( name.substring(1) );
            final XCom.VariableValue varVal  = ( idx <  evalVals.size() ) ? evalVals.get(idx++)    : XCom.VarVal_EmptyValue;
            final boolean            varNull = ( varVal.size() == 1 && varVal.get(0).value.equals(XCom.Str_NullArgument) );
            execData.execState.setArgVar(varName, varNull ? XCom.VarVal_EmptyValue : varVal, false, false);
            execData.execState.delLocalVar(varName);
        }

        // Execute the body
        XCom.ExecuteResult xres = function.executeBody(execData);

        // Disable shell operation
        execData.enShellOper = false;

        // Pop the program state
        execData.execState.popProgState();

        // Check for error
        if( function.isErrorBlockSet() ) {
            setErrorFromBlock( function.getErrorBlock() );
        }
        // Save the return value
        else if(xres == XCom.ExecuteResult.FunctionReturn) {
            _retVal = function.getReturnValue();
            xres    = XCom.ExecuteResult.Done;
        }

        // Execute pending shell/operating system commands as needed
        if( xres == XCom.ExecuteResult.Done && execData.shellOper.hasPendingOperations() ) {
            // Execute shell/operating system commands
            if( !execData.shellOper.executeCommands(execData.execState) ) {
                setErrorFromString( "'$exec(" + ufuncName + ")': " + execData.shellOper.getErrorString() );
                xres = XCom.ExecuteResult.Error;
            }
        }

        // Done
        return xres;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveFuncSpec    (dos, _funcSpec );
        XSaver.saveReadVarSpecs(dos, _rvarSpecs);
    }

    public static FuncCall loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new FuncCall(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadFuncSpec(dis),
            XLoader.loadReadVarSpecs(dis)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new FuncCall(
            getPath(),
            getLNum(),
            getCNum(),
            _funcSpec,
            _rvarSpecs
        );
    }

} // class FuncCall
