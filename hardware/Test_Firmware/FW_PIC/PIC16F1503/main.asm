    ; clear && make clean && make && make clean

    LIST  P=PIC16F1503
    RADIX DEC

    #include p16f1503.inc

    ERRORLEVEL 0, -1302

    #define LED_1_PIN RC1 ; External LED
    #define LED_2_PIN RC2 ; External LED
    #define SW_PIN    RC3 ; External SW

    ; ----------------------------------------------------------------------------------------------------

    __CONFIG _CONFIG1, _CLKOUTEN_OFF & _BOREN_OFF & _CP_OFF & _MCLRE_ON & _PWRTE_ON & _WDTE_OFF & _FOSC_INTOSC
    __CONFIG _CONFIG2, _LVP_ON & _LPBOR_OFF & _BORV_LO & _STVREN_ON & _WRT_OFF

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
    ; Initialize the internal oscillator (use 250 kHz clock)
    BANKSEL OSCCON
    MOVLW   b'00110010'
    MOVWF   OSCCON

    ; Initialize pins
    BANKSEL TRISC
    MOVLW   b'11111001' ; Set LED_1_PIN as output ; LED_2_PIN as output ; SW_PIN as input
    MOVWF   TRISC

    MOVLW   b'00000000' ; Disable all ADC pins
    BANKSEL ANSELA
    MOVWF   ANSELA
    BANKSEL ANSELC
    MOVWF   ANSELC

MAIN_LOOP:
    ; Turn on LED1 and turn off LED2
    BANKSEL LATC
    BSF     LATC, LED_1_PIN
    BCF     LATC, LED_2_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Turn off LED1 and turn on  LED2
    BANKSEL LATC
    BCF     LATC, LED_1_PIN
    BSF     LATC, LED_2_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BANKSEL PORTC
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
    MOVLW   0x02
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
