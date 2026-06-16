#!/bin/bash
# Detect how long the CDC-ACM device takes to enumerate after the hub has enumerated

START_TS=$(dmesg | tail -1 | awk 'match($0, /\[\s*([0-9]+\.[0-9]+)\s*\]/, ts) {print ts[1]}')
echo
echo "Baseline timestamp: $START_TS"
echo
echo "Plug in the hub now... (Ctrl+C to quit)"
echo

dmesg -w | awk -v start_ts="$START_TS" '
{
    match($0, /\[\s*([0-9]+\.[0-9]+)\s*\]/, ts)
    line_ts = ts[1]
    if (line_ts <= start_ts) next
}
/hub.*ports detected/ {
    hub_ts = line_ts
    printf "\n[%s] Hub detected\n", hub_ts
    fflush()
}
/USB disconnect/ {
    hub_ts = ""
    printf "[%s] Disconnected\n", line_ts
    fflush()
}
/ttyACM.*USB ACM device/ {
    acm_ts = line_ts
    printf "[%s] ttyACM appeared\n", acm_ts
    if (hub_ts != "") {
        printf ">>> Hub to ttyACM: %.3fs\n\n", acm_ts - hub_ts
    }
    fflush()
}
'
