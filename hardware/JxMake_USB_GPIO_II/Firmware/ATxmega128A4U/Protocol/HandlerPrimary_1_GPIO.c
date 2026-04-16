/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_HW_GPIO_SET_MODE: __CASE_BEG__
	__DPRINTFS_I__("CMD_HW_GPIO_SET_MODE %02X %02X %02X", _abuffData[0], _abuffData[1], _abuffData[2]);
	hwgpio_setMode(_abuffData[0], _abuffData[1], _abuffData[2]);
	__CASE_END__

case CMD_HW_GPIO_SET_VALUES: __CASE_BEG__
	__DPRINTFS_I__("CMD_HW_GPIO_SET_VALUES %02X %02X", _abuffData[0], _abuffData[1]);
	hwgpio_setValues(_abuffData[0], _abuffData[1]);
	__CASE_END__

case CMD_HW_GPIO_GET_VALUES: __CASE_BEG__
	_wbuffData[0] = hwgpio_getValues();
	_wbuffLen     = 1;
	__DPRINTFS_I__("CMD_HW_GPIO_GET_VALUES %02X", _wbuffData[0]);
	__CASE_END__

case CMD_HW_GPIO_SET_PWM: __CASE_BEG__
	__DPRINTFS_I__("CMD_HW_GPIO_SET_PWM %02X %02X", _abuffData[0], _abuffData[1]);
	hwgpio_setPWM(_abuffData[0], _abuffData[1]);
	__CASE_END__

case CMD_HW_GPIO_GET_ADC: __CASE_BEG__
	if(true) {
		const uint16_t res = hwgpio_getADC(_abuffData[0]);
		__DPRINTFS_I__("CMD_HW_GPIO_GET_ADC %04X", res);
		_wbuffData[0] = (res >> 8) & 0xFF;
		_wbuffData[1] = (res >> 0) & 0xFF;
		_wbuffLen     = 2;
	}
	__CASE_END__

case CMD_HW_GPIO_GET_VREAD: __CASE_BEG__
	if(true) {
		const uint16_t res = hwgpio_getVREAD();
		__DPRINTFS_I__("CMD_HW_GPIO_GET_VREAD %04X", res);
		_wbuffData[0] = (res >> 8) & 0xFF;
		_wbuffData[1] = (res >> 0) & 0xFF;
		_wbuffLen     = 2;
	}
	__CASE_END__

case CMD_HW_GPIO_CALIBRATE_VREAD: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_GPIO_CALIBRATE_VREAD, "CMD_HW_GPIO_CALIBRATE_VREAD");
	if(true) {
		const uint16_t expectedValue = ( ( (uint16_t) _abuffData[0] ) << 8 )
		                             | ( ( (uint16_t) _abuffData[1] ) << 0 );
		__DPRINTFS_I__("%S %%04X", SPGM_CMD_HW_GPIO_CALIBRATE_VREAD, expectedValue);
		if( !hwgpio_calibrateVREAD(expectedValue) ) {
			__DPRINTFS_E__("%S - failed: calibration error", SPGM_CMD_HW_GPIO_CALIBRATE_VREAD);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to calibrate VREAD");
		}
	}
	__CASE_END__

