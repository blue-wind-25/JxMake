/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_HW_SPI_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_ENABLE, "CMD_HW_SPI_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_ENABLE);
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: JTAG is enabled", SPGM_CMD_HW_SPI_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable HW-SPI because JTAG is already enabled");
	}
	else if( hwuxrt_ss_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: BB-USRT is enabled or HW-USRT has enabled SS support", SPGM_CMD_HW_SPI_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable HW-SPI because BB-USRT is already enabled or HW-USRT has enabled slave selection support");
	}
	else if( bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: BB-SWIM is enabled", SPGM_CMD_HW_SPI_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable HW-SPI because BB-SWIM is already enabled");
	}
	else if( hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_HW_SPI_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-SPI is already enabled");
	}
	else {
		const uint8_t spiMode = (_abuffData[0] >> 5) & 0x03;
		const bool    ssMode  = (_abuffData[0] >> 4) & 0x01;
		const uint8_t clkDiv  = (_abuffData[0]     ) & 0x0F;
		__DPRINTFS_I__("%S %d %d %d", SPGM_CMD_HW_SPI_ENABLE, spiMode, ssMode, clkDiv);
		if( !hwspi_begin(spiMode, ssMode, clkDiv) ) {
			__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_SPI_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to initialize HW-SPI");
		}
	}
	__CASE_END__

case CMD_HW_SPI_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_DISABLE, "CMD_HW_SPI_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_DISABLE);
	if( !hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
	}
	else {
		hwspi_end();
	}
	__CASE_END__

case CMD_HW_SPI_SELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SELECT_SLAVE, "CMD_HW_SPI_SELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SELECT_SLAVE);
	if( !hwspi_selectSlave() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_SPI_SELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-SPI failed to select slave");
	}
	__CASE_END__

case CMD_HW_SPI_DESELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_DESELECT_SLAVE, "CMD_HW_SPI_DESELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_DESELECT_SLAVE);
	if( !hwspi_deselectSlave() ) {
		__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_SPI_DESELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("HW-SPI failed to deselect slave");
	}
	__CASE_END__

case CMD_HW_SPI_TRANSFER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_TRANSFER, "CMD_HW_SPI_TRANSFER");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_HW_SPI_TRANSFER, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER, _abuffData[0]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(hwspiNByOutOfRange);
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
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_SPI_TRANSFER, _dataTransferSize);
		// Perform SPI transfer
		if( !hwspi_transfer(_abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_HW_SPI_TRANSFER);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(hwspiTransferFaild);
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize;
		}
	}
	__CASE_END__

case CMD_HW_SPI_TRANSFER_W16ND_R16DN: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, "CMD_HW_SPI_TRANSFER_W16ND_R16DN");
	if(!_wSize && !_rSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%d] [%d]", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[1], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ||
			( (_SInt_IZArg_t) _abuffData[3] ) > sizeof(_abuffData)
		) {
			if( ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) __DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[1]);
			if( ( (_SInt_IZArg_t) _abuffData[3] ) > sizeof(_abuffData) ) __DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[3]);
#pragma GCC diagnostic pop
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(hwspiNByOutOfRange);
		}
		else {
			// Save the parameters
			_wAfterDelayUs25_wSPIMode = _abuffData[0];
			_wSize                    = _abuffData[1];
			_rInterDelayUs10_rSPIMode = _abuffData[2];
			_rSize                    = _abuffData[3];
			_rDummyValue              = _abuffData[4];
			// Get more parameter byte(s) as needed
			if(_wSize > 0) {
				_cmdPBLTot    = _wSize;
				_cmdPBLIdx    = 0;
				_cmdPBLMillis = millis();
				return;
			}
		}
	}
	if(_wSize || _rSize) {
		__DPRINTFS_I__("%S - PHASE 2 [%d] [%d]", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _wSize, _rSize);
		if( !hwspi_transfer_w16Nd_r16dN(_wAfterDelayUs25_wSPIMode, _wSize, _rInterDelayUs10_rSPIMode, _rSize, _rDummyValue, _abuffData) ) {
			__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(hwspiTransferFaild);
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _rSize;
		}
	}
	__CASE_END__

case CMD_HW_SPI_SET_SCK_FREQUENCY: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY, "CMD_HW_SPI_SET_SCK_FREQUENCY");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
	if( !hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
	}
	else {
		const uint8_t clkDiv = _abuffData[0] & 0x7F;
		__DPRINTFS_I__("%S %d", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY, clkDiv);
		if( !hwspi_setClkDiv(clkDiv) ) {
			__DPRINTFS_E__("%S - failed: set clock divider error", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to set HW-SPI clock divider");
		}
	}
	__CASE_END__

case CMD_HW_SPI_SET_SPI_MODE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_SPI_MODE, "CMD_HW_SPI_SET_SPI_MODE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_SPI_MODE);
	if( !hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_SPI_MODE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
	}
	else {
		const uint8_t spiMode = _abuffData[0] & 0x03;
		__DPRINTFS_I__("%S %d", SPGM_CMD_HW_SPI_SET_SPI_MODE, spiMode);
		if( !hwspi_setSPIMode(spiMode) ) {
			__DPRINTFS_E__("%S - failed: set SPI mode error", SPGM_CMD_HW_SPI_SET_SPI_MODE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to set HW-SPI SPI mode");
		}
	}
	__CASE_END__

case CMD_HW_SPI_SET_CLR_BREAK:  /* FALLTHROUGH */
case CMD_HW_SPI_SET_CLR_BREAK_EXT: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_CLR_BREAK, "CMD_HW_SPI_SET_CLR_BREAK[_EXT]");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
	if( !hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
	}
	else {
		const uint8_t b = _abuffData[0] & 0x80;
		__DPRINTFS_I__("%S %08X", SPGM_CMD_HW_SPI_SET_CLR_BREAK, _abuffData[0]);
		if(b) {
			const uint8_t o = _abuffData[0] & 0x02;
			const uint8_t c = _abuffData[0] & 0x01;
			if( _cmd == CMD_HW_SPI_SET_CLR_BREAK_EXT ) {
				const int8_t r = hwspi_setBreakExt(o, c);
				if(r < 0) {
					__DPRINTFS_E__("%S - failed: set break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("failed to set HW-SPI break");
				}
				_wbuffData[0] = r;
				_wbuffLen     = 1;
			}
			else {
				if( !hwspi_setBreak(o, c) ) {
					__DPRINTFS_E__("%S - failed: set break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("failed to set HW-SPI break");
				}
			}
		}
		else {
			if( !hwspi_clrBreak() ) {
				__DPRINTFS_E__("%S - failed: clear break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FSTR("failed to clear HW-SPI break");
			}
			else if( _cmd == CMD_HW_SPI_SET_CLR_BREAK_EXT ) {
				_wbuffData[0] = 0;
				_wbuffLen     = 1;
			}
		}
	}
	__CASE_END__

case CMD_HW_SPI_XB_TRANSFER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_XB_TRANSFER, "CMD_HW_SPI_XB_TRANSFER");
	if(!_dataTransferSize) {
		__DPRINTFS_I__("%S - PHASE 1 [%02X:%02X] [%d]", SPGM_CMD_HW_SPI_XB_TRANSFER, _abuffData[0], _abuffData[1], _abuffData[2]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
		if( !_abuffData[2] || ( (_SInt_IZArg_t) (_abuffData[2] * 2) ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
			__DPRINTFS_E__("%S - failed: the number of pairs (%d) is out of range", SPGM_CMD_HW_SPI_XB_TRANSFER, _abuffData[2]);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-SPI the number of pairs is out of the allowed range");
		}
		else {
			// Save the parameters
			_hwspi_xbt_ioocc_eooc = _abuffData[0];
			_hwspi_xbt_vvvvvvvv   = _abuffData[1];
			_dataTransferSize     = _abuffData[2];
			// Get more parameter byte(s)
			_cmdPBLTot    = _abuffData[2] * 2;
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			return;
		}
	}
	else {
		__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_SPI_XB_TRANSFER, _dataTransferSize);
		// Perform SPI transfer
		if( !hwspi_xb_transfer(_hwspi_xbt_ioocc_eooc, _hwspi_xbt_vvvvvvvv, _abuffData, _dataTransferSize) ) {
			__DPRINTFS_E__("%S - failed: XB transfer error", SPGM_CMD_HW_SPI_XB_TRANSFER);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-SPI XB transfer failed");
		}
		// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
		else {
			_wbuffLenArg = _dataTransferSize * 2;
		}
	}
	__CASE_END__

case CMD_HW_SPI_XB_SPECIAL: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_XB_SPECIAL, "CMD_HW_SPI_XB_SPECIAL");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_XB_SPECIAL);
	if( !hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_XB_SPECIAL);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
	}
	else {
		__DPRINTFS_I__("%S %08X", SPGM_CMD_HW_SPI_XB_SPECIAL, _abuffData[0]);
		const int8_t res = hwspi_xb_special(_abuffData[0]);
		if(res < 0) {
			__DPRINTFS_E__("%S - failed: XB special error", SPGM_CMD_HW_SPI_XB_SPECIAL);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-SPI XB special failed");
		}
		else {
			_wbuffData[0] = res;
			_wbuffLen     = 1;
		}
	}
	__CASE_END__
