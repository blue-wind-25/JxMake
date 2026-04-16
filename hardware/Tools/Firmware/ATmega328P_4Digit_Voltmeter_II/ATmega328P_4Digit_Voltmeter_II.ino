/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Set configuration and include the OLED library files
#define USE_WIRE             0
#define OPTIMIZE_I2C         1

#define AVRI2C_FASTMODE      1

#define INCLUDE_SCROLLING    0
#define ENABLE_NONFONT_SPACE 0

#if USE_WIRE
	#include "SSD1306Ascii/SSD1306AsciiWire.h"
	#define  SSD1306AsciiCLASS SSD1306AsciiWire
#else
	#include "SSD1306Ascii/SSD1306AsciiAvrI2c.h"
	#define  SSD1306AsciiCLASS SSD1306AsciiAvrI2c
#endif
	#include "SSD1306Ascii/SSD1306Ascii.cpp"


// Include the watchdog timer header
#include <avr/wdt.h>


// The OLED module settings
#define I2C_ADDRESS        0x3C


// Firmware version numbers
#define FIRMWARE_VERSION_M 1
#define FIRMWARE_VERSION_N 0
#define FIRMWARE_VERSION_R 0


// The OLED module handle
static uint8_t           xsize = 0;
static uint8_t           yrows = 0;
static SSD1306AsciiCLASS oled;


// Function to handle MCU Status Register and watchdog reset
void preinit() __attribute__((naked)) __attribute__((section(".init3")));
void preinit()
{
    MCUSR = 0;
    wdt_disable();
}


// Read ADC
static uint16_t adcRead()
{
	// Start a conversion
	ADCSRA |= _BV(ADSC);

	// Wait for the conversion to complete (ADSC becomes '0' when finished)
	while( ADCSRA & _BV(ADSC) );

	// Return the ADC result (10-bit resolution)
	return ADC;
}

static void adcMuxSel_ADC0()
{
	// Select internal 1.1V voltage reference and ADC0 single ended input
	ADMUX = _BV(REFS1) | _BV(REFS0);

	// Wait for a while for stabilization
	delay(5);

	// Discard one conversion
	adcRead();
}

static void adcMuxSel_VBG()
{
	// Select AVcc voltage reference and Vbg input
	ADMUX = _BV(REFS0) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);

	// Wait for a while for stabilization
	delay(5);

	// Discard one conversion
	adcRead();
}


// Display 4 digits
static void display4Digits(int16_t value)
{
	/*
	 * 01234567
	 * sab.cd V
	 * ^^^ ^^     (only update these four characters)
	 */

	const uint8_t  w = oled.fontWidth() + + oled.letterSpacing();

	const bool     n = (value < 0);
	const uint16_t v = n ? (-value) : value;

	const char     s = n ? '-' : '+';
	const char     a = '0' + (v / 1000) % 10;
	const char     b = '0' + (v /  100) % 10;
	const char     c = '0' + (v /   10) % 10;
	const char     d = '0' + (v /    1) % 10;

	oled.setCol(w * 0); oled.print(s);
	oled.setCol(w * 1); oled.print(a);
	oled.setCol(w * 2); oled.print(b);
	oled.setCol(w * 4); oled.print(c);
	oled.setCol(w * 5); oled.print(d);
}


// Moving average
#define MA_DEPTH 4

static uint32_t movingAverage(uint32_t buffer[MA_DEPTH], uint32_t newValue)
{
	// Shift-store the values
	for(uint8_t i = 0; i < MA_DEPTH - 1; ++i) {
		buffer[i] = buffer[i + 1];
	}

	buffer[MA_DEPTH - 1] = newValue;

	// Calculate and return the average
	uint32_t sum = 0;

	for(uint8_t i = 0; i < MA_DEPTH; ++i) sum += buffer[i];

	return sum / MA_DEPTH;
}


// The setup function
void setup()
{
	// Initialize indicator LED
	pinMode(LED_BUILTIN, OUTPUT);
	digitalWrite(LED_BUILTIN, LOW);

	// Initialize the OLED display
#if USE_WIRE
	Wire.begin();
	Wire.setClock(AVRI2C_FASTMODE ? 400000L : 100000L);
#endif

	oled.begin(&Adafruit128x64, I2C_ADDRESS);
    // ##### ??? TODO : 'oled.displayRemap(true)' ??? #####
	oled.setContrast(0x3F); // 63

	oled.clear();

	xsize = oled.displayWidth();
	yrows = oled.displayRows ();

	// Print the title and version
	oled.setFont( System5x7                         );

	uint8_t w1 = oled.fontWidth() + oled.letterSpacing();

	oled.setRow ( yrows - 3                         );
	oled.setCol ( xsize - 6 * w1                    );
	oled.print  ( F("JxMake")                       );
	oled.setRow ( yrows - 2                         );
	oled.setCol ( xsize - 20 * w1                   );
	oled.print  ( F("4-Digit Voltmeter II")         );

	oled.setRow ( yrows - 1                         );
	oled.setCol ( xsize - 15 * w1                   );
	oled.print  ( F("Firmware v")                   );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_M) );
	oled.print  (         '.'                       );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_N) );
	oled.print  (         '.'                       );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_R) );

	// Print the inital value (and the '+', '.', and 'V' markers)
	oled.setFont( CalBlk36     );

	uint8_t w2 = oled.fontWidth() + oled.letterSpacing();

	oled.setRow ( 0            );
	oled.setCol ( xsize - w2   );
	oled.print  ( 'V'          );

	oled.setFont( lcdnums14x24 );
	oled.set1X  (              );

	oled.setCol ( 0            );
	oled.print  ( F("+00.00")  );

	// Initialize ADC
	DIDR0  =  _BV(ADC0D);                           // Disable digital input on ADC0 to save power
	ADMUX  =  _BV(REFS1) | _BV(REFS0);              // Select internal 1.1V voltage reference and ADC0 single ended input
	ADCSRA =  _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0); // Set prescaler to 128
	ADCSRA |= _BV(ADEN);                            // Enable ADC

	// Initialize PWM on D9 (PB.1) for the charge pump circuit - use Timer1
	pinMode(9, OUTPUT);

	TCCR1A = _BV(COM1A1) | _BV(WGM11);  // Non-inverting mode - phase correct PWM - no prescaler
	TCCR1B = _BV(WGM13 ) | _BV(CS10 );  // ---
	ICR1   = 499;                       // TOP value for 8kHz
	OCR1A  = 249;                       // 50% duty cycle

	// Initialize watchdog
	wdt_enable(WDTO_500MS);
	wdt_reset();
}


// The loop function
void loop()
{
	// Reset the watchdog timer
	wdt_reset();

	// The module only update up to 10x per seconds, so delay as needed
	static uint32_t prvTick = 0;
	const  uint32_t curTick = millis();

	if(curTick - prvTick < 100) return; // (38.656mS + 38.656mS) = 77.312mS (< 100mS)
	prvTick = curTick;

	// Toggle the indicator LED
	static bool ledOn = true;

	digitalWrite(LED_BUILTIN, ledOn ? HIGH : LOW);
	ledOn = !ledOn;

	// Reset the watchdog timer
	wdt_reset();

	/*
	 * With F_CPU = 8MHz, the ADC clock frequency is 8000000Hz / 128 = 62500Hz.
	 *
	 * Since each ADC conversion requires 13 clock cycles, the ADC can perform a maximum of approximately
	 * 4807 conversions per second. Therefore, accuracy can be improved by supersampling with an amount
	 * much lower than 4807. In this case, a value of 32 is selected, with a delay of 1mS between each reading.
	 */
	#define SUPERSAMPLE_COUNT 32

	// Read the AVcc using Vbg
	uint32_t adcValAcc_AVcc = 0;

	adcMuxSel_VBG();

	for(uint8_t i = 0; i < SUPERSAMPLE_COUNT; ++i) { // (1mS + 0.208mS) * 32 = 38.656mS
		_delay_ms(1);
		adcValAcc_AVcc += adcRead();
	}

	// Reset the watchdog timer
	wdt_reset();

	/*
	 * Calculate the AVcc and Vadd
	 *
	 * The +1.1V Vadd is derived from the +3.3V supply using a voltage divider.
	 *
	 * However, if the +3.3V regulator is bypassed during development and power is supplied via the
	 * AVR-ISP socket or the serial port, the exact supply voltage may vary. Therefore, adjustment
	 * to Vadd is required.
	 */
	const uint32_t avcc     = 1100UL * 1023UL * SUPERSAMPLE_COUNT / adcValAcc_AVcc;
	const uint32_t vadd_new = avcc * 2500UL / 3L / 1100L;

	// Apply moving average filter
	static uint32_t vadd_buff[MA_DEPTH] = { 0 };

	const uint32_t vadd = movingAverage(vadd_buff, vadd_new);

	// Reset the watchdog timer
	wdt_reset();

#if 0
#if 0
	display4Digits(avcc / 10); // Print the AVcc digits
#else

	display4Digits(vadd     ); // Print the Vadd digits
#endif
	// Reset the watchdog timer and exit
	wdt_reset();
	return;
#endif

	/*
	 * Voltage to ADC scaling:
	 *     -25V ... +25V => -1.1V ... +1.1V
	 *                   =>  0.0V ... +1.1V
	 *                   =>  0        1023
	 *
	 * ----------------------------------------------------------------------------------------------------
	 *
	 * The primary scaling factor should universally be 2.27273 because:
	 *     217.36k / 4940k = 0.044
	 *                     ≈ 1 / 22.72727
	 *
	 * Then because:
	 *     Vsum = (Vdiv + 1.1V) / 2
	 * the factor will become ≈ 45.45454
	 *
	 * Finally, because this firmware will display two digits before and after the decimal point,
	 * the read value will be further multiplied by 1100 (instead of 1.1). Therefore the factor will
	 * become 4.54545.
	 *
	 * ----------------------------------------------------------------------------------------------------
	 *
	 * For the calibration process, please read '../../KiCad8/4Digit_Voltmeter_II/NOTES.txt'.
	 */
	#define SCALE_FACTOR 4.54545f

	/*
	 * Vsum subtraction factor
	 */
#ifdef R4_R5
	#define R4 R4_R5
	#define R5 R4_R5
#endif

#ifndef R4
	#define R4 22
#endif

#ifndef R5
	#define R5 R4
#endif

#if ( (R4) != 0 ) && ( (R5) != 0 )
	#define VOFS       ( 207.79616f + ( (R4) * 1000.0f ) ) / ( 207.79616f + ( (R4) * 1000.0f ) + ( (R5) * 1000.0f ) ) * 1.1f - 0.0001f
	#define SUB_FACTOR ( (VOFS) / 0.044f * 2.0f - 25.0f )
#else
	#define SUB_FACTOR 0.0f
#endif

	// Read the ADC0
	uint32_t adcValAcc_ADC0 = 0;

	adcMuxSel_ADC0();

	for(uint8_t i = 0; i < SUPERSAMPLE_COUNT; ++i) { // (1mS + 0.208mS) * 32 = 38.656mS
		_delay_ms(1);
		adcValAcc_ADC0 += adcRead();
	}

	// Reset the watchdog timer
	wdt_reset();

	// Calculate the ADC0 voltage
	const uint32_t scale     = (uint32_t) (SCALE_FACTOR * 1100.0f + 0.5f);
	const uint32_t vsub      = (uint32_t) (SUB_FACTOR   *  100.0f + 0.5f);
	const uint32_t vadc0_new = (adcValAcc_ADC0 * scale) / (1024UL * SUPERSAMPLE_COUNT) - vsub;

	// Apply moving average filter
	static uint32_t vadc0_buff[MA_DEPTH] = { 0 };

	const uint32_t vadc0 = movingAverage(vadc0_buff, vadc0_new);

	// Calculate the voltage
	const int16_t voltage = ( (int16_t) vadc0 ) - ( (int16_t) vadd );

	// Reset the watchdog timer
	wdt_reset();

	// Print the digits
	display4Digits(voltage);

	// Reset the watchdog timer
	wdt_reset();
}
