====================================================================================================

This directory contains a flattened and simplified AVR DU Series USB CDC‑ACM stack, configured as
a single CDC‑ACM interface for a USB‑to‑serial converter.

The flattening process, designed to minimize bloat and improve execution speed, was performed with
the assistance of Claude Sonnet 4.6, using high‑effort runs via the Claude Code CLI.

Each source file includes a marker at the top indicating that it has been modified by the JxMake
project.

To conserve space, files and directories not essential for program execution have been omitted.

This streamlined USB CDC‑ACM stack does not support advanced USB events (e.g., suspend, wake‑up,
etc.) and is restricted to compilation with AVR‑GCC.

----------------------------------------------------------------------------------------------------

The original source code files in this directory were downloaded from:

GitHub Account : Microchip PIC & AVR Examples - Microchip Technology
Repository     : USB Communication Device Class (CDC) Data Logger with AVR® DU
Repository URL : https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc
License        : https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc/blob/main/LICENSE.txt

----------------------------------------------------------------------------------------------------

(C) 2021-2025 Microchip Technology Inc. and its subsidiaries.

Subject to your compliance with these terms, you may use Microchip software and any derivatives
exclusively with Microchip products. You're responsible for complying with 3rd party license terms
applicable to your use of 3rd party software (including open source software) that may accompany
Microchip software.

SOFTWARE IS "AS IS". NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS SOFTWARE,
INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR
PURPOSE.

IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE, INCIDENTAL OR
CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER
CAUSED, EVEN IF MICROCHIP HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE. TO THE
FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL CLAIMS RELATED TO THE SOFTWARE
WILL NOT EXCEED AMOUNT OF FEES, IF ANY, YOU PAID DIRECTLY TO MICROCHIP FOR THIS SOFTWARE.

====================================================================================================
