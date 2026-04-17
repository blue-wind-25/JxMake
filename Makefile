#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


##### ANSI escape codes for colors
C_GRAY    = "\\033[90m"
C_RED     = "\\033[91m"
C_GREEN   = "\\033[92m"
C_YELLOW  = "\\033[93m"
C_BLUE    = "\\033[94m"
C_MAGENTA = "\\033[95m"
C_CYAN    = "\\033[96m"
C_WHITE   = "\\033[97m"
C_RESET   = "\\033[0m"


##### JxMake source directory
JXM_SRC_DIR = src


##### The default target
default:
	@echo
	@echo -e  "$(C_WHITE)Helper Targets$(C_RESET)"
	@echo
	@echo -e  "    $(C_GREEN)clean sdiff arcv$(C_RESET)"
	@echo


##### Restore KiCad8 'fp-info-cache' files
kic:
	@$(MAKE) -C $(JXM_SRC_DIR) -s kicad8_fp-info-cache

##### Invoke MeshLab to display KiCad 3D rendering
ML_BIN := /usr/local/bin/MeshLab2020.03-linux.AppImage

ml_338:
	@$(ML_BIN) hardware/Experiment/KiCad8/LM338/338.wrl

ml_a28:
	@$(ML_BIN) hardware/Experiment/KiCad8/AVR_DxExSx_SSOP28/A28.wrl

ml_vmp2:
	@$(ML_BIN) hardware/JxMake_USB_GPIO_II/KiCad8/Versatile_MCU_Programmer_II/VMP.wrl

ml_hva2:
	@$(ML_BIN) hardware/JxMake_USB_GPIO_II/KiCad8/High_Voltage_Attachment_II/HVA.wrl

ml_abc:
	@$(ML_BIN) hardware/JxMake_USB_GPIO_II/KiCad8/HVA_II_Alternative_Boost_Converter/ABC.wrl

ml_asv:
	@$(ML_BIN) hardware/Tools/KiCad8/Analog_Switch_Variant/ASV.wrl

ml_psc:
	@$(ML_BIN) hardware/Tools/KiCad8/Power_Supply_Control/PSC.wrl

ml_ush2:
	@$(ML_BIN) hardware/Tools/KiCad8/USB_Serial_Hub_GLST/USH.wrl


##### Clean
clean:
	@$(MAKE) -C $(JXM_SRC_DIR)                                                      -s clean
	@$(MAKE) -C 3rd_party/0_experimental/hid_bootloader_cli/src-libusb1             -s clean
	@$(MAKE) -C hardware/Tools/Firmware/ATmega328P_4Digit_Voltmeter                 -s clean
	@$(MAKE) -C hardware/Tools/Firmware/ATmega328P_4Digit_Voltmeter_II              -s clean
	@$(MAKE) -C hardware/Tools/Firmware/ATtiny44A_Power_Supply_Control___Selector   -s clean
	@$(MAKE) -C hardware/Tools/Firmware/ATtiny44A_Power_Supply_Control___Sequencer  -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO/Bootloader                                 -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO/Firmware                                   -s clean
#	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Bootloader/ATxmega128A4U-CDC_ACM_AVR109 -s clean
#	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Bootloader/ATxmega128A4U-UART_AVR109    -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Firmware/ATxmega128A4U                  -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Firmware/ATmega8A_Boost                 -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Firmware/ATmega8A_UPDI                  -s clean
	@$(MAKE) -C hardware/JxMake_USB_GPIO_II/Firmware/ATmega1284P                    -s clean
	@rm -rvf hardware/Tools/Firmware/USB_Serial_Hub_GLST/build
	@rm -rvf hardware/Tools/Firmware/Serial_WiFi_Bridge/PicoW/build
	@ls -d   hardware/Test_Firmware/FW_AVR/*   | tr '\n' '\0' | xargs -0 -i -I{} sh -c '$(MAKE) -s -k -C {} clean cleanall 2>/dev/null || true'
	@ls -d   hardware/Test_Firmware/FW_LGT8F/* | tr '\n' '\0' | xargs -0 -i -I{} sh -c '$(MAKE) -s -k -C {} clean cleanall 2>/dev/null || true'
	@ls -d   hardware/Test_Firmware/FW_PIC/*   | tr '\n' '\0' | xargs -0 -i -I{} sh -c '$(MAKE) -s -k -C {} clean cleanall 2>/dev/null || true'
	@rm -rvf hardware/Test_Firmware/FW_PICO/RP2350/build
	@ls -d   hardware/Test_Firmware/FW_STM32/* | tr '\n' '\0' | xargs -0 -i -I{} sh -c '$(MAKE) -s -k -C {} clean cleanall 2>/dev/null || true'

dist_clean: clean
	@$(MAKE) -C $(JXM_SRC_DIR) -s dist_clean


##### Display SVN diff
sclean: clean
	@echo -e "\n\n\n\n\n\n\n\n\n\n"
	@clear

sstat:
	@svn status | grep -v '^\?'

sdiff:
	@svn diff | perl 3rd_party/tools/colordiff/colordiff.pl | more

xdiff: sclean
	@diff -ru ../Shadow/jxmake . | grep -v '^Only in' | perl 3rd_party/tools/colordiff/colordiff.pl | more


##### Archive the whole project
arcv: dist_clean
	@(                                                                                                     \
		COPY_NAME=JxMake-`date +'%Y%m%d-%H%M'`;                                                        \
		echo;                                                                                          \
		echo -e "$(C_MAGENTA)Cleaning-up the project tree ...$(C_RESET)";                              \
		cp -Rav src/jxm/gcomp  src/jxm/gcomp2 > /dev/null;                                             \
		rm -rvf src/jxm/gcomp                 > /dev/null;                                             \
		mv      src/jxm/gcomp2 src/jxm/gcomp  > /dev/null;                                             \
		cp -Rav src/jxm/tool   src/jxm/tool2  > /dev/null;                                             \
		rm -rvf src/jxm/tool                  > /dev/null;                                             \
		mv      src/jxm/tool2  src/jxm/tool   > /dev/null;                                             \
		cp -Rav src/jxm/ugc    src/jxm/ugc2   > /dev/null;                                             \
		rm -rvf src/jxm/ugc                   > /dev/null;                                             \
		mv      src/jxm/ugc2   src/jxm/ugc    > /dev/null;                                             \
		echo -e "$(C_GREEN)Copying the project tree (excluding  the '.svn' directory) ...$(C_RESET)";  \
		cd ..;                                                                                         \
		rsync -av --exclude={'.svn','.git'} JxMake/ "$$COPY_NAME";                                     \
		echo;                                                                                          \
		echo -e "$(C_CYAN)Archiving files from the copied project tree ...$(C_RESET)";                 \
		tar -cjvpf $${COPY_NAME}.tar.bz2 $$COPY_NAME > /dev/null;                                      \
		echo -e "$(C_WHITE)Done '$${COPY_NAME}.tar.bz2'$(C_RESET)";                                    \
		echo;                                                                                          \
	)
