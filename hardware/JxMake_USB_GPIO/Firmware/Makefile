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


LUFA_PATH          = ../../../3rd_party/libs/lufa/lufa-LUFA-210130/LUFA/

COMPILER_PATH      = /opt/avr-gcc-8.3.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-9.2.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-10.1.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-11.1.0-x64-linux/bin/
COMPILER_PATH      = /opt/avr-gcc-12.1.0-x64-linux/bin/

AVRDUDE_PATH       = /opt/avrdude-7.3-usbasp-pdi/bin/
AVRDUDE_PATH       = /opt/avrdude-8.0-usbasp-pdi/bin/


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
                     Protocol/HandlerPrimary.c   \
                     Protocol/HandlerSecondary.c


MCU                = atmega32u4
ARCH               = AVR8
BOARD              = USER
F_CPU              = 16000000
F_USB              = $(F_CPU)
OPTIMIZATION       = s
SRC                = $(SOURCES) $(LUFA_SRC_USB) $(LUFA_SRC_USBCLASS_DEVICE) $(LUFA_SRC_TWI)
WARNING_FLAGS      = -Werror -Wall -Wextra -Wno-misleading-indentation -Wno-array-bounds -Wno-unused-parameter
INCLUDE_FLAGS      = -IBoard -IConfig
EXTRA_FLAGS        = -fmerge-constants -fmerge-all-constants -fdata-sections -ffunction-sections -finline-small-functions
ACTIVITY_LED_FLAGS = -DACTIVITY_LED_IO=D,7
DEBUG_STREAM_FLAGS = -DDEBUG_STREAM_USE_AVR_TINY_UART=1 -DENABLE_DEBUG_STREAM=0
AVR_TINYUART_FLAGS = -DAVR_TINYUART_F_CPU=$(F_CPU) -DAVR_TINYUART_BAUD=460800 -DAVR_TINYUART_IO_TX=ACTIVITY_LED_IO
MACROS             = $(ACTIVITY_LED_FLAGS) $(DEBUG_STREAM_FLAGS) $(AVR_TINYUART_FLAGS) -DUSE_LUFA_CONFIG_HEADER
CC_FLAGS           = $(WARNING_FLAGS) $(INCLUDE_FLAGS) $(EXTRA_FLAGS) $(MACROS)
LD_FLAGS           =


AVRDUDE_MCU        = m32u4
AVRDUDE_PROGRAMMER = avr109
AVRDUDE_PORT       = /dev/ttyACM0
AVRDUDE_FLAGS      =
AVRDUDE_MEMORY     = flash


# The default target
build_firmware: all
	@echo > /dev/null
	@cp $(TARGET).hex ../Firmware-JxMakeUSBGPPIO.hex

# Upload
upload: build_firmware
	@python3 ../../1200bps_Touch.py /dev/ttyACM0
	@(                                          \
		export PATH=$(AVRDUDE_PATH):$$PATH; \
		$(MAKE) avrdude;                    \
	)


# Include LUFA-specific DMBS extension modules
DMBS_LUFA_PATH ?= $(LUFA_PATH)/Build/LUFA
include $(DMBS_LUFA_PATH)/lufa-sources.mk
include $(DMBS_LUFA_PATH)/lufa-gcc.mk


# Include common DMBS build system modules
DMBS_PATH ?= $(LUFA_PATH)/Build/DMBS/DMBS
include $(DMBS_PATH)/core.mk
include $(DMBS_PATH)/gcc.mk
include $(DMBS_PATH)/avrdude.mk
