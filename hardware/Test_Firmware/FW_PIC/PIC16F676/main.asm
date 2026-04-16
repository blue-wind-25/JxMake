    ; clear && make clean && make && make clean

    LIST  P=PIC16F676
    RADIX DEC

    #include p16f676.inc

    ERRORLEVEL 0, -1302

    #define LED_1_PIN RC3 ; External LED
    #define LED_2_PIN RC5 ; External LED
    #define SW_PIN    RC4 ; External SW

    ; ----------------------------------------------------------------------------------------------------

     __CONFIG _CPD_OFF & _CP_OFF & _BODEN_OFF & _MCLRE_ON  & _PWRTE_ON & _WDTE_OFF & _FOSC_INTRCIO
    ;__CONFIG _CPD_OFF & _CP_OFF & _BODEN_OFF & _MCLRE_OFF & _PWRTE_ON & _WDTE_OFF & _FOSC_INTRCIO ; ##### EXPERIMENT : !WARNING!                     #####
    ;__CONFIG _CPD_ON  & _CP_ON  & _BODEN_OFF & _MCLRE_OFF & _PWRTE_ON & _WDTE_OFF & _FOSC_INTRCIO ; ##### EXPERIMENT : !WARNING! !WARNING! !WARNING! #####
     __IDLOCS 0x1357

    ; ----------------------------------------------------------------------------------------------------

    ORG __EEPROM_START
    DB "Test EEPROM Data"

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
    ; Calibrate the internal oscillator
    BANKSEL OSCCAL
    CALL    0x03FF
    MOVWF   OSCCAL

    ; ##### EXPERIMENT : !WARNING! #####
   ;BANKSEL TRISA       ; Select bank 1
   ;MOVLW   b'11110100' ; Set PGD, PGC, and nMCLR as output (this would need Vpp before Vdd to enter programming mode!)
   ;MOVWF   TRISA

    ; Initialize pins
    BANKSEL TRISC       ; Select bank 1
    CLRF    ANSEL       ; Digital I/O
    MOVLW   b'11010111' ; Set LED_1_PIN as output ; LED_2_PIN as output ; SW_PIN as input
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
    RETURN

    ; ----------------------------------------------------------------------------------------------------

    END
