# Synchronize the version with the string in 'show_banner()' in 'hid_bootloader_cli.c'
PROGRAM_VERSION   := 1.0.0
COMMON_ARCV_NAME  := hid_bootloader_cli-$(PROGRAM_VERSION)-

COMMON_BUILD_ROOT := build
COMMON_BUILD_ARGS  = -O2 -Wall -o $(OUTPUT_EXE_NAME) hid_bootloader_cli.c
GCC_BUILD_ARGS     = $(COMMON_BUILD_ARGS) -Wno-misleading-indentation -s
CLANG_BUILD_ARGS   = $(COMMON_BUILD_ARGS)

default:
	@echo
	@echo '    lin         :       build the Linux   x86-64 binaries (libusb 1.x)'
	@echo '    xwin        : cross build the Windows x86-64 binary   (HidUsb    ) - needs https://github.com/mxe/mxe'
	@echo '    xwin-libusb : cross build the Windows x86-64 binary   (WinUSB    ) - needs https://github.com/mxe/mxe'
	@echo '    xmac        : cross build the MacOS   x86-64 binary   (IOKit     ) - needs https://github.com/tpoechtrager/osxcross'
	@echo
	@echo '    all         : build             all the above binaries'
	@echo '    tar         : build and archive all the above binaries'
	@echo '    clean       : delete            all the generated files'
	@echo
	@echo '    testlin     : test the Linux binaries'
	@echo

all:
	@$(MAKE) -s clean
	@$(MAKE) -s lin
	@$(MAKE) -s xwin
	@$(MAKE) -s xwin-libusb
	@$(MAKE) -s xmac

tar: all
	@(                                                                     \
		echo                                                         ; \
		cd $(COMMON_BUILD_ROOT)                                      ; \
		tar -cjvpf $(COMMON_ARCV_NAME)linux-x64.tar.bz2   linux*     ; \
		tar -cjvpf $(COMMON_ARCV_NAME)windows-x64.tar.bz2 windows*   ; \
		tar -cjvpf $(COMMON_ARCV_NAME)macos-x64.tar.bz2       macos* ; \
		echo                                                         ; \
	)

clean:
	@$(MAKE) -s lin_clean
	@$(MAKE) -s xwin_clean
	@$(MAKE) -s xwin-libusb_clean
	@$(MAKE) -s xmac_clean
	@rm -vf $(COMMON_BUILD_ROOT)/*.bz2

testlin: lin
	@./$(COMMON_BUILD_ROOT)/linux-x64-static/hid_bootloader_cli -mmcu=at90usb162 at90usb162-test.hex

####################################################################################################

LIN_CC     := gcc
LIN_MKBLOB := mkblob
LIN_STRIP  := strip
LIN_LDD    := ldd

lin lin_clean: OUTPUT_EXE_NAME := hid_bootloader_cli
lin lin_clean: OUTPUT_DIR_NAME := $(COMMON_BUILD_ROOT)/linux-x64

lin_clean:
	@rm -vf $(OUTPUT_DIR_NAME)/$(OUTPUT_EXE_NAME)
	@rm -vf $(OUTPUT_DIR_NAME)-static/$(OUTPUT_EXE_NAME)

lin: lin_clean
	@(                                                                                         \
		unset LD_LIBRARY_PATH                                                            ; \
		$(LIN_CC) $(GCC_BUILD_ARGS) -DUSE_LIBUSB -lusb-1.0                               ; \
		echo                                                                             ; \
		$(LIN_MKBLOB) `pwd`/$(OUTPUT_EXE_NAME) -o $(OUTPUT_EXE_NAME)_static -static -dae ; \
		$(LIN_STRIP) -s $(OUTPUT_EXE_NAME)                                               ; \
		$(LIN_STRIP) -s $(OUTPUT_EXE_NAME)_static                                        ; \
		echo                                                                             ; \
		$(LIN_LDD) $(OUTPUT_EXE_NAME)                                                    ; \
		echo                                                                             ; \
		$(LIN_LDD) $(OUTPUT_EXE_NAME)_static                                             ; \
		mv $(OUTPUT_EXE_NAME)        $(OUTPUT_DIR_NAME)                                  ; \
		mv $(OUTPUT_EXE_NAME)_static $(OUTPUT_DIR_NAME)-static/$(OUTPUT_EXE_NAME)        ; \
		echo                                                                             ; \
	)

####################################################################################################

MXE_ROOT_DIR := /opt/mxe
MXE_CC       := $(MXE_ROOT_DIR)/bin/x86_64-w64-mingw32.static-gcc
MXE_STRIP    := $(MXE_ROOT_DIR)/bin/x86_64-w64-mingw32.static-strip
MXE_OBJDUMP  := $(MXE_ROOT_DIR)/bin/x86_64-w64-mingw32.static-objdump

# This is not a static executable because it needs 'msvcrt.dll', 'HID.DLL', 'KERNEL32.dll', and 'SETUPAPI.dll'
xwin xwin_clean: OUTPUT_EXE_NAME := hid_bootloader_cli.exe
xwin xwin_clean: OUTPUT_DIR_NAME := $(COMMON_BUILD_ROOT)/windows-x64

xwin_clean:
	@rm -vf $(OUTPUT_DIR_NAME)/$(OUTPUT_EXE_NAME)

xwin: xwin_clean
	@(                                                                 \
		unset LD_LIBRARY_PATH                                    ; \
		$(MXE_CC) $(GCC_BUILD_ARGS) -DUSE_WIN32 -lsetupapi -lhid ; \
		$(MXE_STRIP) -s $(OUTPUT_EXE_NAME)                       ; \
		echo                                                     ; \
		$(MXE_OBJDUMP) -x $(OUTPUT_EXE_NAME) | grep 'DLL Name:'  ; \
		mv $(OUTPUT_EXE_NAME) $(OUTPUT_DIR_NAME)                 ; \
		echo                                                     ; \
	)

# This is not a static executable because it needs 'msvcrt.dll' and 'KERNEL32.dll'
xwin-libusb xwin-libusb_clean: OUTPUT_EXE_NAME := hid_bootloader_cli.exe
xwin-libusb xwin-libusb_clean: OUTPUT_DIR_NAME := $(COMMON_BUILD_ROOT)/windows-x64-libusb

xwin-libusb_clean:
	@rm -vf $(OUTPUT_DIR_NAME)/$(OUTPUT_EXE_NAME)

xwin-libusb: xwin-libusb_clean
	@(                                                                \
		unset LD_LIBRARY_PATH                                   ; \
		$(MXE_CC) $(GCC_BUILD_ARGS) -DUSE_LIBUSB -lusb-1.0      ; \
		$(MXE_STRIP) -s $(OUTPUT_EXE_NAME)                      ; \
		echo                                                    ; \
		$(MXE_OBJDUMP) -x $(OUTPUT_EXE_NAME) | grep 'DLL Name:' ; \
		mv $(OUTPUT_EXE_NAME) $(OUTPUT_DIR_NAME)                ; \
		echo                                                    ; \
	)

####################################################################################################

MAC_LD_LIB   := /opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
MAC_ROOT_DIR := /opt/osxcross
MAC_CC       := $(MAC_ROOT_DIR)/bin/x86_64-apple-darwin20.4-cc
MAC_LDD      := $(MAC_ROOT_DIR)/bin/x86_64-apple-darwin20.4-dyldinfo -dylibs
MAC_SDK      := $(MAC_ROOT_DIR)/SDK/MacOSX11.3.sdk

# This is not a static executable because it needs 'CoreFoundation.tbd', 'IOKit.tbd', and 'libSystem.B.dylib'
xmac xmac_clean: OUTPUT_EXE_NAME := hid_bootloader_cli
xmac xmac_clean: OUTPUT_DIR_NAME := $(COMMON_BUILD_ROOT)/macos-x64

xmac_clean:
	@rm -vf $(OUTPUT_DIR_NAME)/$(OUTPUT_EXE_NAME)

xmac: xmac_clean
	@(                                                                                                                                                   \
		export LD_LIBRARY_PATH=$(MAC_LD_LIB)                                                                                                       ; \
		export OSXCROSS_ENABLE_WERROR_IMPLICIT_FUNCTION_DECLARATION=1                                                                              ; \
		$(MAC_CC) $(CLANG_BUILD_ARGS) -DUSE_APPLE_IOKIT -isysroot $(MAC_SDK) -Wl,-syslibroot,$(MAC_SDK) -framework IOKit -framework CoreFoundation ; \
		echo                                                                                                                                       ; \
		$(MAC_LDD) $(OUTPUT_EXE_NAME)                                                                                                              ; \
		mv $(OUTPUT_EXE_NAME) $(OUTPUT_DIR_NAME)                                                                                                   ; \
		echo                                                                                                                                       ; \
	)
