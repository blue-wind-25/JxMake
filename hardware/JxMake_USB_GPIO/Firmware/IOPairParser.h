/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __IO_PAIR_PARSER_H__
#define __IO_PAIR_PARSER_H__


#define IO_PAIR_DDR_GET(REG,BIT) (DDR ## REG)
#define IO_PAIR_DDR_VAL(REG_BIT) IO_PAIR_DDR_GET(REG_BIT)

#define IO_PAIR_PRT_GET(REG,BIT) (PORT ## REG)
#define IO_PAIR_PRT_VAL(REG_BIT) IO_PAIR_PRT_GET(REG_BIT)

#define IO_PAIR_PIN_GET(REG,BIT) (PIN ## REG)
#define IO_PAIR_PIN_VAL(REG_BIT) IO_PAIR_PIN_GET(REG_BIT)

#define IO_PAIR_BIT_GET(REG,BIT) (BIT)
#define IO_PAIR_BIT_VAL(REG_BIT) IO_PAIR_BIT_GET(REG_BIT)


#endif // __IO_PAIR_PARSER_H__
