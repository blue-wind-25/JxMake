/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>

#include "SS_USRT_Support.h"


// State variables
static bool _hwuxrtSSEnabled = false;
static bool _hwuxrtInvertSS  = false;

static bool _hwuxrtSelected  = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


bool hwuxrt_ss_begin(bool ssMode)
{
	// Simply exit if the SS support is already enabled
	if( hwuxrt_ss_isEnabled() ) return true;

	// Save the SS mode
	_hwuxrtInvertSS = !!ssMode;

	// Ensure the slave is deselected
	if(_hwuxrtInvertSS) HW_UXRT_SS_PORT &= ~_BV(HW_UXRT_SS_BIT);
	else                HW_UXRT_SS_PORT |=  _BV(HW_UXRT_SS_BIT);

	// Set GPIO direction to output
	HW_UXRT_SS_DDR |= _BV(HW_UXRT_SS_BIT);

	// Set flag
	_hwuxrtSSEnabled = true;

	// Done
	return true;
}


void hwuxrt_ss_end(void)
{
	// Simply exit if the SS support is not enabled
	if( !hwuxrt_ss_isEnabled() ) return;

	// Set GPIO direction to input
	HW_UXRT_SS_DDR &= ~_BV(HW_UXRT_SS_BIT);

	// Clear flag
	_hwuxrtSSEnabled = false;
}


bool hwuxrt_ss_isEnabled(void)
{ return _hwuxrtSSEnabled; }


bool hwuxrt_ss_selectSlave(void)
{
	// Error if the SS support is not enabled
	if( !hwuxrt_ss_isEnabled() ) return false;

	/*
	// Error if the SS support is not enabled or already selected
	if( !hwuxrt_ss_isEnabled() || _hwuxrtSelected ) return false;
	*/

	// Select slave
	if(_hwuxrtInvertSS) HW_UXRT_SS_PORT |=  _BV(HW_UXRT_SS_BIT);
	else                HW_UXRT_SS_PORT &= ~_BV(HW_UXRT_SS_BIT);

	// Set flag
	_hwuxrtSelected = true;

	// Done
	return true;
}


bool hwuxrt_ss_deselectSlave(void)
{
	// Error if the SS support is not enabled
	if( !hwuxrt_ss_isEnabled() ) return false;

	/*
	// Error if the SS support is not enabled or not selected
	if( !hwuxrt_ss_isEnabled() || !_hwuxrtSelected ) return false;
	*/

	// Deselect slave
	if(_hwuxrtInvertSS) HW_UXRT_SS_PORT &= ~_BV(HW_UXRT_SS_BIT);
	else                HW_UXRT_SS_PORT |=  _BV(HW_UXRT_SS_BIT);

	// Clear flag
	_hwuxrtSelected = false;

	// Done
	return true;
}


bool hwuxrt_ss_isSlaveSelected(void)
{ return _hwuxrtSelected; }
