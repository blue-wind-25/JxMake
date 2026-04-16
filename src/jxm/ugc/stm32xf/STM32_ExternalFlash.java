/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


import jxm.ugc.*;


public interface STM32_ExternalFlash  {

    public abstract SWDExecInstBuilder swdExecInstBuilder();

    public abstract boolean mmapEnabled();

    public abstract long[][] instruction_resetHaltInitClock();
    public abstract long[][] instruction_initGPIO_XF();
    public abstract long[][] instruction_initFlash();

    public abstract long[][] instruction_softwareReset();

    public abstract long[][] instruction_chipErase();

    public abstract long[][] instruction_readPage();
    public abstract long[][] instruction_readPageDMA();   // NOTE : Not all implementations will support this!
    public abstract long[][] instruction_readPageMMap();  // NOTE : Not all implementations will support this!

    public abstract long[][] instruction_writePage();
    public abstract long[][] instruction_writePageDMA();  // NOTE : Not all implementations will support this!
    public abstract long[][] instruction_writePageMMap(); // NOTE : Not all implementations will support this!

    public abstract long[][] instruction_endMMap();       // NOTE : Not all implementations will support this!

    public abstract long[][] instruction_deinitAll();

} // interface STM32_ExternalFlash
