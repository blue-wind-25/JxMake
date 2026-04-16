#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


ARDUINO_CLI_PATH := '/opt/arduino-cli_1.2.2_Linux_64bit'
AVRRDUDE_PATH    := '/opt/avrdude-8.0-usbasp-pdi/bin/avrdude'

SSD1306ASCII_SRC := '../../../../3rd_party/libs/arduino_library/SSD1306Ascii/'
SSD1306ASCII_DST := 'SSD1306Ascii'

BUILD_DIR_NAME   := build
TARGET_FILE_NAME  = $(BUILD_DIR_NAME)/$(SKETCH_FILE_NAME).hex
BUILD_FQBN       := arduino:avr:pro:cpu=8MHzatmega328
BUILD_FLAGS      := --verbose --log-level info --warnings default
UPLOAD_FLAGS     := --verbose --log-level info
UPLOAD_PROG_TYPE := usbasp
UPLOAD_MCU       := m328p
UPLOAD_PROPERTY  := 'program.extra_params=-P usb -B 1.5MHz' # NOTE : See 'usbaspSCKoptions' at 'https://github.com/avrdudes/avrdude/blob/main/src/usbasp.c'


# The default target
$(TARGET_FILE_NAME): SSD1306Ascii_Files = $(shell                                \
	rsync -a --update $(SSD1306ASCII_SRC) $(SSD1306ASCII_DST)                    \
	&&                                                                           \
	find $(SSD1306ASCII_DST) -type f -name '*.h' -o -name '*.c' -o -name '*.cpp' \
)

$(TARGET_FILE_NAME): BUILD_FLAGS += $(if $(EXTRA_C_FLAGS), --build-property compiler.cpp.extra_flags="$(EXTRA_C_FLAGS)")

$(TARGET_FILE_NAME): $(SKETCH_FILE_NAME) $(SSD1306Ascii_Files)
	@if [ -z "$(SSD1306Ascii_Files)" ]; then exit -1; fi
	@$(ARDUINO_CLI_PATH)/arduino-cli compile $(BUILD_FLAGS) --fqbn $(BUILD_FQBN) --output-dir $(BUILD_DIR_NAME)
	@if [ -n "$(RESULT_FILE_NAME)" ]; then cp $(TARGET_FILE_NAME) $(RESULT_FILE_NAME); fi

# Upload
upload: $(TARGET_FILE_NAME)
	@$(ARDUINO_CLI_PATH)/arduino-cli upload $(UPLOAD_FLAGS) --fqbn $(BUILD_FQBN) --input-dir $(BUILD_DIR_NAME) --programmer $(UPLOAD_PROG_TYPE) --upload-property $(UPLOAD_PROPERTY)

# Clean
clean:
	@rm -rvf build
	@rm -rvf $(SSD1306ASCII_DST)


# Target for reading fuses
fuse_read:
	@$(AVRRDUDE_PATH) -c $(UPLOAD_PROG_TYPE) -p $(UPLOAD_MCU) -U lfuse:r:-:h -U hfuse:r:-:h -U efuse:r:-:h

# Target for setting fuses
fuse_x8_bl:
# Standard fuse settings for Arduino Pro Mini (3.3V, 8MHz) or (5V, 16MHz) with ATmega328P
#     Do not divide clock by 8 ; No clock output on PB.0 ; Start-up time 16CK/14CK+65mS ; Low-power crystal oscillator 8MHz+
#     External reset enabled   ; debugWIRE disabled      ; SPI programming enabled      ; Watchdog timer not always on       ; EEPROM not preserved through chip erase ; Boot size 1024 words ; Boot reset vector enabled
#     Brown-out level 2.7V
	@$(AVRRDUDE_PATH) -c $(UPLOAD_PROG_TYPE) -p $(UPLOAD_MCU) -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0x05:m

fuse_x8:
# Modified fuse settings that disable the boot reset vector
#     Do not divide clock by 8 ; No clock output on PB.0 ; Start-up time 16CK/14CK+65mS ; Low-power crystal oscillator 8MHz+
#     External reset enabled   ; debugWIRE disabled      ; SPI programming enabled      ; Watchdog timer not always on       ; EEPROM not preserved through chip erase ; Boot size 1024 words ; Boot reset vector disabled
#     Brown-out level 2.7V
	@$(AVRRDUDE_PATH) -c $(UPLOAD_PROG_TYPE) -p $(UPLOAD_MCU) -U lfuse:w:0xFF:m -U hfuse:w:0xDB:m -U efuse:w:0x05:m

fuse_i8:
# Modified fuse settings that use the internal calibrated 8MHz RC oscillator and disable the boot reset vector
#     Do not divide clock by 8 ; No clock output on PB.0 ; Start-up time  6CK/14CK+65mS ; Internal 8MHz RC oscillator
#     External reset enabled   ; debugWIRE disabled      ; SPI programming enabled      ; Watchdog timer not always on       ; EEPROM not preserved through chip erase ; Boot size 1024 words ; Boot reset vector disabled
#     Brown-out level 2.7V
	@$(AVRRDUDE_PATH) -c $(UPLOAD_PROG_TYPE) -p $(UPLOAD_MCU) -U lfuse:w:0xE2:m -U hfuse:w:0xDB:m -U efuse:w:0x05:m

