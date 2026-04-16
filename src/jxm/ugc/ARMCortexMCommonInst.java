/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.xb.*;

import static jxm.xb.XCom._BV;
import static jxm.xb.XCom._BVs;


public abstract class ARMCortexMCommonInst {

    protected static final long   SCS_BASE     =            0xE000E000L;
    protected static final long   SysTick_BASE = SCS_BASE + 0x00000010L;
    protected static final long   NVIC_BASE    = SCS_BASE + 0x00000100L;
    protected static final long   SCB_BASE     = SCS_BASE + 0x00000D00L;
    protected static final long   MPU_BASE     = SCS_BASE + 0x00000D90L;

    protected static final long   SCB_SHCSR    = SCB_BASE + 0x00000024L;

    protected static final long   MPU_CTRL     = MPU_BASE + 0x00000004L;
    protected static final long   MPU_RNR      = MPU_BASE + 0x00000008L;
    protected static final long   MPU_RBAR     = MPU_BASE + 0x0000000CL;
    protected static final long   MPU_RASR     = MPU_BASE + 0x00000010L;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void put_instruction_initMPU_QSPI(final SWDExecInstBuilder ib, final long QSPI_BASE, final long flashCapacityBytes)
    {
        /*
         * INSTRUCTION_ACCESS_DISABLE : [   28] XN   = 1
         * REGION_FULL_ACCESS         : [26:24] AP   = 3
         * TEX_LEVEL0                 : [21:19] TEX  = 0
         * ACCESS_NOT_SHAREABLE       : [   18] S    = 0
         * ACCESS_CACHEABLE           : [   17] C    = 1
         * ACCESS_BUFFERABLE          : [   16] B    = 1
         * SUB_REGION_DISABLE         : [15:08] SRD  = 0
         * MPU_REGION_SIZE_?          : [05:01] SIZE = ...
         * REGION_ENABLE              : [   00] E    = 1
         */
        final long RASR = _BVs(28, 17, 16, 0) | _BV(3, 24) | _BV( XCom.log2(flashCapacityBytes) - 1, 1 );

        // Disable MPU
        ib.clrBits( SCB_SHCSR, _BV(16)    ); // Clear MEMFAULTENA
        ib.wrtBits( MPU_CTRL , 0          ); // Clear PRIVDEFENA HFNMIENA ENABLE

        // Initializes and configures the region and the memory to be protected
        ib.wrtBits( MPU_RNR  , 0          ); // Set   REGION to region 0
        ib.clrBits( MPU_RASR , 0          ); // Clear ENABLE
        ib.wrtBits( MPU_RBAR , QSPI_BASE  ); // Set   ADDR   to QSPI_BASE
        ib.wrtBits( MPU_RASR , RASR       ); // Set   REGION to region 0

        // Enable MPU
        ib.wrtBits( MPU_CTRL , 0b00000101 ); // Set   PRIVDEFENA ENABLE
        ib.setBits( SCB_SHCSR, _BV(16)    ); // Set   MEMFAULTENA
    }

    protected static void put_instruction_disableMPU(final SWDExecInstBuilder ib)
    {
        // Disable MPU
        ib.clrBits( SCB_SHCSR, _BV(16)    ); // Clear MEMFAULTENA
        ib.wrtBits( MPU_CTRL , 0          ); // Clear PRIVDEFENA HFNMIENA ENABLE
    }

} // class ARMCortexMCommonInst
