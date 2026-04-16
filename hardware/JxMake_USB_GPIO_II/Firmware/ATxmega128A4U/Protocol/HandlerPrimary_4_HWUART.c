/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


/***** WARNING : This file is meant to be included from large 'switch' block in 'HandlerPrimary.c' *****/


case CMD_HW_UART_ENABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_ENABLE, "CMD_HW_UART_ENABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_ENABLE);
	if( jtag_isEnabled() ) {
		__DPRINTFS_E__("%S - failed: JTAG is enabled", SPGM_CMD_HW_UART_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FSTR("cannot enable HW-UART because JTAG is already enabled");
	}
	else if( hwuxrt_isEnabled() ) {
		if( hwuxrt_isInSyncMode() ) __DPRINTFS_E__("%S - failed: HW-USRT is enabled", SPGM_CMD_HW_UART_ENABLE);
		else                        __DPRINTFS_E__("%S - failed: already enabled"   , SPGM_CMD_HW_UART_ENABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = hwuxrt_isInSyncMode()
		            ? _FSTR("cannot enable HW-UART because HW-USRT is already enabled")
		            : _FSTR("HW-UART is already enabled");
	}
	else {
		if( !initSecondary(GBCID_HW_UART) ) {
			__DPRINTFS_E__("%S - failed: secondary virtual serial port error", SPGM_CMD_HW_UART_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FPGM(failInitSInterface);
		}
		else if( !hwuart_begin() ) {
			__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_UART_ENABLE);
			_wbuffLen   = -1;
			_ebuffPtr_P = _FSTR("failed to initialize HW-UART");
		}
	}
	__CASE_END__

case CMD_HW_UART_DISABLE: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE, "CMD_HW_UART_DISABLE");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE);
	if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
	}
	else {
		hwuart_end();
		uninitSecondary(GBCID_HW_UART);
	}
	__CASE_END__

case CMD_HW_UART_ENABLE_TX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_ENABLE_TX, "CMD_HW_UART_ENABLE_TX");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_ENABLE_TX);
	if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_ENABLE_TX);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
	}
	else {
		hwuxrt_enableTx();
	}
	__CASE_END__

case CMD_HW_UART_DISABLE_TX: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE_TX, "CMD_HW_UART_DISABLE_TX");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE_TX);
	if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE_TX);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
	}
	else {
		hwuxrt_disableTx();
	}
	__CASE_END__

case CMD_HW_UART_DISABLE_TX_AFTER: __CASE_BEG__
	__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE_TX_AFTER, "CMD_HW_UART_DISABLE_TX_AFTER");
	__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
	if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
		__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
	}
	else if( !secondaryRdBufIsEmpty() ) {
		__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
		_wbuffLen   = -1;
		_ebuffPtr_P = _FPGM(failRdNESInterface);
	}
	else {
		const uint8_t nb = _abuffData[0] & 0xFF;
		__DPRINTFS_I__("%S %d", SPGM_CMD_HW_UART_DISABLE_TX_AFTER, nb);
		hwuxrt_disableTxAfter(nb);
	}
	__CASE_END__
