    ; clear && make clean && make && make clean

    LIST  P=PIC10F320
    RADIX DEC

    #include p10f320.inc

    ERRORLEVEL 0, -1302

    #define LED_PIN RA2 ; External LED
    #define SW_PIN  RA1 ; External SW

    ; ----------------------------------------------------------------------------------------------------

     __CONFIG _WRT_OFF & _BORV_HI & _LPBOR_OFF & _LVP_ON & _CP_OFF & _MCLRE_ON & _PWRTE_OFF & _WDTE_OFF & _BOREN_OFF & _FOSC_INTOSC
     __IDLOCS 0x1357

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x5D
    #define DelayCounter1      0x5E
    #define DelayCounter2      0x5F

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x00

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Select 8MHZ internal oscillator
    MOVLW   b'01100000'
    MOVWF   OSCCON
    CLRF    CLKROE      ; Disable clock output

    ; Initialize pins
    CLRF    ANSELA      ; Digital I/O
    MOVLW   b'11111011' ; Set LED_PIN as output ; SW_PIN as input
    MOVWF   TRISA
    MOVLW   b'00000000' ; Disable all pull-up
    MOVWF   WPUA

MAIN_LOOP:
    ; Turn on LED
    BSF     PORTA, LED_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Turn off LED
    BCF     PORTA, LED_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC   PORTA, SW_PIN
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
    RETLW   0

    ; Delay
DELAY:
    MOVLW   0x24
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
    RETLW   0

    ; ----------------------------------------------------------------------------------------------------

    END
