/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Fixed-point 16.10
#define FP_L_BITS 16 // Maximum integer    part = ±32767
#define FP_R_BITS 10 // Minimum fractional part =  0.001

#define FP_LARGE  ( (1L << FP_L_BITS) / 2L - 1L )
#define FP_SCALE  (  1L << FP_R_BITS            )

#define TO_FP(N)  ( (int32_t) ( (N) * FP_SCALE ) )

#define MIN_I     TO_FP(-FP_LARGE) // -32767 => 33553408
#define MAX_I     TO_FP( FP_LARGE) //  32767 => 33553408

#define Kp        TO_FP(0.230f)    // Proportional gain (to prevent overflow due to FP_SCALE * 1023, the maximum value is 2.00195 => 2049)
#define Ki        TO_FP(0.001f)    // Integral     gain (to prevent overflow due to FP_LARGE       , the maximum value is 0.06251 =>   64)
#define Kd        TO_FP(0.007f)    // Derivative   gain (to prevent overflow due to FP_SCALE * 1023, the maximum value is 2.00195 => 2049)

// Wait cycles for stabilizing the voltage
#define BWAIT_DCYCLES 5


// PID variables
static volatile uint8_t  boostWaitDis = BWAIT_DCYCLES; // Wait counter
static volatile uint8_t  boostWaitEna = BWAIT_DCYCLES; // Wait counter
static volatile uint16_t boostVoltage = 0;             // Reference value

static volatile int32_t  prevError    = 0;             // Previous error
static volatile int32_t  integral     = 0;             // Integral term
static volatile int32_t  derivative   = 0;             // Derivative term


// Reset the PID
static __force_inline void boostResetPID()
{
	boostWaitDis = BWAIT_DCYCLES;
	boostWaitEna = BWAIT_DCYCLES;

	prevError    = 0;
	integral     = 0;
	derivative   = 0;
}


// Calculate the boost voltage based on the values of the DIP switch
static inline uint16_t boostGetDIPVoltage()
{
	const uint8_t  dsw = gget_dipAll();
	      uint8_t  cnt = 0;
	      uint16_t sum = 0;

	if( ( dsw & _BV(DIP_BIT_13V) ) == 0 ) { ++cnt; sum += ADC_13V; }
	if( ( dsw & _BV(DIP_BIT_12V) ) == 0 ) { ++cnt; sum += ADC_12V; }
	if( ( dsw & _BV(DIP_BIT_11V) ) == 0 ) { ++cnt; sum += ADC_11V; }
	if( ( dsw & _BV(DIP_BIT_10V) ) == 0 ) { ++cnt; sum += ADC_10V; }
	if( ( dsw & _BV(DIP_BIT_09V) ) == 0 ) { ++cnt; sum += ADC_09V; }
	if( ( dsw & _BV(DIP_BIT_08V) ) == 0 ) { ++cnt; sum += ADC_08V; }

	return (cnt == 0) ? 0 : (sum / cnt);
}


// ADC ISR
ISR(ADC_vect)
{
	// Read the value
	const uint16_t adc = ADC;

#if CALIBRATE_VR

	// ===== VR calibration mode =====

	// Print the value and delay
	printf("%d\n", adc);
	delayMS(1000);

#else

	// ===== Normal operation mode =====

	// Reset watchdog
	wdt_reset();

	// Disable PWM and reset PID if the boosted voltage is zero
	if(boostVoltage == 0) {
		pwmDis();
		boostResetPID();
		gset_nBoostNotReady();
		// ##### ??? TODO : Undervoltage and overvoltage check of VIn ??? #####
	}

	// Wait for a while at the minimum duty cycle
	else if(boostWaitDis > 0) {
		// Wait for a few cycles at minimum duty cycle
		pwmSet(PWM_DUTY_MIN);
		--boostWaitDis;
	}

	// Execute PID control
	else {

		// Calculate error
		const int32_t error = TO_FP( ( (int32_t) boostVoltage ) - ( (int32_t) adc ) );

		integral   += error;
		derivative  = error - prevError;

		     if(integral < MIN_I) integral = MIN_I;
		else if(integral > MAX_I) integral = MAX_I;

		// Compute the terms
		const int32_t P = Kp * error      / FP_SCALE; // Proportional term
		const int32_t I = Ki * integral   / FP_SCALE; // Integral term
		const int32_t D = Kd * derivative / FP_SCALE; // Derivative term

		// Compute the PID output
		const int32_t PID = (P + I + D) / FP_SCALE;

		// Save current error for next iteration
		prevError = error;

		// Adjust the duty cyle PWM
		int16_t duty = ( (int16_t) pwmGet() ) + (int16_t) (PID / 4);

		     if(duty < PWM_DUTY_MIN) duty = PWM_DUTY_MIN;
		else if(duty > PWM_DUTY_MAX) duty = PWM_DUTY_MAX;

		/*
		printf( "%+03d | %03d -> %03d\n",(int16_t) (PID / FP_SCALE / 4), pwmGet(), duty );
		//*/

		pwmSet(duty);

		// Decrement counter and set the signal
		if(boostWaitEna > 0) {
			--boostWaitEna;
			if(boostWaitEna == 0) gset_nBoostReady();
		}

	} // if

#endif

	// Start the next conversion
	ADCSRA |= _BV(ADSC);

	// Reset watchdog
	wdt_reset();
}
