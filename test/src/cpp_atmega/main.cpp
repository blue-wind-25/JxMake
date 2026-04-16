#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>

#include <util/delay.h>


#if defined(__AVR_AVR32DU28__)
    //*
    #define ENABLE_AVR_DU_USB
    //*/
#endif

#ifdef ENABLE_AVR_DU_USB
// Forward declaration
static void USBDevice_CDCACMHandler();
#endif


// From 'halt.S'
extern "C" void halt();

// From 'nop.S'
extern "C" void nop();


#ifdef ENABLE_AVR_DU_USB
    static void delayMS(uint32_t mS);
    #define _delayMS  delayMS
#else
    #define _delayMS _delay_ms
#endif


#if defined(__AVR_ATmega32__) || defined(__AVR_ATmega32A__)
    #include "inc_switch_atmega32.h"
#else

static constexpr unsigned delay_s = 150;
static constexpr unsigned delay_l = 550;

#if defined(__AVR_AVR128DA28__) || defined(__AVR_AVR32DA28__) || defined(__AVR_AVR32DB28__) || defined(__AVR_AVR32DD28__) || defined(__AVR_AVR32DU28__) || defined(__AVR_AVR32EA28__) || defined(__AVR_AVR32EB28__) || defined(__AVR_AVR32SD28__)
    #include "inc_switch_avr_desx.h"
#endif

#endif


#ifdef __AVR_AVR32SD28__
    #include "inc_errctrl_avr_sx.h"
#endif

#ifdef ENABLE_AVR_DU_USB
    #include "inc_usb_avr_du.h"
#endif


int main()
{
    nop();

#ifdef ENABLE_AVR_DU_USB
    usb_init();
#endif

#if defined(__AVR_ATmega32__) || defined(__AVR_ATmega32A__)
    #include "inc_anim_atmega32.h"
#elif defined(__AVR_ATmega328P__) || defined(__AVR_AT90USB162__) || defined(__AVR_AT90USB1286__)
    #include "inc_anim_atmega328_at90usb.h"
#elif defined(__AVR_ATmega32U4__)
    #include "inc_anim_atmega32u4.h"
#elif defined(__AVR_ATmega2560__)
    #include "inc_anim_atmega2560.h"
#elif defined(__AVR_ATmega4808__) || defined(__AVR_ATmega4809__)
    #include "inc_anim_atmega480x.h"
#elif defined(JXMAKE_AVR_DX_EX_SX)
    #include "inc_anim_avr_desx.h"
#else
    #error "unsupported MCU"
#endif

    halt();

    return 0;
}
