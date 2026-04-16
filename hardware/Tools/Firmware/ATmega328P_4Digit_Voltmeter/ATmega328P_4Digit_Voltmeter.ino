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
#define FIRMWARE_VERSION_R 5


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


// Display 4 digits
static void display4Digits(uint16_t value)
{
	/*
	 * 0123456
	 * ab.cd V
	 * ^^ ^^     (only update these four digits)
	 */

	const uint8_t w = oled.fontWidth() + + oled.letterSpacing();

	const char    a = '0' + (value / 1000) % 10;
	const char    b = '0' + (value /  100) % 10;
	const char    c = '0' + (value /   10) % 10;
	const char    d = '0' + (value /    1) % 10;

	oled.setCol(w * 0); oled.print(a);
	oled.setCol(w * 1); oled.print(b);
	oled.setCol(w * 3); oled.print(c);
	oled.setCol(w * 4); oled.print(d);
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

	oled.begin(&Adafruit128x32, I2C_ADDRESS);
    // ##### ??? TODO : 'oled.displayRemap(true)' ??? #####
	oled.setContrast(0x3F); // 63

	oled.clear();

	xsize = oled.displayWidth();
	yrows = oled.displayRows ();

	// Print the title and version
	oled.setFont( Wendy3x5                          );

	uint8_t w = oled.fontWidth() + oled.letterSpacing();

	oled.setRow ( yrows - 2                         );
	oled.setCol ( xsize - 24 * w                    );
	oled.print  ( F("JxMake 4-Digit Voltmeter")     );

	oled.setRow ( yrows - 1                         );
	oled.setCol ( xsize - 22 * w                    );
	oled.print  ( F("Firmware Version ")            );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_M) );
	oled.print  (         '.'                       );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_N) );
	oled.print  (         '.'                       );
	oled.print  ( (char) ('0' + FIRMWARE_VERSION_R) );

	// Print the inital value (and the '.' and 'V' markers)
	oled.setFont( System5x7    );
	oled.set2X  (              );

	oled.setRow ( 0            );
	oled.setCol ( 0            );
	oled.print  ( F("00.00 V") );

	// Initialize ADC
	DIDR0  =  _BV(ADC0D);                           // Disable digital input on ADC0 to save power
	ADMUX  =  _BV(REFS1) | _BV(REFS0);              // Select internal 1.1V voltage reference and ADC0 single ended input
	ADCSRA =  _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0); // Set prescaler to 128
	delay(500);                                     // Wait for a while for stabilization
	ADCSRA |= _BV(ADEN);                            // Enable ADC
	delay(500);                                     // Wait for a while for stabilization
	adcRead();                                      // Start the first conversion

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

	if(curTick - prvTick < 100) return; // 77.312mS < 100mS
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
	 * much lower than 4807. In this case, a value of 64 is selected, with a delay of 1mS between each reading.
	 */
	#define SUPERSAMPLE_COUNT 64

	/*
	 * The scaling factor should universally be 2.27273 because:
	 *     220k    / 5000k = 0.044          (when using the original 'JxMake 4-Digit Voltmeter'    module)
	 *                     ≈ 1 / 22.72727
	 *     217.36k / 4940k = 0.044          (when using the newer    'JxMake 4-Digit Voltmeter II' module)
	 *                     ≈ 1 / 22.72727
	 * Now, because this firmware will display two digits before and after the decimal point, the read
	 * value will be further multiplied by 1100 (instead of 1.1). Therefore the factor will become 2.27273.
	 *
	 * For the calibration process, please first read '../../KiCad8/4Digit_Voltmeter/NOTES.txt' and proceed
	 * to the next section of this text only if necessary.
	 *
	 * There might be a special or uncommon situation that prevents proper calibration by simply tuning RV1
	 * to achieve the correct or accurate reading. In this case, calibration must be performed by adjusting
	 * the SCALE_FACTOR in the firmware. Please continue reading the text below.
	 *
	 * ----------------------------------------------------------------------------------------------------
	 *
	 * To adjust the SCALE_FACTOR, follow these steps:
	 *     1. Ensure that RV1 is adjusted according to the instructions outlined in the notes from the
	 *        file referenced above.
	 *     2. Set the SCALE_FACTOR to 1.0f and reupload the rebuilt firmware.
	 *     3. Measure a known voltage (Vref) and record its displayed value (Vdsp).
	 *     4. The SCALE_FACTOR can be calculated by dividing Vref by Vdsp.
	 *     5. Set the SCALE_FACTOR to the calculated value and reupload the rebuilt firmware again.
	 *     6. Measure the known voltage again and fine-tune RV1 for the final adjustment, if necessary.
	 *
	 * Example:
	 *     Vref = 5.346V (manually calculated as the mean of several multimeter readings)
	 *     Vdsp = 2.395V (manually calculated as the mean of several displayed  values  )
	 *
	 *     SCALE_FACTOR = Vref   / Vdsp
	 *                  = 5.346V / 2.395V
	 *                  ≈ 2.23215
	 */
	#define SCALE_FACTOR 2.27273f

	// Read the ADC
	uint32_t adcValAcc = 0;

	for(uint8_t i = 0; i < SUPERSAMPLE_COUNT; ++i) { // (1mS + 0.208mS) * 64 = 77.312mS
		_delay_ms(1);
		adcValAcc += adcRead();
	}

	// Reset the watchdog timer
	wdt_reset();

	// Calculate the voltage
	const uint32_t scale       = (uint32_t) (SCALE_FACTOR * 1100.0f + 0.5f);
	const uint32_t voltage_new = (adcValAcc * scale) / (1024UL * SUPERSAMPLE_COUNT);

	// Apply moving average filter
	static uint32_t voltage_buff[MA_DEPTH] = { 0 };

	const uint32_t voltage = movingAverage(voltage_buff, voltage_new);

	// Reset the watchdog timer
	wdt_reset();

	// Print the digits
	display4Digits(voltage);

	// Reset the watchdog timer
	wdt_reset();
}
