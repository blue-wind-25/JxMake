/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Print the opening title
static inline void printIMsgTitle()
{
	wdt_reset();
	printf_P( PSTR("\n########################################"    )                                                             );
	printf_P( PSTR("\nJxMake High-Voltage-Attachment Module II"    )                                                             );
	printf_P( PSTR("\nFirmware Version %d.%d.%d"                   ), FIRMWARE_VERSION_M, FIRMWARE_VERSION_N, FIRMWARE_VERSION_R );
	printf_P( PSTR("\n########################################\n\n")                                                             );
	wdt_reset();
}


// Print the 'XXX' initialization complete message
static inline void printIMsgDone(const char* section_P)
{
	wdt_reset();
	printf_P( PSTR("%S initialization completed.\n"), section_P );
	wdt_reset();
}


// Print a newline character
static inline void printIMsgNLC()
{
	wdt_reset();
	putchar('\n');
	wdt_reset();
}


// Print fatal error
static inline void printIMsgFELine(const char* pstrMessage)
{
	wdt_reset();
	printf_P( PSTR("\n!!! FATAL ERROR : %S !!!\n"), pstrMessage );
	wdt_reset();
}


// Print a generic informational message
static inline void printIMsg(const char* format, ...)
{
	va_list args;
	va_start(args, format);

	wdt_reset();
	vfprintf_P(stdout, format, args);
	wdt_reset();

	va_end(args);
}


// Print a generic informational message followed by a newline character
static inline void printIMsgln(const char* format, ...)
{
	va_list args;
	va_start(args, format);

	wdt_reset();
	vfprintf_P(stdout, format, args);
	wdt_reset();

	va_end(args);

	printIMsgNLC();
}


// Print raw informational message
static __force_inline void printIMsgRaw(const char* chars)
{
	wdt_reset();
	fputs(chars, stdout);
	wdt_reset();
}
