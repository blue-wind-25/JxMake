TOOLKIT_PATH := $(HOME)/xsdk/pic/xc-dsc/bin

CC           := $(TOOLKIT_PATH)/xc-dsc-gcc
OD           := $(TOOLKIT_PATH)/xc-dsc-objdump
BH           := $(TOOLKIT_PATH)/xc-dsc-bin2hex

OUT_HEX       = ../../../../src/1-TestData/$(OUT_HEX_NAME)

include ../Makefile.xc.common.mk
