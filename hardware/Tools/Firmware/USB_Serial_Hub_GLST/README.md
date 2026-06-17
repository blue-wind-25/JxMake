# USB Serial Hub GLST — Firmware

CDC-ACM USB-to-serial bridge firmware for the JxMake USB Serial Hub GLST module,
targeting the STM32F103CBT6 (Blue Pill compatible) microcontroller.

The module exposes a single CDC-ACM virtual COM port to the host and bridges it to
a hardware USART3 port with RTS/CTS flow control and DTR output.


## Hardware

| Signal | Pin  | Direction      | Notes                          |
|--------|------|----------------|--------------------------------|
| TXD    | PB10 | STM32 → target | UART TX (AF push-pull)         |
| RXD    | PB11 | Target → STM32 | UART RX (AF input, pull-up)    |
| XCK    | PB12 | STM32 → target | Synchronous clock (optional)   |
| CTS    | PB13 | Target → STM32 | Flow control input (pull-down) |
| RTS    | PB14 | STM32 → target | Flow control output            |
| DTR    | PB15 | STM32 → target | Data Terminal Ready output     |
| LED    | PC13 | Output         | Activity/status indicator      |
| VBUS   | PA9  | Input          | USB VBUS detect                |
| DP_PU  | PA10 | Output         | 1.5 kΩ USB D+ pull-up enable   |

CTS is pulled low internally (active = asserted), so the UART transmits freely
when no external CTS signal is connected.


## Supported Line Coding

| Parameter | Supported values      |
|-----------|-----------------------|
| Baud rate | 1200 – 1843200 bps    |
| Data bits | 7 (with parity), 8, 9 |
| Stop bits | 1, 2                  |
| Parity    | None, Odd, Even       |

STM32F1 UART word length includes the parity bit, so the firmware transparently
adjusts `WordLength` based on the `dataBits`+`parity` combination:

| CDC dataBits | Parity   | STM32 WordLength       |
|--------------|----------|------------------------|
| 7            | Odd/Even | 8B (7 data + 1 parity) |
| 8            | None     | 8B                     |
| 8            | Odd/Even | 9B (8 data + 1 parity) |
| 9            | None     | 9B                     |

CDC `SEND_BREAK` is fully host-controlled via GPIO:

| `wValue` | Action |
|----------|--------|
| `0xFFFF` | Flushes UART TX/RX and CDC RX ring buffers, then reconfigures TXD (PB10) as GPIO output driven **low** — holds the line low indefinitely |
| `0x0000` | Drives TXD high for 1 ms, then restores it as USART3 AF push-pull |
| other    | Rejected (`USBD_FAIL`) |

This allows the PC application to start and stop a break condition at will,
matching the behaviour expected by tools that use break for reset (e.g. some
UART bootloaders).


## Building

```sh
./jxmake build   # Compile and link
./jxmake uswd    # Upload via SWD
./jxmake rswd    # Reset via SWD
./jxmake clean   # Clean build artefacts
```

Requires the JxMake build system and the STM32 GCC toolchain at the paths
configured in `JxMakeFile`.


## Loopback stress test

`loopback_test.py` exercises the firmware with a TXD↔RXD hardware loopback.

```sh
python3 loopback_test.py
```

The script performs, at each configured baud rate:

1. **Limit finder** — binary-searches the minimum inter-byte delay at which all
   echoes are received correctly, revealing the firmware's effective round-trip
   latency.
2. **Correctness** — sends a 64-byte `0x00–0xFF` pattern at the found limit and
   verifies every byte matches.
3. **Throughput** — sends 256 bytes and measures effective bytes/sec.
4. **Overflow stress** — blasts 4096 bytes with no pacing to deliberately fill
   the 1024-byte USB CDC RX ring buffer, then verifies the device recovers cleanly
   (endpoint not locked up).
5. **Break** — asserts `SEND_BREAK(0xFFFF)` via `port.break_condition = True`,
   verifies no echo is returned while TXD is held low, then deasserts
   (`port.break_condition = False`) and verifies that normal echo resumes.

> **Note:** the loopback topology measures the round-trip USB→UART→USB path.
> The firmware's TX and RX paths run concurrently but share the same USB polling
> window, so loopback throughput is lower than one-directional throughput.
> In real use (USB→UART→target→UART→USB) each direction operates independently.


## Firmware architecture

### Original (blocking)

```
[USB RX buffer] ──► HAL_UART_Transmit(HAL_MAX_DELAY)  ← blocks main loop
                                                          ↓
                    HAL_UART_Receive(timeout=0)  ──────► [USB TX]
```

Both directions ran sequentially in the main loop.  While `HAL_UART_Transmit`
held the loop (up to ~107 ms at 9600 baud for a 128-byte chunk), any bytes
arriving on RXD were lost — the STM32F1 UART has no hardware FIFO, and the
single-byte shift register was overwritten on every new byte.

### Improved (interrupt-driven)

```
USB RX ──► UART_TX_Enqueue() ──► uartTxBuffer[512]
                                        │
                              USART3 TXE ISR (TXEIE)
                                        │
                                   USART3->DR ──► TXD

RXD ──► USART3 RXNE ISR ──► uartRxBuffer[512]
                                        │
                              main loop drain ──► CDC_Transmit_FS()
                                        │
                                    USB TX
```

Two bare-metal ring buffers decouple USB and UART completely:

- **`uartTxBuffer`** (512 bytes) — main loop enqueues here via `UART_TX_Enqueue`
  and returns immediately.  The `TXE` ISR drains it byte by byte; `TXEIE` is
  enabled on enqueue and cleared when the buffer empties.
- **`uartRxBuffer`** (512 bytes) — the `RXNE` ISR stores every incoming byte
  here.  The main loop drains it in up to 64-byte chunks per `CDC_Transmit_FS`
  call, reducing USB transaction overhead.

The HAL interrupt machinery (`HAL_UART_Receive_IT` / `HAL_UART_IRQHandler`) is
bypassed entirely.  Under heavy burst RX load (e.g. 921600 baud), HAL's
`RxState` can become permanently stuck in `HAL_UART_STATE_BUSY_RX`, killing the
interrupt chain.  Reading and writing `USART3->DR` directly in the ISR is immune
to this.

Error flags (`ORE`, `FE`, `NE`) are cleared at the end of every ISR invocation
to prevent re-entry loops.

### Baud rate change behaviour

On `CDC_SET_LINE_CODING`, the UART TX ring buffer is flushed and `HAL_UART_Init`
is called immediately, matching the behaviour of FT232 and CH34x adapters.
Programming tools always assert a hardware reset before changing baud rate, so
any bytes pending in the TX buffer at that moment are irrelevant.


## Changes from the original STM32CubeMX-generated code

### `__package__/device_cdc/usbd_cdc_if.h`

| Location | Change | Reason |
|----------|--------|--------|
| `APP_RX_DATA_SIZE` | 1000 → 64 | `USBD_LL_PrepareReceive` always uses `CDC_DATA_FS_OUT_PACKET_SIZE` (64); the extra 936 bytes were wasted SRAM |
| `APP_TX_DATA_SIZE` | 1000 → 64 | `UserTxBufferFS` is never used — every `CDC_Transmit_FS` call overrides the buffer pointer; size is irrelevant |

### `__package__/device_cdc/usbd_cdc_if.c`

| Location | Change | Reason |
|----------|--------|--------|
| `HL_RX_BUFFER_SIZE` | 256 → 1024 | Larger headroom for burst reception |
| `CDC_Receive_FS` | `uint8_t len` → `uint32_t len` | Silent truncation if `*Len ≥ 256` (latent on FS, real on HS) |
| `CDC_Receive_FS` | Check-before-write order | Old code wrote a byte then checked for overflow — off-by-one |
| `CDC_Receive_FS` | Call `USBD_CDC_ReceivePacket` on overflow path | Old code omitted it, permanently locking the endpoint on buffer-full |
| `CDC_GetRxBufferBytesAvailable_FS` | Explicit `head >= tail` branch | Original modulo formula gave wrong results when the buffer was nearly full |
| `CDC_ReadRxBuffer_FS` / `CDC_PeekRxBuffer_FS` | Loop variable `uint8_t i` → `uint16_t i` | Would wrap at 256 if `Len > 255` |
| `CDC_FlushRxBuffer_FS` | Removed O(N) zero-fill loop | Resetting head/tail pointers is sufficient; zeroing 1024 bytes on every flush was wasteful |

### `main.cpp`

| Location | Change | Reason |
|----------|--------|--------|
| `blinkLED` | Cache `HAL_GetTick()` into `currentTick` | Two separate calls could return different values if a tick fires between them |
| `UART3_Init` — `bDataBits` | `UART_WORDLENGTH_8B` (=0) → `8` | `GET_LINE_CODING` was reporting 0 data bits to the host on initial connect |
| `UART3_Init` — sync-mode bits | Moved to after `HAL_UART_Init` | `HAL_UART_Init` resets CR2, so bits set before the call were silently discarded |
| `HAL_UART_MspInit` — XCK write | `UART_TXD_GPIO` → `UART_XCK_GPIO` | Typo; both resolve to GPIOB so was harmless, but corrected for clarity |
| `UART3_SetBaudrate` / `UART3_WriteBuffer` | Non-blocking ring buffer | See architecture section above |
| `USART3_IRQHandler` | New — bare-metal RXNE + TXE handler | See architecture section above |
| `USART3_IRQHandler` — error path | `if(ORE\|FE\|NE)` changed to `else if` | When RXNE+ORE fire together the RXNE branch already reads DR, clearing both flags; a second DR read in the error branch discarded the next incoming byte |
| `UART_TX_Enqueue` | New | Enqueue helper for the TX ring buffer |
| `UART_TX_FreeSpace` | New | Returns available space in `uartTxBuffer`; used by `handleUSB_CDC_ACM` to cap USB→UART draining |
| `handleUSB_CDC_ACM` | Non-blocking in both directions | Main loop no longer stalls on UART TX |
| `handleUSB_CDC_ACM` — USB→UART drain cap | Read from `rxBuffer` only up to `UART_TX_FreeSpace()` bytes | Old code consumed bytes from `rxBuffer` even when `uartTxBuffer` was full (e.g. CTS held), silently dropping them; now `rxBuffer` acts as a second-level backpressure buffer, raising effective USB→UART buffering from 512 to 1536 bytes before any loss occurs |
| `CDC_SET_LINE_CODING` — validate-before-copy | Read from `pbuf`, validate, then copy to `cdcLineCoding` | Old order copied first, so a rejected request left `cdcLineCoding` with invalid values |
| `CDC_SET_LINE_CODING` — parity+wordlength | Word length now accounts for parity bit | Old code mapped `dataBits=8` → `WORDLENGTH_8B` unconditionally; with parity enabled that gives only 7 effective data bits. Fixed: `8E/8O` → `WORDLENGTH_9B`, `7E/7O` → `WORDLENGTH_8B` |
| `CDC_SET_LINE_CODING` — UART RX flush | Flush `uartRxBuffer` ring buffer on baud-rate change | TX buffer was already flushed; RX buffer now also cleared so stale bytes at the old baud rate can't be forwarded |
| `CDC_SEND_BREAK` | GPIO-based infinite break | `0xFFFF` reconfigures TXD as GPIO output driven low; `0x0000` drives high for 1 ms then restores USART3 AF — holds the line low for as long as the host requires, unlike the `SBK` bit which only sends a single break frame |
| `CDC_SEND_BREAK` — buffer flush on `0xFFFF` | Flush `uartTxBuffer`, `uartRxBuffer`, and `rxBuffer` before asserting break | Stale queued bytes accumulated before break (e.g. from a preceding overflow burst) would be transmitted after break deasserts, corrupting the target's receive stream; flushing on assert matches hardware UART FIFO semantics |


## Performance

> **Important:** all figures below were measured on a specific test system
> (CentOS 7, kernel 6.0.0, i5-4460, USB FS hub) using `loopback_test.py`.
> Actual throughput will vary depending on:
>
> - **Host OS and kernel version** — Linux, Windows, and macOS have different
>   USB CDC-ACM drivers with different polling intervals and transfer batch sizes
> - **USB topology** — direct connection vs hub, number of hub hops, hub speed
>   (FS vs HS), and bus contention from other devices on the same controller
> - **System load** — CPU scheduling latency affects how quickly the host driver
>   services USB interrupts and how fast userspace reads the virtual COM port
> - **Application behaviour** — tools that read in large blocks (e.g. `dd`) will
>   see higher throughput than tools that read one byte at a time
>
> The figures here should be treated as a baseline reference, not absolute limits.

### Loopback throughput (TXD↔RXD, STM32F103CBT6 @ 72 MHz, USB FS behind hub)

| Baud rate | UART max   | Loopback throughput | Notes                   |
|-----------|------------|---------------------|-------------------------|
| 1200      | 120 B/s    | ~109 B/s            | UART-limited            |
| 2400      | 240 B/s    | ~204 B/s            | UART-limited            |
| 4800      | 480 B/s    | ~370 B/s            | UART-limited            |
| 9600      | 960 B/s    | ~640 B/s            | UART-limited            |
| 19200     | 1920 B/s   | ~1118 B/s           | UART-limited            |
| 57600     | 5760 B/s   | ~2197 B/s           | UART-limited            |
| 115200    | 11520 B/s  | ~2792 B/s           | Approaching USB ceiling |
| 230400    | 23040 B/s  | ~3288 B/s           | USB ceiling             |
| 460800    | 46080 B/s  | ~3457 B/s           | USB ceiling             |
| 921600    | 92160 B/s  | ~3657 B/s           | USB ceiling             |
| 1843200   | 184320 B/s | ~3857 B/s           | USB ceiling             |

The USB FS loopback ceiling is a measurement artefact: TX and RX share the same
1 ms USB frame window in a loopback topology, so each direction gets roughly half
the available bandwidth.  In real use (USB→UART→target device→UART→USB) the two
directions are independent.  One-directional throughput approaches the 64 KB/s
USB FS theoretical maximum at high baud rates.

The non-blocking interrupt-driven architecture means neither direction stalls the
other in real use.  Loopback figures at high baud rates are lower than the bridge's
actual capacity because the echo arrives in a later USB poll cycle than it did with
the old blocking TX path.

### Overflow buffering (USB→UART, TXD↔RXD loopback)

The overflow stress test blasts 4096 bytes with no pacing. The number of bytes
received during the burst reflects how much data the firmware absorbed before the
USB CDC RX ring buffer filled:

| Baud rate | Received during 4096 B burst | Notes                              |
|-----------|------------------------------|------------------------------------|
| 1200      | ~240 B                       | UART-rate limited (120 B/s × 2 s)  |
| 2400      | ~512 B                       | UART-rate limited (240 B/s × 2 s)  |
| 4800      | ~1024 B                      | UART-rate limited (480 B/s × 2 s)  |
| 9600      | ~1536 B                      | Full buffer absorbed               |
| 19200     | ~1536 B                      | Full buffer absorbed               |
| 57600     | ~1536 B                      | Full buffer absorbed               |
| 115200    | ~1600 B                      | Full buffer absorbed               |
| 230400    | ~1605 B                      | Full buffer absorbed               |
| 460800    | ~1608 B                      | Full buffer absorbed               |
| 921600    | ~1631 B                      | Full buffer absorbed               |
| 1843200   | ~1656 B                      | Full buffer absorbed               |

The baseline figure of ~1536 B = `rxBuffer` (1024) + `uartTxBuffer` (512) confirms
that the USB CDC RX ring buffer and the UART TX ring buffer are both fully utilised
before any data is dropped.  At higher baud rates the UART drains `uartTxBuffer`
faster during the burst, freeing space for slightly more USB data (or the loopback
echo fills `uartRxBuffer`, displacing the accounting).  All rates recover cleanly
after the burst (endpoint never locks up).

At 1200–4800 baud the UART is so slow that it can only drain `baud/10 × 2` bytes
during the 2-second burst window before the measurement ends — the internal buffers
are fully loaded but the loopback echo has not yet returned.  These rates require
a proportionally longer post-burst drain wait before the next test step, which the
test script handles with an adaptive read-until-idle loop.


## Contributing

Improvements contributed by **Claude Sonnet 4.6** (Anthropic), June 2026,
in collaboration with Aloysius Indrayanto.

The session covered:

- Review of the original ST-generated CDC-ACM code and identification of latent
  bugs in `usbd_cdc_if.c` (truncation, off-by-one write, missing
  `USBD_CDC_ReceivePacket` on overflow, and wrong circular-buffer byte-count
  formula when the buffer was nearly full)
- Root-cause analysis of why a loopback test is harder than testing against a
  real target for a bridge with blocking UART I/O
- Design and implementation of the bare-metal RXNE/TXE interrupt ring buffer
  architecture replacing the original blocking HAL calls
- Iterative debugging of the ISR chain (missing re-arm after `CDC_SET_LINE_CODING`,
  HAL state machine corruption under burst load)
- Development of `loopback_test.py`, including the binary-search limit finder that
  revealed the USB polling ceiling and the loopback artefact at high baud rates
- GPIO-based `CDC_SEND_BREAK` implementation for host-controlled infinite break
  (required by UART bootloaders that use break for reset)
- Fix for the `CDC_SET_LINE_CODING` parity+wordlength mapping (STM32F1 UART word
  length includes the parity bit; `8E/8O` requires `WORDLENGTH_9B`)
- Identification and fix of the USB→UART backpressure bug: the drain loop was
  consuming bytes from the 1024-byte `rxBuffer` even when `uartTxBuffer` was full,
  silently dropping them; capping the drain by `UART_TX_FreeSpace()` restores the
  intended 1536-byte two-stage buffer, confirmed by the overflow stress test
- Reduction of `APP_RX_DATA_SIZE`/`APP_TX_DATA_SIZE` from 1000 to 64 bytes each
  (the USB stack never uses more than one 64-byte packet at a time; saves 1872 B of BSS)
- Break test (test 5) added to `loopback_test.py`
- Code style reformatting of `main.cpp` per the project style guide
- Diagnosis and fix of 1200 and 2400 baud test failures, which had two root causes:
  (1) firmware: `CDC_SEND_BREAK(0xFFFF)` now flushes all UART TX/RX and CDC RX ring
  buffers before asserting break, so stale overflow bytes cannot contaminate the
  post-deassert echo window; (2) test: the fixed `RECOVERY_WAIT` sleep after the
  overflow burst is replaced with an adaptive read-until-idle drain loop, correctly
  waiting for the UART pipeline to empty at any baud rate (e.g. ~12 s at 1200 baud)
- Extended `BAUD_RATES` in `loopback_test.py` to cover 1200–921600; all 50 tests
  across 10 baud rates now pass
