    ; clear && make clean && make && make clean

    LIST  P=18F4520
    RADIX DEC

    #include p18f4520.inc

    #define LED_1_PIN RD3 ; External LED
    #define LED_2_PIN RD2 ; External LED
    #define SW_PIN    RD1 ; External SW

    ; ----------------------------------------------------------------------------------------------------

    CONFIG OSC     = INTIO7
    CONFIG FCMEN   = OFF
    CONFIG IESO    = OFF
    CONFIG PWRT    = ON
    CONFIG BOREN   = SBORDIS
    CONFIG BORV    = 3
    CONFIG WDT     = OFF
    CONFIG WDTPS   = 32768
    CONFIG CCP2MX  = PORTC
    CONFIG PBADEN  = OFF
    CONFIG LPT1OSC = OFF
    CONFIG MCLRE   = ON
    CONFIG STVREN  = ON
    CONFIG LVP     = ON
    CONFIG XINST   = OFF
    CONFIG DEBUG   = OFF

    ; ----------------------------------------------------------------------------------------------------

    ORG __EEPROM_START
    DB "Test EEPROM Data"

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x01
    #define DelayCounter       0x02

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x00
    GOTO START

    ORG 0x08
    RETFIE

    ORG 0x18
    RETFIE

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Use 125 kHz clock
    MOVLW  b'00010010'
    MOVWF  OSCCON

    ; Initialize pins
    CLRF   PORTD
    CLRF   LATD
    BCF    TRISD, LED_1_PIN ; Set LED_1_PIN as output
    BCF    TRISD, LED_2_PIN ; Set LED_2_PIN as output
    BSF    TRISD, SW_PIN    ; Set SW_PIN    as input

    BCF    LATD, LED_1_PIN
    BSF    LATD, LED_2_PIN

MAIN_LOOP:
    ; Delay and check the switch
    CALL   DELAY_AND_CHECK_SWITCH, 1

    ; Toggle the LEDs
    BTG    LATD, LED_1_PIN
    BTG    LATD, LED_2_PIN

    ; Loop back to the beginning
    GOTO   MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC  PORTD, SW_PIN
    GOTO   DELAY_SLOW
    MOVLW  BLINK_DELAY_FAST
    MOVWF  CheckSwitchCounter, A
    GOTO   CHECK_SWITCH_LOOP
DELAY_SLOW:
    MOVLW  BLINK_DELAY_SLOW
    MOVWF  CheckSwitchCounter, A
    ; Loop
CHECK_SWITCH_LOOP:
    CALL   DELAY, 1
    DECFSZ CheckSwitchCounter, F, A
    GOTO   CHECK_SWITCH_LOOP
    ; Done
    RETURN

    ; Delay
DELAY:
    MOVLW  0xFF
    MOVWF  DelayCounter, A
DELAY_LOOP:
    CLRWDT
    DECFSZ DelayCounter, F, A
    GOTO   DELAY_LOOP
    RETURN

    ; ----------------------------------------------------------------------------------------------------

    END
