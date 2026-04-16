build: main.hex
	@echo > /dev/null

clean:
	@rm -vf *.i *.s *.o *.elf *.lst *.hex


%.hex: %.c
	rm -f $@ $(OUT_HEX)
	$(CC) -save-temps -mcpu=$(MCU) -T p$(MCU).gld -O2 main.c -o main.elf
	$(OD) -D    main.elf > main.lst
	$(BH) -a -u main.elf
	cp $@ $(OUT_HEX)
