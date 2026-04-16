#
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake program, see LICENSE file for the license details.
#


import os
import serial
import sys
import time
import traceback


"""
for x in range(256) : print("/* 0x%02X */ -1, // <reserved>" % x)
sys.exit(0)
#"""


MAGICR_BAUDRATE = 1200
LOWEST_BAUDRATE = 2400

RESET_STEP_TIME = 0.1
RESET_WAIT_TIME = 0.8


if( len(sys.argv) != 2 ) :
    print("\n")
    print("Usage: ResetToBootloader <device>")
    print("\n")
    sys.exit(1)

device = sys.argv[1]


def wait_for_device_state(path, available, timeout) :
    deadline = time.time() + timeout
    while time.time() < deadline :
        exists = os.path.exists(path)
        if exists == available :
            return True
        time.sleep(0.1)
    return False


try :
    ser = serial.Serial(port=device, baudrate=LOWEST_BAUDRATE)
    ser.stopbits = serial.STOPBITS_TWO
    ser.stopbits = serial.STOPBITS_ONE
    ser.close()
    time.sleep(RESET_STEP_TIME)

    ser = serial.Serial(port=device, baudrate=MAGICR_BAUDRATE)
    ser.stopbits = serial.STOPBITS_TWO
    ser.stopbits = serial.STOPBITS_ONE
    ser.close()
    if not True :
        time.sleep(RESET_WAIT_TIME * 3)
    else :
        if wait_for_device_state(device, False, RESET_WAIT_TIME * 1.25) : wait_for_device_state(device, True, RESET_WAIT_TIME * 3)

    ser = serial.Serial(port=device, baudrate=LOWEST_BAUDRATE)
    ser.stopbits = serial.STOPBITS_TWO
    ser.stopbits = serial.STOPBITS_ONE
    ser.close()

except :
    traceback.print_exc()
