#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####

OUT_HEX            = ../../../../src/1-TestData/PTest1B.Blink-ATmega8A.hex

COMPILER_PATH      = /opt/avr-gcc-12.1.0-x64-linux/bin/
AVRDUDE_PATH       = /opt/avrdude-8.0-usbasp-pdi/bin/avrdude

MCU                = atmega8a
F_CPU              = 11059200UL
WARNING_FLAGS      = -Werror -Wall -Wextra -Wno-misleading-indentation -Wno-array-bounds
WARNING_FLAGS     += -Wno-unused-function -Wno-unused-parameter -Wno-unused-variable
EXTRA_FLAGS        = -fmerge-constants -fmerge-all-constants -finline-small-functions
EXTRA_FLAGS       += -fdata-sections -ffunction-sections
SPECIAL_FLAGS      = -Wl,-u,vfprintf -lprintf_min -Wl,--gc-sections -Wl,--relax
MACROS            += -ggdb -Os -mmcu=$(MCU) -DF_CPU=$(F_CPU)
CC_FLAGS           = -std=gnu99 $(WARNING_FLAGS) $(EXTRA_FLAGS) $(SPECIAL_FLAGS) $(MACROS)

AVRDUDE_MCU        = m8a
AVRDUDE_PORT       = /dev/ttyUSB0
AVRDUDE_PROGRAMMER = urclock
AVRDUDE_FLAGS      =


# The default target
STRIPPED_OUT_HEX = $(strip $(OUT_HEX))

main.hex: main.elf
	$(COMPILER_PATH)avr-objcopy -R .eeprom -R .fuse -R .lock -R .signature -O ihex $< $@
	if [ -n "$(STRIPPED_OUT_HEX)" ]; then cp $@ $(STRIPPED_OUT_HEX); fi

main.elf: main.c
	$(COMPILER_PATH)avr-gcc     $(CC_FLAGS) $< -o $@
	$(COMPILER_PATH)avr-objdump -h -d -S -z $@ > $(@:.elf=.lss)
	$(COMPILER_PATH)avr-size $@

main.c: Makefile
	touch $@

# Upload
upload: main.hex
	$(AVRDUDE_PATH) -p $(AVRDUDE_MCU) -P $(AVRDUDE_PORT) -c $(AVRDUDE_PROGRAMMER) -b 115200 -D -U flash:w:$<:i

# Clean
clean:
	@rm -vf main.elf
	@rm -vf main.lss
	@rm -vf main.hex
