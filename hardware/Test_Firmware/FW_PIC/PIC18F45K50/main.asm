    ; clear && make clean && make && make clean

    LIST  P=PIC18F45K50
    RADIX DEC

    #include p18f45k50.inc

    #define LED_1_PIN RD3 ; External LED
    #define LED_2_PIN RD2 ; External LED
    #define SW_PIN    RD1 ; External SW

    ; ----------------------------------------------------------------------------------------------------

    CONFIG PLLSEL   = PLL4X
    CONFIG CFGPLLEN = OFF
    CONFIG CPUDIV   = NOCLKDIV
    CONFIG LS48MHZ  = SYS24X4
    CONFIG FOSC     = INTOSCCLKO
    CONFIG PCLKEN   = ON
    CONFIG FCMEN    = OFF
    CONFIG IESO     = OFF
    CONFIG PWRTEN   = ON
    CONFIG BOREN    = SBORDIS
    CONFIG BORV     = 285
    CONFIG LPBOR    = OFF
    CONFIG WDTEN    = OFF
    CONFIG WDTPS    = 32768
    CONFIG CCP2MX   = RC1
    CONFIG PBADEN   = OFF
    CONFIG T3CMX    = RC0
    CONFIG SDOMX    = RB3
    CONFIG MCLRE    = ON
    CONFIG STVREN   = ON
    CONFIG LVP      = ON
    CONFIG ICPRT    = OFF
    CONFIG XINST    = OFF
    CONFIG DEBUG    = OFF

    ; ----------------------------------------------------------------------------------------------------

    ORG __EEPROM_START
    DB "Test EEPROM Data"

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x01
    #define DelayCounter1      0x02
    #define DelayCounter2      0x03

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x00
    GOTO START

    ORG 0x08
    RETFIE

    ORG 0x18
    RETFIE

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Use 250 kHz clock
    MOVLW   b'00010010'
    MOVWF   OSCCON
    MOVLW   b'00000000'
    MOVWF   OSCCON2

    ; Initialize pins
    CLRF    PORTD
    CLRF    LATD

    BANKSEL ANSELD
    CLRF    ANSELD

    BCF     TRISD, LED_1_PIN ; Set LED_1_PIN as output
    BCF     TRISD, LED_2_PIN ; Set LED_2_PIN as output
    BSF     TRISD, SW_PIN    ; Set SW_PIN    as input

    BCF     LATD, LED_1_PIN
    BSF     LATD, LED_2_PIN

MAIN_LOOP:
    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH, 1

    ; Toggle the LEDs
    BTG     LATD, LED_1_PIN
    BTG     LATD, LED_2_PIN

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC   PORTD, SW_PIN
    GOTO    DELAY_SLOW
    MOVLW   BLINK_DELAY_FAST
    MOVWF   CheckSwitchCounter, A
    GOTO    CHECK_SWITCH_LOOP
DELAY_SLOW:
    MOVLW   BLINK_DELAY_SLOW
    MOVWF   CheckSwitchCounter, A
    ; Loop
CHECK_SWITCH_LOOP:
    CALL    DELAY, 1
    DECFSZ  CheckSwitchCounter, F, A
    GOTO    CHECK_SWITCH_LOOP
    ; Done
    RETURN

    ; Delay
DELAY:
    MOVLW   0x02
    MOVWF   DelayCounter1, A
DELAY_LOOP1:
    MOVLW   0xFF
    MOVWF   DelayCounter2, A
DELAY_LOOP2:
    CLRWDT
    DECFSZ  DelayCounter2, F, A
    GOTO    DELAY_LOOP2
    DECFSZ  DelayCounter1, F, A
    GOTO    DELAY_LOOP1
    RETURN

    ; ----------------------------------------------------------------------------------------------------

    END
