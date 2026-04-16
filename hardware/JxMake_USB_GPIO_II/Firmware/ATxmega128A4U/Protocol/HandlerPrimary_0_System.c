/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_PING: __CASE_BEG__
	__DPRINTFS_I__("CMD_PING");
	__CASE_END__

case CMD_GET_PROTOCOL_VERSION: __CASE_BEG__
	__DPRINTFS_I__("CMD_GET_PROTOCOL_VERSION");
	_wbuffData[0] = PROTOCOL_VERSION_M;
	_wbuffData[1] = PROTOCOL_VERSION_N;
	_wbuffData[2] = PROTOCOL_VERSION_R;
	_wbuffData[3] = (!!ENABLE_DEBUG_STREAM) ? 3 : 0;
	_wbuffLen     = 4;
	__CASE_END__

case CMD_GET_FIRMWARE_VERSION: __CASE_BEG__
	__DPRINTFS_I__("CMD_GET_FIRMWARE_VERSION");
	_wbuffData[0] = FIRMWARE_VERSION_M;
	_wbuffData[1] = FIRMWARE_VERSION_N;
	_wbuffData[2] = FIRMWARE_VERSION_R;
	_wbuffData[3] = (!!ENABLE_DEBUG_STREAM) ? 3 : 0;
	_wbuffLen     = 4;
	__CASE_END__

case CMD_ENABLE_DEBUG_MESSAGE: __CASE_BEG__
	if(true) {
		const int dmsgTypeMask = _abuffData[0] & 0x0F;
		__DPRINTFS_I__("CMD_ENABLE_DEBUG_MESSAGE %02X", dmsgTypeMask);
		debug_setMsgTypeMask(dmsgTypeMask);
	}
	__CASE_END__

case CMD_RESET: __CASE_BEG__
	__DPRINTFS_I__("CMD_RESET");
	_resetSystem_setMillis = millis() + RESET_DELAY_MS;
	__CASE_END__

case CMD_RESET_TO_BOOTLOADER: __CASE_BEG__
	__DPRINTFS_I__("CMD_RESET_TO_BOOTLOADER");
	_resetToBL_bps1200Millis = millis() + RESET_DELAY_MS;
	__CASE_END__

case CMD_DETECT: __CASE_BEG__
	__DPRINTFS_I__("CMD_DETECT");
	if( !hwuxrt_isEnabled() &&
		_abuffData[0] == 'h' && _abuffData[1] == 'E' && _abuffData[2] == 'L' && _abuffData[3] == 'L' &&
		_abuffData[4] == 'o' && _abuffData[5] == '\n'
	) {
		_wbuffData[ 0] = 'J'; _wbuffData[ 1] = 'x'; _wbuffData[ 2] = 'M'; _wbuffData[ 3] = 'a';
		_wbuffData[ 4] = 'k'; _wbuffData[ 5] = 'e'; _wbuffData[ 6] = ' '; _wbuffData[ 7] = 'U';
		_wbuffData[ 8] = 'S'; _wbuffData[ 9] = 'B'; _wbuffData[10] = '-'; _wbuffData[11] = 'G';
		_wbuffData[12] = 'P'; _wbuffData[13] = 'I'; _wbuffData[14] = 'O'; _wbuffData[15] = ' ';
		_wbuffData[16] = 'M'; _wbuffData[17] = 'o'; _wbuffData[18] = 'd'; _wbuffData[19] = 'u';
		_wbuffData[20] = 'l'; _wbuffData[21] = 'e'; _wbuffData[22] = '\n';
		_wbuffData[23] = _SYSTEM_ID[0]; _wbuffData[24] = _SYSTEM_ID[1];
		_wbuffData[25] = _SYSTEM_ID[2]; _wbuffData[26] = _SYSTEM_ID[3];
		_wbuffData[27] = _SYSTEM_ID[4]; _wbuffData[28] = _SYSTEM_ID[5];
		_wbuffData[29] = _SYSTEM_ID[6]; _wbuffData[30] = _SYSTEM_ID[7];
		_wbuffData[31] = _SYSTEM_ID[8];
		_wbuffLen      = 32;
		_G_CMD_DETECT  = true;
	}
	else {
		_wbuffLen   = -1;
		_ebuffPtr_P =  0;
	}
	__CASE_END__
