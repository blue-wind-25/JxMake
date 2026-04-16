    ; clear && make clean && make && make clean

    LIST  P=PIC16F73
    RADIX DEC

    #include p16f73.inc

    ERRORLEVEL 0, -1302

    #define LED_1_PIN RC4 ; External LED
    #define LED_2_PIN RC3 ; External LED
    #define SW_PIN    RC2 ; External SW

    ; ----------------------------------------------------------------------------------------------------

    __CONFIG _BODEN_OFF & _CP_OFF & _PWRTE_OFF & _WDTE_OFF & _RC_OSC

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x20
    #define DelayCounter1      0x21
    #define DelayCounter2      0x22

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x00
    GOTO START

    ORG 0x04
    RETFIE

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Initialize pins
    BANKSEL TRISC       ; Select bank 1
    MOVLW   b'11100111' ; Set LED_1_PIN as output ; LED_2_PIN as output ; SW_PIN as input
    MOVWF   TRISC
    BANKSEL PORTC       ; Select bank 0

MAIN_LOOP:
    ; Turn on LED1 and turn off LED2
    BSF     PORTC, LED_1_PIN
    BCF     PORTC, LED_2_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Turn off LED1 and turn on  LED2
    BCF     PORTC, LED_1_PIN
    BSF     PORTC, LED_2_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC   PORTC, SW_PIN
    GOTO    DELAY_SLOW
    MOVLW   BLINK_DELAY_FAST
    MOVWF   CheckSwitchCounter
    GOTO    CHECK_SWITCH_LOOP
DELAY_SLOW:
    MOVLW   BLINK_DELAY_SLOW
    MOVWF   CheckSwitchCounter
    ; Loop
CHECK_SWITCH_LOOP:
    CALL    DELAY
    DECFSZ  CheckSwitchCounter, F
    GOTO    CHECK_SWITCH_LOOP
    ; Done
    RETURN

    ; Delay
DELAY:
    MOVLW   0x12
    MOVWF   DelayCounter1
DELAY_LOOP_1:
    MOVLW   0xFF
    MOVWF   DelayCounter2
DELAY_LOOP_2:
    CLRWDT
    DECFSZ  DelayCounter2, F
    GOTO    DELAY_LOOP_2
    DECFSZ  DelayCounter1, F
    GOTO    DELAY_LOOP_1
    RETURN

    ; ----------------------------------------------------------------------------------------------------

    END
