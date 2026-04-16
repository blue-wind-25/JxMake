    ; clear && make clean && make && make clean

    LIST  P=PIC12F675
    RADIX DEC

    #include p12f675.inc

    ERRORLEVEL 0, -1302

    #define LED_PIN GP2 ; External LED
    #define SW_PIN  GP1 ; External SW

    ; ----------------------------------------------------------------------------------------------------

     __CONFIG _CPD_OFF & _CP_OFF & _BODEN_OFF & _MCLRE_ON & _PWRTE_ON & _WDTE_OFF & _FOSC_INTRCCLK
     __IDLOCS 0x1357

    ; ----------------------------------------------------------------------------------------------------

    ORG __EEPROM_START
    DB "Test EEPROM Data"

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x2D
    #define DelayCounter1      0x2E
    #define DelayCounter2      0x2F

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x00
    GOTO START

    ORG 0x04
    RETFIE

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Calibrate the internal oscillator
    BANKSEL OSCCAL
    CALL    0x03FF
    MOVWF   OSCCAL

    ; Initialize pins
    BANKSEL CMCON
    MOVLW   b'00000111' ; Disconnect the comparator
    MOVWF   CMCON

    BANKSEL TRISIO
    CLRF    ANSEL       ; Digital I/O
    MOVLW   b'11111011' ; Set LED_PIN as output ; SW_PIN as input
    MOVWF   TRISIO
    BANKSEL GPIO

MAIN_LOOP:
    ; Turn on LED
    BSF     GPIO, LED_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Turn off LED
    BCF     GPIO, LED_PIN

    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC   GPIO, SW_PIN
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
