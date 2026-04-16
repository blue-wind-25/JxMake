#define JXMAKE_AVR_DX_EX_SX

#if defined(__AVR_AVR128DA28__)

    // The DAC0 output is connected to PD.6

    // The LED is connected to PD.3
    #define LED_PORT PORTD
    #define LED_PIN  PIN3_bm

    // The switch is connected to PD.0
    #define SW_PORT  PORTD
    #define SW_PIN   PIN0_bm
    #define SW_PINC  PIN0CTRL

#else

    // The DAC0 output is connected to PD.6 (DA, DB, DD, EA, and SD)
    #define DAC_PORT PORTD
    #define DAC_PIN  PIN6_bm

    // The LED is connected to PD.5
    #define LED_PORT PORTD
    #define LED_PIN  PIN5_bm

    // The switch is connected to PD.2
    #define SW_PORT  PORTD
    #define SW_PIN   PIN2_bm
    #define SW_PINC  PIN2CTRL

#endif

// DAC values
#ifdef DAC0
static constexpr float    vSupply   = 5.0f; // Assume the supply is 5.0V
static constexpr float    vLED      = 1.8f; // Assume the LED is on at 1.8V

static constexpr uint16_t dacValue0 = (uint16_t) ( (vLED / vSupply) * 1023  + 0.5f );
static constexpr uint16_t dacValue1 = 1023;
static           uint16_t dacValue  = dacValue0;
#endif

static inline bool __readSwitch()
{ return SW_PORT.IN & SW_PIN; }

#ifdef ENABLE_AVR_DU_USB
static void delay(uint16_t mS)
{
    for(uint16_t i = 0; i < (mS / 5); ++i) {
        _delayMS(5);
    }
}
#else
#define delay(mS) _delay_ms(mS)
#endif

static void delayAndCheckSwitch(uint16_t mS)
{
#ifdef DAC0

    // Delay and check the switch
    for(uint16_t i = 0; i < (mS / 5); ++i) {
        // Delay for a while
        _delayMS(5);
        // Change the DAC value
        DAC0.DATA = ( !__readSwitch() ? dacValue1 : dacValue0 ) << 6;
    }

#else

    // Delay and check the switch
    for(uint16_t i = 0; i < (mS / 5); ++i) {
        // Delay for a while
        _delayMS(5);
        // Check if the switch is pressed
        if( !__readSwitch() ) {
            // Debouncing
            _delayMS(100);
            // Turn on the LED
            DAC_PORT.OUTSET = DAC_PIN;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !__readSwitch() ) {
#ifdef ENABLE_AVR_DU_USB
        USBDevice_CDCACMHandler();
#endif
    }

    // Turn off the LED
    DAC_PORT.OUTCLR = DAC_PIN;

#endif
}
