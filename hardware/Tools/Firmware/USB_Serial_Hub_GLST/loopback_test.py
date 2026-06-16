#!/usr/bin/env python3
"""
loopback_test.py — CDC-ACM loopback stress test for USB Serial Hub GLST
Hardware: connect STM32 TXD <-> RXD
Device:   /dev/ttyACM0

Firmware architecture:
  USB->UART: non-blocking enqueue into uartTxBuffer[512]; USART3 TXE ISR drains it.
  UART->USB: USART3 RXNE ISR fills uartRxBuffer[512]; main loop drains in 64-byte chunks.
  Both directions run concurrently via bare-metal ring buffers — neither blocks the other.

Tests per baud rate:
  1. Limit finder  — binary-search the minimum inter-byte delay for 100% echo
  2. Correctness   — 64 bytes at the found limit delay, verify byte-for-byte
  3. Throughput    — 256 bytes at the found limit delay, measure effective B/s
  4. Overflow      — blast 4096 B, verify endpoint recovers
  5. Break         — assert infinite break, verify no echo, deassert, verify echo resumes
"""

import sys, os, time, serial

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
DEV               = "/dev/ttyACM0"
BAUD_RATES        = [1200, 9600, 19200, 57600, 115200, 230400, 460800, 921600, 1843200]
CORRECTNESS_BYTES = 64
THROUGHPUT_BYTES  = 256
OVERFLOW_BURST    = 4096
RECOVERY_BYTES    = 16
RECOVERY_WAIT     = 1.5
HANDSHAKE_WAIT    = 1.5

# Limit finder config
LIMIT_PROBE_BYTES  = 32    # bytes per probe during binary search
LIMIT_RETRIES      = 3     # passes at each delay to confirm stability
LIMIT_DELAY_MAX    = 0.010 # 10ms starting point (confirmed working)
LIMIT_DELAY_MIN    = 0.000 # 0ms floor
LIMIT_ITERATIONS   = 11    # binary search depth → resolution ~0.01ms

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED="\033[0;31m"; GRN="\033[0;32m"; YLW="\033[0;33m"
BLU="\033[0;34m"; CYN="\033[0;36m"; BLD="\033[1m";  RST="\033[0m"
passed=0; failed=0

def section(t): print(f"\n{BLU}{BLD}{'═'*56}{RST}\n{BLU}{BLD}  {t}{RST}\n{BLU}{BLD}{'═'*56}{RST}")
def log_test(m): print(f"{CYN}  ▶ {m}{RST}")
def log_info(m): print(f"{YLW}    {m}{RST}")
def log_pass(m):
    global passed; passed+=1; print(f"{GRN}  ✔ PASS — {m}{RST}")
def log_fail(m):
    global failed; failed+=1; print(f"{RED}  ✘ FAIL — {m}{RST}")

def make_pattern(n):
    return bytes([i % 256 for i in range(n)])

def flush_port(port, wait=0.3):
    time.sleep(wait)
    port.reset_input_buffer()

def send_recv_1by1(port, data, delay, read_timeout=1.0):
    """Send data 1 byte at a time, inter-byte delay between each."""
    received = bytearray()
    port.timeout = read_timeout
    for byte in data:
        port.write(bytes([byte]))
        port.flush()
        if delay > 0.0001:
            time.sleep(delay)
        b = port.read(1)
        received.extend(b)
        if not b:
            break
    return bytes(received)

def probe_delay(port, delay, n=LIMIT_PROBE_BYTES):
    """
    Send n bytes with given inter-byte delay.
    Returns True if all n bytes echoed back correctly.
    """
    flush_port(port, wait=0.05)
    pattern  = make_pattern(n)
    received = send_recv_1by1(port, pattern, delay, read_timeout=max(delay*20, 0.5))
    return received == pattern

def find_limit(port, baud):
    """
    Binary search for the minimum inter-byte delay that gives 100% echo.
    Returns (delay_seconds, effective_bps).
    """
    lo = LIMIT_DELAY_MIN
    hi = LIMIT_DELAY_MAX

    log_info(f"Binary search: {LIMIT_ITERATIONS} iterations, "
             f"probe={LIMIT_PROBE_BYTES} B, retries={LIMIT_RETRIES}")

    for i in range(LIMIT_ITERATIONS):
        mid = (lo + hi) / 2
        # Test LIMIT_RETRIES times at this delay for stability
        ok = all(probe_delay(port, mid) for _ in range(LIMIT_RETRIES))
        marker = f"{GRN}OK{RST}" if ok else f"{RED}FAIL{RST}"
        print(f"    [{i+1:2d}/{LIMIT_ITERATIONS}] delay={mid*1000:6.2f}ms → {marker}  "
              f"(range [{lo*1000:5.2f}, {hi*1000:5.2f}]ms)")
        if ok:
            hi = mid   # can go faster
        else:
            lo = mid   # too fast, back off

    # hi is the lowest delay that passed — add 10% safety margin
    safe_delay = hi * 1.1
    effective_bps = 1.0 / safe_delay if safe_delay > 0 else float('inf')
    return safe_delay, effective_bps

# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------
print(f"{BLD}USB Serial Hub GLST — Loopback Stress Test{RST}")
print(f"Device: {DEV}")

if not os.path.exists(DEV):
    print(f"{RED}ERROR: {DEV} not found.{RST}"); sys.exit(1)
if not os.access(DEV, os.W_OK):
    print(f"{RED}ERROR: No write permission. Try: sudo usermod -aG dialout $USER{RST}"); sys.exit(1)

# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------
for baud in BAUD_RATES:
    section(f"Baud rate: {baud}  (UART theoretical max: {baud//10} B/s)")

    try:
        port = serial.Serial(
            port=DEV, baudrate=baud,
            bytesize=serial.EIGHTBITS, parity=serial.PARITY_NONE,
            stopbits=serial.STOPBITS_ONE,
            xonxoff=False, rtscts=False, dsrdtr=False,
        )
    except serial.SerialException as e:
        log_fail(f"Could not open port: {e}"); continue

    log_info("Waiting for CDC handshake...")
    time.sleep(HANDSHAKE_WAIT)
    flush_port(port)

    # ------------------------------------------------------------------
    # 1. Limit finder
    # ------------------------------------------------------------------
    log_test("Limit finder — binary search for minimum reliable inter-byte delay")

    limit_delay, limit_bps = find_limit(port, baud)
    uart_max   = baud / 10
    efficiency = limit_bps / uart_max * 100 if uart_max > 0 else 0

    log_info(f"Minimum reliable delay: {limit_delay*1000:.2f}ms → "
             f"{limit_bps:.0f} B/s effective  "
             f"({efficiency:.1f}% of {uart_max:.0f} B/s UART max)")

    flush_port(port)

    # ------------------------------------------------------------------
    # 2. Correctness at found limit
    # ------------------------------------------------------------------
    log_test(f"Correctness ({CORRECTNESS_BYTES} bytes at {limit_delay*1000:.2f}ms delay)")

    pattern  = make_pattern(CORRECTNESS_BYTES)
    received = send_recv_1by1(port, pattern, limit_delay)

    if len(received) != CORRECTNESS_BYTES:
        log_fail(f"Expected {CORRECTNESS_BYTES} bytes, got {len(received)}")
        log_info(f"Got:  {' '.join(f'{b:02X}' for b in received[:16])}...")
    elif received != pattern:
        bad = next(i for i,(a,b) in enumerate(zip(pattern,received)) if a!=b)
        log_fail(f"Mismatch at byte {bad}: sent 0x{pattern[bad]:02X} got 0x{received[bad]:02X}")
    else:
        log_pass(f"All {CORRECTNESS_BYTES} bytes match")

    flush_port(port)

    # ------------------------------------------------------------------
    # 3. Throughput at found limit
    # ------------------------------------------------------------------
    log_test(f"Throughput ({THROUGHPUT_BYTES} bytes at {limit_delay*1000:.2f}ms delay)")

    pattern  = make_pattern(THROUGHPUT_BYTES)
    t_start  = time.monotonic()
    received = send_recv_1by1(port, pattern, limit_delay)
    elapsed  = time.monotonic() - t_start
    bps      = len(received) / elapsed if elapsed > 0 else 0

    if len(received) != THROUGHPUT_BYTES:
        log_fail(f"Only {len(received)}/{THROUGHPUT_BYTES} bytes in {elapsed:.1f}s")
    else:
        log_pass(f"{len(received)} bytes in {elapsed:.1f}s → {bps:.0f} B/s  "
                 f"({bps/uart_max*100:.1f}% of {uart_max:.0f} B/s UART max)")

    flush_port(port)

    # ------------------------------------------------------------------
    # 4. Overflow stress
    # ------------------------------------------------------------------
    log_test(f"Overflow stress ({OVERFLOW_BURST} B blast, no pacing)")
    log_info("Deliberately fills rxBuffer — partial loss expected")
    log_info("Key check: endpoint not locked up after overflow")

    port.timeout = 2.0
    port.write(make_pattern(OVERFLOW_BURST))
    port.flush()

    drained  = bytearray()
    deadline = time.monotonic() + 2.0
    while time.monotonic() < deadline:
        b = port.read(256)
        if b: drained.extend(b)
    log_info(f"Received {len(drained)} bytes during burst")

    log_info(f"Waiting {RECOVERY_WAIT}s for recovery...")
    time.sleep(RECOVERY_WAIT)
    flush_port(port)

    probe     = make_pattern(RECOVERY_BYTES)
    recovered = send_recv_1by1(port, probe, limit_delay)

    if len(recovered) != RECOVERY_BYTES:
        log_fail(f"Recovery failed — {len(recovered)}/{RECOVERY_BYTES} bytes echoed")
    elif recovered != probe:
        log_fail("Data corrupted after overflow recovery")
    else:
        log_pass(f"Device recovered — {RECOVERY_BYTES} clean bytes echoed correctly")

    flush_port(port)

    # ------------------------------------------------------------------
    # 5. Break test
    # ------------------------------------------------------------------
    log_test("Break — assert infinite break (0xFFFF), verify no echo, deassert (0x0000), verify recovery")

    # Assert break: firmware reconfigures TXD as GPIO output driven low
    port.break_condition = True
    time.sleep(0.2)

    # Flush any noise that arrived before break took effect
    port.reset_input_buffer()

    # While break is asserted, TXD is held low — no valid UART framing can be received,
    # so any bytes we send should NOT echo back
    port.timeout = 0.3
    port.write(make_pattern(8))
    port.flush()
    noise = port.read(8)

    if len(noise) != 0:
        log_fail(f"Break not working — received {len(noise)} byte(s) while break asserted (expected 0)")
    else:
        log_pass("No echo while break asserted — TXD held low correctly")

    # Deassert break: firmware drives TXD high for 1 ms then restores USART3 AF
    port.break_condition = False
    time.sleep(0.2)   # Let the 1 ms idle pulse and AF restoration complete
    flush_port(port)

    # Verify normal echo resumes
    probe    = make_pattern(RECOVERY_BYTES)
    returned = send_recv_1by1(port, probe, limit_delay)

    if len(returned) != RECOVERY_BYTES:
        log_fail(f"After break deassert — {len(returned)}/{RECOVERY_BYTES} bytes echoed")
    elif returned != probe:
        log_fail("Data corrupted after break deassert")
    else:
        log_pass(f"Echo resumed correctly after break deassert — {RECOVERY_BYTES} bytes match")

    port.close()
    time.sleep(0.5)

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
total = passed + failed
print(f"\n{BLD}{'═'*50}{RST}")
print(f"{BLD}  Results: {GRN}{passed} passed{RST}{BLD} / "
      f"{RED}{failed} failed{RST}{BLD} / {total} total{RST}")
print(f"{BLD}{'═'*50}{RST}")
sys.exit(0 if failed == 0 else 1)
