/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


static __force_inline void printIMsg1(const char* section_P)
{
	printf_P( PSTR("\n###################################################\n")                                                             );
	printf_P( PSTR("JxMake High-Voltage-Attachment Module\n"                )                                                             );
	printf_P( PSTR("%S Section\n"                                           ), section_P                                                  );
	printf_P( PSTR("Firmware Version %d.%d.%d\n"                            ), FIRMWARE_VERSION_M, FIRMWARE_VERSION_N, FIRMWARE_VERSION_R );
}


static __force_inline void printIMsg2(const char* mode_P)
{
	printf_P( PSTR("#############%S#############\n\n"), mode_P ? mode_P : PSTR(" NORMAL OPERATIONAL MODE ") );
	delayMS(500);
}
