#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####

#
#             LUFA Library
#     Copyright (C) Dean Camera, 2021.
#
#  dean [at] fourwalledcubicle [dot] com
#           www.lufa-lib.org
#
# --------------------------------------
#         LUFA Project Makefile.
# --------------------------------------

# Run "make help" for target help.


LUFA_PATH          = ../../../../3rd_party/libs/lufa/lufa-LUFA-210130/LUFA/

COMPILER_PATH      = /opt/avr-gcc-8.3.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-9.2.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-10.1.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-11.1.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-12.1.0-x64-linux/bin/

AVRDUDE_PATH       = /opt/avrdude-7.3-usbasp-pdi/bin/avrdude
AVRDUDE_PATH       = /opt/avrdude-8.0-usbasp-pdi/bin/avrdude

DFUPROG_PATH       = /opt/dfu-programmer-1.1.0/bin/dfu-programmer


OBJDIR             = std_build
TARGET             = JxMakeUSBGPPIO
SOURCES            = $(TARGET).c                 \
                     Descriptors.c               \
                     Millis.c                    \
                     Utils.c                     \
                     Modules/HW_GPIO.c           \
                     Modules/HW_SPI.c            \
                     Modules/HW_TWI.c            \
                     Modules/SS_USRT_Support.c   \
                     Modules/HW_UART_USRT.c      \
                     Modules/BB_USRT.c           \
                     Modules/BB_SWIM.c           \
                     Modules/JTAG.c              \
                     Protocol/HandlerPrimary.c   \
                     Protocol/HandlerSecondary.c \
                     EEPROM/eeprom_driver.c


MCU                = atxmega128a4u
AVRDUDE_MCU        = x128a4u
ARCH               = XMEGA
BOARD              = USER
F_XTAL             =  8000000
F_CPU              = 32000000
F_USB              = 48000000
# BOOTLOADER_TYPE  = ATMEL_DFU
OPTIMIZATION       = s
SRC                = $(SOURCES) $(LUFA_SRC_USB) $(LUFA_SRC_USBCLASS_DEVICE) $(LUFA_SRC_TWI)
WARNING_FLAGS      = -Werror -Wall -Wextra -Wno-misleading-indentation -Wno-array-bounds -Wno-unused-parameter
# WARNING_FLAGS   += -Wno-unused-function -Wno-unused-variable -Wno-unused-label
INCLUDE_FLAGS      = -IBoard -IConfig
EXTRA_FLAGS        = -fmerge-constants -fmerge-all-constants -fdata-sections -ffunction-sections -finline-small-functions
ACTIVITY_LED_FLAGS = -DSYSTEM_LED_IO=D,5 -DACTIVITY_LED_IO=D,4
DEBUG_STREAM_FLAGS = -DENABLE_DEBUG_STREAM=1 -DDEBUG_UART_BAUD=1000000
MACROS             = -DF_XTAL=$(F_XTAL)UL -DUSE_LUFA_CONFIG_HEADER -DUSE_HEURISTIC_BAUDRATE_CALCULATION
# MACRO           += -DBL_$(BOOTLOADER_TYPE)
MACROS            += -DUSE_USB_INITIALIZATION_DELAY -DUSE_USB_INITIALIZATION_CHECK
CC_FLAGS           = $(WARNING_FLAGS) $(INCLUDE_FLAGS) $(EXTRA_FLAGS) $(ACTIVITY_LED_FLAGS) $(DEBUG_STREAM_FLAGS) $(MACROS)
LD_FLAGS           =


# The default target
build_firmware: all
	@echo > /dev/null
	@cp $(TARGET).hex ../Firmware-JxMakeUSBGPPIO_II.hex

# Upload
dfu: build_firmware
	$(MAKE) t1200bps
	$(MAKE) dfunr

dfunr: build_firmware
	@$(DFUPROG_PATH) $(MCU) flash --erase-first $(TARGET).hex
	@$(DFUPROG_PATH) $(MCU) launch

t1200bps:
	@python3 ../../../1200bps_Touch.py /dev/ttyACM0


# Include LUFA-specific DMBS extension modules
DMBS_LUFA_PATH ?= $(LUFA_PATH)/Build/LUFA
include $(DMBS_LUFA_PATH)/lufa-sources.mk
include $(DMBS_LUFA_PATH)/lufa-gcc.mk


# Include common DMBS build system modules
DMBS_PATH ?= $(LUFA_PATH)/Build/DMBS/DMBS
include $(DMBS_PATH)/core.mk
include $(DMBS_PATH)/gcc.mk
