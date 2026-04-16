/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_BB_SWIM_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_ENABLE, "CMD_BB_SWIM_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_ENABLE);
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: JTAG is enabled", SPGM_CMD_BB_SWIM_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-SWIM because JTAG is already enabled");
	}
	else if( hwuxrt_ss_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: BB-USRT is enabled or HW-USRT has enabled SS support", SPGM_CMD_BB_SWIM_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-SWIM because BB-USRT is already enabled or HW-USRT has enabled slave selection support");
	}
	else if( hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: HW-SPI is enabled", SPGM_CMD_BB_SWIM_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-SWIM because HW-SPI is already enabled");
	}
	else if( bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_BB_SWIM_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-SWIM is already enabled");
	}
	else {
		__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_ENABLE);
		if( !bbswim_begin() ) {
			__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_BB_SWIM_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to initialize BB-SWIM");
		}
	}
	__CASE_END__

case CMD_BB_SWIM_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_DISABLE, "CMD_BB_SWIM_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_DISABLE);
	if( !bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_BB_SWIM_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(bbswimIsNotEnabled);
	}
	else {
		bbswim_end();
	}
	__CASE_END__

case CMD_BB_SWIM_LINE_RESET: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_LINE_RESET, "CMD_BB_SWIM_LINE_RESET");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_LINE_RESET);
	if( !bbswim_lineReset() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_SWIM_LINE_RESET);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-SWIM failed to line reset");
	}
	__CASE_END__

/*
case CMD_BB_SWIM_TRANSFER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_TRANSFER, "CMD_BB_SWIM_TRANSFER");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d] [%d]", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[0], _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFS_E__("%S - failed: the number of bits (%d) is out of range", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[0]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM the number of bits is out of the allowed range");
		}
		else if(_abuffData[1] >= _abuffData[0]) {
			__DPRINTFS_E__("%S - failed: the number of 2X bits (%d) is out of range", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[1]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM the number of 2X bits is out of the allowed range");
		}
		else {
			// Save the number of bits
			_dataTransferSize  = _abuffData[0];
			_dataTransferSize2 = _abuffData[1];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[0];
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d] [%d]", SPGM_CMD_BB_SWIM_TRANSFER, _dataTransferSize, _dataTransferSize2);
		// Perform SWIM transfer
		if( !bbswim_transfer(_abuffData, _dataTransferSize, _dataTransferSize2) ) {
			__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_BB_SWIM_TRANSFER);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM transfer failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__
*/

case CMD_BB_SWIM_SRST: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_SRST, "CMD_BB_SWIM_SRST");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_SRST);
	if( !bbswim_cmd_srst() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_SWIM_SRST);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-SWIM command SRST failed");
	}
	__CASE_END__

case CMD_BB_SWIM_ROTF: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_ROTF, "CMD_BB_SWIM_ROTF");
	__DPRINTFS_I__("%S [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_ROTF, _abuffData[0], _abuffData[1], _abuffData[2], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
		__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_SWIM_ROTF, _abuffData[0]);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-SWIM the number of bytes is out of the allowed range");
	}
	else {
		// Save the address and number of bytes
		_dataTransferSize = _abuffData[0];
		_swimEAddress     = _abuffData[1];
		_swimHAddress     = _abuffData[2];
		_swimLAddress     = _abuffData[3];
		// Perform SWIM command ROTF
		if( !bbswim_cmd_rotf(_abuffData, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress) ) {
			__DPRINTFS_E__("%S - failed: receive error", SPGM_CMD_BB_SWIM_ROTF);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM command ROTF failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__

case CMD_BB_SWIM_WOTF: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_WOTF, "CMD_BB_SWIM_WOTF");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_WOTF, _abuffData[0], _abuffData[1], _abuffData[2], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_SWIM_WOTF, _abuffData[0]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM the number of bytes is out of the allowed range");
		}
		else {
			// Save the address and number of bytes
			_dataTransferSize = _abuffData[0];
			_swimEAddress     = _abuffData[1];
			_swimHAddress     = _abuffData[2];
			_swimLAddress     = _abuffData[3];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[0];
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_WOTF, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress);
		// Perform SWIM command WOTF
		if( !bbswim_cmd_wotf(_abuffData, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress) ) {
			__DPRINTFS_E__("%S - failed: write error", SPGM_CMD_BB_SWIM_WOTF);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-SWIM command WOTF failed");
		}
	}
	__CASE_END__
