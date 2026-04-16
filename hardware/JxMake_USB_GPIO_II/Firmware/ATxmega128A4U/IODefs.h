/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __IO_DEFS_H__
#define __IO_DEFS_H__


// DMA channel assigment
#define DEBUG_UART_DMA      DMA.CH3
#define DEBUG_UART_DMA_TRIG DMA_CH_TRIGSRC_USARTE0_DRE_gc

#define HW_UXRT_DMA         DMA.CH2
#define HW_UXRT_DMA_TRIG    DMA_CH_TRIGSRC_USARTD0_DRE_gc



// Debugging UART
#define DEBUG_UART          USARTE0
#define DEBUG_UART_PORT     E
#define DEBUG_UART_RX_BIT   2 // RXD0 -> PE.2
#define DEBUG_UART_TX_BIT   3 // TXD0 -> PE.3


// ADC pin for VREAD
#define ADC_VREAD_CHANNEL   ADC_CH_MUXPOS_PIN8_gc
#define ADC_VREAD_PORT      B
#define ADC_VREAD_BIT       0


// Control pins used by 'Versatile MCU Programmer'
#define NCTL0_PORT          B
#define NCTL0_BIT           1 // nCTL0 -> PB.1 : XCK→  nSS→  SCK→

#define CTL0_PORT           B
#define CTL0_BIT            2 //  CTL0 -> PB.2 : RXD0← MISO←      ; NOTE : These two pins are always in input mode.

#define CTL1_PORT           B
#define CTL1_BIT            3 //  CTL1 -> PB.3 : TXD0→

#define CTL2_PORT           D
#define CTL2_BIT            0 //  CTL2 -> PD.0 :       MOSI→


// HW-SPI
#define HW_SPI_SPI          SPIC
#define HW_SPI_PORT         C

#define HW_SPI_NSS_BIT      4 // nSS  -> PC.4
#define HW_SPI_SCK_BIT      7 // SCK  -> PC.7
#define HW_SPI_MOSI_BIT     5 // MOSI -> PC.5
#define HW_SPI_MISO_BIT     6 // MISO -> PC.6

#define HW_SPI_VPORT        VPORT0
#define HW_SPI_VPORTCFG()   do { PORTCFG.VPCTRLA = (PORTCFG.VPCTRLA & ~PORTCFG_VP0MAP_gm) | (PORTCFG_VP02MAP_PORTC_gc << PORTCFG_VP0MAP_gp); } while(0)

#define HW_SPI_NSS_LLT      HW_nSS
#define HW_SPI_SCK_LLT      HW_SCK
#define HW_SPI_MOSI_LLT     HW_MOSI


// HW-TWI
#define TWI_TWI             TWIE
#define TWI_PORT            E
#define TWI_SDA_BIT         0 // SDA -> PE.0
#define TWI_SCL_BIT         1 // SCL -> PE.1


// HW-UXRT
#define HW_UXRT             USARTD0
#define HW_UXRT_ISR_RXC     USARTD0_RXC_vect
#define HW_UXRT_PORT        D
#define HW_UXRT_RXD_BIT     2 // RXD0 -> PD.2
#define HW_UXRT_TXD_BIT     3 // TXD0 -> PD.3
#define HW_UXRT_XCK_BIT     1 // XCK0 -> PD.1

#define HW_UXRT_NSS_PORT    HW_SPI_PORT
#define HW_UXRT_NSS_BIT     HW_SPI_NSS_BIT

#define HW_UXRT_TXD_LLT     HW_TXD
#define HW_UXRT_XCK_LLT     HW_XCK

#define HW_UXRT_NSS_LLT     HW_SPI_NSS_LLT


// BB-USRT
#define BB_USRT_PORT        HW_SPI_PORT

#define BB_USRT_XCK_BIT     HW_SPI_SCK_BIT
#define BB_USRT_TXD_BIT     HW_SPI_MOSI_BIT
#define BB_USRT_RXD_BIT     HW_SPI_MISO_BIT

#define BB_USRT_TXD_LLT     HW_SPI_MOSI_LLT
#define BB_USRT_XCK_LLT     HW_SPI_SCK_LLT


// BB-SWIM
#define BB_SWIM_PORT        HW_SPI_PORT

#define BB_SWIM_SDO_BIT     HW_SPI_MOSI_BIT
#define BB_SWIM_SDI_BIT     HW_SPI_MISO_BIT

#define BB_SWIM_VPORT       HW_SPI_VPORT
#define BB_SWIM_VPORTCFG()  HW_SPI_VPORTCFG()


// BB-JTAG
#define BB_JTAG_M_PORT      HW_SPI_PORT  // TDI TDO TCK nRST
#define BB_JTAG_S_PORT      HW_UXRT_PORT // TMS         nTRST

#define BB_JTAG_S_NTRST_BIT HW_UXRT_XCK_BIT
#define BB_JTAG_M_TDI_BIT   HW_SPI_MOSI_BIT
#define BB_JTAG_S_TMS_BIT   HW_UXRT_TXD_BIT
#define BB_JTAG_M_TCK_BIT   HW_SPI_SCK_BIT
#define BB_JTAG_M_TDO_BIT   HW_SPI_MISO_BIT
#define BB_JTAG_M_NRST_BIT  HW_SPI_NSS_BIT

#define BB_JTAG_S_NTRST_LLT HW_UXRT_XCK_LLT // NOTE : These values should be unused because the code will use SPI and UART
#define BB_JTAG_M_TDI_LLT   HW_SPI_MOSI_LLT //        functions to initialize JTAG
#define BB_JTAG_S_TMS_LLT   HW_UXRT_TXD_LLT // ---
#define BB_JTAG_M_TCK_LLT   HW_SPI_SCK_LLT  // ---
#define BB_JTAG_M_NRST_LLT  HW_SPI_NSS_LLT  // ---


#endif // __IO_DEFS_H__
