#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


COMPILER_PATH       = /opt/avr-gcc-8.3.0-x64-linux/bin/
COMPILER_PATH       = /opt/avr-gcc-9.2.0-x64-linux/bin/
COMPILER_PATH       = /opt/avr-gcc-10.1.0-x64-linux/bin/
COMPILER_PATH       = /opt/avr-gcc-11.1.0-x64-linux/bin/
COMPILER_PATH       = /opt/avr-gcc-12.1.0-x64-linux/bin/

AVRDUDE_PATH        = /opt/avrdude-7.3-usbasp-pdi/bin/avrdude
AVRDUDE_PATH        = /opt/avrdude-8.0-usbasp-pdi/bin/avrdude

MCU                 = atmega1284p
F_CPU               = 11059200UL
DEBUG_HW_UART_BAUD ?=   115200UL
UPDI_HW_UART_BAUD  ?=    19200UL
WARNING_FLAGS       = -Werror -Wall -Wextra -Wno-misleading-indentation -Wno-array-bounds
WARNING_FLAGS      += -Wno-unused-function -Wno-unused-parameter -Wno-unused-variable
EXTRA_FLAGS         = -fmerge-constants -fmerge-all-constants -finline-small-functions
EXTRA_FLAGS        += -fdata-sections -ffunction-sections
SPECIAL_FLAGS       = -Wl,--gc-sections -Wl,--relax
MACROS             += -ggdb -Os -mmcu=$(MCU) -DF_CPU=$(F_CPU)
MACROS             += -DDEBUG_HW_UART_BAUD=$(DEBUG_HW_UART_BAUD) -DUPDI_HW_UART_BAUD=$(UPDI_HW_UART_BAUD)
CC_FLAGS            = -std=gnu99 $(WARNING_FLAGS) $(EXTRA_FLAGS) $(SPECIAL_FLAGS) $(MACROS)

AVRDUDE_MCU         = m1284p
AVRDUDE_PORT        = /dev/ttyUSB0
AVRDUDE_PROGRAMMER  = urclock
AVRDUDE_BAUDRATE    = 230400
AVRDUDE_FLAGS       =


# The default target
STRIPPED_TF_HEX_FN = $(strip $(TARGET_FIRMWARE_HEX_FILE_NAME))

main.hex: main.elf
	$(COMPILER_PATH)avr-objcopy -R .eeprom -R .fuse -R .lock -R .signature -O ihex $< $@
	if [ -n "$(STRIPPED_TF_HEX_FN)" ]; then cp $@ ../$(STRIPPED_TF_HEX_FN); fi

main.elf: main.c
	$(COMPILER_PATH)avr-gcc     $(CC_FLAGS) $< -o $@
	$(COMPILER_PATH)avr-objdump -h -d -S -z $@ > $(@:.elf=.lss)
	$(COMPILER_PATH)avr-size $@

main.c: Makefile atmega1284p.mk *.h
	touch $@

# Upload
upload: main.hex
	@echo
	$(AVRDUDE_PATH) -p $(AVRDUDE_MCU) -P $(AVRDUDE_PORT) -c $(AVRDUDE_PROGRAMMER) -b $(AVRDUDE_BAUDRATE) -D -U flash:w:$<:i
	@sleep 0.5
	@echo
	@$(MAKE) --no-print-directory info

info:
	$(AVRDUDE_PATH) -p $(AVRDUDE_MCU) -P $(AVRDUDE_PORT) -c $(AVRDUDE_PROGRAMMER) -b $(AVRDUDE_BAUDRATE) -x showall

# Clean
clean:
	@rm -vf main.elf
	@rm -vf main.lss
	@rm -vf main.hex
