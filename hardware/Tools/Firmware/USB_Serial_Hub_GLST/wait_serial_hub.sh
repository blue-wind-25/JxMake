#!/bin/bash
# Measure CDC-ACM enumeration time after a USB hub enumerates.

START_TS=$(dmesg | tail -1 | awk '
    match($0, /\[\s*([0-9]+\.[0-9]+)\s*\]/, ts) { print ts[1] }
')

echo
echo "Baseline timestamp: $START_TS"

echo
echo "Plug in the hub now... (Ctrl+C to quit)"
echo

dmesg -w | awk -v start_ts="$START_TS" '

# Ignore historical dmesg entries; process only events that occur after the script begins monitoring.
{
    match($0, /\[\s*([0-9]+\.[0-9]+)\s*\]/, ts)
    line_ts = ts[1]

    if(line_ts <= start_ts) next
}

# Cache VID:PID by USB topology path.
#
# Example:
#   usb 2-1.4   -> 05e3:0610
#   usb 2-1.4.4 -> 0483:5740
#
/idVendor=.*idProduct/ {
    match($0, /(usb [0-9.-]+)/, dev)
    dev_path = dev[1]

    match($0,
          /idVendor=([0-9a-f]+), idProduct=([0-9a-f]+)/,
          ids)

    vidpid_map[dev_path] = ids[1] ":" ids[2]
}

# Hub detection.
/USB hub found/ {
    hub_ts = line_ts

    match($0, /(hub ([0-9.-]+):)/, hub)
    hub_usb_path = "usb " hub[2]

    printf "\n[%s] Hub detected\n", hub_ts
    printf "    Path    : %s\n", hub_usb_path

    if(hub_usb_path in vidpid_map) printf "    VID:PID : %s\n", vidpid_map[hub_usb_path]

    fflush()
}

# CDC-ACM device appeared.
/ttyACM[0-9]+/ {
    acm_ts = line_ts

    match($0, /(ttyACM[0-9]+)/, dev)
    acm_dev = dev[1]

    match($0, /(cdc_acm ([0-9.-]+):)/, usb)
    acm_usb_path = "usb " usb[2]

    printf "\n[%s] CDC-ACM device appeared\n", acm_ts
    printf "    Device  : %s\n", acm_dev
    printf "    Path    : %s\n", acm_usb_path

    if(acm_usb_path in vidpid_map) printf "    VID:PID : %s\n", vidpid_map[acm_usb_path]

    if(hub_ts != "") {
        printf "\n>>> Time from hub detection to %s appearance: %.3f s\n\n", acm_dev, acm_ts - hub_ts
    }

    fflush()
}

# USB device disconnected.
/USB disconnect/ {
    match($0, /(usb [0-9.-]+)/, dev)
    dev_path = dev[1]

    printf "\n[%s] USB device disconnected\n", line_ts
    printf "    Path    : %s\n", dev_path

    if(dev_path in vidpid_map) {
        printf "    VID:PID : %s\n", vidpid_map[dev_path]
        delete vidpid_map[dev_path]
    }

    if(dev_path == hub_usb_path) hub_ts = ""

    fflush()
}

'
