#include <p30F3011.h>


// ====== Configuration words =====
// FOSC
#pragma config FCKSMEN = CSW_FSCM_OFF
#pragma config FOSFPR  = FRC // XT_PLL4

// FWDT
#pragma config WDT     = WDT_OFF
#pragma config FWPSA   = WDTPSA_512
#pragma config FWPSB   = WDTPSB_16

// FBORPOR
#pragma config MCLRE   = MCLR_EN
#pragma config PWMPIN  = RST_IOPIN
#pragma config HPOL    = PWMxH_ACT_HI
#pragma config LPOL    = PWMxL_ACT_LO
#pragma config BOREN   = PBOR_OFF
#pragma config BODENV  = BORV42
#pragma config FPWRT   = PWRT_64 // PWRT_OFF

// FGS
#pragma config GCP     = CODE_PROT_OFF
#pragma config GWRP    = GWRP_OFF

// FICD
#pragma config ICS     = ICS_PGD // ICS_PGD1


// ===== Pins =====
#define LED_1_LAT  LATEbits .LATE3
#define LED_1_TRIS TRISEbits.TRISE3

#define LED_2_LAT  LATEbits .LATE4
#define LED_2_TRIS TRISEbits.TRISE4

#define SW_PORT    PORTEbits.RE5
#define SW_TRIS    TRISEbits.TRISE5


// ===== Blink delay =====
#define BLINK_DELAY_SLOW 10
#define BLINK_DELAY_FAST  3


// ===== EEPROM data =====
  const char     __attribute__((space(eedata))) EEPROM_DATA[16] = "Test EEPROM Data";
//const char     __attribute__((space(eedata))) EEPROM_DATA[]   = { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88 };
//const uint32_t __attribute__((space(eedata))) EEPROM_DATA[]   = { 0x11223344, 0x55667788 };


// ===== Helper functions =====
extern void __delay32(unsigned long cycles);

void delay_and_check_switch()
{
    unsigned int delay = (SW_PORT == 0) ? BLINK_DELAY_FAST : BLINK_DELAY_SLOW;

    while(delay--) __delay32(7370000 / 100);
}


// ===== Main program =====
int main(void)
{
//#define SPI_TEST
//#define SPI_MASTER
    
    LED_1_TRIS = 0; // Set LED1 as output
    LED_2_TRIS = 0; // Set LED2 as output
    SW_TRIS    = 1; // Set SW   as input

    // Set all pins to digital
    ADPCFG = 0xFFFF;

#ifdef SPI_TEST

    // Set SPI pins as digital I/O
    TRISFbits.TRISF6   = 0; // SCK1 - Output
    TRISFbits.TRISF2   = 1; // SDI1 - Input
    TRISFbits.TRISF3   = 0; // SDO1 - Output
#ifdef SPI_MASTER
#else // SPI_SLAVE
    TRISBbits.TRISB2   = 1; // SS1  - Input
#endif

    // Enable SPI port
    SPI1STATbits.SPIEN = 1;

    // Configure clock polarity and phase
    SPI1CONbits.FRMEN  = 0;
    SPI1CONbits.SPIFSD = 0;
    SPI1CONbits.DISSDO = 0;
    SPI1CONbits.MODE16 = 0;
    SPI1CONbits.SMP    = 0;
    SPI1CONbits.CKE    = 1;
#ifdef SPI_MASTER
    SPI1CONbits.SSEN   = 0;
#else // SPI_SLAVE
    SPI1CONbits.SSEN   = 1;
#endif
    SPI1CONbits.CKP    = 0;
#ifdef SPI_MASTER
    SPI1CONbits.MSTEN  = 1;
#else // SPI_SLAVE
    SPI1CONbits.MSTEN  = 0;
#endif
    SPI1CONbits.SPRE   = 0;
    SPI1CONbits.PPRE   = 0;

    uint8_t spiData = 0;

    while(1) {

#ifdef SPI_MASTER
        SPI1BUF = 0b11000010;
#endif
        while(SPI1STATbits.SPIRBF == 0);

        const char data = SPI1BUF;
        if(data) {
            LED_1_LAT = 1;
            LED_2_LAT = 0;
        }
        else {
            LED_1_LAT = 0;
            LED_2_LAT = 1;
        }

#ifdef SPI_MASTER
#else // SPI_SLAVE
        SPI1BUF = data;
        while(SPI1STATbits.SPITBF != 0);
#endif

    } // while

#else

    while(1) {

        // Turn on  LED1 and turn off LED2
        LED_1_LAT = 1;
        LED_2_LAT = 0;

        // Delay and check the switch
        delay_and_check_switch();

        // Turn off LED1 and turn on  LED2
        LED_1_LAT = 0;
        LED_2_LAT = 1;

        // Delay and check the switch
        delay_and_check_switch();

    } // while

#endif

    return 0;
}
