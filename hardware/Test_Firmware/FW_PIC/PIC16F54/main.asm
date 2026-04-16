    ; clear && make clean && make && make clean

    LIST  P=PIC16F54
    RADIX DEC

    #include p16f54.inc

    ERRORLEVEL 0, -1302

    #define LED_1_PIN RB1 ; External LED
    #define LED_2_PIN RB3 ; External LED
    #define SW_PIN    RB2 ; External SW

    ; ----------------------------------------------------------------------------------------------------

     __CONFIG _CP_OFF & _WDT_OFF & _OSC_HS

    ; ----------------------------------------------------------------------------------------------------

    #define BLINK_DELAY_SLOW   10
    #define BLINK_DELAY_FAST    3

    #define CheckSwitchCounter 0x1D
    #define DelayCounter1      0x1E
    #define DelayCounter2      0x1F

    ; ----------------------------------------------------------------------------------------------------

    ORG 0x1FF
    GOTO START

    ORG 0x000

    ; ----------------------------------------------------------------------------------------------------

START:
    ; Initialize pins
    MOVLW  b'11110101' ; Set LED_1_PIN as output ; LED_2_PIN as output ; SW_PIN as input
    TRIS   PORTB

MAIN_LOOP:
    ; Turn on LED1 and turn off LED2
    BSF    PORTB, LED_1_PIN
    BCF    PORTB, LED_2_PIN

    ; Delay and check the switch
    CALL   DELAY_AND_CHECK_SWITCH

    ; Turn off LED1 and turn on  LED2
    BCF    PORTB, LED_1_PIN
    BSF    PORTB, LED_2_PIN

    ; Delay and check the switch
    CALL   DELAY_AND_CHECK_SWITCH

    ; Loop back to the beginning
    GOTO   MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC  PORTB, SW_PIN
    GOTO   DELAY_SLOW
    MOVLW  BLINK_DELAY_FAST
    MOVWF  CheckSwitchCounter
    GOTO   CHECK_SWITCH_LOOP
DELAY_SLOW:
    MOVLW  BLINK_DELAY_SLOW
    MOVWF  CheckSwitchCounter
    ; Loop
CHECK_SWITCH_LOOP:
    CALL   DELAY
    DECFSZ CheckSwitchCounter, F
    GOTO   CHECK_SWITCH_LOOP
    ; Done
    RETLW  0

    ; Delay
DELAY:
    MOVLW  0x12
    MOVWF  DelayCounter1
DELAY_LOOP_1:
    MOVLW  0xFF
    MOVWF  DelayCounter2
DELAY_LOOP_2:
    CLRWDT
    DECFSZ DelayCounter2, F
    GOTO   DELAY_LOOP_2
    DECFSZ DelayCounter1, F
    GOTO   DELAY_LOOP_1
    RETLW  0

    ; ----------------------------------------------------------------------------------------------------

    END
