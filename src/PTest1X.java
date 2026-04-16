/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;
import jxm.xb.*;


//
// Test class (the test application entry point)
//
public class PTest1X {

    public static void main(final String[] args)
    {
        /*
        for(int i = 0; i < 1024; i += 2) {
            final int n = ProgressCB.getNumTicksExt(i, 5);
          //if(n ==  7) SysUtil.stdDbg().println(i);
            if(n == 21) SysUtil.stdDbg().println(i);
        }
        //*/

        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        // Instantiate the programmer
              USB2GPIO        usb2gpio   = null;

              ProgISP.Config  ispConfig  = new ProgISP.Config();
              ProgISP         isp        = null;

              ProgTPI.Config  tpiConfig  = new ProgTPI.Config();
              ProgTPI         tpi        = null;

              ProgUPDI.Config updiConfig = new ProgUPDI.Config();
              ProgUPDI        updi       = null;

              ProgPDI.Config  pdiConfig  = new ProgPDI.Config();
              ProgPDI         pdi        = null;

              ProgLGT8.Config lgt8Config = new ProgLGT8.Config();
              ProgLGT8        lgt8       = null;

              ProgJTAG        jtag       = null;

        final IntConsumer     stdPP      = ProgressCB.getStdProgressPrinter( SysUtil.stdDbg() );

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // Select the backend
            /*
             * NOTE : # Using JxMake DASA     is faster (and probably more stable) than using USB-ISS
             *        # Using JxMake USB-GPIO is faster (and probably more stable) than using JxMake DASA
             */
            final int backend = 1; // 0 : JxMake USB-GPIO Module
                                   // 1 : JxMake DASA
                                   // 2 : USB-ISS

            switch(backend) {

                // Use JxMake USB-GPIO Module
                case 0:
                    final boolean enDM_Error       =  true;
                    final boolean enDM_Warning     =  true;
                    final boolean enDM_Notice      =  true;
                    final boolean enDM_Information = !true;

                    if(true) {

                        final USB_GPIO dev = true ? USB_GPIO.autoConnectFirst() : new USB_GPIO("/dev/ttyACM0", "/dev/ttyACM1");
                        dev.setAutoNotifyErrorMessage(true);

                        SysUtil.stdDbg().print("USB-GPIO Ping         = "); SysUtil.stdDbg().printf( "%b\n", dev.ping() );
                        SysUtil.stdDbg().print("USB-GPIO Version      = "); for( final int v : dev.getVersion() ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().print("USB-GPIO En-Debug-Msg = "); SysUtil.stdDbg().printf( "%b\n", dev.enableDebugMessage(enDM_Error, enDM_Warning, enDM_Notice, enDM_Information) );
                        SysUtil.stdDbg().println();

                        usb2gpio = dev;

                        /*
                        SysUtil.stdDbg().print("USB-GPIO En-Debug-Msg = "); SysUtil.stdDbg().printf( "%b\n", dev.enableDebugMessage(true, true, true, true) );
                        dev.reset();
                        dev.resetToBootloader();
                        SysUtil.systemExit();
                        //*/

                        /*
                        if(true) {
                            final boolean res = dev.hwusartEnable();
                            SysUtil.stdDbg().print("HW-USART Enable       = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            usb2gpio.rawSerialPort().setBreak  (); SysUtil.sleepMS(100);
                            usb2gpio.rawSerialPort().clearBreak(); SysUtil.sleepMS(100);
                            usb2gpio.rawSerialPort().setBreak  (); SysUtil.sleepMS(100);
                            usb2gpio.rawSerialPort().clearBreak(); SysUtil.sleepMS(100);
                            usb2gpio.rawSerialPort().setBreak  (); SysUtil.sleepMS(100);
                            usb2gpio.rawSerialPort().clearBreak(); SysUtil.sleepMS(100);
                            dev.hwusartDisable();
                        }
                        else {
                            final boolean res = dev.hwuartEnable();
                            SysUtil.stdDbg().print("HW-UART Enable        = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                        }
                        SysUtil.stdDbg().println();
                        SysUtil.systemExit();
                        //*/

                        /*
                        if(true) {

                            boolean res = dev.hwtwiBegin(100000, -1, true);
                            SysUtil.stdDbg().print("HW-TWI Enable  = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            final int[] scan = dev.hwtwiScan();
                            SysUtil.stdDbg().print("HW-TWI Scan    = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            for(int n = 0; n < scan.length; ++n) {
                                if( scan[n] != 0) SysUtil.stdDbg().printf("    Found device at 0x%02X\n", n);
                            }

                            res = dev.hwtwiEnd();
                            SysUtil.stdDbg().print("HW-TWI Disable = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                        } // if
                        //*/

                        /*
                        // NOTE : Assume that a PCF8574 is attached to the TWI bus
                        if(true) {

                            final int     deviceAddress = 0x27;
                                  boolean res;

                            res = dev.hwtwiBegin(2000000, -1, true);
                            SysUtil.stdDbg().print("\n\nHW-TWI Enable  = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            if(true) {
                                                                           dev.hwtwiWriteOneCF(100000, deviceAddress, 0b11111111)  ;
                                SysUtil.stdDbg().printf( "    0x%02X\n\n", dev.hwtwiReadOneCF (100000, deviceAddress            ) );
                            }

                            if(true) {
                                for(int i = 0; i <= 7; ++i) {
                                    dev.hwtwiWriteOneCF( 100000, deviceAddress, ( ~(1 << i) ) & 0xFF ); System.out.printf("#%d on\n"   , i); SysUtil.sleepMS( 500);
                                    dev.hwtwiWriteOneCF( 100000, deviceAddress, 0b11111111           ); System.out.printf("#%d off\n\n", i); SysUtil.sleepMS(1000);
                                }
                            }

                            if(true) {
                                dev.hwtwiWriteOneCF( 100000, deviceAddress, ~(0b00011000) & 0xFF ); System.out.printf("#3 #4    on\n"   ); SysUtil.sleepMS( 500);
                                dev.hwtwiWriteOneCF( 100000, deviceAddress,   0b11111111         ); System.out.printf("         off\n\n"); SysUtil.sleepMS(1000);
                                dev.hwtwiWriteOneCF( 100000, deviceAddress, ~(0b11100000) & 0xFF ); System.out.printf("#5 #6 #7 on\n"   ); SysUtil.sleepMS( 500);
                                dev.hwtwiWriteOneCF( 100000, deviceAddress,   0b11111111         ); System.out.printf("         off\n\n"); SysUtil.sleepMS(1000);
                            }

                            res = dev.hwtwiEnd();
                            SysUtil.stdDbg().print("HW-TWI Disable = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                        } // if
                        //*/

                    }
                    else {

                        // ##### !!! TODO : 'USB_GPIO.autoConnectFirst()' does not work well for 'UGM II' !!! #####
                        final USB_GPIO dev = true ? USB_GPIO.autoConnectFirst() : new USB_GPIO("/dev/ttyACM0", "/dev/ttyACM1");
                        dev.setAutoNotifyErrorMessage(false);

                        boolean res;

                        //*
                        res = dev.invalidCommand();
                        SysUtil.stdDbg().print("USB-GPIO Invalid Cmd  = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                        res = dev.ping();
                        SysUtil.stdDbg().print("USB-GPIO Ping         = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                        final int[] ver = dev.getVersion();
                        SysUtil.stdDbg().print("USB-GPIO Version      = "); for(final int v : ver) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();

                        SysUtil.stdDbg().print("USB-GPIO En-Debug-Msg = "); SysUtil.stdDbg().printf( "%b\n", dev.enableDebugMessage(enDM_Error, enDM_Warning, enDM_Notice, enDM_Information) );

                        res = dev.invalidCommand();
                        SysUtil.stdDbg().print("USB-GPIO Invalid Cmd  = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                        SysUtil.stdDbg().println();
                        //*/

                        // Flags
                        final boolean testUSRT    = !true;
                        final boolean testTWI     =  true;
                        final boolean testSPI     = !true;
                        final boolean testBBUSRT  = !true;
                        final boolean testLED     =  true;
                        final boolean testADC     =  true;

                        final String MsgNextStage = ">>> Press the button to proceed to the next test stage...\n";

                        //*
                        if(true) {
                            if(testUSRT) {
                                res = dev.hwusrtEnable();
                                SysUtil.stdDbg().print("HW-USRT Enable        = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            }
                            else {
                                res = dev.hwuartEnable();
                                SysUtil.stdDbg().print("HW-UART Enable        = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            }
                            SysUtil.stdDbg().println();
                        }
                        else {
                            res = dev.uartBegin(USB2GPIO.UXRTMode._8N1, 115200);
                            SysUtil.stdDbg().print("HW-UART Begin         = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            res = dev.uartDisableTxAfter(8);
                            SysUtil.stdDbg().print("HW-UART Disable Tx Af = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            SysUtil.stdDbg().println();
                        }

                        //*/
                        if(true) {
                            final int MI = USB_GPIO.HW_GPIO_SET_MODE_INP_PU;
                            final int MO = USB_GPIO.HW_GPIO_SET_MODE_OUT;
                            //                         GPIO number   0      1      2      3      4      5      6      7
                            //                         Purpose       PWM    PWM    GND    GND    VCC    ADC    GND    BTN
                            res = dev.hwgpioSetMode( new int    [] { MO   , MO   , MO   , MO   , MO   , MI   , MO   , MI    },
                                                     new boolean[] { false, false, false, false, true , false, false, false }
                                                   );
                            SysUtil.stdDbg().print("HW-GPIO Set Mode      = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            SysUtil.stdDbg().println();
                        }

                        // NOTE : Assume that an AT24C02 EEPROM is attached to the TWI bus
                        if(testTWI) {
                            SysUtil.stdDbg().println(MsgNextStage);

                                                                    // 7 6 5 4 3 2  1  0
                            final int   eepromDeviceAddress = 0x57; // . 1 0 1 0 A2 A1 A0
                                                                    // . 1 0 1 0 1  1  1

                            final int   eepromDelayWaitMS   = 3;

                            final int[] eepromPageBuffer4   = new int[4];
                            final int[] eepromPageBuffer8   = new int[8];

                            res = dev.hwtwiBegin(100000, -1, true);
                            SysUtil.stdDbg().print("HW-TWI Enable         = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            /*
                            dev.hwtwiEnd();
                            SysUtil.systemExit();
                            //*/

                            final int[] eepromWordAddress = new int[] { 0x00 };

                            final long tv1 = SysUtil.getNS();

                            //*
                            res = dev.hwtwiWrite( eepromDeviceAddress, eepromWordAddress, false ); // Send the address only
                            SysUtil.stdDbg().print("HW-TWI Write          = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            SysUtil.sleepMS(eepromDelayWaitMS);

                            res = dev.hwtwiRead ( eepromDeviceAddress, eepromPageBuffer8        );
                            SysUtil.stdDbg().print("HW-TWI Receive        = "); SysUtil.stdDbg().printf( "%-5b (%s) " , res, dev.getLastErrorMessage() );
                            for( final int v : eepromPageBuffer8 ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                            SysUtil.sleepMS(eepromDelayWaitMS);
                            //*/

                            for(int i = 0; i < eepromPageBuffer8.length; ++i) ++eepromPageBuffer8[i];

                            //*
                            res = dev.hwtwiWrite( eepromDeviceAddress, XCom.arrayConcatCopy(eepromWordAddress, eepromPageBuffer8) );
                            SysUtil.stdDbg().print("HW-TWI Write          = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            SysUtil.sleepMS(eepromDelayWaitMS);
                            //*/

                            //*
                            res = dev.hwtwiWrite( eepromDeviceAddress, eepromWordAddress, false ); // Send the address only
                            SysUtil.stdDbg().print("HW-TWI Write          = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            SysUtil.sleepMS(eepromDelayWaitMS);

                            res = dev.hwtwiRead ( eepromDeviceAddress, eepromPageBuffer4, false ); // Continue the reading later
                            SysUtil.stdDbg().print("HW-TWI Receive        = "); SysUtil.stdDbg().printf( "%-5b (%s) ", res, dev.getLastErrorMessage() );
                            for( final int v : eepromPageBuffer4 ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                            SysUtil.sleepMS(eepromDelayWaitMS);

                            res = dev.hwtwiRead ( eepromDeviceAddress, eepromPageBuffer4        );
                            SysUtil.stdDbg().print("HW-TWI Receive        = "); SysUtil.stdDbg().printf( "%-5b (%s) " , res, dev.getLastErrorMessage() );
                            for( final int v : eepromPageBuffer4 ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                            SysUtil.sleepMS(eepromDelayWaitMS);
                            //*/

                            final long tv2 = SysUtil.getNS();
                            res = dev.hwtwiEnd();
                            SysUtil.stdDbg().print("HW-TWI Disable        = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            SysUtil.stdDbg().printf( "### %07.2fmS ###\n\n", (tv2 - tv1) * 0.000001 );

                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);

                        } // if

                        if(testSPI) {
                            SysUtil.stdDbg().println(MsgNextStage);

                            res = dev.spiBegin( USB2GPIO.SPIMode._0, USB2GPIO.SSMode.ActiveLow, dev.spiClkFreqToClkDiv(1000000) );
                            SysUtil.stdDbg().print("HW-SPI Begin          = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            /*
                            dev.spiSetClkDiv(8);
                            dev.spiTransferIgnoreSS( new int[255 * 255] );
                            //*/

                            /*
                            if(true) {
                                res = dev.spiSetBreak(false, false);
                                SysUtil.stdDbg().print("HW-SPI Set Break      = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                                if(true) for(;;) {
                                    res = dev.spiXBTransferIgnoreSS(
                                        USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, 0, USB2GPIO.IEVal._X, USB2GPIO.IEVal._X,
                                        java.util.stream.IntStream.range(0, 200).map( i -> (i % 2 == 0) ? 7 : 0 ).toArray()
                                    );
                                    SysUtil.stdDbg().print("HW-SPI XB Trans No-SS = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                                }
                                res = dev.spiClrBreak();
                                SysUtil.stdDbg().print("HW-SPI Clear Break    = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                            }
                            //*/

                            //*
                            boolean doneStep = false;
                            while(!doneStep) {
                                final int[] data = new int[] { 0xAA, 0x55 };
                                res = dev.spiTransferIgnoreSS(data);
                                SysUtil.stdDbg().print("HW-SPI Transfer No-SS = ");
                                SysUtil.stdDbg().printf( "%-5b (%s) : %02X %02X\n", res, dev.getLastErrorMessage(), data[0], data[1] );
                                for( int i = 0; !doneStep && (i < 100); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                            }
                            //*/

                            res = dev.spiEnd();
                            SysUtil.stdDbg().print("HW-SPI End            = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);

                        }

                        if(testBBUSRT) {
                            SysUtil.stdDbg().println(MsgNextStage);

                            res = dev.bbusrtBegin(USB2GPIO.UXRTMode._8E2, USB2GPIO.SSMode.ActiveLow, 115200);
                            SysUtil.stdDbg().print("BB-USRT Begin         = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            SysUtil.sleepMS(10);

                            res = dev.bbusrtSelectSlave();
                            SysUtil.stdDbg().print("BB-USRT Select Slave  = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            //*
                            boolean doneStep = false;
                            while(!doneStep) {
                                final int[] data = new int[] { 0xAA, 0x55 };
                                res = dev.bbusrtTx(data);
                                SysUtil.stdDbg().print("BB-USRT Tx              ");
                                SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                                for( int i = 0; !doneStep && (i < 100); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                            }
                            //*/

                            res = dev.bbusrtEnd();
                            SysUtil.stdDbg().print("BB-USRT End           = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );

                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);
                        }

                        if(testLED) {
                            SysUtil.stdDbg().println(MsgNextStage);
                            boolean doneStep = false;
                            while(!doneStep) {
                                dev.hwgpioSetValues( new int[] { 1, 0, -1, -1, -1, -1, -1, -1 } );
                                for( int i = 0; !doneStep && (i < 10); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                                dev.hwgpioSetValues( new int[] { 0, 1, -1, -1, -1, -1, -1, -1 } );
                                for( int i = 0; !doneStep && (i < 10); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                            }
                            dev.hwgpioSetValues( new int[] { 0, 0, -1, -1, -1, -1, -1, -1 } );
                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);
                        }

                        if(testLED) {
                            SysUtil.stdDbg().println(MsgNextStage);
                            int pwm = 0;
                            int dir = 1;
                            while( dev.hwgpioGetValues()[7] ) {
                                dev.hwgpioSetPWM(0,       pwm);
                                dev.hwgpioSetPWM(1, 255 - pwm);
                                pwm += dir;
                                if(pwm > 255) {
                                    pwm = 255;
                                    dir = -1;
                                }
                                if(pwm < 0) {
                                    pwm = 0;
                                    dir = 1;
                                }
                                SysUtil.sleepMS(2);
                            }
                            dev.hwgpioSetValues( new int[] { 0, 0, -1, -1, -1, -1, -1, -1 } );
                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);
                        }

                        if(testLED) {
                            SysUtil.stdDbg().println(MsgNextStage);
                            boolean doneStep = false;
                            while(!doneStep) {
                                dev.hwgpioSetValues( new int[] { 1, 0, -1, -1, -1, -1, -1, -1 } );
                                for( int i = 0; !doneStep && (i < 10); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                                dev.hwgpioSetValues( new int[] { 0, 1, -1, -1, -1, -1, -1, -1 } );
                                for( int i = 0; !doneStep && (i < 10); ++i ) {
                                    if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                    SysUtil.sleepMS(10);
                                }
                            }
                            dev.hwgpioSetValues( new int[] { 0, 0, -1, -1, -1, -1, -1, -1 } );
                            // ##### !!! TODO : Implement a better method !!! #####
                            SysUtil.sleepMS(50);
                            while( !dev.hwgpioGetValues()[7] );
                            SysUtil.sleepMS(50);
                        }

                        if(testADC) {
                            SysUtil.stdDbg().println(MsgNextStage);
                            if(true) {
                                boolean doneStep = false;
                                while(!doneStep) {
                                    SysUtil.stdDbg().println( "HW-GPIO Get ADC (5)   = " + dev.hwgpioGetADC(5) );
                                    for( int i = 0; !doneStep && (i < 100); ++i ) {
                                        if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                        SysUtil.sleepMS(10);
                                    }
                                }
                            }
                            if(ver[0] == 2) {
                                if(!true) {
                                    final boolean ok = dev.hwgpioCalibrateVREAD(329);
                                    SysUtil.stdDbg().print("HW-GPIO Calib VREAD   = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", ok, dev.getLastErrorMessage() );
                                }
                                SysUtil.sleepMS(50);
                                while( !dev.hwgpioGetValues()[7] );
                                SysUtil.sleepMS(50);
                                SysUtil.stdDbg().println(MsgNextStage);
                                boolean doneStep = false;
                                while(!doneStep) {
                                    SysUtil.stdDbg().println( "HW-GPIO Get VREAD     = " + dev.hwgpioGetVREAD() );
                                    for( int i = 0; !doneStep && (i < 100); ++i ) {
                                        if( !dev.hwgpioGetValues()[7] ) { doneStep = true; break; }
                                        SysUtil.sleepMS(10);
                                    }
                                }
                            }
                            SysUtil.stdDbg().println();
                        }

                        res = dev.hwgpioSetAllHighImpedance();
                        SysUtil.stdDbg().print("HW-GPIO Set All Z     = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                        SysUtil.stdDbg().println();
                        if(testUSRT) {
                            res = dev.hwusrtDisable();
                            SysUtil.stdDbg().print("HW-USRT Disable       = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                        }
                        else {
                            res = dev.hwuartDisable();
                            SysUtil.stdDbg().print("HW-UART Disable       = "); SysUtil.stdDbg().printf( "%-5b (%s)\n", res, dev.getLastErrorMessage() );
                        }
                        SysUtil.stdDbg().println();

                        usb2gpio = dev;

                    }
                    break;

                // Use JxMake DASA
                case 1:
                    if(true) {
                        final DASA dev = new DASA("/dev/ttyUSB0");
                        usb2gpio = dev;
                    }
                    break;

                // Use USB-ISS
                case 2:
                    if(true) {
                        final USB_ISS dev = new USB_ISS("/dev/ttyACM0");
                        SysUtil.stdDbg().print("USB-ISS Version       = "); for( final int v : dev.getVersion     () ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().print("USB-ISS Serial Number = "); for( final int v : dev.getSerialNumber() ) SysUtil.stdDbg().printf("%c"   , v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().println();
                        usb2gpio = dev;
                    }
                    break;

                // Invalid index
                default:
                    SysUtil.systemExitError();
                    break;

            } // switch

            // Test 'USB2GPIO' and 'ProgISP' - ATmega328P - 8MHz
            if(!true) {

                /*
                ispConfig.memoryFlash.totalSize  = 32768;
                ispConfig.memoryFlash.pageSize   =   128;
                ispConfig.memoryFlash.numPages   =   256;

                ispConfig.memoryEEPROM.totalSize =  1024;
                */

                ispConfig = ProgISP.Config.ATmega328P();

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(ispConfig) ); SysUtil.systemExit();

                isp = new ProgISP(usb2gpio, ispConfig);

                //*
                final String hex = true
                                 ? "../src/1-TestData/PTest1X.Blink-ATmega328P.cpp.hex"
                                 : "../src/1-TestData/PTest1X.Blink-ATmega328P.ino.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( isp._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !isp.begin(500000) ) throw XCom.newException("ERR: isp.begin()");

                //*
                if( !isp.readSignature() ) throw XCom.newException("ERR: isp.readSignature()");

                final int[] sigBytes = new int[] { 0x1E, 0x95, 0x0F };
                if( !isp.verifySignature(sigBytes) ) throw XCom.newException("ERR: isp.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : isp.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                int lfuse = isp.readLFuse();
                if(lfuse < 0) throw XCom.newException("ERR: isp.readLFuse()");

                int hfuse = isp.readHFuse();
                if(hfuse < 0) throw XCom.newException("ERR: isp.readHFuse()");

                int efuse = isp.readEFuse();
                if(efuse < 0) throw XCom.newException("ERR: isp.readEFuse()");

                if(!true) {
                    if( !isp.writeLFuse( (byte) lfuse ) ) throw XCom.newException("ERR: isp.writeLFuse()");
                    lfuse = isp.readLFuse();
                    if(lfuse < 0) throw XCom.newException("ERR: isp.readLFuse()");

                    if( !isp.writeHFuse( (byte) hfuse ) ) throw XCom.newException("ERR: isp.writeHFuse()");
                    hfuse = isp.readHFuse();
                    if(hfuse < 0) throw XCom.newException("ERR: isp.readHFuse()");

                    if( !isp.writeEFuse( (byte) efuse ) ) throw XCom.newException("ERR: isp.writeEFuse()");
                    efuse = isp.readEFuse();
                    if(efuse < 0) throw XCom.newException("ERR: isp.readEFuse()");
                }

                SysUtil.stdDbg().print("Fuse (Low High Ext)   = ");
                SysUtil.stdDbg().printf("%02X %02X %02X\n", lfuse, hfuse, efuse);
                SysUtil.stdDbg().println();
                //*/

                //*
                long lbs = isp.readLockBits();
                if(lbs < 0) throw XCom.newException("ERR: isp.readLockBits()");

                if(!true) {
                    if( !isp.writeLockBits(lbs) ) throw XCom.newException("ERR: isp.writeLockBits()");
                    lbs = isp.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: isp.readLockBits()");
                }

                SysUtil.stdDbg().print("Lock Bits             = ");
                SysUtil.stdDbg().printf("%02X\n", lbs);
                SysUtil.stdDbg().println();
                //*/

                //*
                final int clb = isp.readCalibrationByte();
                if(clb < 0) throw XCom.newException("ERR: isp.readCalibrationByte()");

                SysUtil.stdDbg().print("Calibration Byte      = ");
                SysUtil.stdDbg().printf("%02X\n", clb);
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !isp.chipErase() ) throw XCom.newException("ERR: isp.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( isp._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !isp.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: isp.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = isp.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: isp.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: isp.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], isp.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, ispConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !isp.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: isp.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : isp.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( isp.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, ispConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = isp.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: isp.readEEPROM()");

                            if( !isp.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: isp.writeEEPROM()");

                            final int v1 = isp.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: isp.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !isp.end() ) throw XCom.newException("ERR: isp.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgISP' - AT89S8253
            if(!true) {

                ispConfig.spiMode                                               = USB2GPIO.SPIMode._1;
                ispConfig.ssMode                                                = !true
                                                                                ? USB2GPIO.SSMode.ActiveHigh // AT89S8253 reset pin is active high
                                                                                : USB2GPIO.SSMode.ActiveLow; // Some development boards may reverse this by using transistors

                ispConfig.readSignature.instruction                             = new int[] { 0x28, 0x00, 0x30, 0x00,
                                                                                              0x28, 0x00, 0x31, 0x00 };
                ispConfig.readSignature.responseIndex                           = new int[] { 3, 7 };

                ispConfig.memoryFlash.paged                                     = false;

                ispConfig.memoryFlash.totalSize                                 = 12288;
                ispConfig.memoryFlash.pageSize                                  =     0;
                ispConfig.memoryFlash.numPages                                  =     0;

                ispConfig.memoryFlash.instruction_LoadExtAddrByte               = null;

                ispConfig.memoryFlash.instruction_ReadProgMemLB                 = new int[] { 0x20, -1, -1, 0x00 };
                ispConfig.memoryFlash.instruction_ReadProgMemHB                 = null;

                ispConfig.memoryFlash.instruction_LoadProgMemLB                 = null;
                ispConfig.memoryFlash.instruction_LoadProgMemHB                 = null;

                ispConfig.memoryFlash.instruction_WriteProgMemPage              = null;

            if(true) {
                // Using direct page mode is faster that using byte mode AND the only way to  have successful programming
                // when using the JxMake USB-GPIO Module in higher SPI clock
                ispConfig.memoryFlash.paged_direct                              = true;
                ispConfig.memoryFlash.pageSize_direct                           =  64;
                ispConfig.memoryFlash.numPages_direct                           = 192;

                ispConfig.memoryFlash.instruction_WritePageDirect               = new int[] { 0x50, -1, -1       };
            }

                ispConfig.memoryFlash.instruction_WriteProgMemLB                = new int[] { 0x40, -1, -1, -1   };
                ispConfig.memoryFlash.instruction_WriteProgMemHB                = null;

                ispConfig.memoryEEPROM.instruction_ReadEEPROMMem                = null;
                ispConfig.memoryEEPROM.instruction_WriteEEPROMMem               = null;

                ispConfig.memoryLFuse.instruction_ReadLFuse                     = null;
                ispConfig.memoryLFuse.instruction_WriteLFuse                    = null;

                ispConfig.memoryLFuse.instruction_ReadLFuse                     = null;
                ispConfig.memoryLFuse.instruction_WriteLFuse                    = null;

                ispConfig.memoryHFuse.instruction_ReadHFuse                     = null;
                ispConfig.memoryHFuse.instruction_WriteHFuse                    = null;

                ispConfig.memoryEFuse.instruction_ReadEFuse                     = null;
                ispConfig.memoryEFuse.instruction_WriteEFuse                    = null;

                ispConfig.memoryLockBits.instruction_ReadLockBits               = null;
                ispConfig.memoryLockBits.instruction_WriteLockBits              = null;

                ispConfig.memoryCalibrationByte.instruction_ReadCalibrationByte = null;

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(ispConfig) ); SysUtil.systemExit();

                isp = new ProgISP(usb2gpio, ispConfig);

                //*
                final String hex = "../src/1-TestData/PTest1X.Blink-AT89S8253.cpp.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( isp._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !isp.begin(125000) ) throw XCom.newException("ERR: isp.begin()");

                //*
                if( !isp.readSignature() ) throw XCom.newException("ERR: isp.readSignature()");

                final int[] sigBytes = new int[] { 0x1E, 0x73 };
                if( !isp.verifySignature(sigBytes) ) throw XCom.newException("ERR: isp.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : isp.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !isp.chipErase() ) throw XCom.newException("ERR: isp.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( isp._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !isp.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: isp.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = isp.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: isp.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: isp.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], isp.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, ispConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !isp.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: isp.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : isp.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !isp.end() ) throw XCom.newException("ERR: isp.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgTPI' - ATtiny10
            if(!true) {

                /*
                tpiConfig.memoryFlash.totalSize     = 1024;
                tpiConfig.memoryFlash.pageSize      =   16;
                tpiConfig.memoryFlash.numPages      =   64;
                tpiConfig.memoryFlash.numWordWrites =    1;
                */

                tpiConfig = ProgTPI.Config.ATtiny10();

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(tpiConfig) ); SysUtil.systemExit();

                tpi = new ProgTPI(usb2gpio, tpiConfig);

                //*
                final String hex = "../src/1-TestData/PTest1X.Blink-ATtiny10.cpp.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( tpi._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !tpi.begin(128000) ) throw XCom.newException("ERR: tpi.begin()");

                //*
                if( !tpi.readSignature() ) throw XCom.newException("ERR: tpi.readSignature()");

                final int[] sigBytes = new int[] { 0x1E, 0x90, 0x03 };
                if( !tpi.verifySignature(sigBytes) ) throw XCom.newException("ERR: tpi.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : tpi.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                int fuse = tpi.readFuse();
                if(fuse < 0) throw XCom.newException("ERR: tpi.readFuse()");

                if(!true) {
                    if( !tpi.writeFuse( (byte) fuse ) ) throw XCom.newException("ERR: tpi.writeFuse()");
                    fuse = tpi.readFuse();
                    if(fuse < 0) throw XCom.newException("ERR: tpi.readFuse()");
                }

                SysUtil.stdDbg().print("Fuse                  = ");
                SysUtil.stdDbg().printf("%02X\n", fuse);
                SysUtil.stdDbg().println();
                //*/

                //*
                long lbs = tpi.readLockBits();
                if(lbs < 0) throw XCom.newException("ERR: tpi.readLockBits()");

                if(!true) {
                    if( !tpi.writeLockBits(lbs) ) throw XCom.newException("ERR: tpi.writeLockBits()");
                    lbs = tpi.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: tpi.readLockBits()");
                }

                SysUtil.stdDbg().print("Lock Bits             = ");
                SysUtil.stdDbg().printf("%02X\n", lbs);
                SysUtil.stdDbg().println();
                //*/

                //*
                final int clb = tpi.readCalibrationByte();
                if(clb < 0) throw XCom.newException("ERR: tpi.readCalibrationByte()");

                SysUtil.stdDbg().print("Calibration Byte      = ");
                SysUtil.stdDbg().printf("%02X\n", clb);
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !tpi.chipErase() ) throw XCom.newException("ERR: tpi.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( tpi._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !tpi.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: tpi.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = tpi.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: tpi.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: tpi.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], tpi.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, tpiConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !tpi.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: tpi.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : tpi.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !tpi.end() ) throw XCom.newException("ERR: tpi.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgUPDI' - ATmega4808/ATtiny3226/AVR128DA/AVR32DU/AVR32EA/AVR32EB/AVR32SD
            if(!!true) {

/*
NVM type 0: 16-bit, page oriented write
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p m4808 -v
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p m4808 -D -U fuses:r:-:h
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p m4808 -e -D -V -U flash:w:PTest1X.Blink-ATmega4808.cpp.hex:i
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyACM1 -b 115200 -p m4808 -e -D -V -U flash:w:PTest1X.Blink-ATmega4808.cpp.hex:i

NVM type 0: 16-bit, page oriented write
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p t3226 -v
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p t3226 -D -U fuses:r:-:h
/opt/avrdude-7.3-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p t3226 -e -D -V -U flash:w:PTest1X.Blink-ATtiny3226.cpp.hex:i

NVM type 2: 24-bit, word oriented write
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr128da28 -v
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr128da28 -D -U fuses:r:-:h
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr128da28 -D -U lock:r:-:h
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr128da28 -e -D -V -U flash:w:PTest1X.Blink-AVR128DA.cpp.hex:i

NVM type 4: 24-bit, word oriented
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32du28 -v
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32du28 -D -U fuses:r:-:h
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32du28 -D -U lock:r:-:h

NVM type 3: 24-bit, page oriented
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32ea28 -v
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32ea28 -D -U fuses:r:-:h
/opt/avrdude-8.0-usbasp-pdi/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32ea28 -D -U lock:r:-:h

##### !!! TODO !!! : ??? #####
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32eb28 -v
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32eb28 -D -U fuses:r:-:h
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 115200 -p avr32eb28 -D -U lock:r:-:h

NVM type 6: 24-bit, word oriented
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 19200 -p avr32sd28 -v
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 19200 -p avr32sd28 -D -U fuses:r:-:h
/opt/avrdude-8.1/bin/avrdude -c serialupdi -P /dev/ttyUSB0 -b 19200 -p avr32sd28 -D -U lock:r:-:h
*/

                final int ATmega4808 = 0;
                final int ATtiny3226 = 1;
                final int AVR128DA   = 2;
                final int AVR32DU    = 3;
                final int AVR32EA    = 4;
                final int AVR32EB    = 5; // ##### !!! TODO !!! ##### ↻↻↻ ../test/src/cpp_atmega/JxMakeFile ↻↻↻ TODO items ↻↻↻
                final int AVR32SD    = 6;

                final int SelMCU     = AVR32DU;

                updiConfig = (SelMCU == ATmega4808) ? ProgUPDI.Config.ATmega4808()
                           : (SelMCU == ATtiny3226) ? ProgUPDI.Config.ATtiny3226()
                           : (SelMCU == AVR128DA  ) ? ProgUPDI.Config.AVR128DA  ()
                           : (SelMCU == AVR32DU   ) ? ProgUPDI.Config.AVR32DU   ()
                           : (SelMCU == AVR32EA   ) ? ProgUPDI.Config.AVR32EA   ()
                           : (SelMCU == AVR32EB   ) ? ProgUPDI.Config.AVR32EB   ()
                           : (SelMCU == AVR32SD   ) ? ProgUPDI.Config.AVR32SD   ()
                           :                          null;

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(updiConfig) ); SysUtil.systemExit();

                updi = new ProgUPDI(usb2gpio, updiConfig);

                //*
                final String hex = (SelMCU == ATmega4808) ? "../src/1-TestData/PTest1X.Blink-ATmega4808.cpp.hex"
                                 : (SelMCU == ATtiny3226) ? "../src/1-TestData/PTest1X.Blink-ATtiny3226.cpp.hex"
                                 : (SelMCU == AVR128DA  ) ? "../src/1-TestData/PTest1X.Blink-AVR128DA.cpp.hex"
                                 : (SelMCU == AVR32DU   ) ? "../src/1-TestData/PTest1X.Blink-AVR32DU.cpp.hex"
                                 : (SelMCU == AVR32EA   ) ? "../src/1-TestData/PTest1X.Blink-AVR32EA.cpp.hex"
                                 : (SelMCU == AVR32EB   ) ? "../src/1-TestData/PTest1X.Blink-AVR32EB.cpp.hex"
                                 : (SelMCU == AVR32SD   ) ? "../src/1-TestData/PTest1X.Blink-AVR32SD.cpp.hex"
                                 :                          null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( updi._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                final int brate = ( (usb2gpio instanceof USB_GPIO) && ( (USB_GPIO) usb2gpio ).isHWv2() )
                                ? 1000000
                                :  115200;

                if( !updi.begin(brate) ) throw XCom.newException("ERR: updi.begin()");

                updi.dumpDeviceDetails( SysUtil.stdDbg() );

                //*
                if( !updi.readSignature() ) throw XCom.newException("ERR: updi.readSignature()");

                final int[] sigBytes = (SelMCU == ATmega4808) ? new int[] { 0x1E, 0x96, 0x50 } // ATmega4808
                                     : (SelMCU == ATtiny3226) ? new int[] { 0x1E, 0x95, 0x27 } // ATtiny3226
                                     : (SelMCU == AVR128DA  ) ? new int[] { 0x1E, 0x97, 0x0A } // AVR128DA28
                                     : (SelMCU == AVR32DU   ) ? new int[] { 0x1E, 0x95, 0x40 } // AVR32DU28
                                     : (SelMCU == AVR32EA   ) ? new int[] { 0x1E, 0x95, 0x3E } // AVR32EA28
                                     : (SelMCU == AVR32EB   ) ? new int[] { 0x1E, 0x95, 0x2B } // AVR32EB28
                                     : (SelMCU == AVR32SD   ) ? new int[] { 0x1E, 0x95, 0x53 } // AVR32SD28
                                     :                          null;
                if( !updi.verifySignature(sigBytes) ) throw XCom.newException("ERR: updi.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : updi.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                int[] fuses = updi.readFuses();
                if(fuses == null) throw XCom.newException("ERR: updi.readFuses()");

                if(!true) {
                    if(SelMCU == ATmega4808) fuses[5] = 0xC9; // Enable external reset          on PF.6   and   EEPROM preservation during a chip erase
                    if(SelMCU == ATtiny3226) fuses[5] = 0xCC; // Enable external reset and UPDI on PA.0
                    if(SelMCU == AVR128DA  ) fuses[5] = 0xC8; // Enable external reset          on PF.6
                    if(SelMCU == AVR32DU   ) fuses[5] = 0xD8; // Enable external reset          on PF.6   and   UPDI on PF.7
                    if(SelMCU == AVR32EA   ) fuses[5] = 0xD8; // Enable external reset          on PF.6   and   UPDI on PF.7
                    if(SelMCU == AVR32EB   ) fuses[5] = 0xD8; // Enable external reset          on PF.6   and   UPDI on PF.7
                    if(SelMCU == AVR32SD   ) fuses[5] = 0x01; // Enable EEPROM preservation during a chip erase
                    if( !updi.writeFuses(fuses) ) throw XCom.newException("ERR: updi.writeFuses()");
                    fuses = updi.readFuses();
                    if(fuses == null) throw XCom.newException("ERR: updi.readFuses()");
                }

                SysUtil.stdDbg().print("Fuses                 = ");
                for(final int f : fuses) {
                    if(f < 0) SysUtil.stdDbg().printf("NA "     );
                    else      SysUtil.stdDbg().printf("%02X ", f);
                }
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                long lbs = updi.readLockBits();
                if(lbs < 0) throw XCom.newException("ERR: updi.readLockBits()");

                if(!true) {
                    if( !updi.writeLockBits(lbs) ) throw XCom.newException("ERR: updi.writeLockBits()");
                    lbs = updi.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: updi.readLockBits()");
                }

                final int lbSize = Math.max(updiConfig.memoryLockBits.size, 1) * 2;

                SysUtil.stdDbg().print("Lock Bits             = ");
                SysUtil.stdDbg().printf( "%0" + lbSize + "X\n", lbs );
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !updi.chipErase() ) throw XCom.newException("ERR: updi.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( updi._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !updi.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: updi.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = updi.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: updi.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: updi.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], updi.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                if( updi.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, updiConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = updi.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: updi.readEEPROM()");

                            if( !updi.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: updi.writeEEPROM()");

                            final int v1 = updi.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: updi.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 1, updiConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !updi.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: updi.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!!true) {
                    for(final int b : updi.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !updi.end() ) throw XCom.newException("ERR: updi.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgPDI' - ATxmega16D4
            if(!true) {

                /*
                pdiConfig.memoryFlash.totalSize  = 16384;
                pdiConfig.memoryFlash.pageSize   =   256;
                pdiConfig.memoryFlash.numPages   =    64;

                pdiConfig.memoryEEPROM.totalSize = 1024;
                pdiConfig.memoryEEPROM.pageSize  =   32;
                pdiConfig.memoryEEPROM.numPages  =   32;
                */

                pdiConfig = ProgPDI.Config.ATxmega16D4();

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(pdiConfig) ); SysUtil.systemExit();

                pdi = new ProgPDI(usb2gpio, pdiConfig);

                //*
                final String hex = "../src/1-TestData/PTest1X.Blink-ATxmega16D4.cpp.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( pdi._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                /*
                 * ##### !!! WARNING !!! #####
                 * This programmer is currently not very reliable when:
                 *     # Using hardware version 1 (the older 'JxMake USB GPIO'    module) at any baud rate.
                 *     # Using hardware version 2 (the newer 'JxMake USB GPIO II' module) at low baud rates.
                 *
                 * 57600   115200   230400   460800   921600
                 * 1       2        4        8        16
                 * v1      ?        ?        v2       v2
                 */
                final int bmul = ( (usb2gpio instanceof USB_GPIO) && ( (USB_GPIO) usb2gpio ).isHWv2() ) ? 16 : 2;

                if( !pdi.begin(57600 * bmul) ) throw XCom.newException("ERR: pdi.begin()");

                //*
                if( !pdi.readSignature() ) throw XCom.newException("ERR: pdi.readSignature()");

                final int[] sigBytes = new int[] { 0x1E, 0x94, 0x42 };
                if( !pdi.verifySignature(sigBytes) ) throw XCom.newException("ERR: pdi.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : pdi.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                int[] fuses = pdi.readFuses();
                if(fuses == null) throw XCom.newException("ERR: pdi.readFuses()");

                if(!true) {
                    if( !pdi.writeFuses(fuses) ) throw XCom.newException("ERR: pdi.writeFuses()");
                    fuses = pdi.readFuses();
                    if(fuses == null) throw XCom.newException("ERR: pdi.readFuses()");
                }

                SysUtil.stdDbg().print("Fuses                 = ");
                for(final int f : fuses) {
                    if(f < 0) SysUtil.stdDbg().printf("NA "     );
                    else      SysUtil.stdDbg().printf("%02X ", f);
                }
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                long lbs = pdi.readLockBits();
                if(lbs < 0) throw XCom.newException("ERR: pdi.readLockBits()");

                if(!true) {
                    if( !pdi.writeLockBits(lbs) ) throw XCom.newException("ERR: pdi.writeLockBits()");
                    lbs = pdi.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: pdi.readLockBits()");
                }

                SysUtil.stdDbg().print("Lock Bits             = ");
                SysUtil.stdDbg().printf("%02X\n", lbs);
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !pdi.chipErase() ) throw XCom.newException("ERR: pdi.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( pdi._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !pdi.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: pdi.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = pdi.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: pdi.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: pdi.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], pdi.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(fwLength, pdiConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !pdi.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: pdi.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                SysUtil.stdDbg().printf( "### %b ###\n", Arrays.equals( pdi.config().memoryFlash.readDataBuff, USB2GPIO.ba2ia(fwDataBuff) ) );
                if(!true) {
                    for(final int b : pdi.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( pdi.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, pdiConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = pdi.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: pdi.readEEPROM()");

                            if( !pdi.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: pdi.writeEEPROM()");

                            final int v1 = pdi.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: pdi.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    if(false) {
                        // NOTE : 'commitEEPROM()' will be called automatically by 'pdi.end()'
                        if( !pdi.commitEEPROM() ) throw XCom.newException("ERR: pdi.commitEEPROM()");
                    }

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !pdi.end() ) throw XCom.newException("ERR: pdi.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgLGT8' - LGT8F328P
            if(!true) {

                lgt8Config = ProgLGT8.Config.LGT8F328P();

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(lgt8Config) ); SysUtil.systemExit();

                lgt8 = new ProgLGT8(usb2gpio,lgt8Config);

                //*
                final String hex = "../src/1-TestData/PTest1X.Blink-LGT8F328P.ino.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( lgt8._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !lgt8.begin() ) throw XCom.newException("ERR: lgt8.begin()");

                //*
                if( !lgt8.readSignature() ) throw XCom.newException("ERR: lgt8.readSignature()");

                final int[] sigBytes = new int[] { 0xA2, 0x50, 0xE9 };
                if( !lgt8.verifySignature(sigBytes) ) throw XCom.newException("ERR: lgt8.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : lgt8.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                final int offset = 1024 * 0;

                SysUtil.stdDbg().println("Erasing Chip");
                if( !lgt8.chipErase() ) throw XCom.newException("ERR: lgt8.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( lgt8._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !lgt8.writeFlash(fwDataBuff, fwStartAddress + offset, fwLength, stdPP) ) throw XCom.newException("ERR: lgt8.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = lgt8.verifyFlash(fwDataBuff, fwStartAddress + offset, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: lgt8.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: lgt8.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], lgt8.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                if( !lgt8.end() ) throw XCom.newException("ERR: lgt8.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgJTAG' - ATmega32A - 4MHz
            if(!true) {

                final int svfFileSel = 2;

                final String[] svfFile = new String[] {
                    "../src/1-TestData/PTest1X.Blink-ATmega32A.sig.svf"       , /* 0 */
                    "../src/1-TestData/PTest1X.Blink-ATmega32A.cpp.serial.svf", /* 1 */
                    "../src/1-TestData/PTest1X.Blink-ATmega32A.cpp.pgload.svf"  /* 2 */
                };

                final JTAGBitstream jbs = new JTAGBitstream();
                jbs.loadSVF(svfFile[svfFileSel]);

                jtag = new ProgJTAG(usb2gpio);

                if( !jtag.begin( true ? (2000000) : 32000 ) ) throw XCom.newException("ERR: jtag.begin()");

                /*
                    // ##### ??? TODO : REMOVE THIS BLOCK LATER ??? #####
                    // ##### ??? TODO : REMOVE THIS BLOCK LATER ??? #####
                    // ##### ??? TODO : REMOVE THIS BLOCK LATER ??? #####
                    final int[] rj2 = new int[2];

                                                  ( (USB_GPIO) usb2gpio ).jtagTMS(true, true, true, 6, 0b1111111);
                                                  ( (USB_GPIO) usb2gpio ).jtagTMS(true, true, true, 6, 0b0000000);

                    //                                                                  U      D      I
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , false, true ,  3, new int[] { 0x0C   } ); // AVR_RESET
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false,  0, new int[] { 0x01   } );

                    //                                                                  U      D      I
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , false, true ,  3, new int[] { 0x04   } ); // PROG_ENABLE
                    rj2[1] = 0xA3; rj2[0] = 0x70; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 15, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);

                    //                                                                  U      D      I
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , false, true ,  3, new int[] { 0x05   } ); // PROG_COMMANDS
                    rj2[1] = 0x23; rj2[0] = 0x08; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);

                    rj2[1] = 0x03; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  ); // #0
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x32; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x33; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X []\n", rj2[1], rj2[0]);

                    rj2[1] = 0x03; rj2[0] = 0x01; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  ); // #0
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x32; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x33; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X []\n", rj2[1], rj2[0]);

                    rj2[1] = 0x03; rj2[0] = 0x02; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  ); // #0
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x32; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);
                    rj2[1] = 0x33; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 14, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X []\n", rj2[1], rj2[0]);

                    //                                                                  U      D      I
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , false, true ,  3, new int[] { 0x04   } ); // !PROG_ENABLE
                    rj2[1] = 0x00; rj2[0] = 0x00; ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false, 15, rj2                  );
                                                  SysUtil.stdDbg().printf("%02X%02X\n", rj2[1], rj2[0]);

                    //                                                                  U      D      I
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , false, true ,  3, new int[] { 0x0C   } ); // !AVR_RESET
                                                  ( (USB_GPIO) usb2gpio ).jtagTransfer( true , true , false,  0, new int[] { 0x01   } );

                                                  ( (USB_GPIO) usb2gpio ).jtagTMS(true, true, true, 6, 0b1111111);
                //*/

                //*
                final int bz   = jbs.getEffByteSize();
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(bz) );
                final long tv1 = SysUtil.getNS();
                if( !jtag.execute(jbs, stdPP) ) throw XCom.newException("ERR: jtag.execute()");
                final long tv2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", bz, (tv2 - tv1) * 0.000000001, 1000000000.0 * bz / (tv2 - tv1) );
                SysUtil.stdDbg().println();
                //*/

                if( !jtag.end() ) throw XCom.newException("ERR: jtag.end()");

                usb2gpio.shutdown();

            } // if

        } // try
        catch(final Exception e) {
            if(isp      != null) isp     .end();
            if(tpi      != null) tpi     .end();
            if(updi     != null) updi    .end();
            if(pdi      != null) pdi     .end();
            if(lgt8     != null) lgt8    .end();
            if(jtag     != null) jtag    .end();
            if(usb2gpio != null) usb2gpio.resetAndShutdown();
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTest1X
