#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


include ../arduino_pro_mini_3v3_8mhz_ssd1306ascii.mk

EXTRA_C_FLAGS    := -DR4_R5=10 # NOTE : Use 10M R4 and R5
EXTRA_C_FLAGS    := -DR4_R5=22 # NOTE : Use 22M R4 and R5
EXTRA_C_FLAGS    := -DR4_R5=0  # NOTE : Use Rb1 ~200k     (the default setting)

SKETCH_FILE_NAME := ATmega328P_4Digit_Voltmeter_II.ino
RESULT_FILE_NAME := ../Firmware-JxMakeVM4_II.hex
