/* ATmega32(A) fuse
 *     Low  = 0xBF
 *     High = 0x9E
 *
 * AVR(R) Fuse Calculator
 * https://www.engbedded.com/fusecalc
 *     CKSEL  = 1111 ; SUT      = 11 => external crystal oscillator, high frequency; start-up time = 16CK + 64mS
 *     BODEN  = 0    ; BODLEVEL = 1  => brown-out detection enabled; detection level at VCC = 2.7V
 *     BOOTSZ = 01   ; BOOTRST  = 0  => boot flash section size = 256 words; boot start address = $3F00
 *     SPIEN  = 0                    => serial program downloading (SPI) enabled
 *     JTAGEN = 0                    => JTAG interface enabled
 *
 * avrdude -c usbasp -B 2.6 -p m32 -U lfuse:w:0xBF:m -U hfuse:w:0x9E:m
 * avrdude -c usbasp -B 2.6 -p m32 -U lfuse:r:-:h    -U hfuse:r:-:h
 *
 * avrdude -c usbasp -B 2.6 -p m32 -e -D -V -U flash:w:../test/src/cpp_atmega/MightyCoreBootloaders/optiboot_flash_atmega32_UART0_19200_4000000L_B0.hex:i
 */

// Set PD.6/7 to output
DDRD |= _BV(6);
DDRD |= _BV(7);

// Set and PD.3 to input with pull-up
DDRD  &= ~_BV(3);
PORTD |=  _BV(3);

// Animate PD.6/7 (the LEDs are active low)
fast = !fast;

for(;;) {
    PORTD |=  _BV(6); PORTD &= ~_BV(7); delayAndCheckSwitch();
    PORTD &= ~_BV(6); PORTD |=  _BV(7); delayAndCheckSwitch();
}
