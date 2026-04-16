/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>

#include "SS_USRT_Support.h"
#include "../Utils.h"


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
	if(_hwuxrtInvertSS) { IO_SET_PDNVAL0(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT); }
	else                { IO_SET_PUPVAL1(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT); }

	// Set GPIO direction to output
	IO_SETMODE_OUT_LLT(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT, HW_UXRT_NSS_LLT);

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
	IO_MODINP_NPXX_LLT(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT, HW_UXRT_NSS_LLT);

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
	if(_hwuxrtInvertSS) IO_SET_VALUE_1(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT);
	else                IO_SET_VALUE_0(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT);

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
	if(_hwuxrtInvertSS) IO_SET_VALUE_0(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT);
	else                IO_SET_VALUE_1(HW_UXRT_NSS_PORT, HW_UXRT_NSS_BIT);

	// Clear flag
	_hwuxrtSelected = false;

	// Done
	return true;
}


bool hwuxrt_ss_isSlaveSelected(void)
{ return _hwuxrtSelected; }
