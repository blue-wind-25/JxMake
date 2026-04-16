/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// PWM pin
#define PWM_DDR  DDRB
#define PWM_PORT PORTB
#define PWM_BIT  PB1

// Minimum and maximum PWM duty cycle
#define PWM_DUTY_MIN  13 // ~05%
#define PWM_DUTY_MAX 179 // ~70%


// Initialize PWM
static inline void pwmInit()
{
	// Set the duty cycle to its minimum value
	OCR1A = PWM_DUTY_MIN;

	// Configure Timer1 for non-inverting fast 8-bit PWM mode with the fastest frequency
	TCCR1A = _BV(COM1A1) | _BV(WGM10)            ;
	TCCR1B =               _BV(WGM12) | _BV(CS10);

	// Set PB.1 (OC1A) as output
	PWM_DDR |= _BV(PWM_BIT);
}


// Turn off the PWM
static __force_inline void pwmDis()
{
	// Set the duty cycle to its minimum value
	OCR1A = PWM_DUTY_MIN;

	// Set PB.1 (OC1A) as input
	PWM_DDR  &= ~_BV(PWM_BIT);
	PWM_PORT &= ~_BV(PWM_BIT);
}


// Turn on the PWM
static __force_inline void pwmEna()
{
	// Set PB.1 (OC1A) as output
	PWM_DDR |= _BV(PWM_BIT);
}


// Get the PWM duty cycle
static __force_inline uint8_t pwmGet()
{
	// Get and return the duty cycle
	return OCR1A;
}


// Set the PWM duty cycle
static __force_inline void pwmSet(uint8_t value)
{
	// Set the duty cycle
	OCR1A = value;

	// Ensure that the PWM is enabled
	pwmEna();
}
