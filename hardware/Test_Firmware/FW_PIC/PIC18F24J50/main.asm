    ; clear && make clean && make && make clean

    LIST  P=PIC18F24J50
    RADIX DEC

    #include p18f24j50.inc

    #define LED_1_PIN RB5 ; External LED
    #define LED_2_PIN RB4 ; External LED
    #define SW_PIN    RC2 ; External SW

    ; ----------------------------------------------------------------------------------------------------

    CONFIG DEBUG     = OFF
    CONFIG XINST     = OFF
    CONFIG STVREN    = ON
    CONFIG PLLDIV    = 1
    CONFIG WDTEN     = OFF
    CONFIG CP0       = OFF
    CONFIG CPUDIV    = OSC1
    CONFIG IESO      = OFF
    CONFIG FCMEN     = OFF
    CONFIG LPT1OSC   = OFF
    CONFIG T1DIG     = OFF
    CONFIG OSC       = INTOSCO
    CONFIG WDTPS     = 32768
    CONFIG DSWDTPS   = G2
    CONFIG DSWDTEN   = OFF
    CONFIG DSBOREN   = OFF
    CONFIG RTCOSC    = INTOSCREF
    CONFIG DSWDTOSC  = INTOSCREF
    CONFIG MSSP7B_EN = MSK5
    CONFIG IOL1WAY   = OFF
    CONFIG WPCFG     = OFF
    CONFIG WPEND     = PAGE_0
    CONFIG WPFP      = PAGE_0
    CONFIG WPDIS     = OFF

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
    MOVLW   b'00100000'
    MOVWF   OSCCON

    ; Initialize pins
    CLRF    PORTB
    CLRF    PORTC
    CLRF    LATB
    CLRF    LATC

    BCF     UCON, USBEN      ; Disable USB

    MOVLW   b'00011111'      ; Disable all ADC pins
    BANKSEL ANCON1
    MOVWF   ANCON1
    BANKSEL ANCON0
    SETF    ANCON0

    BCF     TRISB, LED_1_PIN ; Set LED_1_PIN as output
    BCF     TRISB, LED_2_PIN ; Set LED_2_PIN as output
    BSF     TRISC, SW_PIN    ; Set SW_PIN    as input

    BCF     LATB, LED_1_PIN
    BSF     LATB, LED_2_PIN

MAIN_LOOP:
    ; Delay and check the switch
    CALL    DELAY_AND_CHECK_SWITCH, 1

    ; Toggle the LEDs
    BTG     LATB, LED_1_PIN
    BTG     LATB, LED_2_PIN

    ; Loop back to the beginning
    GOTO    MAIN_LOOP

    ; ----------------------------------------------------------------------------------------------------

    ; Delay and check the switch
DELAY_AND_CHECK_SWITCH:
    ; Determine the blink speed using the switch
    BTFSC   PORTC, SW_PIN
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
