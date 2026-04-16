/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_JTAG_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_ENABLE, "CMD_JTAG_ENABLE");
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_JTAG_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("JTAG is already enabled");
	}
	else if( hwspi_isEnabled() || hwuxrt_isEnabled() || bbusrt_isEnabled() || bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: HW-SPI/HW-UXRT/BB-USRT/BB-SWIM is enabled", SPGM_CMD_JTAG_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable JTAG because HW-SPI/HW-UXRT/BB-USRT/BB-SWIM is already enabled");
	}
	else {
		__DPRINTFS_I__("%S", SPGM_CMD_JTAG_ENABLE);
		if( !initSecondary(GBCID_JTAG) ) {
			__DPRINTFS_E__("%S - failed: secondary virtual serial port initialization error", SPGM_CMD_JTAG_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(failInitSInterface);
		}
		else {
			const uint8_t clkDiv = _abuffData[0] & 0x7F;
			__DPRINTFS_I__("%S %d", SPGM_CMD_JTAG_ENABLE, clkDiv);
			if( !jtag_begin(clkDiv) ) {
				__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_JTAG_ENABLE);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FSTR("failed to initialize JTAG");
			}
		}
	}
	__CASE_END__

case CMD_JTAG_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_DISABLE, "CMD_JTAG_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_JTAG_DISABLE);
	if( !jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_JTAG_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(jtagIsNotEnabled);
	}
	else {
		jtag_end();
		uninitSecondary(GBCID_JTAG);
	}
	__CASE_END__

case CMD_JTAG_SET_FREQUENCY: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_SET_FREQUENCY, "CMD_JTAG_SET_FREQUENCY");
	__DPRINTFS_I__("%S", SPGM_CMD_JTAG_SET_FREQUENCY);
	if( !jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_JTAG_SET_FREQUENCY);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(jtagIsNotEnabled);
	}
	else {
		const uint8_t clkDiv = _abuffData[0] & 0x7F;
		__DPRINTFS_I__("%S %d", SPGM_CMD_JTAG_SET_FREQUENCY, clkDiv);
		if( !jtag_setClkDiv(clkDiv) ) {
			__DPRINTFS_E__("%S - failed: set clock divider error", SPGM_CMD_JTAG_SET_FREQUENCY);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to set JTAG clock divider");
		}
	}
	__CASE_END__

case CMD_JTAG_SET_RESET: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_SET_RESET, "CMD_JTAG_SET_RESET");
	__DPRINTFS_I__("%S", SPGM_CMD_JTAG_SET_RESET);
	if( !jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_JTAG_SET_RESET);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(jtagIsNotEnabled);
	}
	else {
		const uint8_t r = _abuffData[0] & 0x80;
		const uint8_t t = _abuffData[0] & 0x40;
		const uint8_t i = _abuffData[0] & 0x20;
		__DPRINTFS_I__("%S %d %d %d", SPGM_CMD_JTAG_SET_RESET, r, t, i);
		if( !jtag_setReset(r, t, i) ) {
			__DPRINTFS_E__("%S - failed: set nRST/nTRST/TDI error", SPGM_CMD_JTAG_SET_RESET);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to set JTAG nRST/nTRST/TDI");
		}
	}
	__CASE_END__

case CMD_JTAG_TMS: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_TMS, "CMD_JTAG_TMS");
	__DPRINTFS_I__("%S", SPGM_CMD_JTAG_TMS);
	if( !jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_JTAG_TMS);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(jtagIsNotEnabled);
	}
	else {
		const uint8_t r = _abuffData[0] & 0x80;
		const uint8_t t = _abuffData[0] & 0x40;
		const uint8_t i = _abuffData[0] & 0x20;
		const uint8_t n = _abuffData[0] & 0x07;
		const uint8_t v = _abuffData[1];
		__DPRINTFS_I__("%S %d %d %d | %d [%02X]", SPGM_CMD_JTAG_TMS, r, t, i, n, v);
		if( !jtag_tms(r, t, i, n, v) ) {
			__DPRINTFS_E__("%S - failed: TMS error", SPGM_CMD_JTAG_TMS);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("JTAG TMS failed");
		}
	}
	__CASE_END__

case CMD_JTAG_TRANSFER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_TRANSFER, "CMD_JTAG_TRANSFER");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_JTAG_TRANSFER, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[1] || ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_JTAG_TRANSFER, _abuffData[1]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("JTAG the number of bytes is out of the allowed range");
		}
		else {
			// Save the configuration bits and the number of bytes
			_jtagConfigBits   = _abuffData[0];
			_dataTransferSize = _abuffData[1];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[1];
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_JTAG_TRANSFER, _dataTransferSize);
		// Perform JTAG transfer
		const uint8_t u = _jtagConfigBits & 0x80;
		const uint8_t d = _jtagConfigBits & 0x40;
		const uint8_t i = _jtagConfigBits & 0x20;
		const uint8_t n = _jtagConfigBits & 0x07;
		if( !jtag_transfer(u, d, i, n, _abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_JTAG_TRANSFER);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("JTAG transfer failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__

case CMD_JTAG_XB_TRANSFER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_JTAG_XB_TRANSFER, "CMD_JTAG_XB_TRANSFER");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_JTAG_XB_TRANSFER, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[1] || ( (_SInt_IZArg_t) (_abuffData[1] * 2) ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFS_E__("%S - failed: the number of pairs (%d) is out of range", SPGM_CMD_JTAG_XB_TRANSFER, _abuffData[1]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("JTAG the number of pairs is out of the allowed range");
		}
		else {
			// Save the configuration bits and the number of bytes
			_jtagConfigBits   = _abuffData[0];
			_dataTransferSize = _abuffData[1];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[1] * 2;
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_JTAG_XB_TRANSFER, _dataTransferSize);
		// Perform JTAG transfer
		const uint8_t u = _jtagConfigBits & 0x80;
		const uint8_t d = _jtagConfigBits & 0x40;
		const uint8_t i = _jtagConfigBits & 0x20;
		if( !jtag_xb_transfer(u, d, i, _abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: XB transfer error", SPGM_CMD_JTAG_XB_TRANSFER);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("JTAG XB transfer failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize * 2;
		}
	}
	__CASE_END__
