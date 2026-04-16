/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// VBoost ADC pin
#define VBOOST_ADC_DDR DDRA
#define VBOOST_ADC_BIT PA0
#define VBOOST_ADC_DID ADC0D


// Read ADC
static inline uint16_t __adcRead_impl()
{
	ADCSRA |= _BV(ADSC);
	while ( ADCSRA & _BV(ADSC) ) wdt_reset();

	return ADC;
}

static inline uint16_t __adcRead_implM()
{
	const uint8_t repeat = 8;

	uint16_t sum = 0;
	for(uint8_t i = 0; i < repeat; ++i) {
		sum += __adcRead_impl();
		delayMS(1);
	}

	return sum / repeat;
}

static inline uint16_t __adcReadVBoost_impl()
{
	// Read and calculate (ADC0 * 100)
	return ( __adcRead_implM() * 256UL * 16UL ) / 1024UL;
}

static inline uint16_t adcReadVBoost()
{
	__adcRead_implM(); // Ignore the first reading to ensure signal stabilization

	delayMS(10);

	return __adcReadVBoost_impl();
}


// Initialize ADC
static uint16_t g_AVcc       = 0;
static uint16_t g_VBoostIdle = 0;

static inline void adcInit()
{
	// Set the VBoost ADC pin as analog input
	VBOOST_ADC_DDR &= ~_BV(VBOOST_ADC_BIT);
	DIDR0           =  _BV(VBOOST_ADC_DID);

	/*
	 * Configure ADC : Prescaler 128x
	 *
	 * With F_CPU = 11.0592MHz, the ADC clock will be 86.4kHz.
	 */
	ADCSRA = _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0);

	// Select AVcc as the reference voltage and read the internal 1.1V bandgap voltage (VBG)
	ADMUX   = _BV(REFS0) | _BV(MUX4) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);
	ADCSRA |= _BV(ADEN);

	delayMS(10);

	__adcRead_implM(); // Ignore the first reading to ensure signal stabilization

	g_AVcc = (110UL * 1024UL) / __adcRead_implM(); // Read and calculate (AVcc * 100)

	// Select the internal 2.56 V reference voltage and select ADC0 (VBoost / 16) for subsequent reading
	// NOTE : It seems that the internal 1.1V reference voltage is not selectable as ADC reference
	//        voltage on this MCU
	ADCSRA &= ~_BV(ADEN);
	ADMUX   =  _BV(REFS1) | _BV(REFS0);
	ADCSRA |=  _BV(ADEN);

	delayMS(10);

	// Perform the first VBoost readout for validation
	g_VBoostIdle = adcReadVBoost();

	// Print message
	printIMsgDone( PSTR("ADC") );
}
