/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Attributes
#define ATTR_USED            __attribute__ ((used))
#define ATTR_UNUSED          __attribute__ ((unused))
#define ATTR_NO_RETURN       __attribute__ ((noreturn))
#define ATTR_NO_INLINE       __attribute__ ((noinline))
#define ATTR_ALWAYS_INLINE   __attribute__ ((always_inline))
#define ATTR_NO_INIT         __attribute__ ((section (".noinit")))
#define ATTR_ALIAS(F)        __attribute__ ((alias(#F)))
#define ATTR_INIT_SECTION(S) __attribute__ ((used, naked, section (".init" #S)))

#define __force_inline inline ATTR_ALWAYS_INLINE
#define __never_inline        ATTR_NO_INLINE
