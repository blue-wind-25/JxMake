/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


/*
 * Please refer to the comment block before the 'STM32QSPI' class definition in the 'STM32QSPI.java' file for more details and information.
 */
public class STM32GPIO {

    public final int gpio;
    public final int pin;
    public final int afn;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private STM32GPIO(final int gpio_, final int pin_, final int afn_)
    {
        gpio = gpio_ & 0x0F;
        pin  = pin_  & 0x0F;
        afn  = afn_  & 0x0F;
    }

    private STM32GPIO(final int gpio, final int pin)
    { this(gpio, pin, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static STM32GPIO A(final int pin               ) { return new STM32GPIO(  0, pin      ); }
    public static STM32GPIO B(final int pin               ) { return new STM32GPIO(  1, pin      ); }
    public static STM32GPIO C(final int pin               ) { return new STM32GPIO(  2, pin      ); }
    public static STM32GPIO D(final int pin               ) { return new STM32GPIO(  3, pin      ); }
    public static STM32GPIO E(final int pin               ) { return new STM32GPIO(  4, pin      ); }
    public static STM32GPIO F(final int pin               ) { return new STM32GPIO(  5, pin      ); }
    public static STM32GPIO G(final int pin               ) { return new STM32GPIO(  6, pin      ); }
    public static STM32GPIO H(final int pin               ) { return new STM32GPIO(  7, pin      ); }
    public static STM32GPIO I(final int pin               ) { return new STM32GPIO(  8, pin      ); }
    public static STM32GPIO J(final int pin               ) { return new STM32GPIO(  9, pin      ); }
    public static STM32GPIO K(final int pin               ) { return new STM32GPIO( 10, pin      ); }

    public static STM32GPIO A(final int pin, final int afn) { return new STM32GPIO(  0, pin, afn ); }
    public static STM32GPIO B(final int pin, final int afn) { return new STM32GPIO(  1, pin, afn ); }
    public static STM32GPIO C(final int pin, final int afn) { return new STM32GPIO(  2, pin, afn ); }
    public static STM32GPIO D(final int pin, final int afn) { return new STM32GPIO(  3, pin, afn ); }
    public static STM32GPIO E(final int pin, final int afn) { return new STM32GPIO(  4, pin, afn ); }
    public static STM32GPIO F(final int pin, final int afn) { return new STM32GPIO(  5, pin, afn ); }
    public static STM32GPIO G(final int pin, final int afn) { return new STM32GPIO(  6, pin, afn ); }
    public static STM32GPIO H(final int pin, final int afn) { return new STM32GPIO(  7, pin, afn ); }
    public static STM32GPIO I(final int pin, final int afn) { return new STM32GPIO(  8, pin, afn ); }
    public static STM32GPIO J(final int pin, final int afn) { return new STM32GPIO(  9, pin, afn ); }
    public static STM32GPIO K(final int pin, final int afn) { return new STM32GPIO( 10, pin, afn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int GPIOENBit() { return 0b0000000000000001 << gpio; }

} // class STM32GPIO
