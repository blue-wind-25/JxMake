#include <hardware/adc.h>

#include <pico/time.h>


static bool __checkPicoW_impl__()
{
    /*
     * This code is written based on:
     *     https://github.com/earlephilhower/arduino-pico/issues/849
     */

    const auto fnc25 = gpio_get_function(25);
    const auto dir25 = gpio_get_dir     (25);

    const auto fnc29 = gpio_get_function(29);
    const auto dir29 = gpio_get_dir     (29);

    gpio_init       (25         );
    gpio_set_dir    (25, GPIO_IN);

    adc_init        (           );
    adc_gpio_init   (29         );
    adc_select_input( 3         );

    const auto gpv25 = gpio_get(25);
    const auto adc29 = adc_read(  );

    gpio_set_function(25, fnc25);
    gpio_set_dir     (25, dir25);

    gpio_set_function(29, fnc29);
    gpio_set_dir     (29, dir29);

    return gpv25 || (adc29 < 0x100);
}


extern bool checkPicoW()
{
    sleep_ms(100);

    bool isPicoW = __checkPicoW_impl__();

    for(uint i = 0; i < 3; ++i) {
        sleep_ms(100);
        isPicoW &= __checkPicoW_impl__();
    }

    return isPicoW;
}
