TOOLKIT_DIR       := $$HOME/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin

CC = $(TOOLKIT_DIR)/arm-none-eabi-gcc
SIZE = $(TOOLKIT_DIR)/arm-none-eabi-size
OBJCOPY := $(TOOLKIT_DIR)/arm-none-eabi-objcopy

OPENOCD_DIR := $$HOME/0-JxMake/tools/xpackopenocd

COMMON_FLAGS = -mcpu=cortex-m3 -mthumb -mfloat-abi=soft -g

OPT = -Os

DEFINES = -DSTM32L1XX_MDP -DUSE_STDPERIPH_DRIVER

INCLUDE += -I CMSIS/Device/ST/STM32L1xx/Include -I CMSIS/Include -I./ -I STM32L1xx_StdPeriph_Driver/inc

CFLAGS := $(COMMON_FLAGS) $(INCLUDE) $(DEFINES) $(OPT) -std=gnu11 -Wall -pipe -ffunction-sections -fdata-sections

SUBMAKEFILES := main/main.mk startup_code/startup_code.mk SEGGER/SEGGER.mk STM32L1xx_StdPeriph_Driver/library.mk

BUILD_DIR  := build

TARGET_DIR := .

LIBC_SPECS = --specs=nano.specs
#LIBC_SPECS = --specs=nosys.specs #see newlib documentation if you need more functionality

LDFLAGS = $(COMMON_FLAGS) $(LIBC_SPECS) -T linker_script/STM32_flash.ld -Wl,--gc-sections

TARGET := main.elf

SOURCES := \
    CMSIS/Device/ST/STM32L1xx/Source/Templates/system_stm32l1xx.c

size: main.elf
	$(SIZE) main.elf

main.hex: main.elf
	$(OBJCOPY) -O ihex main.elf main.hex

upload: main.hex
	$(OPENOCD_DIR)/bin/openocd                         \
		-f $(OPENOCD_DIR)/scripts/interface/stlink.cfg \
		-c "transport select hla_swd"                  \
		-c "adapter speed 2000"                        \
		-f $(OPENOCD_DIR)/scripts/target/stm32l1.cfg   \
		-c "program main.hex verify reset exit"        \

cleanall: clean
		rm -f   main.hex
		rm -rvf build
