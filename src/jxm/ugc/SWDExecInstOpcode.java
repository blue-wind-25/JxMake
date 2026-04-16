/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


public class SWDExecInstOpcode {

    /*
     * Specification of instruction opcodes
     *
     * Index# 0                               1                      2                     3                           4
     *
     *        INS_NOP
     *
     *        INS_GET_SWD_FREQUENCY_VI         <dst_variable_index>
     *        INS_SET_SWD_FREQUENCY_VI         <src_variable_index>
     *
     *        INS_GET_CORE_S_LOCKUP_VI         <dst_variable_index>
     *        INS_GET_CORE_S_SLEEPY_VI         <dst_variable_index>
     *        INS_GET_CORE_S_HALT_VI           <dst_variable_index>
     *        INS_GET_CORE_IS_RUNNING_VI       <dst_variable_index>
     *
     *        INS_GET_DP_VERSION_VI            <dst_variable_index>
     *
     *        INS_EXIT_RET_VI                  <val_variable_index>
     *        INS_EXIT_RET_IM                  <32_bit_value>
     *        INS_EXIT_ERR_VI                  <val_variable_index>
     *        INS_EXIT_ERR_IM                  <32_bit_value>
     *
     *        INS_EXIT_RET_IF_CMP_EQ_VI_VI     <chk_variable_index>   <ref_variable_index>
     *        INS_EXIT_RET_IF_CMP_EQ_VI_IM     <chk_variable_index>   <32_bit_value>
     *        INS_EXIT_RET_IF_CMP_NEQ_VI_VI    <chk_variable_index>   <ref_variable_index>
     *        INS_EXIT_RET_IF_CMP_NEQ_VI_IM    <chk_variable_index>   <32_bit_value>
     *
     *        INS_DELAY_US_IM                  <32_bit_delay_time>
     *        INS_DELAY_MS_IM                  <32_bit_delay_time>
     *        INS_THREAD_YIELD
     *
     *        INS_GET_US_VI                    <dst_variable_index>
     *        INS_GET_MS_VI                    <dst_variable_index>
     *        INS_SET_WHILE_TIMEOUT_MS_VI      <src_variable_index>
     *        INS_SET_WHILE_TIMEOUT_MS_IM      <32_bit_value>
     *
     *        INS_HALT_CORE_IM                 <reset_flag>
     *        INS_UNHALT_CORE_IM_IM            <reset_flag>           <enable_debug_flag>
     *
     *        INS_HALT_ALL_CORES
     *        INS_RESET_UNHALT_ALL_CORES
     *
     *        INS_WR_CREG_IM_VI                <reg_index>            <src_variable_index>
     *        INS_WR_CREG_IM_IM                <reg_index>            <32_bit_value>
     *
     *        INS_RD_CREG_VI                   <32_bit_address>       <dst_variable_index>
     *
     *        INS_RD_CREG_ERR_CMP_EQ_IM_VI     <reg_index>            <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_CREG_ERR_CMP_NEQ_IM_VI    <reg_index>            <32_bit_AND_bitmask>   <ref_variable_index>
     *
     *        INS_WR_CMEM_VI_VI                <adr_variable_index>   <cnt_variable_index>   <32_bit_transfer_size>
     *        INS_WR_CMEM_VI_IM                <adr_variable_index>   <32_bit_data_count>    <32_bit_transfer_size>
     *        INS_WR_CMEM_IM_VI                <32_bit_address>       <cnt_variable_index>   <32_bit_transfer_size>
     *        INS_WR_CMEM_IM_IM                <32_bit_address>       <32_bit_data_count>    <32_bit_transfer_size>
     *
     *        INS_RD_CMEM_VI_VI                <adr_variable_index>   <cnt_variable_index>   <32_bit_transfer_size>
     *        INS_RD_CMEM_VI_IM                <adr_variable_index>   <32_bit_data_count>    <32_bit_transfer_size>
     *        INS_RD_CMEM_IM_VI                <32_bit_address>       <cnt_variable_index>   <32_bit_transfer_size>
     *        INS_RD_CMEM_IM_IM                <32_bit_address>       <32_bit_data_count>    <32_bit_transfer_size>
     *
     *        INS_WR_BUS_IM_VI                 <32_bit_address>       <src_variable_index>
     *        INS_WR_BUS_IM_IM                 <32_bit_address>       <32_bit_value>
     *        INS_WR_BUS_VI_VI                 <adr_variable_index>   <src_variable_index>
     *        INS_WR_BUS_VI_IM                 <adr_variable_index>   <32_bit_value>
     *
     *        INS_SET_WR_BUS16
     *        INS_SET_WR_BUS32
     *        INS_WR_BUS16X1_IM_VI             <32_bit_address>       <src_variable_index>
     *        INS_WR_BUS16X2_IM_VI             <32_bit_address>       <src_variable_index>
     *
     *        INS_RD_BUS_RET_VAL_IM            <32_bit_address>       <32_bit_AND_bitmask>
     *        INS_RD_BUS_RET_CMP_EQ_IM_VI      <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_RET_CMP_EQ_IM_IM      <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *        INS_RD_BUS_RET_CMP_NEQ_IM_VI     <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_RET_CMP_NEQ_IM_IM     <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *        INS_RD_BUS_WHILE_EQ_IM_VI        <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_WHILE_EQ_IM_IM        <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *        INS_RD_BUS_WHILE_NEQ_IM_VI       <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_WHILE_NEQ_IM_IM       <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *        INS_RD_BUS_ERR_CMP_EQ_IM_VI      <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_ERR_CMP_EQ_IM_IM      <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *        INS_RD_BUS_ERR_CMP_NEQ_IM_VI     <32_bit_address>       <32_bit_AND_bitmask>   <ref_variable_index>
     *        INS_RD_BUS_ERR_CMP_NEQ_IM_IM     <32_bit_address>       <32_bit_AND_bitmask>   <32_bit_reference_value>
     *
     *        INS_RD_BUS_STR_IM_VI             <32_bit_address>       <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STR_H16_IM_VI         <32_bit_address>       <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STRE_L16_IM_VI        <32_bit_address>       <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STRE_HL16_IM_VI       <32_bit_address>       <32_bit_AND_bitmask>   <dst_variable_index>       <dst_variable_index>
     *
     *        INS_RD_BUS_STR_VI_VI             <src_variable_index>   <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STR_H16_VI_VI         <src_variable_index>   <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STR_L16_VI_VI         <src_variable_index>   <32_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_STR_HL16_VI_VI        <src_variable_index>   <32_bit_AND_bitmask>   <dst_variable_index>       <dst_variable_index>
     *
     *        INS_RD_BUS_16_STR_IM_VI          <32_bit_address>       <16_bit_AND_bitmask>   <dst_variable_index>
     *        INS_RD_BUS_16_STR_VI_VI          <src_variable_index>   <16_bit_AND_bitmask>   <dst_variable_index>
     *
     *        INS_RD_BUS_STR_IM_VI_TRY_CHK     <32_bit_address>       <32_bit_AND_bitmask>   <dst_variable_index>       <32_bit_err_value>     <stt_variable_index>
     *
     *        INS_MD_BUS_IM_IM_VI              <32_bit_address>       <32_bit_clr_mask>      <set_variable_index>
     *        INS_MD_BUS_IM_IM_IM              <32_bit_address>       <32_bit_clr_mask>      <32_bit_set_mask>
     *        INS_MD_BUS_IM_VI_VI              <32_bit_address>       <clr_variable_index>   <set_variable_index>
     *        INS_MD_BUS_IM_VI_IM              <32_bit_address>       <clr_variable_index>   <32_bit_set_mask>
     *
     *        INS_MD_BUS_VI_IM_VI              <adr_variable_index>   <32_bit_clr_mask>      <set_variable_index>
     *        INS_MD_BUS_VI_IM_IM              <adr_variable_index>   <32_bit_clr_mask>      <32_bit_set_mask>
     *        INS_MD_BUS_VI_VI_VI              <adr_variable_index>   <clr_variable_index>   <set_variable_index>
     *        INS_MD_BUS_VI_VI_IM              <adr_variable_index>   <clr_variable_index>   <32_bit_set_mask>
     *
     *        INS_WR_BUS_DB_IM_IM              <32_bit_address>                              <ofs_value>
     *        INS_WR_BUS_DB_IM_VI              <32_bit_address>                              <ofs_variable_index>
     *        INS_WR_BUS_DB_VI_IM              <adr_variable_index>                          <ofs_value>
     *        INS_WR_BUS_DB_VI_VI              <adr_variable_index>                          <ofs_variable_index>
     *
     *        INS_RD_BUS_STR_DB_IM_IM          <32_bit_address>       <32_bit_AND_bitmask>   <ofs_value>
     *        INS_RD_BUS_STR_DB_IM_VI          <32_bit_address>       <32_bit_AND_bitmask>   <ofs_variable_index>
     *        INS_RD_BUS_STR_DB_VI_IM          <adr_variable_index>   <32_bit_AND_bitmask>   <ofs_value>
     *        INS_RD_BUS_STR_DB_VI_VI          <adr_variable_index>   <32_bit_AND_bitmask>   <ofs_variable_index>
     *
     *        INS_WR_BUS_SB_IM_IM              <32_bit_address>                              <ofs_value>
     *        INS_WR_BUS_SB_IM_VI              <32_bit_address>                              <ofs_variable_index>
     *        INS_WR_BUS_SB_VI_IM              <adr_variable_index>                          <ofs_value>
     *        INS_WR_BUS_SB_VI_VI              <adr_variable_index>                          <ofs_variable_index>
     *
     *        INS_RD_BUS_STR_SB_IM_IM          <32_bit_address>       <32_bit_AND_bitmask>   <ofs_value>
     *        INS_RD_BUS_STR_SB_IM_VI          <32_bit_address>       <32_bit_AND_bitmask>   <ofs_variable_index>
     *        INS_RD_BUS_STR_SB_VI_IM          <adr_variable_index>   <32_bit_AND_bitmask>   <ofs_value>
     *        INS_RD_BUS_STR_SB_VI_VI          <adr_variable_index>   <32_bit_AND_bitmask>   <ofs_variable_index>
     *
     *        INS_WR_RAW_MEMAP_IM_IM_IM        <32_bit_value_ap>      <32_bit_value_bnofs>   <32_bit_value>
     *        INS_WR_RAW_MEMAP_IM_IM_VI        <32_bit_value_ap>      <32_bit_value_bnofs>   <src_variable_index>
     *        INS_RD_RAW_MEMAP_STR_IM_IM_VI    <32_bit_value_ap>      <32_bit_value_bnofs>   <dst_variable_index>
     *
     *        INS_WR_RAW_MEMAP3_IM_IM_IM       <address28_4>          <32_bit_value>
     *        INS_WR_RAW_MEMAP3_IM_IM_VI       <address28_4>          <src_variable_index>
     *        INS_RD_RAW_MEMAP3_STR_IM_IM_VI   <address28_4>          <dst_variable_index>
     *
     *
     *        INS_VI_STR_VI                    <src_variable_index>                          <dst_variable_index>
     *        INS_IM_STR_VI                    <32_bit_value>                                <dst_variable_index>
     *
     *        INS_PUSH_VI                      <src_variable_index>...
     *        INS_POP_VI                       <dst_variable_index>...
     *
     *        INS_VI_STR_DB_VI                 <src_variable_index>                          <ofs_variable_index>
     *        INS_IM_STR_DB_VI                 <32_bit_value>                                <ofs_variable_index>
     *        INS_DB_VI_STR_VI                 <ofs_variable_index>                          <dst_variable_index>
     *        INS_DB_IM_STR_VI                 <ofs_value>                                   <dst_variable_index>
     *
     *        INS_VI_STR_SB_VI                 <src_variable_index>                          <ofs_variable_index>
     *        INS_IM_STR_SB_VI                 <32_bit_value>                                <ofs_variable_index>
     *        INS_SB_VI_STR_VI                 <ofs_variable_index>                          <dst_variable_index>
     *        INS_SB_IM_STR_VI                 <ofs_value>                                   <dst_variable_index>
     *
     *        INS_PACK_DB_4X8_32
     *        INS_UNPACK_DB_32_4X8
     *
     *        INS_PUSH_DB
     *        INS_POP_DB
     *
     *        INS_VI_ADD_VI_VI                 <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_ADD_VI_IM                 <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_SUB_VI_VI                 <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_SUB_VI_IM                 <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_MUL_VI_VI                 <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_MUL_VI_IM                 <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_DIV_VI_VI                 <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_DIV_VI_IM                 <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_MOD_VI_VI                 <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_MOD_VI_IM                 <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *
     *        INS_VI_BW_NOT_VI                 <src_variable_index>                          <dst_variable_index>
     *        INS_VI_BW_AND_VI_VI              <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_BW_AND_VI_IM              <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_BW_OR_VI_VI               <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_BW_OR_VI_IM               <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_BW_XOR_VI_VI              <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_BW_XOR_VI_IM              <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_BW_LSH_VI_VI              <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_BW_LSH_VI_IM              <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *        INS_VI_BW_RSH_VI_VI              <src_variable_index>   <src_variable_index>   <dst_variable_index>
     *        INS_VI_BW_RSH_VI_IM              <src_variable_index>   <32_bit_value>         <dst_variable_index>
     *
     *        INS_ERR_CMP_EQ_VI_IM             <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_EQ_VI_VI             <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_NEQ_VI_IM            <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_NEQ_VI_VI            <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_GT_VI_IM             <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_GT_VI_VI             <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_GT_VI_IM_TOUT        <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_GT_VI_VI_TOUT        <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_GTE_VI_IM            <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_GTE_VI_VI            <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_GTE_VI_IM            <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_GTE_VI_VI            <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_LT_VI_IM             <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_LT_VI_VI             <chk_variable_index>   <ref_variable_index>
     *        INS_ERR_CMP_LTE_VI_IM            <chk_variable_index>   <32_bit_value>
     *        INS_ERR_CMP_LTE_VI_VI            <chk_variable_index>   <ref_variable_index>
     *
     *        INS_J                                                                          <relative_jump_index>
     *        INS_J_CMP_EQ_VI_VI               <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_EQ_VI_IM               <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *        INS_J_CMP_NEQ_VI_VI              <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_NEQ_VI_IM              <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *        INS_J_CMP_GT_VI_VI               <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_GT_VI_IM               <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *        INS_J_CMP_GTE_VI_VI              <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_GTE_VI_IM              <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *        INS_J_CMP_LT_VI_VI               <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_LT_VI_IM               <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *        INS_J_CMP_LTE_VI_VI              <src_variable_index>   <src_variable_index>   <relative_jump_index>
     *        INS_J_CMP_LTE_VI_IM              <src_variable_index>   <32_bit_value>         <relative_jump_index>
     *
     *        INS_CALL                                                                      <relative_call_index>
     *        INS_RETURN
     *
     *        INS_LP_LOAD_VI_VI                <lpi_variable_index>   <aps_variable_index>
     *        INS_LP_EXECUTE
     *        INS_LP_CONTINUE
     *
     *        INS_DEBUG_PRINTLN
     *        INS_DEBUG_PRINTLN_SDEC_NN        <src_variable_index>
     *        INS_DEBUG_PRINTLN_SDEC_03        <src_variable_index>
     *        INS_DEBUG_PRINTLN_SDEC_05        <src_variable_index>
     *        INS_DEBUG_PRINTLN_SDEC_08        <src_variable_index>
     *        INS_DEBUG_PRINTLN_SDEC_10        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UDEC_NN        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UDEC_03        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UDEC_05        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UDEC_08        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UDEC_10        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UBIN_NN        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UBIN_08        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UBIN_16        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UBIN_24        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UBIN_32        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UOCT_NN        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UOCT_03        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UOCT_06        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UOCT_08        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UOCT_11        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UHEX_NN        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UHEX_02        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UHEX_04        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UHEX_06        <src_variable_index>
     *        INS_DEBUG_PRINTLN_UHEX_08        <src_variable_index>
     *        INS_DEBUG_PRINTF                 <fmt_variable_index>   <arg_variable_index>...
     */

    //                                                       0-GG-SS-NN
    public static final int INS_NOP                        = 0000000000;

    public static final int INS_GET_SWD_FREQUENCY_VI       = 0000000010;
    public static final int INS_SET_SWD_FREQUENCY_VI       = 0000000011;
    public static final int INS_SWD_LINE_RESET             = 0000000017;

    public static final int INS_GET_CORE_S_LOCKUP_VI       = 0000000020;
    public static final int INS_GET_CORE_S_SLEEP_VI        = 0000000021;
    public static final int INS_GET_CORE_S_HALT_VI         = 0000000022;
    public static final int INS_GET_CORE_IS_RUNNING_VI     = 0000000023;

    public static final int INS_GET_DP_VERSION_VI          = 0000000024;

    public static final int INS_EXIT_RET_VI                = 0000001000;
    public static final int INS_EXIT_RET_IM                = 0000001001;
    public static final int INS_EXIT_ERR_VI                = 0000001010;
    public static final int INS_EXIT_ERR_IM                = 0000001011;

    public static final int INS_EXIT_RET_IF_CMP_EQ_VI_VI   = 0000001020;
    public static final int INS_EXIT_RET_IF_CMP_EQ_VI_IM   = 0000001021;
    public static final int INS_EXIT_RET_IF_CMP_NEQ_VI_VI  = 0000001022;
    public static final int INS_EXIT_RET_IF_CMP_NEQ_VI_IM  = 0000001023;

    public static final int INS_DELAY_US_IM                = 0000002000;
    public static final int INS_DELAY_MS_IM                = 0000002001;
    public static final int INS_THREAD_YIELD               = 0000002002;

    public static final int INS_GET_US_VI                  = 0000002010;
    public static final int INS_GET_MS_VI                  = 0000002011;
    public static final int INS_SET_WHILE_TIMEOUT_MS_VI    = 0000002012;
    public static final int INS_SET_WHILE_TIMEOUT_MS_IM    = 0000002013;

    public static final int INS_HALT_CORE_IM               = 0000003000;
    public static final int INS_UNHALT_CORE_IM_IM          = 0000003001;

    public static final int INS_HALT_ALL_CORES             = 0000003002;
    public static final int INS_RESET_UNHALT_ALL_CORES     = 0000003003;

    public static final int INS_WR_CREG_IM_VI              = 0001000000;
    public static final int INS_WR_CREG_IM_IM              = 0001000001;

    public static final int INS_RD_CREG_VI                 = 0001000010;

    public static final int INS_RD_CREG_ERR_CMP_EQ_IM_VI   = 0001000020;
    public static final int INS_RD_CREG_ERR_CMP_NEQ_IM_VI  = 0001000021;

    public static final int INS_WR_CMEM_VI_VI              = 0001001000; // Write from 'dataBuff[...]'
    public static final int INS_WR_CMEM_VI_IM              = 0001001001; // ---
    public static final int INS_WR_CMEM_IM_VI              = 0001001002; // ---
    public static final int INS_WR_CMEM_IM_IM              = 0001001003; // ---

    public static final int INS_RD_CMEM_VI_VI              = 0001001010; // Read to 'dataBuff[...]'
    public static final int INS_RD_CMEM_VI_IM              = 0001001011; // ---
    public static final int INS_RD_CMEM_IM_VI              = 0001001012; // ---
    public static final int INS_RD_CMEM_IM_IM              = 0001001013; // ---

    public static final int INS_WR_BUS_IM_VI               = 0002000000;
    public static final int INS_WR_BUS_IM_IM               = 0002000001;
    public static final int INS_WR_BUS_VI_VI               = 0002000010;
    public static final int INS_WR_BUS_VI_IM               = 0002000011;

    public static final int INS_SET_WR_BUS16               = 0002000100;
    public static final int INS_SET_WR_BUS32               = 0002000101;
    public static final int INS_WR_BUS16X1_IM_VI           = 0002000110;
    public static final int INS_WR_BUS16X2_IM_VI           = 0002000111;

    public static final int INS_RD_BUS_RET_VAL_IM          = 0002001000;
    public static final int INS_RD_BUS_RET_CMP_EQ_IM_VI    = 0002002000;
    public static final int INS_RD_BUS_RET_CMP_EQ_IM_IM    = 0002002001;
    public static final int INS_RD_BUS_RET_CMP_NEQ_IM_VI   = 0002002002;
    public static final int INS_RD_BUS_RET_CMP_NEQ_IM_IM   = 0002002003;
    public static final int INS_RD_BUS_WHILE_EQ_IM_VI      = 0002003000;
    public static final int INS_RD_BUS_WHILE_EQ_IM_IM      = 0002003001;
    public static final int INS_RD_BUS_WHILE_NEQ_IM_VI     = 0002003002;
    public static final int INS_RD_BUS_WHILE_NEQ_IM_IM     = 0002003003;
    public static final int INS_RD_BUS_ERR_CMP_EQ_IM_VI    = 0002004000;
    public static final int INS_RD_BUS_ERR_CMP_EQ_IM_IM    = 0002004001;
    public static final int INS_RD_BUS_ERR_CMP_NEQ_IM_VI   = 0002004002;
    public static final int INS_RD_BUS_ERR_CMP_NEQ_IM_IM   = 0002004003;

    public static final int INS_RD_BUS_STR_IM_VI           = 0002005000;
    public static final int INS_RD_BUS_STR_H16_IM_VI       = 0002005001;
    public static final int INS_RD_BUS_STR_L16_IM_VI       = 0002005002;
    public static final int INS_RD_BUS_STR_HL16_IM_VI      = 0002005003;

    public static final int INS_RD_BUS_STR_VI_VI           = 0002005010;
    public static final int INS_RD_BUS_STR_H16_VI_VI       = 0002005011;
    public static final int INS_RD_BUS_STR_L16_VI_VI       = 0002005012;
    public static final int INS_RD_BUS_STR_HL16_VI_VI      = 0002005013;

    public static final int INS_RD_BUS_16_STR_IM_VI        = 0002005020;
    public static final int INS_RD_BUS_16_STR_VI_VI        = 0002005021;

    public static final int INS_RD_BUS_STR_IM_VI_TRY_CHK   = 0002005070;

    public static final int INS_MD_BUS_IM_IM_VI            = 0002006000;
    public static final int INS_MD_BUS_IM_IM_IM            = 0002006001;
    public static final int INS_MD_BUS_IM_VI_VI            = 0002006010;
    public static final int INS_MD_BUS_IM_VI_IM            = 0002006011;

    public static final int INS_MD_BUS_VI_IM_VI            = 0002006100;
    public static final int INS_MD_BUS_VI_IM_IM            = 0002006101;
    public static final int INS_MD_BUS_VI_VI_VI            = 0002006110;
    public static final int INS_MD_BUS_VI_VI_IM            = 0002006111;

    public static final int INS_WR_BUS_DB_IM_IM            = 0002007000; // Write from 'dataBuff[ofs]'
    public static final int INS_WR_BUS_DB_IM_VI            = 0002007010; // ---
    public static final int INS_WR_BUS_DB_VI_IM            = 0002007020; // ---
    public static final int INS_WR_BUS_DB_VI_VI            = 0002007030; // ---

    public static final int INS_RD_BUS_STR_DB_IM_IM        = 0002007100; // Read to 'dataBuff[ofs]'
    public static final int INS_RD_BUS_STR_DB_IM_VI        = 0002007110; // ---
    public static final int INS_RD_BUS_STR_DB_VI_IM        = 0002007120; // ---
    public static final int INS_RD_BUS_STR_DB_VI_VI        = 0002007130; // ---

    public static final int INS_WR_BUS_SB_IM_IM            = 0002007001; // Write from 'scratchBuff[ofs]' ; NOTE : The LSB of the 'INS_*' opcode must always be set to 0b1 because it serves as a marker bitmask
    public static final int INS_WR_BUS_SB_IM_VI            = 0002007011; // ---
    public static final int INS_WR_BUS_SB_VI_IM            = 0002007021; // ---
    public static final int INS_WR_BUS_SB_VI_VI            = 0002007031; // ---

    public static final int INS_RD_BUS_STR_SB_IM_IM        = 0002007101; // Read to 'scratchBuff[ofs]'    ; NOTE : The LSB of the 'INS_*' opcode must always be set to 0b1 because it serves as a marker bitmask
    public static final int INS_RD_BUS_STR_SB_IM_VI        = 0002007111; // ---
    public static final int INS_RD_BUS_STR_SB_VI_IM        = 0002007121; // ---
    public static final int INS_RD_BUS_STR_SB_VI_VI        = 0002007131; // ---

    public static final int INS_WR_RAW_MEMAP_IM_IM_IM      = 0002007200;
    public static final int INS_WR_RAW_MEMAP_IM_IM_VI      = 0002007201;
    public static final int INS_RD_RAW_MEMAP_STR_IM_IM_VI  = 0002007202;

    public static final int INS_WR_RAW_MEMAP3_IM_IM_IM     = 0002007230;
    public static final int INS_WR_RAW_MEMAP3_IM_IM_VI     = 0002007231;
    public static final int INS_RD_RAW_MEMAP3_STR_IM_IM_VI = 0002007232;

    public static final int INS_VI_STR_VI                  = 0003000000;
    public static final int INS_IM_STR_VI                  = 0003000001;

    public static final int INS_PUSH_VI                    = 0003000010;
    public static final int INS_POP_VI                     = 0003000011;

    public static final int INS_VI_STR_DB_VI               = 0003001000; // Write to  'dataBuff[ofs]'
    public static final int INS_IM_STR_DB_VI               = 0003001010; // ---
    public static final int INS_DB_VI_STR_VI               = 0003001020; // Read from 'dataBuff[ofs]'
    public static final int INS_DB_IM_STR_VI               = 0003001030; // ---

    public static final int INS_VI_STR_SB_VI               = 0003001001; // Write to  'scratchBuff[ofs]'  ; NOTE : The LSB of the 'INS_*' opcode must always be set to 0b1 because it serves as a marker bitmask
    public static final int INS_IM_STR_SB_VI               = 0003001011; // ---
    public static final int INS_SB_VI_STR_VI               = 0003001021; // Read from 'scratchBuff[ofs]'  ; NOTE : The LSB of the 'INS_*' opcode must always be set to 0b1 because it serves as a marker bitmask
    public static final int INS_SB_IM_STR_VI               = 0003001031; // ---

    public static final int INS_PACK_DB_4X8_32             = 0003001100; // Pack    'dataBuff[...]' (four  8-bit bytes to one  32-bit word )
    public static final int INS_UNPACK_DB_32_4X8           = 0003001110; // Unpack  'dataBuff[...]' (one  32-bit word  to four  8-bit bytes)

    public static final int INS_PUSH_DB                    = 0003001120; // Save    'dataBuff[...]'
    public static final int INS_POP_DB                     = 0003001130; // Restore 'dataBuff[...]'

    public static final int INS_VI_ADD_VI_VI               = 0003002000;
    public static final int INS_VI_ADD_VI_IM               = 0003002001;
    public static final int INS_VI_SUB_VI_VI               = 0003002010;
    public static final int INS_VI_SUB_VI_IM               = 0003002011;
    public static final int INS_VI_MUL_VI_VI               = 0003002020;
    public static final int INS_VI_MUL_VI_IM               = 0003002021;
    public static final int INS_VI_DIV_VI_VI               = 0003002030;
    public static final int INS_VI_DIV_VI_IM               = 0003002031;
    public static final int INS_VI_MOD_VI_VI               = 0003002040;
    public static final int INS_VI_MOD_VI_IM               = 0003002041;

    public static final int INS_VI_BW_NOT_VI               = 0003003000;
    public static final int INS_VI_BW_AND_VI_VI            = 0003003010;
    public static final int INS_VI_BW_AND_VI_IM            = 0003003011;
    public static final int INS_VI_BW_OR_VI_VI             = 0003003020;
    public static final int INS_VI_BW_OR_VI_IM             = 0003003021;
    public static final int INS_VI_BW_XOR_VI_VI            = 0003003030;
    public static final int INS_VI_BW_XOR_VI_IM            = 0003003031;
    public static final int INS_VI_BW_LSH_VI_VI            = 0003003040;
    public static final int INS_VI_BW_LSH_VI_IM            = 0003003041;
    public static final int INS_VI_BW_RSH_VI_VI            = 0003003050;
    public static final int INS_VI_BW_RSH_VI_IM            = 0003003051;

    public static final int INS_ERR_CMP_EQ_VI_IM           = 0003004000;
    public static final int INS_ERR_CMP_EQ_VI_VI           = 0003004010;
    public static final int INS_ERR_CMP_NEQ_VI_IM          = 0003004020;
    public static final int INS_ERR_CMP_NEQ_VI_VI          = 0003004030;
    public static final int INS_ERR_CMP_GT_VI_IM           = 0003004100;
    public static final int INS_ERR_CMP_GT_VI_VI           = 0003004110;
    public static final int INS_ERR_CMP_GT_VI_IM_TOUT      = 0003004101; // NOTE : The LSB of the 'INS_*' opcode must always be set to 0b1 because it serves as a marker bitmask
    public static final int INS_ERR_CMP_GT_VI_VI_TOUT      = 0003004111; // ---
    public static final int INS_ERR_CMP_GTE_VI_IM          = 0003004120;
    public static final int INS_ERR_CMP_GTE_VI_VI          = 0003004130;
    public static final int INS_ERR_CMP_LT_VI_IM           = 0003004200;
    public static final int INS_ERR_CMP_LT_VI_VI           = 0003004210;
    public static final int INS_ERR_CMP_LTE_VI_IM          = 0003004220;
    public static final int INS_ERR_CMP_LTE_VI_VI          = 0003004230;

    public static final int INS_J                          = 0004000000;
    public static final int INS_J_CMP_EQ_VI_VI             = 0004001000;
    public static final int INS_J_CMP_EQ_VI_IM             = 0004001001;
    public static final int INS_J_CMP_NEQ_VI_VI            = 0004001010;
    public static final int INS_J_CMP_NEQ_VI_IM            = 0004001011;
    public static final int INS_J_CMP_GT_VI_VI             = 0004002020;
    public static final int INS_J_CMP_GT_VI_IM             = 0004002021;
    public static final int INS_J_CMP_GTE_VI_VI            = 0004002030;
    public static final int INS_J_CMP_GTE_VI_IM            = 0004002031;
    public static final int INS_J_CMP_LT_VI_VI             = 0004003040;
    public static final int INS_J_CMP_LT_VI_IM             = 0004003041;
    public static final int INS_J_CMP_LTE_VI_VI            = 0004003050;
    public static final int INS_J_CMP_LTE_VI_IM            = 0004003051;

    public static final int INS_CALL                       = 0004004000;
    public static final int INS_RETURN                     = 0004004001;

    public static final int INS_LP_LOAD_VI_VI              = 0005000000;
    public static final int INS_LP_EXECUTE                 = 0005000001;
    public static final int INS_LP_CONTINUE                = 0005000002;

    public static final int INS_DEBUG_PRINTLN              = 0077000000;
    public static final int INS_DEBUG_PRINTLN_SDEC_NN      = 0077000010;
    public static final int INS_DEBUG_PRINTLN_SDEC_03      = 0077000011;
    public static final int INS_DEBUG_PRINTLN_SDEC_05      = 0077000012;
    public static final int INS_DEBUG_PRINTLN_SDEC_08      = 0077000013;
    public static final int INS_DEBUG_PRINTLN_SDEC_10      = 0077000014;
    public static final int INS_DEBUG_PRINTLN_UDEC_NN      = 0077000020;
    public static final int INS_DEBUG_PRINTLN_UDEC_03      = 0077000021;
    public static final int INS_DEBUG_PRINTLN_UDEC_05      = 0077000022;
    public static final int INS_DEBUG_PRINTLN_UDEC_08      = 0077000023;
    public static final int INS_DEBUG_PRINTLN_UDEC_10      = 0077000024;
    public static final int INS_DEBUG_PRINTLN_UBIN_NN      = 0077000030;
    public static final int INS_DEBUG_PRINTLN_UBIN_08      = 0077000031;
    public static final int INS_DEBUG_PRINTLN_UBIN_16      = 0077000032;
    public static final int INS_DEBUG_PRINTLN_UBIN_24      = 0077000033;
    public static final int INS_DEBUG_PRINTLN_UBIN_32      = 0077000034;
    public static final int INS_DEBUG_PRINTLN_UOCT_NN      = 0077000040;
    public static final int INS_DEBUG_PRINTLN_UOCT_03      = 0077000041;
    public static final int INS_DEBUG_PRINTLN_UOCT_06      = 0077000042;
    public static final int INS_DEBUG_PRINTLN_UOCT_08      = 0077000043;
    public static final int INS_DEBUG_PRINTLN_UOCT_11      = 0077000044;
    public static final int INS_DEBUG_PRINTLN_UHEX_NN      = 0077000050;
    public static final int INS_DEBUG_PRINTLN_UHEX_02      = 0077000051;
    public static final int INS_DEBUG_PRINTLN_UHEX_04      = 0077000052;
    public static final int INS_DEBUG_PRINTLN_UHEX_06      = 0077000053;
    public static final int INS_DEBUG_PRINTLN_UHEX_08      = 0077000054;
    public static final int INS_DEBUG_PRINTF               = 0077000070;

    @package_private
    static final int __INS_J__BEG__ = INS_J;

    @package_private
    static final int __INS_J__END__ = INS_CALL;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Constants for signals that are used to synchronize work between the main thread and worker threads
     */

    public static final int XVI_SIGNAL_WORKER_WAIT     =  0; // Instruct the worker thread to wait
    public static final int XVI_SIGNAL_WORKER_EXECUTE  =  1; // Instruct the worker thread to execute the job
    public static final int XVI_SIGNAL_WORKER_EXIT     = -1; // Instruct the worker thread to exit

    public static final int XVI_SIGNAL_JOB_IN_PROGRESS =  0; // Tell the main thread that the job is in progress (or the worker thread is ready)
    public static final int XVI_SIGNAL_JOB_COMPLETE    =  1; // Tell the main thread that the job is complete
    public static final int XVI_SIGNAL_JOB_ERROR       = -1; // Tell the main thread that there is an unrecoverable error (the worker thread will exit after sending this notification)
    public static final int XVI_SIGNAL_JOB_WAIT        =  2; // Set by the main thread before starting the worker thread and preparing the next job for the worker thread so that the main thread can wait until the worker thread is ready

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int INS_EXEC_VAR_BUFF_SIZE_USER               = 1024;
    public static final int INS_EXEC_VAR_BUFF_SIZE_BUILDER_INTERNAL   =  768;
    public static final int INS_EXEC_VAR_BUFF_SIZE_BUILDER_TRANSITORY =  256;
    public static final int INS_EXEC_VAR_BUFF_SIZE_TOTAL              = INS_EXEC_VAR_BUFF_SIZE_USER + INS_EXEC_VAR_BUFF_SIZE_BUILDER_INTERNAL + INS_EXEC_VAR_BUFF_SIZE_BUILDER_TRANSITORY;

    public static enum XVI {

        /*
         * User-accessible execution-variable indexes
         *
         * echo; for I in {0..1023}; do printf '_%04d(0x%03X), ' $I $I; if (( ($I + 1) % 32 == 0 )); then echo; fi; done; echo
         */
        _0000(0x000), _0001(0x001), _0002(0x002), _0003(0x003), _0004(0x004), _0005(0x005), _0006(0x006), _0007(0x007), _0008(0x008), _0009(0x009), _0010(0x00A), _0011(0x00B), _0012(0x00C), _0013(0x00D), _0014(0x00E), _0015(0x00F), _0016(0x010), _0017(0x011), _0018(0x012), _0019(0x013), _0020(0x014), _0021(0x015), _0022(0x016), _0023(0x017), _0024(0x018), _0025(0x019), _0026(0x01A), _0027(0x01B), _0028(0x01C), _0029(0x01D), _0030(0x01E), _0031(0x01F),
        _0032(0x020), _0033(0x021), _0034(0x022), _0035(0x023), _0036(0x024), _0037(0x025), _0038(0x026), _0039(0x027), _0040(0x028), _0041(0x029), _0042(0x02A), _0043(0x02B), _0044(0x02C), _0045(0x02D), _0046(0x02E), _0047(0x02F), _0048(0x030), _0049(0x031), _0050(0x032), _0051(0x033), _0052(0x034), _0053(0x035), _0054(0x036), _0055(0x037), _0056(0x038), _0057(0x039), _0058(0x03A), _0059(0x03B), _0060(0x03C), _0061(0x03D), _0062(0x03E), _0063(0x03F),
        _0064(0x040), _0065(0x041), _0066(0x042), _0067(0x043), _0068(0x044), _0069(0x045), _0070(0x046), _0071(0x047), _0072(0x048), _0073(0x049), _0074(0x04A), _0075(0x04B), _0076(0x04C), _0077(0x04D), _0078(0x04E), _0079(0x04F), _0080(0x050), _0081(0x051), _0082(0x052), _0083(0x053), _0084(0x054), _0085(0x055), _0086(0x056), _0087(0x057), _0088(0x058), _0089(0x059), _0090(0x05A), _0091(0x05B), _0092(0x05C), _0093(0x05D), _0094(0x05E), _0095(0x05F),
        _0096(0x060), _0097(0x061), _0098(0x062), _0099(0x063), _0100(0x064), _0101(0x065), _0102(0x066), _0103(0x067), _0104(0x068), _0105(0x069), _0106(0x06A), _0107(0x06B), _0108(0x06C), _0109(0x06D), _0110(0x06E), _0111(0x06F), _0112(0x070), _0113(0x071), _0114(0x072), _0115(0x073), _0116(0x074), _0117(0x075), _0118(0x076), _0119(0x077), _0120(0x078), _0121(0x079), _0122(0x07A), _0123(0x07B), _0124(0x07C), _0125(0x07D), _0126(0x07E), _0127(0x07F),
        _0128(0x080), _0129(0x081), _0130(0x082), _0131(0x083), _0132(0x084), _0133(0x085), _0134(0x086), _0135(0x087), _0136(0x088), _0137(0x089), _0138(0x08A), _0139(0x08B), _0140(0x08C), _0141(0x08D), _0142(0x08E), _0143(0x08F), _0144(0x090), _0145(0x091), _0146(0x092), _0147(0x093), _0148(0x094), _0149(0x095), _0150(0x096), _0151(0x097), _0152(0x098), _0153(0x099), _0154(0x09A), _0155(0x09B), _0156(0x09C), _0157(0x09D), _0158(0x09E), _0159(0x09F),
        _0160(0x0A0), _0161(0x0A1), _0162(0x0A2), _0163(0x0A3), _0164(0x0A4), _0165(0x0A5), _0166(0x0A6), _0167(0x0A7), _0168(0x0A8), _0169(0x0A9), _0170(0x0AA), _0171(0x0AB), _0172(0x0AC), _0173(0x0AD), _0174(0x0AE), _0175(0x0AF), _0176(0x0B0), _0177(0x0B1), _0178(0x0B2), _0179(0x0B3), _0180(0x0B4), _0181(0x0B5), _0182(0x0B6), _0183(0x0B7), _0184(0x0B8), _0185(0x0B9), _0186(0x0BA), _0187(0x0BB), _0188(0x0BC), _0189(0x0BD), _0190(0x0BE), _0191(0x0BF),
        _0192(0x0C0), _0193(0x0C1), _0194(0x0C2), _0195(0x0C3), _0196(0x0C4), _0197(0x0C5), _0198(0x0C6), _0199(0x0C7), _0200(0x0C8), _0201(0x0C9), _0202(0x0CA), _0203(0x0CB), _0204(0x0CC), _0205(0x0CD), _0206(0x0CE), _0207(0x0CF), _0208(0x0D0), _0209(0x0D1), _0210(0x0D2), _0211(0x0D3), _0212(0x0D4), _0213(0x0D5), _0214(0x0D6), _0215(0x0D7), _0216(0x0D8), _0217(0x0D9), _0218(0x0DA), _0219(0x0DB), _0220(0x0DC), _0221(0x0DD), _0222(0x0DE), _0223(0x0DF),
        _0224(0x0E0), _0225(0x0E1), _0226(0x0E2), _0227(0x0E3), _0228(0x0E4), _0229(0x0E5), _0230(0x0E6), _0231(0x0E7), _0232(0x0E8), _0233(0x0E9), _0234(0x0EA), _0235(0x0EB), _0236(0x0EC), _0237(0x0ED), _0238(0x0EE), _0239(0x0EF), _0240(0x0F0), _0241(0x0F1), _0242(0x0F2), _0243(0x0F3), _0244(0x0F4), _0245(0x0F5), _0246(0x0F6), _0247(0x0F7), _0248(0x0F8), _0249(0x0F9), _0250(0x0FA), _0251(0x0FB), _0252(0x0FC), _0253(0x0FD), _0254(0x0FE), _0255(0x0FF),
        _0256(0x100), _0257(0x101), _0258(0x102), _0259(0x103), _0260(0x104), _0261(0x105), _0262(0x106), _0263(0x107), _0264(0x108), _0265(0x109), _0266(0x10A), _0267(0x10B), _0268(0x10C), _0269(0x10D), _0270(0x10E), _0271(0x10F), _0272(0x110), _0273(0x111), _0274(0x112), _0275(0x113), _0276(0x114), _0277(0x115), _0278(0x116), _0279(0x117), _0280(0x118), _0281(0x119), _0282(0x11A), _0283(0x11B), _0284(0x11C), _0285(0x11D), _0286(0x11E), _0287(0x11F),
        _0288(0x120), _0289(0x121), _0290(0x122), _0291(0x123), _0292(0x124), _0293(0x125), _0294(0x126), _0295(0x127), _0296(0x128), _0297(0x129), _0298(0x12A), _0299(0x12B), _0300(0x12C), _0301(0x12D), _0302(0x12E), _0303(0x12F), _0304(0x130), _0305(0x131), _0306(0x132), _0307(0x133), _0308(0x134), _0309(0x135), _0310(0x136), _0311(0x137), _0312(0x138), _0313(0x139), _0314(0x13A), _0315(0x13B), _0316(0x13C), _0317(0x13D), _0318(0x13E), _0319(0x13F),
        _0320(0x140), _0321(0x141), _0322(0x142), _0323(0x143), _0324(0x144), _0325(0x145), _0326(0x146), _0327(0x147), _0328(0x148), _0329(0x149), _0330(0x14A), _0331(0x14B), _0332(0x14C), _0333(0x14D), _0334(0x14E), _0335(0x14F), _0336(0x150), _0337(0x151), _0338(0x152), _0339(0x153), _0340(0x154), _0341(0x155), _0342(0x156), _0343(0x157), _0344(0x158), _0345(0x159), _0346(0x15A), _0347(0x15B), _0348(0x15C), _0349(0x15D), _0350(0x15E), _0351(0x15F),
        _0352(0x160), _0353(0x161), _0354(0x162), _0355(0x163), _0356(0x164), _0357(0x165), _0358(0x166), _0359(0x167), _0360(0x168), _0361(0x169), _0362(0x16A), _0363(0x16B), _0364(0x16C), _0365(0x16D), _0366(0x16E), _0367(0x16F), _0368(0x170), _0369(0x171), _0370(0x172), _0371(0x173), _0372(0x174), _0373(0x175), _0374(0x176), _0375(0x177), _0376(0x178), _0377(0x179), _0378(0x17A), _0379(0x17B), _0380(0x17C), _0381(0x17D), _0382(0x17E), _0383(0x17F),
        _0384(0x180), _0385(0x181), _0386(0x182), _0387(0x183), _0388(0x184), _0389(0x185), _0390(0x186), _0391(0x187), _0392(0x188), _0393(0x189), _0394(0x18A), _0395(0x18B), _0396(0x18C), _0397(0x18D), _0398(0x18E), _0399(0x18F), _0400(0x190), _0401(0x191), _0402(0x192), _0403(0x193), _0404(0x194), _0405(0x195), _0406(0x196), _0407(0x197), _0408(0x198), _0409(0x199), _0410(0x19A), _0411(0x19B), _0412(0x19C), _0413(0x19D), _0414(0x19E), _0415(0x19F),
        _0416(0x1A0), _0417(0x1A1), _0418(0x1A2), _0419(0x1A3), _0420(0x1A4), _0421(0x1A5), _0422(0x1A6), _0423(0x1A7), _0424(0x1A8), _0425(0x1A9), _0426(0x1AA), _0427(0x1AB), _0428(0x1AC), _0429(0x1AD), _0430(0x1AE), _0431(0x1AF), _0432(0x1B0), _0433(0x1B1), _0434(0x1B2), _0435(0x1B3), _0436(0x1B4), _0437(0x1B5), _0438(0x1B6), _0439(0x1B7), _0440(0x1B8), _0441(0x1B9), _0442(0x1BA), _0443(0x1BB), _0444(0x1BC), _0445(0x1BD), _0446(0x1BE), _0447(0x1BF),
        _0448(0x1C0), _0449(0x1C1), _0450(0x1C2), _0451(0x1C3), _0452(0x1C4), _0453(0x1C5), _0454(0x1C6), _0455(0x1C7), _0456(0x1C8), _0457(0x1C9), _0458(0x1CA), _0459(0x1CB), _0460(0x1CC), _0461(0x1CD), _0462(0x1CE), _0463(0x1CF), _0464(0x1D0), _0465(0x1D1), _0466(0x1D2), _0467(0x1D3), _0468(0x1D4), _0469(0x1D5), _0470(0x1D6), _0471(0x1D7), _0472(0x1D8), _0473(0x1D9), _0474(0x1DA), _0475(0x1DB), _0476(0x1DC), _0477(0x1DD), _0478(0x1DE), _0479(0x1DF),
        _0480(0x1E0), _0481(0x1E1), _0482(0x1E2), _0483(0x1E3), _0484(0x1E4), _0485(0x1E5), _0486(0x1E6), _0487(0x1E7), _0488(0x1E8), _0489(0x1E9), _0490(0x1EA), _0491(0x1EB), _0492(0x1EC), _0493(0x1ED), _0494(0x1EE), _0495(0x1EF), _0496(0x1F0), _0497(0x1F1), _0498(0x1F2), _0499(0x1F3), _0500(0x1F4), _0501(0x1F5), _0502(0x1F6), _0503(0x1F7), _0504(0x1F8), _0505(0x1F9), _0506(0x1FA), _0507(0x1FB), _0508(0x1FC), _0509(0x1FD), _0510(0x1FE), _0511(0x1FF),
        _0512(0x200), _0513(0x201), _0514(0x202), _0515(0x203), _0516(0x204), _0517(0x205), _0518(0x206), _0519(0x207), _0520(0x208), _0521(0x209), _0522(0x20A), _0523(0x20B), _0524(0x20C), _0525(0x20D), _0526(0x20E), _0527(0x20F), _0528(0x210), _0529(0x211), _0530(0x212), _0531(0x213), _0532(0x214), _0533(0x215), _0534(0x216), _0535(0x217), _0536(0x218), _0537(0x219), _0538(0x21A), _0539(0x21B), _0540(0x21C), _0541(0x21D), _0542(0x21E), _0543(0x21F),
        _0544(0x220), _0545(0x221), _0546(0x222), _0547(0x223), _0548(0x224), _0549(0x225), _0550(0x226), _0551(0x227), _0552(0x228), _0553(0x229), _0554(0x22A), _0555(0x22B), _0556(0x22C), _0557(0x22D), _0558(0x22E), _0559(0x22F), _0560(0x230), _0561(0x231), _0562(0x232), _0563(0x233), _0564(0x234), _0565(0x235), _0566(0x236), _0567(0x237), _0568(0x238), _0569(0x239), _0570(0x23A), _0571(0x23B), _0572(0x23C), _0573(0x23D), _0574(0x23E), _0575(0x23F),
        _0576(0x240), _0577(0x241), _0578(0x242), _0579(0x243), _0580(0x244), _0581(0x245), _0582(0x246), _0583(0x247), _0584(0x248), _0585(0x249), _0586(0x24A), _0587(0x24B), _0588(0x24C), _0589(0x24D), _0590(0x24E), _0591(0x24F), _0592(0x250), _0593(0x251), _0594(0x252), _0595(0x253), _0596(0x254), _0597(0x255), _0598(0x256), _0599(0x257), _0600(0x258), _0601(0x259), _0602(0x25A), _0603(0x25B), _0604(0x25C), _0605(0x25D), _0606(0x25E), _0607(0x25F),
        _0608(0x260), _0609(0x261), _0610(0x262), _0611(0x263), _0612(0x264), _0613(0x265), _0614(0x266), _0615(0x267), _0616(0x268), _0617(0x269), _0618(0x26A), _0619(0x26B), _0620(0x26C), _0621(0x26D), _0622(0x26E), _0623(0x26F), _0624(0x270), _0625(0x271), _0626(0x272), _0627(0x273), _0628(0x274), _0629(0x275), _0630(0x276), _0631(0x277), _0632(0x278), _0633(0x279), _0634(0x27A), _0635(0x27B), _0636(0x27C), _0637(0x27D), _0638(0x27E), _0639(0x27F),
        _0640(0x280), _0641(0x281), _0642(0x282), _0643(0x283), _0644(0x284), _0645(0x285), _0646(0x286), _0647(0x287), _0648(0x288), _0649(0x289), _0650(0x28A), _0651(0x28B), _0652(0x28C), _0653(0x28D), _0654(0x28E), _0655(0x28F), _0656(0x290), _0657(0x291), _0658(0x292), _0659(0x293), _0660(0x294), _0661(0x295), _0662(0x296), _0663(0x297), _0664(0x298), _0665(0x299), _0666(0x29A), _0667(0x29B), _0668(0x29C), _0669(0x29D), _0670(0x29E), _0671(0x29F),
        _0672(0x2A0), _0673(0x2A1), _0674(0x2A2), _0675(0x2A3), _0676(0x2A4), _0677(0x2A5), _0678(0x2A6), _0679(0x2A7), _0680(0x2A8), _0681(0x2A9), _0682(0x2AA), _0683(0x2AB), _0684(0x2AC), _0685(0x2AD), _0686(0x2AE), _0687(0x2AF), _0688(0x2B0), _0689(0x2B1), _0690(0x2B2), _0691(0x2B3), _0692(0x2B4), _0693(0x2B5), _0694(0x2B6), _0695(0x2B7), _0696(0x2B8), _0697(0x2B9), _0698(0x2BA), _0699(0x2BB), _0700(0x2BC), _0701(0x2BD), _0702(0x2BE), _0703(0x2BF),
        _0704(0x2C0), _0705(0x2C1), _0706(0x2C2), _0707(0x2C3), _0708(0x2C4), _0709(0x2C5), _0710(0x2C6), _0711(0x2C7), _0712(0x2C8), _0713(0x2C9), _0714(0x2CA), _0715(0x2CB), _0716(0x2CC), _0717(0x2CD), _0718(0x2CE), _0719(0x2CF), _0720(0x2D0), _0721(0x2D1), _0722(0x2D2), _0723(0x2D3), _0724(0x2D4), _0725(0x2D5), _0726(0x2D6), _0727(0x2D7), _0728(0x2D8), _0729(0x2D9), _0730(0x2DA), _0731(0x2DB), _0732(0x2DC), _0733(0x2DD), _0734(0x2DE), _0735(0x2DF),
        _0736(0x2E0), _0737(0x2E1), _0738(0x2E2), _0739(0x2E3), _0740(0x2E4), _0741(0x2E5), _0742(0x2E6), _0743(0x2E7), _0744(0x2E8), _0745(0x2E9), _0746(0x2EA), _0747(0x2EB), _0748(0x2EC), _0749(0x2ED), _0750(0x2EE), _0751(0x2EF), _0752(0x2F0), _0753(0x2F1), _0754(0x2F2), _0755(0x2F3), _0756(0x2F4), _0757(0x2F5), _0758(0x2F6), _0759(0x2F7), _0760(0x2F8), _0761(0x2F9), _0762(0x2FA), _0763(0x2FB), _0764(0x2FC), _0765(0x2FD), _0766(0x2FE), _0767(0x2FF),
        _0768(0x300), _0769(0x301), _0770(0x302), _0771(0x303), _0772(0x304), _0773(0x305), _0774(0x306), _0775(0x307), _0776(0x308), _0777(0x309), _0778(0x30A), _0779(0x30B), _0780(0x30C), _0781(0x30D), _0782(0x30E), _0783(0x30F), _0784(0x310), _0785(0x311), _0786(0x312), _0787(0x313), _0788(0x314), _0789(0x315), _0790(0x316), _0791(0x317), _0792(0x318), _0793(0x319), _0794(0x31A), _0795(0x31B), _0796(0x31C), _0797(0x31D), _0798(0x31E), _0799(0x31F),
        _0800(0x320), _0801(0x321), _0802(0x322), _0803(0x323), _0804(0x324), _0805(0x325), _0806(0x326), _0807(0x327), _0808(0x328), _0809(0x329), _0810(0x32A), _0811(0x32B), _0812(0x32C), _0813(0x32D), _0814(0x32E), _0815(0x32F), _0816(0x330), _0817(0x331), _0818(0x332), _0819(0x333), _0820(0x334), _0821(0x335), _0822(0x336), _0823(0x337), _0824(0x338), _0825(0x339), _0826(0x33A), _0827(0x33B), _0828(0x33C), _0829(0x33D), _0830(0x33E), _0831(0x33F),
        _0832(0x340), _0833(0x341), _0834(0x342), _0835(0x343), _0836(0x344), _0837(0x345), _0838(0x346), _0839(0x347), _0840(0x348), _0841(0x349), _0842(0x34A), _0843(0x34B), _0844(0x34C), _0845(0x34D), _0846(0x34E), _0847(0x34F), _0848(0x350), _0849(0x351), _0850(0x352), _0851(0x353), _0852(0x354), _0853(0x355), _0854(0x356), _0855(0x357), _0856(0x358), _0857(0x359), _0858(0x35A), _0859(0x35B), _0860(0x35C), _0861(0x35D), _0862(0x35E), _0863(0x35F),
        _0864(0x360), _0865(0x361), _0866(0x362), _0867(0x363), _0868(0x364), _0869(0x365), _0870(0x366), _0871(0x367), _0872(0x368), _0873(0x369), _0874(0x36A), _0875(0x36B), _0876(0x36C), _0877(0x36D), _0878(0x36E), _0879(0x36F), _0880(0x370), _0881(0x371), _0882(0x372), _0883(0x373), _0884(0x374), _0885(0x375), _0886(0x376), _0887(0x377), _0888(0x378), _0889(0x379), _0890(0x37A), _0891(0x37B), _0892(0x37C), _0893(0x37D), _0894(0x37E), _0895(0x37F),
        _0896(0x380), _0897(0x381), _0898(0x382), _0899(0x383), _0900(0x384), _0901(0x385), _0902(0x386), _0903(0x387), _0904(0x388), _0905(0x389), _0906(0x38A), _0907(0x38B), _0908(0x38C), _0909(0x38D), _0910(0x38E), _0911(0x38F), _0912(0x390), _0913(0x391), _0914(0x392), _0915(0x393), _0916(0x394), _0917(0x395), _0918(0x396), _0919(0x397), _0920(0x398), _0921(0x399), _0922(0x39A), _0923(0x39B), _0924(0x39C), _0925(0x39D), _0926(0x39E), _0927(0x39F),
        _0928(0x3A0), _0929(0x3A1), _0930(0x3A2), _0931(0x3A3), _0932(0x3A4), _0933(0x3A5), _0934(0x3A6), _0935(0x3A7), _0936(0x3A8), _0937(0x3A9), _0938(0x3AA), _0939(0x3AB), _0940(0x3AC), _0941(0x3AD), _0942(0x3AE), _0943(0x3AF), _0944(0x3B0), _0945(0x3B1), _0946(0x3B2), _0947(0x3B3), _0948(0x3B4), _0949(0x3B5), _0950(0x3B6), _0951(0x3B7), _0952(0x3B8), _0953(0x3B9), _0954(0x3BA), _0955(0x3BB), _0956(0x3BC), _0957(0x3BD), _0958(0x3BE), _0959(0x3BF),
        _0960(0x3C0), _0961(0x3C1), _0962(0x3C2), _0963(0x3C3), _0964(0x3C4), _0965(0x3C5), _0966(0x3C6), _0967(0x3C7), _0968(0x3C8), _0969(0x3C9), _0970(0x3CA), _0971(0x3CB), _0972(0x3CC), _0973(0x3CD), _0974(0x3CE), _0975(0x3CF), _0976(0x3D0), _0977(0x3D1), _0978(0x3D2), _0979(0x3D3), _0980(0x3D4), _0981(0x3D5), _0982(0x3D6), _0983(0x3D7), _0984(0x3D8), _0985(0x3D9), _0986(0x3DA), _0987(0x3DB), _0988(0x3DC), _0989(0x3DD), _0990(0x3DE), _0991(0x3DF),
        _0992(0x3E0), _0993(0x3E1), _0994(0x3E2), _0995(0x3E3), _0996(0x3E4), _0997(0x3E5), _0998(0x3E6), _0999(0x3E7), _1000(0x3E8), _1001(0x3E9), _1002(0x3EA), _1003(0x3EB), _1004(0x3EC), _1005(0x3ED), _1006(0x3EE), _1007(0x3EF), _1008(0x3F0), _1009(0x3F1), _1010(0x3F2), _1011(0x3F3), _1012(0x3F4), _1013(0x3F5), _1014(0x3F6), _1015(0x3F7), _1016(0x3F8), _1017(0x3F9), _1018(0x3FA), _1019(0x3FB), _1020(0x3FC), _1021(0x3FD), _1022(0x3FE), _1023(0x3FF),

        /*
         * Internal and transitory execution-variable indexes
         *
         * echo; for I in {1024..2047}; do printf '_%04d(0x%03X), ' $I $I; if (( ($I + 1) % 32 == 0 )); then echo; fi; done; echo
         */
        _1024(0x400), _1025(0x401), _1026(0x402), _1027(0x403), _1028(0x404), _1029(0x405), _1030(0x406), _1031(0x407), _1032(0x408), _1033(0x409), _1034(0x40A), _1035(0x40B), _1036(0x40C), _1037(0x40D), _1038(0x40E), _1039(0x40F), _1040(0x410), _1041(0x411), _1042(0x412), _1043(0x413), _1044(0x414), _1045(0x415), _1046(0x416), _1047(0x417), _1048(0x418), _1049(0x419), _1050(0x41A), _1051(0x41B), _1052(0x41C), _1053(0x41D), _1054(0x41E), _1055(0x41F),
        _1056(0x420), _1057(0x421), _1058(0x422), _1059(0x423), _1060(0x424), _1061(0x425), _1062(0x426), _1063(0x427), _1064(0x428), _1065(0x429), _1066(0x42A), _1067(0x42B), _1068(0x42C), _1069(0x42D), _1070(0x42E), _1071(0x42F), _1072(0x430), _1073(0x431), _1074(0x432), _1075(0x433), _1076(0x434), _1077(0x435), _1078(0x436), _1079(0x437), _1080(0x438), _1081(0x439), _1082(0x43A), _1083(0x43B), _1084(0x43C), _1085(0x43D), _1086(0x43E), _1087(0x43F),
        _1088(0x440), _1089(0x441), _1090(0x442), _1091(0x443), _1092(0x444), _1093(0x445), _1094(0x446), _1095(0x447), _1096(0x448), _1097(0x449), _1098(0x44A), _1099(0x44B), _1100(0x44C), _1101(0x44D), _1102(0x44E), _1103(0x44F), _1104(0x450), _1105(0x451), _1106(0x452), _1107(0x453), _1108(0x454), _1109(0x455), _1110(0x456), _1111(0x457), _1112(0x458), _1113(0x459), _1114(0x45A), _1115(0x45B), _1116(0x45C), _1117(0x45D), _1118(0x45E), _1119(0x45F),
        _1120(0x460), _1121(0x461), _1122(0x462), _1123(0x463), _1124(0x464), _1125(0x465), _1126(0x466), _1127(0x467), _1128(0x468), _1129(0x469), _1130(0x46A), _1131(0x46B), _1132(0x46C), _1133(0x46D), _1134(0x46E), _1135(0x46F), _1136(0x470), _1137(0x471), _1138(0x472), _1139(0x473), _1140(0x474), _1141(0x475), _1142(0x476), _1143(0x477), _1144(0x478), _1145(0x479), _1146(0x47A), _1147(0x47B), _1148(0x47C), _1149(0x47D), _1150(0x47E), _1151(0x47F),
        _1152(0x480), _1153(0x481), _1154(0x482), _1155(0x483), _1156(0x484), _1157(0x485), _1158(0x486), _1159(0x487), _1160(0x488), _1161(0x489), _1162(0x48A), _1163(0x48B), _1164(0x48C), _1165(0x48D), _1166(0x48E), _1167(0x48F), _1168(0x490), _1169(0x491), _1170(0x492), _1171(0x493), _1172(0x494), _1173(0x495), _1174(0x496), _1175(0x497), _1176(0x498), _1177(0x499), _1178(0x49A), _1179(0x49B), _1180(0x49C), _1181(0x49D), _1182(0x49E), _1183(0x49F),
        _1184(0x4A0), _1185(0x4A1), _1186(0x4A2), _1187(0x4A3), _1188(0x4A4), _1189(0x4A5), _1190(0x4A6), _1191(0x4A7), _1192(0x4A8), _1193(0x4A9), _1194(0x4AA), _1195(0x4AB), _1196(0x4AC), _1197(0x4AD), _1198(0x4AE), _1199(0x4AF), _1200(0x4B0), _1201(0x4B1), _1202(0x4B2), _1203(0x4B3), _1204(0x4B4), _1205(0x4B5), _1206(0x4B6), _1207(0x4B7), _1208(0x4B8), _1209(0x4B9), _1210(0x4BA), _1211(0x4BB), _1212(0x4BC), _1213(0x4BD), _1214(0x4BE), _1215(0x4BF),
        _1216(0x4C0), _1217(0x4C1), _1218(0x4C2), _1219(0x4C3), _1220(0x4C4), _1221(0x4C5), _1222(0x4C6), _1223(0x4C7), _1224(0x4C8), _1225(0x4C9), _1226(0x4CA), _1227(0x4CB), _1228(0x4CC), _1229(0x4CD), _1230(0x4CE), _1231(0x4CF), _1232(0x4D0), _1233(0x4D1), _1234(0x4D2), _1235(0x4D3), _1236(0x4D4), _1237(0x4D5), _1238(0x4D6), _1239(0x4D7), _1240(0x4D8), _1241(0x4D9), _1242(0x4DA), _1243(0x4DB), _1244(0x4DC), _1245(0x4DD), _1246(0x4DE), _1247(0x4DF),
        _1248(0x4E0), _1249(0x4E1), _1250(0x4E2), _1251(0x4E3), _1252(0x4E4), _1253(0x4E5), _1254(0x4E6), _1255(0x4E7), _1256(0x4E8), _1257(0x4E9), _1258(0x4EA), _1259(0x4EB), _1260(0x4EC), _1261(0x4ED), _1262(0x4EE), _1263(0x4EF), _1264(0x4F0), _1265(0x4F1), _1266(0x4F2), _1267(0x4F3), _1268(0x4F4), _1269(0x4F5), _1270(0x4F6), _1271(0x4F7), _1272(0x4F8), _1273(0x4F9), _1274(0x4FA), _1275(0x4FB), _1276(0x4FC), _1277(0x4FD), _1278(0x4FE), _1279(0x4FF),
        _1280(0x500), _1281(0x501), _1282(0x502), _1283(0x503), _1284(0x504), _1285(0x505), _1286(0x506), _1287(0x507), _1288(0x508), _1289(0x509), _1290(0x50A), _1291(0x50B), _1292(0x50C), _1293(0x50D), _1294(0x50E), _1295(0x50F), _1296(0x510), _1297(0x511), _1298(0x512), _1299(0x513), _1300(0x514), _1301(0x515), _1302(0x516), _1303(0x517), _1304(0x518), _1305(0x519), _1306(0x51A), _1307(0x51B), _1308(0x51C), _1309(0x51D), _1310(0x51E), _1311(0x51F),
        _1312(0x520), _1313(0x521), _1314(0x522), _1315(0x523), _1316(0x524), _1317(0x525), _1318(0x526), _1319(0x527), _1320(0x528), _1321(0x529), _1322(0x52A), _1323(0x52B), _1324(0x52C), _1325(0x52D), _1326(0x52E), _1327(0x52F), _1328(0x530), _1329(0x531), _1330(0x532), _1331(0x533), _1332(0x534), _1333(0x535), _1334(0x536), _1335(0x537), _1336(0x538), _1337(0x539), _1338(0x53A), _1339(0x53B), _1340(0x53C), _1341(0x53D), _1342(0x53E), _1343(0x53F),
        _1344(0x540), _1345(0x541), _1346(0x542), _1347(0x543), _1348(0x544), _1349(0x545), _1350(0x546), _1351(0x547), _1352(0x548), _1353(0x549), _1354(0x54A), _1355(0x54B), _1356(0x54C), _1357(0x54D), _1358(0x54E), _1359(0x54F), _1360(0x550), _1361(0x551), _1362(0x552), _1363(0x553), _1364(0x554), _1365(0x555), _1366(0x556), _1367(0x557), _1368(0x558), _1369(0x559), _1370(0x55A), _1371(0x55B), _1372(0x55C), _1373(0x55D), _1374(0x55E), _1375(0x55F),
        _1376(0x560), _1377(0x561), _1378(0x562), _1379(0x563), _1380(0x564), _1381(0x565), _1382(0x566), _1383(0x567), _1384(0x568), _1385(0x569), _1386(0x56A), _1387(0x56B), _1388(0x56C), _1389(0x56D), _1390(0x56E), _1391(0x56F), _1392(0x570), _1393(0x571), _1394(0x572), _1395(0x573), _1396(0x574), _1397(0x575), _1398(0x576), _1399(0x577), _1400(0x578), _1401(0x579), _1402(0x57A), _1403(0x57B), _1404(0x57C), _1405(0x57D), _1406(0x57E), _1407(0x57F),
        _1408(0x580), _1409(0x581), _1410(0x582), _1411(0x583), _1412(0x584), _1413(0x585), _1414(0x586), _1415(0x587), _1416(0x588), _1417(0x589), _1418(0x58A), _1419(0x58B), _1420(0x58C), _1421(0x58D), _1422(0x58E), _1423(0x58F), _1424(0x590), _1425(0x591), _1426(0x592), _1427(0x593), _1428(0x594), _1429(0x595), _1430(0x596), _1431(0x597), _1432(0x598), _1433(0x599), _1434(0x59A), _1435(0x59B), _1436(0x59C), _1437(0x59D), _1438(0x59E), _1439(0x59F),
        _1440(0x5A0), _1441(0x5A1), _1442(0x5A2), _1443(0x5A3), _1444(0x5A4), _1445(0x5A5), _1446(0x5A6), _1447(0x5A7), _1448(0x5A8), _1449(0x5A9), _1450(0x5AA), _1451(0x5AB), _1452(0x5AC), _1453(0x5AD), _1454(0x5AE), _1455(0x5AF), _1456(0x5B0), _1457(0x5B1), _1458(0x5B2), _1459(0x5B3), _1460(0x5B4), _1461(0x5B5), _1462(0x5B6), _1463(0x5B7), _1464(0x5B8), _1465(0x5B9), _1466(0x5BA), _1467(0x5BB), _1468(0x5BC), _1469(0x5BD), _1470(0x5BE), _1471(0x5BF),
        _1472(0x5C0), _1473(0x5C1), _1474(0x5C2), _1475(0x5C3), _1476(0x5C4), _1477(0x5C5), _1478(0x5C6), _1479(0x5C7), _1480(0x5C8), _1481(0x5C9), _1482(0x5CA), _1483(0x5CB), _1484(0x5CC), _1485(0x5CD), _1486(0x5CE), _1487(0x5CF), _1488(0x5D0), _1489(0x5D1), _1490(0x5D2), _1491(0x5D3), _1492(0x5D4), _1493(0x5D5), _1494(0x5D6), _1495(0x5D7), _1496(0x5D8), _1497(0x5D9), _1498(0x5DA), _1499(0x5DB), _1500(0x5DC), _1501(0x5DD), _1502(0x5DE), _1503(0x5DF),
        _1504(0x5E0), _1505(0x5E1), _1506(0x5E2), _1507(0x5E3), _1508(0x5E4), _1509(0x5E5), _1510(0x5E6), _1511(0x5E7), _1512(0x5E8), _1513(0x5E9), _1514(0x5EA), _1515(0x5EB), _1516(0x5EC), _1517(0x5ED), _1518(0x5EE), _1519(0x5EF), _1520(0x5F0), _1521(0x5F1), _1522(0x5F2), _1523(0x5F3), _1524(0x5F4), _1525(0x5F5), _1526(0x5F6), _1527(0x5F7), _1528(0x5F8), _1529(0x5F9), _1530(0x5FA), _1531(0x5FB), _1532(0x5FC), _1533(0x5FD), _1534(0x5FE), _1535(0x5FF),
        _1536(0x600), _1537(0x601), _1538(0x602), _1539(0x603), _1540(0x604), _1541(0x605), _1542(0x606), _1543(0x607), _1544(0x608), _1545(0x609), _1546(0x60A), _1547(0x60B), _1548(0x60C), _1549(0x60D), _1550(0x60E), _1551(0x60F), _1552(0x610), _1553(0x611), _1554(0x612), _1555(0x613), _1556(0x614), _1557(0x615), _1558(0x616), _1559(0x617), _1560(0x618), _1561(0x619), _1562(0x61A), _1563(0x61B), _1564(0x61C), _1565(0x61D), _1566(0x61E), _1567(0x61F),
        _1568(0x620), _1569(0x621), _1570(0x622), _1571(0x623), _1572(0x624), _1573(0x625), _1574(0x626), _1575(0x627), _1576(0x628), _1577(0x629), _1578(0x62A), _1579(0x62B), _1580(0x62C), _1581(0x62D), _1582(0x62E), _1583(0x62F), _1584(0x630), _1585(0x631), _1586(0x632), _1587(0x633), _1588(0x634), _1589(0x635), _1590(0x636), _1591(0x637), _1592(0x638), _1593(0x639), _1594(0x63A), _1595(0x63B), _1596(0x63C), _1597(0x63D), _1598(0x63E), _1599(0x63F),
        _1600(0x640), _1601(0x641), _1602(0x642), _1603(0x643), _1604(0x644), _1605(0x645), _1606(0x646), _1607(0x647), _1608(0x648), _1609(0x649), _1610(0x64A), _1611(0x64B), _1612(0x64C), _1613(0x64D), _1614(0x64E), _1615(0x64F), _1616(0x650), _1617(0x651), _1618(0x652), _1619(0x653), _1620(0x654), _1621(0x655), _1622(0x656), _1623(0x657), _1624(0x658), _1625(0x659), _1626(0x65A), _1627(0x65B), _1628(0x65C), _1629(0x65D), _1630(0x65E), _1631(0x65F),
        _1632(0x660), _1633(0x661), _1634(0x662), _1635(0x663), _1636(0x664), _1637(0x665), _1638(0x666), _1639(0x667), _1640(0x668), _1641(0x669), _1642(0x66A), _1643(0x66B), _1644(0x66C), _1645(0x66D), _1646(0x66E), _1647(0x66F), _1648(0x670), _1649(0x671), _1650(0x672), _1651(0x673), _1652(0x674), _1653(0x675), _1654(0x676), _1655(0x677), _1656(0x678), _1657(0x679), _1658(0x67A), _1659(0x67B), _1660(0x67C), _1661(0x67D), _1662(0x67E), _1663(0x67F),
        _1664(0x680), _1665(0x681), _1666(0x682), _1667(0x683), _1668(0x684), _1669(0x685), _1670(0x686), _1671(0x687), _1672(0x688), _1673(0x689), _1674(0x68A), _1675(0x68B), _1676(0x68C), _1677(0x68D), _1678(0x68E), _1679(0x68F), _1680(0x690), _1681(0x691), _1682(0x692), _1683(0x693), _1684(0x694), _1685(0x695), _1686(0x696), _1687(0x697), _1688(0x698), _1689(0x699), _1690(0x69A), _1691(0x69B), _1692(0x69C), _1693(0x69D), _1694(0x69E), _1695(0x69F),
        _1696(0x6A0), _1697(0x6A1), _1698(0x6A2), _1699(0x6A3), _1700(0x6A4), _1701(0x6A5), _1702(0x6A6), _1703(0x6A7), _1704(0x6A8), _1705(0x6A9), _1706(0x6AA), _1707(0x6AB), _1708(0x6AC), _1709(0x6AD), _1710(0x6AE), _1711(0x6AF), _1712(0x6B0), _1713(0x6B1), _1714(0x6B2), _1715(0x6B3), _1716(0x6B4), _1717(0x6B5), _1718(0x6B6), _1719(0x6B7), _1720(0x6B8), _1721(0x6B9), _1722(0x6BA), _1723(0x6BB), _1724(0x6BC), _1725(0x6BD), _1726(0x6BE), _1727(0x6BF),
        _1728(0x6C0), _1729(0x6C1), _1730(0x6C2), _1731(0x6C3), _1732(0x6C4), _1733(0x6C5), _1734(0x6C6), _1735(0x6C7), _1736(0x6C8), _1737(0x6C9), _1738(0x6CA), _1739(0x6CB), _1740(0x6CC), _1741(0x6CD), _1742(0x6CE), _1743(0x6CF), _1744(0x6D0), _1745(0x6D1), _1746(0x6D2), _1747(0x6D3), _1748(0x6D4), _1749(0x6D5), _1750(0x6D6), _1751(0x6D7), _1752(0x6D8), _1753(0x6D9), _1754(0x6DA), _1755(0x6DB), _1756(0x6DC), _1757(0x6DD), _1758(0x6DE), _1759(0x6DF),
        _1760(0x6E0), _1761(0x6E1), _1762(0x6E2), _1763(0x6E3), _1764(0x6E4), _1765(0x6E5), _1766(0x6E6), _1767(0x6E7), _1768(0x6E8), _1769(0x6E9), _1770(0x6EA), _1771(0x6EB), _1772(0x6EC), _1773(0x6ED), _1774(0x6EE), _1775(0x6EF), _1776(0x6F0), _1777(0x6F1), _1778(0x6F2), _1779(0x6F3), _1780(0x6F4), _1781(0x6F5), _1782(0x6F6), _1783(0x6F7), _1784(0x6F8), _1785(0x6F9), _1786(0x6FA), _1787(0x6FB), _1788(0x6FC), _1789(0x6FD), _1790(0x6FE), _1791(0x6FF),
        _1792(0x700), _1793(0x701), _1794(0x702), _1795(0x703), _1796(0x704), _1797(0x705), _1798(0x706), _1799(0x707), _1800(0x708), _1801(0x709), _1802(0x70A), _1803(0x70B), _1804(0x70C), _1805(0x70D), _1806(0x70E), _1807(0x70F), _1808(0x710), _1809(0x711), _1810(0x712), _1811(0x713), _1812(0x714), _1813(0x715), _1814(0x716), _1815(0x717), _1816(0x718), _1817(0x719), _1818(0x71A), _1819(0x71B), _1820(0x71C), _1821(0x71D), _1822(0x71E), _1823(0x71F),
        _1824(0x720), _1825(0x721), _1826(0x722), _1827(0x723), _1828(0x724), _1829(0x725), _1830(0x726), _1831(0x727), _1832(0x728), _1833(0x729), _1834(0x72A), _1835(0x72B), _1836(0x72C), _1837(0x72D), _1838(0x72E), _1839(0x72F), _1840(0x730), _1841(0x731), _1842(0x732), _1843(0x733), _1844(0x734), _1845(0x735), _1846(0x736), _1847(0x737), _1848(0x738), _1849(0x739), _1850(0x73A), _1851(0x73B), _1852(0x73C), _1853(0x73D), _1854(0x73E), _1855(0x73F),
        _1856(0x740), _1857(0x741), _1858(0x742), _1859(0x743), _1860(0x744), _1861(0x745), _1862(0x746), _1863(0x747), _1864(0x748), _1865(0x749), _1866(0x74A), _1867(0x74B), _1868(0x74C), _1869(0x74D), _1870(0x74E), _1871(0x74F), _1872(0x750), _1873(0x751), _1874(0x752), _1875(0x753), _1876(0x754), _1877(0x755), _1878(0x756), _1879(0x757), _1880(0x758), _1881(0x759), _1882(0x75A), _1883(0x75B), _1884(0x75C), _1885(0x75D), _1886(0x75E), _1887(0x75F),
        _1888(0x760), _1889(0x761), _1890(0x762), _1891(0x763), _1892(0x764), _1893(0x765), _1894(0x766), _1895(0x767), _1896(0x768), _1897(0x769), _1898(0x76A), _1899(0x76B), _1900(0x76C), _1901(0x76D), _1902(0x76E), _1903(0x76F), _1904(0x770), _1905(0x771), _1906(0x772), _1907(0x773), _1908(0x774), _1909(0x775), _1910(0x776), _1911(0x777), _1912(0x778), _1913(0x779), _1914(0x77A), _1915(0x77B), _1916(0x77C), _1917(0x77D), _1918(0x77E), _1919(0x77F),
        _1920(0x780), _1921(0x781), _1922(0x782), _1923(0x783), _1924(0x784), _1925(0x785), _1926(0x786), _1927(0x787), _1928(0x788), _1929(0x789), _1930(0x78A), _1931(0x78B), _1932(0x78C), _1933(0x78D), _1934(0x78E), _1935(0x78F), _1936(0x790), _1937(0x791), _1938(0x792), _1939(0x793), _1940(0x794), _1941(0x795), _1942(0x796), _1943(0x797), _1944(0x798), _1945(0x799), _1946(0x79A), _1947(0x79B), _1948(0x79C), _1949(0x79D), _1950(0x79E), _1951(0x79F),
        _1952(0x7A0), _1953(0x7A1), _1954(0x7A2), _1955(0x7A3), _1956(0x7A4), _1957(0x7A5), _1958(0x7A6), _1959(0x7A7), _1960(0x7A8), _1961(0x7A9), _1962(0x7AA), _1963(0x7AB), _1964(0x7AC), _1965(0x7AD), _1966(0x7AE), _1967(0x7AF), _1968(0x7B0), _1969(0x7B1), _1970(0x7B2), _1971(0x7B3), _1972(0x7B4), _1973(0x7B5), _1974(0x7B6), _1975(0x7B7), _1976(0x7B8), _1977(0x7B9), _1978(0x7BA), _1979(0x7BB), _1980(0x7BC), _1981(0x7BD), _1982(0x7BE), _1983(0x7BF),
        _1984(0x7C0), _1985(0x7C1), _1986(0x7C2), _1987(0x7C3), _1988(0x7C4), _1989(0x7C5), _1990(0x7C6), _1991(0x7C7), _1992(0x7C8), _1993(0x7C9), _1994(0x7CA), _1995(0x7CB), _1996(0x7CC), _1997(0x7CD), _1998(0x7CE), _1999(0x7CF), _2000(0x7D0), _2001(0x7D1), _2002(0x7D2), _2003(0x7D3), _2004(0x7D4), _2005(0x7D5), _2006(0x7D6), _2007(0x7D7), _2008(0x7D8), _2009(0x7D9), _2010(0x7DA), _2011(0x7DB), _2012(0x7DC), _2013(0x7DD), _2014(0x7DE), _2015(0x7DF),
        _2016(0x7E0), _2017(0x7E1), _2018(0x7E2), _2019(0x7E3), _2020(0x7E4), _2021(0x7E5), _2022(0x7E6), _2023(0x7E7), _2024(0x7E8), _2025(0x7E9), _2026(0x7EA), _2027(0x7EB), _2028(0x7EC), _2029(0x7ED), _2030(0x7EE), _2031(0x7EF), _2032(0x7F0), _2033(0x7F1), _2034(0x7F2), _2035(0x7F3), _2036(0x7F4), _2037(0x7F5), _2038(0x7F6), _2039(0x7F7), _2040(0x7F8), _2041(0x7F9), _2042(0x7FA), _2043(0x7FB), _2044(0x7FC), _2045(0x7FD), _2046(0x7FE), _2047(0x7FF),

        // A special value to indicate that an execution-variable index is not applicable in the context
        _NA_(-1)

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final int _value;

        private XVI(final int value)
        {
            try {
                if(value >= INS_EXEC_VAR_BUFF_SIZE_TOTAL) throw XCom.newJXMFatalLogicError("XVI(%d)", value);
            }
            catch(final Exception e) {
                e.printStackTrace();
                SysUtil.systemExitError();
            }

            _value = value;
        }

        public int value()
        { return _value; }

        public int xviEnc()
        { return -(_value + 1); }

        public boolean isNA()
        { return _value == _NA_.value(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static boolean isNA(final XVI xvi)
        { return xvi.isNA(); }

        public static boolean isNA(final int value)
        { return value == _NA_.value(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static int xviEnc(final XVI xvi)
        { return xvi.xviEnc(); }

        public static XVI xviDec(final int value)
        { return values()[ -(value + 1) ]; }

        public static XVI xviDec(final long value)
        { return xviDec( (int) value ); }

        public static boolean isXVIEnc(final int value)
        { return value < 0; }

        public static boolean isXVIEnc(final long value)
        { return isXVIEnc( (int) value ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * # This function can be used to obtain 'XVI._0000' ... 'XVI._1023'. It is highly recommended to directly
         *   use the XVI names instead of calling this function.
         * # These XVIs act as global variables that are created and managed by the flash loader program. It is
         *   the responsibility of the flash loader program to use unique XVIs for each need/purpose and prevent
         *   conflicts and unwanted overlaps.
         */
        public static XVI user(final int value)
        { return values()[value]; }

        /*
         * # This function can be used to obtain 'XVI._1024' ... 'XVI._1791'. It is highly recommended to not directly
         *   call this function.
         * # These XVIs act as global variables that are created and managed by the 'SWDExecInstBuilder' instance.
         *   These XVIs can be obtained by calling the 'SWDExecInstBuilder.xviInternal()' function. The function will
         *   provide unique XVIs for each flash loader program. The internal counter will be reset every time the
         *   'SWDExecInstBuilder.link()' function is called, this means that two or more different programs can use the
         *   same XVI.
         */
        public static XVI internal(final int value)
        { return values()[INS_EXEC_VAR_BUFF_SIZE_USER + value]; }

        /*
         * # This function can be used to obtain 'XVI._1792' ... 'XVI._2043'.
         * # These XVIs act as local and short-lived temporary variables. The XVIs obtained by using this function must
         *   be used immediately and then forgotten.
         */
        public static XVI transitory(final int value)
        { return values()[INS_EXEC_VAR_BUFF_SIZE_USER + INS_EXEC_VAR_BUFF_SIZE_BUILDER_INTERNAL + value]; }

    }; // enum XVI

} // class SWDExecInstOpcode
