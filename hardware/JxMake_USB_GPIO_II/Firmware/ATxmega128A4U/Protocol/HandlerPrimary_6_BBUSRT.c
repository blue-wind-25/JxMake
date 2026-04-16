/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_BB_USRT_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_ENABLE, "CMD_BB_USRT_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_ENABLE);
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: JTAG is enabled", SPGM_CMD_BB_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-USRT because JTAG is already enabled");
	}
	else if( bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: BB-SWIM is enabled", SPGM_CMD_BB_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-USRT because BB-SWIM is already enabled");
	}
	else if( hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: HW-SPI is enabled", SPGM_CMD_BB_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable BB-USRT because HW-SPI is already enabled");
	}
	else if( bbusrt_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_BB_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT is already enabled");
	}
	else {
		const uint32_t baudrate    = ( ( (uint32_t) _abuffData[0] ) << 16 )
		                           | ( ( (uint32_t) _abuffData[1] ) <<  8 )
		                           | ( ( (uint32_t) _abuffData[2] ) <<  0 );
		const uint8_t  parityMode  = (_abuffData[3] >> 4) & 0x03;
		const uint8_t  numStopBits = (_abuffData[3] >> 3) & 0x01;
		const bool     ssMode      = (_abuffData[3] >> 0) & 0x01;
		__DPRINTFS_I__("%S %lu %d %d %d", SPGM_CMD_BB_USRT_ENABLE, baudrate, parityMode, numStopBits, ssMode);
		if( !bbusrt_begin(parityMode, numStopBits + 1, ssMode, baudrate) ) {
			__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_BB_USRT_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to initialize BB-USRT");
		}
	}
	__CASE_END__

case CMD_BB_USRT_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_DISABLE, "CMD_BB_USRT_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_DISABLE);
	if( !bbusrt_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_BB_USRT_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT is not enabled");
	}
	else {
		bbusrt_end();
	}
	__CASE_END__

case CMD_BB_USRT_SELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_SELECT_SLAVE, "CMD_BB_USRT_SELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_SELECT_SLAVE);
	if( !bbusrt_selectSlave() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_SELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT failed to deselect slave");
	}
	__CASE_END__

case CMD_BB_USRT_DESELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_DESELECT_SLAVE, "CMD_BB_USRT_DESELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_DESELECT_SLAVE);
	if( !bbusrt_deselectSlave() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_DESELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT failed to deselect slave");
	}
	__CASE_END__

case CMD_BB_USRT_PULSE_XCK: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_PULSE_XCK, "CMD_BB_USRT_PULSE_XCK");
	__DPRINTFS_I__("%S %d %d", SPGM_CMD_BB_USRT_PULSE_XCK, _abuffData[0], _abuffData[1]);
	if( !bbusrt_pulseXck(_abuffData[0], _abuffData[1] != 0) ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_PULSE_XCK);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT failed to pulse the serial clock line");
	}
	__CASE_END__

case CMD_BB_USRT_TX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_TX, "CMD_BB_USRT_TX");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_BB_USRT_TX, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_USRT_TX, _abuffData[0]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-USRT the number of bytes is out of the allowed range");
		}
		else {
			// Save the number of bytes
			_dataTransferSize = _abuffData[0];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[0];
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_BB_USRT_TX, _dataTransferSize);
		// Perform USRT send
		if( !bbusrt_tx(_abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: transmit error", SPGM_CMD_BB_USRT_TX);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-USRT transmit failed");
		}
	}
	__CASE_END__

case CMD_BB_USRT_RX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_RX, "CMD_BB_USRT_RX");
	__DPRINTFS_I__("%S [%d]", SPGM_CMD_BB_USRT_RX, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
	if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
		__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_USRT_RX, _abuffData[0]);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("BB-USRT the number of bytes is out of the allowed range");
	}
	else {
		// Save the data transfer size
		_dataTransferSize = _abuffData[0];
		// Perform USRT receive
		if( !bbusrt_rx(_abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: receive error", SPGM_CMD_BB_USRT_RX);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("BB-USRT receive failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__
