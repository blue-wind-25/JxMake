***

JxMake Release Changelog
========================

[![Language: Java](https://img.shields.io/badge/Language-Java-orange?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Language: C/C++](https://img.shields.io/badge/Language-C%2FC%2B%2B-blue?style=flat&logo=c%2B%2B&logoColor=white)](https://en.cppreference.com)
[![Primary_License: LGPL v3+](https://img.shields.io/badge/Primary_License-LGPL_v3+-green?style=flat&logo=opensourcehardware&logoColor=white)](LICENSE)

***


Technical Preview 2 (TP2)
-------------------------

This is a technical preview release. Only the **primary application JAR** file and its **dependency** files are available for download. Supporting schematics, PCB designs, and other resources can be retrieved from `svn://svn.code.sf.net/p/jxmake/code/tags/release/0.9.9-tp2`.

No individual language dictionary ***.zip** files have been released yet for the JxMake Script Editor.

### Primary Application

+ The ProgUPDI class has been improved and tested with newer AVR MCU series.

+ The development of the JxMake Script Editor, which features syntax highlighting, code folding, and a simple console, is almost complete. Most of the editor's features should be usable, though they may still be unstable and not fully functional. The console supports many ANSI escape sequences and employs custom Unicode handling in the ANSI buffer for cursor positioning and character operations.

+ Support for basic **i18n** has been added, but no **l10n** translation files are available yet.

+ Added the command-line option `--en-headless`, which is useful on Linux when the **DISPLAY** environment variable has an invalid value or the X server is not accessible.

+ Added Makefile targets `dist` and `dist_clean` to build the primary application distribution file `jxmake-x.x.x-xxx.zip` and clean the temporary directory.

+ Various enhancements, bug fixes, and cleanups.

### Hardware (Schematics, PCB Designs, Bootloaders, and Firmwares)

+ Added a new hardware module  `JxMake USB Serial Hub GLST`. The underlying hub IC supports 1‑to‑4 USB 2.0 expansion, but one downstream port is internally allocated to the built‑in USB‑to‑serial converter. As a result, three external downstream ports are available to the user.

+ Initiated development of the `Alternative Boost Converter for High Voltage Attachment II` module and its firmware.

+ Initiated development of the `JxMake Serial-WiFi Bridge` system, covering firmware and program components.

****


Technical Preview 1 (TP1)
-------------------------

This is a technical preview release. Only the **primary application JAR**  file is available for download. Supporting schematics, PCB designs, and other resources can be retrieved from `svn://svn.code.sf.net/p/jxmake/code/tags/release/0.9.9-tp1`.

### Primary Application

+ Added new programmer classes:
    + `ProgBootChip45B2`
    + `ProgBootChip45B3`
    + `ProgBootTSB`
    + `ProgBootURCLOCK`
    + `ProgBootSTM32Serial`
    + `ProgBootOpenBLT`
    + `ProgBootSAMBA`
    + `ProgBootLUFAPrinter`

+ The `SerialDevice_JxMakeUSBGPIO` class has been implemented:
    + Simple bootloaders that connect to the PC via a hardware serial port (either directly or through a USB-to-serial bridge) should support serial uploading using 'JxMake Versatile MCU Programmer II', via its serial port converter cable.
    + More complex bootloaders that impose much stricter timing constraints and/or require control signal manipulation beyond DTR would fail when used with this method.
    + To use this method, instead of specifying a string like:
      ```java
      "/dev/ttyUSB0"
      ```
      as the serial device name, use a string like:
      ```java
      "jxm:/dev/ttyACM0:/dev/ttyACM1"
      ```
      The string syntax is:
      ```java
      "jxm:<primary_serial_device>:<secondary_serial_device>"
      ```

+ The `SerialDevice_Network` class has been implemented:
    + Simple bootloaders that connect to the PC via a hardware serial port (either directly or through a USB-to-serial bridge) should support remote uploading using something like [ESP-LINK](https://github.com/jeelabs/esp-link).
    + More complex bootloaders that impose stricter timing constraints and/or require control signal manipulation would fail when used with this method.
    + All bootloaders that connect to the PC via the MCU's native USB CDC interface are, by design, incompatible with this method.
    + To use remote uploading, instead of specifying a string like:
      ```java
      "/dev/ttyUSB0"
      ```
      as the serial device name, use a string like:
      ```java
      "net:10.0.0.111:2323:console/baud?rate=%d"
      ```
      The string syntax is:
      ```java
      "net:<hostNameOrIP>:<uploadPort>:[urlSetBaudrate_printfFormat]"
      ```
      the third part, if specified, is internally prepended with `http://<hostNameOrIP>/` before the GET request is dispatched).

+ Initiated development of the JxMake Script Editor, featuring syntax highlighting, code folding, and a simple console. The editor is currently unstable, not fully functional, and not yet suitable for general use.

+ Initiated experiment with USB-based programmer devices outside the CDC-ACM class; resulting programmer class(es) from this effort will not be officially included as usable programmer classes until the minimum Java SDK requirement is raised to version 23 or later.

+ Various enhancements, bug fixes, and cleanups.

### Hardware (Schematics, PCB Designs, Bootloaders, and Firmwares)

+ Added new hardware modules (and their corresponding firmware, if applicable):
    + `JxMake High Voltage Attachment II`
    + `JxMake Versatile MCU Programmer II`
    + `JxMake UPDI Connector Converter`

+ Removed select PDF files from the PCB design output of the following legacy hardware modules to conserve space; all corresponding Gerber files have been fully retained:
    + The original `JxMake High Voltage Attachment`
    + The original `JxMake Versatile MCU Programmer`

+ Removed **ATxmega128A4U bootloaders** using the **AVR109** protocol (both the CDC-ACM and hardware UART implementations), along with the **LUFA-151115** dependency. These may be reintroduced in future releases.

+ Various enhancements, bug fixes, and cleanups.

****


Technical Preview 0 (TP0)
-------------------------

+ This is the initial technical preview release. Only the **primary application JAR** file is available for download. Supporting schematics, PCB designs, and other resources can be retrieved from `svn://svn.code.sf.net/p/jxmake/code/tags/release/0.9.9-tp0`.

+ The core functionality of the primary application (`jxm.jar`) and all hardware designs (excluding the `JxMake High Voltage Attachment II` module) are considered feature-complete and operational. However, some glitches and bugs may still be present.

+ The scripting API is not yet fully stable and may change in future releases.

**Note:**
After extracting the ZIP file, use the following commands to display the built-in help and documentation:
```sh
java -jar jxm.jar -h
java -jar jxm.jar --browse-docs
```

***
