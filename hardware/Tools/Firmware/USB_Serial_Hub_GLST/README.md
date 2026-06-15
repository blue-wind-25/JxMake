# USB Serial Hub GLST — Firmware

CDC-ACM USB-to-serial bridge firmware for the JxMake USB Serial Hub GLST module,
targeting the STM32F103CBT6 (Blue Pill compatible) microcontroller.

The module exposes a single CDC-ACM virtual COM port to the host and bridges it to
a hardware USART3 port with RTS/CTS flow control and DTR output.


## Hardware

| Signal  | Pin  | Direction      | Notes                              |
|---------|------|----------------|------------------------------------|
| TXD     | PB10 | STM32 → target | UART TX (AF push-pull)             |
| RXD     | PB11 | Target → STM32 | UART RX (AF input, pull-up)        |
| XCK     | PB12 | STM32 → target | Synchronous clock (optional)       |
| CTS     | PB13 | Target → STM32 | Flow control input (pull-down)     |
| RTS     | PB14 | STM32 → target | Flow control output                |
| DTR     | PB15 | STM32 → target | Data Terminal Ready output         |
| LED     | PC13 | Output         | Activity/status indicator          |
| VBUS    | PA9  | Input          | USB VBUS detect                    |
| DP_PU   | PA10 | Output         | 1.5 kΩ USB D+ pull-up enable       |

CTS is pulled low internally (active = asserted), so the UART transmits freely
when no external CTS signal is connected.


## Supported Line Coding

| Parameter | Supported values                        |
|-----------|-----------------------------------------|
| Baud rate | 1200 – 1843200 bps                      |
| Data bits | 8, 9                                    |
| Stop bits | 1, 2                                    |
| Parity    | None, Odd, Even                         |

CDC `SEND_BREAK` is supported: `breakDuration = 0xFFFF` asserts the break
condition; `breakDuration = 0x0000` clears it.  All other durations are rejected.


## Building

```sh
./jxmake build    # compile and flash via SWD
./jxmake uswd     # upload via SWD
./jxmake rswd     # reset via SWD
./jxmake clean    # clean build artefacts
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

### `__package__/device_cdc/usbd_cdc_if.c`

| Location | Change | Reason |
|----------|--------|--------|
| `HL_RX_BUFFER_SIZE` | 256 → 1024 | Larger headroom for burst reception |
| `CDC_Receive_FS` | `uint8_t len` → `uint32_t len` | Silent truncation if `*Len ≥ 256` (latent on FS, real on HS) |
| `CDC_Receive_FS` | Check-before-write order | Old code wrote a byte then checked for overflow — off-by-one |
| `CDC_Receive_FS` | Call `USBD_CDC_ReceivePacket` on overflow path | Old code omitted it, permanently locking the endpoint on buffer-full |
| `CDC_GetRxBufferBytesAvailable_FS` | Explicit `head >= tail` branch | Original modulo formula gave wrong results on wrap-around |
| `CDC_ReadRxBuffer_FS` / `CDC_PeekRxBuffer_FS` | Loop variable `uint8_t i` → `uint16_t i` | Would wrap at 256 if `Len > 255` |

### `main.cpp`

| Location | Change | Reason |
|----------|--------|--------|
| `blinkLED` | Cache `HAL_GetTick()` into `currentTick` | Two separate calls could return different values if a tick fires between them |
| `UART3_Init` indentation | Fixed spurious extra indent level | Cosmetic — code was at function scope, not inside a nested block |
| `UART3_SetBaudrate` / `UART3_WriteBuffer` | Non-blocking ring buffer | See architecture section above |
| `USART3_IRQHandler` | New — bare-metal RXNE + TXE handler | See architecture section above |
| `UART_TX_Enqueue` | New | Enqueue helper for the TX ring buffer |
| `handleUSB_CDC_ACM` | Non-blocking in both directions | Main loop no longer stalls on UART TX |
| `CDC_SET_LINE_CODING` | Flush TX buffer before `HAL_UART_Init` | Matches FT232/CH34x immediate-apply behaviour |
| `CDC_SEND_BREAK` | Unchanged | `SBK` bit in CR1 is independent of `TXEIE`; no interaction |


## Performance (loopback, STM32F103CBT6 @ 72 MHz, USB FS behind hub)

| Baud rate | UART max | Loopback throughput | Notes |
|-----------|----------|---------------------|-------|
| 9600      | 960 B/s  | ~640 B/s            | UART-limited |
| 19200     | 1920 B/s | ~1130 B/s           | UART-limited |
| 57600     | 5760 B/s | ~2580 B/s           | UART-limited |
| 115200    | 11520 B/s| ~2800 B/s           | Approaching USB ceiling |
| 230400    | 23040 B/s| ~3800 B/s           | USB ceiling |
| 460800    | 46080 B/s| ~3750 B/s           | USB ceiling |
| 921600    | 92160 B/s| ~3420 B/s           | USB ceiling |

The USB FS ceiling in loopback is ~4 KB/s because TX and RX share the same 1 ms
USB frame window.  One-directional throughput approaches the 64 KB/s USB FS
theoretical maximum at high baud rates.


## Contributing

Performance improvements contributed by **Claude Sonnet 4.6** (Anthropic), June 2026,
in collaboration with Aloysius Indrayanto.

The session covered:

- Review of the original ST-generated CDC-ACM code and identification of four
  latent bugs in `usbd_cdc_if.c` (truncation, off-by-one write, missing
  `USBD_CDC_ReceivePacket` on overflow, and wrong circular-buffer byte-count formula)
- Root-cause analysis of why a loopback test is harder than testing against a real
  target for a bridge with blocking UART I/O
- Design and implementation of the bare-metal RXNE/TXE interrupt ring buffer
  architecture replacing the original blocking HAL calls
- Iterative debugging of the ISR chain (missing re-arm after `CDC_SET_LINE_CODING`,
  HAL state machine corruption under burst load)
- Development of `loopback_test.py`, including the binary-search limit finder that
  revealed the USB polling ceiling
