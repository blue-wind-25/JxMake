TOOLKIT_PATH := /opt/gputils-1.5.2
LDS_PATH     := $(TOOLKIT_PATH)/share/gputils/lkr

GPLINK       := $(TOOLKIT_PATH)/bin/gplink
GPASM        := $(TOOLKIT_PATH)/bin/gpasm

OUT_HEX       = ../../../../src/1-TestData/$(OUT_HEX_NAME)


build: main.hex
	@echo > /dev/null

clean:
	@rm -vf *.o *.cod *.cof *.lst *.map *.hex


%.hex: %.asm
	rm -f $@ $(OUT_HEX)
	$(GPASM) $< -o $@
#	$(GPASM) -c $< -o $(@:.hex=.o)
#	$(GPLINK) -c $(LDS_PATH)/16f84a_g.lkr $(@:.hex=.o) -o $@
	cp $@ $(OUT_HEX)
