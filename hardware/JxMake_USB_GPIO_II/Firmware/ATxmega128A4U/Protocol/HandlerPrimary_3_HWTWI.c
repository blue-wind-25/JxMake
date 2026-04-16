/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_HW_TWI_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_ENABLE, "CMD_HW_TWI_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_TWI_ENABLE);
	if( hwtwi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_HW_TWI_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-TWI is already enabled");
	}
	else {
		const uint32_t sclFreq = ( ( (uint32_t) _abuffData[0] ) << 16 )
		                       | ( ( (uint32_t) _abuffData[1] ) <<  8 )
		                       | ( ( (uint32_t) _abuffData[2] ) <<  0 );
		const uint8_t timeout  = (_abuffData[3] >> 4) & 0x0F;
		const bool    enExtPU  = _abuffData[3] & 0x01;
		__DPRINTFS_I__("%S %lu %d %d", SPGM_CMD_HW_TWI_ENABLE, sclFreq, timeout, enExtPU);
		if( !hwtwi_begin(sclFreq, timeout, enExtPU) ) {
			__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_TWI_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to initialize HW-TWI");
		}
	}
	__CASE_END__

case CMD_HW_TWI_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_DISABLE, "CMD_HW_TWI_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_TWI_DISABLE);
	if( !hwtwi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_TWI_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-TWI is not enabled");
	}
	else {
		hwtwi_end();
	}
	__CASE_END__

case CMD_HW_TWI_WRITE: /* FALLTHROUGH */
case CMD_HW_TWI_WRITE_NO_STOP: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_WRITE_NO_STOP, "CMD_HW_TWI_WRITE[_NO_STOP]");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_HW_TWI_WRITE_NO_STOP, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[1] || ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_TWI_WRITE_NO_STOP, _abuffData[1]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI the number of bytes is out of the allowed range");
		}
		else {
			// Save the slave address and number of bytes
			_twiSlaveAddress  = _abuffData[0];
			_dataTransferSize = _abuffData[1];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[1];
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_TWI_WRITE_NO_STOP, _dataTransferSize);
		// Perform TWI send
		if( !hwtwi_write( _twiSlaveAddress, _abuffData, _dataTransferSize, (_cmd != CMD_HW_TWI_WRITE_NO_STOP) ) ) {
			__DPRINTFS_E__("%S - failed: write error", SPGM_CMD_HW_TWI_WRITE_NO_STOP);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI write failed");
		}
	}
	__CASE_END__

case CMD_HW_TWI_READ: /* FALLTHROUGH */
case CMD_HW_TWI_READ_NO_STOP: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_READ_NO_STOP, "CMD_HW_TWI_READ[_NO_STOP]");
	__DPRINTFS_I__("%S [%d]", SPGM_CMD_HW_TWI_READ_NO_STOP, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
	if( !_abuffData[1] || ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
		__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_TWI_READ_NO_STOP, _abuffData[1]);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-TWI the number of bytes is out of the allowed range");
	}
	else {
		// Save the slave address and number of bytes
		_twiSlaveAddress  = _abuffData[0];
		_dataTransferSize = _abuffData[1];
		// Perform TWI read
		if( !hwtwi_read( _twiSlaveAddress, _abuffData, _dataTransferSize, (_cmd != CMD_HW_TWI_READ_NO_STOP) ) ) {
			__DPRINTFS_E__("%S - failed: read error", SPGM_CMD_HW_TWI_READ_NO_STOP);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI read failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__

case CMD_HW_TWI_IS_ENABLED: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_IS_ENABLED, "CMD_HW_TWI_IS_ENABLED");
	if(true) {
		// Send back the response via '_abuffData' and '_wbuffLenArg'
		__DPRINTFS_I__( "%S %d", SPGM_CMD_HW_TWI_IS_ENABLED,  hwtwi_isEnabled() & 0xFF );
		_abuffData[0] = hwtwi_isEnabled() ? 1 : 0;
		_wbuffLenArg  = 1; // The result is always 1 byte
	}
	__CASE_END__

case CMD_HW_TWI_SCAN: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_SCAN, "CMD_HW_TWI_SCAN");
	if(true) {
		// Perform scanning
		if( !hwtwi_scan(_abuffData) ) {
			__DPRINTFS_E__("%S - failed: scanning error", SPGM_CMD_HW_TWI_SCAN);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI scanning failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = 128; // The result is always 128 bytes
		}
	}
	__CASE_END__

case CMD_HW_TWI_WRITE_ONE_CF: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_WRITE_ONE_CF, "CMD_HW_TWI_WRITE_ONE_CF");
	if(true) {
		// Save the clock frequency, slave address, and the data
		const uint32_t sclFreq = ( ( (uint32_t) _abuffData[0] ) << 16 )
	                           | ( ( (uint32_t) _abuffData[1] ) <<  8 )
	                           | ( ( (uint32_t) _abuffData[2] ) <<  0 );
		     _twiSlaveAddress  = _abuffData[3];
		     _dataTransferSize = _abuffData[4]; // Use '_dataTransferSize' to store the data
		// Perform one-byte TWI send, possibly using a different clock frequency
		__DPRINTFS_I__("%S %lu %02X %02X", SPGM_CMD_HW_TWI_WRITE_ONE_CF, sclFreq, _twiSlaveAddress, _dataTransferSize);
		if( !hwtwi_write_one_cf(sclFreq, _twiSlaveAddress, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: write error", SPGM_CMD_HW_TWI_WRITE_ONE_CF);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI write one byte (CF) failed");
		}
	}
	__CASE_END__

case CMD_HW_TWI_READ_ONE_CF: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_READ_ONE_CF, "CMD_HW_TWI_READ_ONE_CF");
	if(true) {
		// Save the clock frequency, slave address, and the data
		const uint32_t sclFreq = ( ( (uint32_t) _abuffData[0] ) << 16 )
	                           | ( ( (uint32_t) _abuffData[1] ) <<  8 )
	                           | ( ( (uint32_t) _abuffData[2] ) <<  0 );
		      _twiSlaveAddress = _abuffData[3];
		// Perform one-byte TWI send, possibly using a different clock frequency
		__DPRINTFS_I__("%S %lu %02X", SPGM_CMD_HW_TWI_READ_ONE_CF, sclFreq, _twiSlaveAddress);
		if( !hwtwi_read_one_cf(sclFreq, _twiSlaveAddress, _abuffData) ) {
			__DPRINTFS_E__("%S - failed: read error", SPGM_CMD_HW_TWI_READ_ONE_CF);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-TWI read one byte (CF) failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = 1; // The result is always 1 byte
		}
	}
	__CASE_END__
