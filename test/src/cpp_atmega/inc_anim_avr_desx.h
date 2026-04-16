    // Set OSCHF as main clock source
    // NOTE : This is not actually required on the AVR Dx/Ex/Sx series, but it is retained for clarity
    _PROTECTED_WRITE(CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSCHF_gc);

    while(CLKCTRL.MCLKSTATUS & CLKCTRL_SOSC_bm);

    // Disable the prescaler
    // NOTE : This is not actually required on the AVR Dx/Sx series, but it is retained for clarity
    _PROTECTED_WRITE(CLKCTRL_MCLKCTRLB, 0x00);

    // Change the OSCHF frequency
    #ifdef CLKCTRL_FRQSEL_gm
        #if (F_CPU == 16000000UL)
            _PROTECTED_WRITE(CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_16M_gc);
        #elif (F_CPU == 20000000UL)
            _PROTECTED_WRITE(CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_20M_gc);
        #elif (F_CPU == 24000000UL)
            _PROTECTED_WRITE(CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_24M_gc);
        #else
            #error "unsupported CPU frequency"
        #endif
    #else
        // NOTE : On the AVR Ex series, OSCHF is always 20MHz or 16MHz, depending on the fuse settings
        #pragma message("⚠ OSCHF frequency is not configurable by firmware ⚠")
    #endif

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );

#ifdef ENABLE_AVR_DU_USB
    usb_init();
#endif

    // Initialize IO
    LED_PORT.DIRSET  = LED_PIN;

    SW_PORT .DIRCLR  = SW_PIN;
    SW_PORT .SW_PINC = PORT_PULLUPEN_bm;

#ifdef DAC0
    // Initialize DAC
    PORTD.PIN6CTRL &= ~PORT_ISC_gm;               // Disable digital input buffer
    PORTD.PIN6CTRL |=  PORT_ISC_INPUT_DISABLE_gc; // ---
    PORTD.PIN6CTRL &= ~PORT_PULLUPEN_bm;          // Disable pull-up

    VREF.DAC0REF = VREF_ALWAYSON_bm | VREF_REFSEL_VDD_gc;

    DAC0.CTRLA   = DAC_OUTEN_bm | DAC_ENABLE_bm;
    DAC0.DATA    = dacValue << 6;
#else
    DAC_PORT.DIRSET = DAC_PIN;
#endif

    // Animate the LEDs and check the switch
    for(;;) {
        for(unsigned i = 0; i < 3; ++i) {
            LED_PORT.OUTSET = LED_PIN; delayAndCheckSwitch(delay_s);
            LED_PORT.OUTCLR = LED_PIN; delayAndCheckSwitch(delay_s);
        }
        delay(delay_l);
    }
