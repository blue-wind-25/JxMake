/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_HW_USRT_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_ENABLE, "CMD_HW_USRT_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_ENABLE);
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: JTAG is enabled", SPGM_CMD_HW_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable HW-USRT because JTAG is already enabled");
	}
	else if( hwuxrt_isEnabled() ) {
		if( !hwuxrt_isInSyncMode() ) __DPRINTFS_E__("%S - failed: HW-UART is enabled", SPGM_CMD_HW_USRT_ENABLE);
		else                         __DPRINTFS_E__("%S - failed: already enabled"   , SPGM_CMD_HW_USRT_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = !hwuxrt_isInSyncMode()
		            ? _FSTR("cannot enable HW-USRT because HW-UART is already enabled")
		            : _FSTR("HW-USRT is already enabled");
	}
	else {
		if( !initSecondary(GBCID_HW_USRT) ) {
			__DPRINTFS_E__("%S - failed: secondary virtual serial port initialization error", SPGM_CMD_HW_USRT_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(failInitSInterface);
		}
		else {
			const bool usePDIMode   = (_abuffData[0] & 0x80) != 0;
			const bool ssActiveHigh = (_abuffData[0] & 0x01) != 0;
			__DPRINTFS_I__("%S %d %d", SPGM_CMD_HW_USRT_ENABLE, usePDIMode, ssActiveHigh);
			if( !( usePDIMode ? hwusrt_begin_pdi() : hwusrt_begin() ) ) {
				__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_USRT_ENABLE);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FSTR("failed to initialize HW-USRT");
			}
			_hwusrtSSMode = ssActiveHigh;
		}
	}
	__CASE_END__

case CMD_HW_USRT_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE, "CMD_HW_USRT_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE);
	if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
	}
	else {
		if( hwuxrt_ss_isEnabled() ) hwuxrt_ss_end();
		hwusrt_end();
		uninitSecondary(GBCID_HW_USRT);
	}
	__CASE_END__

case CMD_HW_USRT_SELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_SELECT_SLAVE, "CMD_HW_USRT_SELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_SELECT_SLAVE);
	if( bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: SS is used by BB-SWIM", SPGM_CMD_HW_USRT_SELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
	}
	else if( hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: SS is used by HW-SPI", SPGM_CMD_HW_USRT_SELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
	}
	else {
		if( !hwuxrt_ss_isEnabled() ) {
			if( !hwuxrt_ss_begin(_hwusrtSSMode) ) {
				__DPRINTFS_E__("%S - failed: SS support initialization error", SPGM_CMD_HW_USRT_SELECT_SLAVE);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FPGM(failInitHWUSRT_SS);
			}
		}
		if( !hwuxrt_ss_selectSlave() ) {
			__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_USRT_SELECT_SLAVE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-USRT failed to select slave");
		}
	}
	__CASE_END__

case CMD_HW_USRT_DESELECT_SLAVE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DESELECT_SLAVE, "CMD_HW_USRT_DESELECT_SLAVE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
	if( bbswim_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: SS is used by BB-SWIM", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
	}
	else if( hwspi_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: SS is used by HW-SPI", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
	}
	else {
		if( !hwuxrt_ss_isEnabled() ) {
			if( !hwuxrt_ss_begin(_hwusrtSSMode) ) {
				__DPRINTFS_E__("%S - failed: SS support initialization error", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FPGM(failInitHWUSRT_SS);
			}
		}
		if( !hwuxrt_ss_deselectSlave() ) {
			__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("HW-USRT failed to deselect slave");
		}
	}
	__CASE_END__

case CMD_HW_USRT_ENABLE_TX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_ENABLE_TX, "CMD_HW_USRT_ENABLE_TX");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_ENABLE_TX);
	if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_ENABLE_TX);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
	}
	else {
		hwuxrt_enableTx();
	}
	__CASE_END__

case CMD_HW_USRT_DISABLE_TX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE_TX, "CMD_HW_USRT_DISABLE_TX");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE_TX);
	if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE_TX);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
	}
	else {
		hwuxrt_disableTx();
	}
	__CASE_END__

case CMD_HW_USRT_DISABLE_TX_AFTER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE_TX_AFTER, "CMD_HW_USRT_DISABLE_TX_AFTER");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
	if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
	}
	else if( !secondaryRdBufIsEmpty() ) {
		__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(failRdNESInterface);
	}
	else {
		const uint8_t nb = _abuffData[0] & 0xFF;
		__DPRINTFS_I__("%S %d", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER, nb);
		hwuxrt_disableTxAfter(nb);
	}
	__CASE_END__
