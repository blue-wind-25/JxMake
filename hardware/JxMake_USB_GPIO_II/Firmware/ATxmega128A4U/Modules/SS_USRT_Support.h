/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__SS_USRT_SUPPORT_H__
#define __MODULES__SS_USRT_SUPPORT_H__


extern bool hwuxrt_ss_begin(bool ssMode);
extern void hwuxrt_ss_end(void);

extern bool hwuxrt_ss_isEnabled(void);

extern bool hwuxrt_ss_selectSlave(void);
extern bool hwuxrt_ss_deselectSlave(void);

extern bool hwuxrt_ss_isSlaveSelected(void);


#endif // __MODULES__SS_USRT_SUPPORT_H__
