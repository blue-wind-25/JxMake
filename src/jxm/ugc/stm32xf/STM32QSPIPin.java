/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


/*
 * Please refer to the comment block before the 'STM32QSPI' class definition in the 'STM32QSPI.java' file for more details and information.
 */
public abstract class STM32QSPIPin {

    @SuppressWarnings("this-escape") public final STM32GPIO CLK = _gpio_CLK();
    @SuppressWarnings("this-escape") public final STM32GPIO NCS = _gpio_NCS();
    @SuppressWarnings("this-escape") public final STM32GPIO IO0 = _gpio_IO0();
    @SuppressWarnings("this-escape") public final STM32GPIO IO1 = _gpio_IO1();
    @SuppressWarnings("this-escape") public final STM32GPIO IO2 = _gpio_IO2();
    @SuppressWarnings("this-escape") public final STM32GPIO IO3 = _gpio_IO3();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract STM32GPIO _gpio_CLK();
    protected abstract STM32GPIO _gpio_NCS();
    protected abstract STM32GPIO _gpio_IO0();
    protected abstract STM32GPIO _gpio_IO1();
    protected abstract STM32GPIO _gpio_IO2();
    protected abstract STM32GPIO _gpio_IO3();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int GPIOxENBits()
    { return CLK.GPIOENBit() | NCS.GPIOENBit() | IO0.GPIOENBit() | IO1.GPIOENBit() | IO2.GPIOENBit() | IO3.GPIOENBit(); }

} // class STM32QSPIPin
