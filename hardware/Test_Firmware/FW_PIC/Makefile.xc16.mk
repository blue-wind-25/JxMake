TOOLKIT_PATH := $(HOME)/xsdk/pic/xc16/v2.10/bin

CC           := $(TOOLKIT_PATH)/xc16-gcc
OD           := $(TOOLKIT_PATH)/xc16-objdump
BH           := $(TOOLKIT_PATH)/xc16-bin2hex

OUT_HEX       = ../../../../src/1-TestData/$(OUT_HEX_NAME)

include ../Makefile.xc.common.mk
