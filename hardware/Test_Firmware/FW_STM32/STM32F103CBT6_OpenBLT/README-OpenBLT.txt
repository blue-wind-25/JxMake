====================================================================================================
OpenBLT Bootloader
====================================================================================================

The complete source code for OpenBLT bootloader can be downloaded from these repositories:

    OpenBLT SourceForge SVN (Primary)
    https://sourceforge.net/projects/openblt
    https://sourceforge.net/p/openblt/code/HEAD/tree/trunk

    OpenBLT GitHub (Mirror)
    https://github.com/feaser/openblt

    OpenBLT Running on Bluepill Plus and Blackpill Boards
    https://github.com/razielgdn/black-and-blue-pill-plus-with-openBLT

The Bootloader Design Manual and the XCP protocol specification can be read and downloaded from:

    OpenBLT
    https://www.feaser.com/openblt/doku.php?id=homepage
    https://www.feaser.com/openblt/doku.php?id=manual:design
    https://www.feaser.com/openblt/lib/exe/fetch.php?media=manual:xcp_1_0_specification.zip

~~~ Last accessed & checked on 2025-09-21 ~~~

----------------------------------------------------------------------------------------------------

The  OpenBLT bootloader is licensed under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the License, or any later version:

    https://www.feaser.com/en/openblt.php#licensing

JxMake is not linked with any OpenBLT binary and does not copy or use code from the OpenBLT source
code.

----------------------------------------------------------------------------------------------------

The precompiled binaries of the OpenBLT bootloader used here were built using GCC ARM Embedded
Toolchain version 10.3 (gcc-arm-none-eabi-10.3). They are based on source code from
'OpenBLT Running on Bluepill Plus and Blackpill Boards', and were subsequently updated using
source code from the OpenBLT GitHub repository referenced above.

The firmware file 'openblt_stm32f103_uart1_57600.bin' was built using the following compile-time
defines:
    #define BOOT_COM_RS232_ENABLE          (1)
    #define BOOT_COM_RS232_BAUDRATE        (57600)
    #define BOOT_COM_RS232_TX_MAX_DATA     (129)
    #define BOOT_COM_RS232_RX_MAX_DATA     (129)
    #define BOOT_COM_RS232_CHANNEL_INDEX   (0)
    #define BOOT_COM_RS232_CS_TYPE         (0)
    #define BOOT_COM_CAN_ENABLE            (0)
    #define BOOT_NVM_CHECKSUM_HOOKS_ENABLE (0)
    #define BOOT_XCP_SEED_KEY_ENABLE       (0)

The firmware file 'openblt_stm32f103_uart1_57600_s55_k54.bin' was built using the following compile-time
defines:
    #define BOOT_COM_RS232_ENABLE          (1)
    #define BOOT_COM_RS232_BAUDRATE        (57600)
    #define BOOT_COM_RS232_TX_MAX_DATA     (129)
    #define BOOT_COM_RS232_RX_MAX_DATA     (129)
    #define BOOT_COM_RS232_CHANNEL_INDEX   (0)
    #define BOOT_COM_RS232_CS_TYPE         (0)
    #define BOOT_COM_CAN_ENABLE            (0)
    #define BOOT_NVM_CHECKSUM_HOOKS_ENABLE (0)
    #define BOOT_XCP_SEED_KEY_ENABLE       (1)
The seed is { 0x55 } and the key is { 0x54 }.

The firmware file 'openblt_stm32f103_uart1_57600_s55_k54_cs.bin' was built using the following compile-time
defines:
    #define BOOT_COM_RS232_ENABLE          (1)
    #define BOOT_COM_RS232_BAUDRATE        (57600)
    #define BOOT_COM_RS232_TX_MAX_DATA     (129)
    #define BOOT_COM_RS232_RX_MAX_DATA     (129)
    #define BOOT_COM_RS232_CHANNEL_INDEX   (0)
    #define BOOT_COM_RS232_CS_TYPE         (1)
    #define BOOT_COM_CAN_ENABLE            (0)
    #define BOOT_NVM_CHECKSUM_HOOKS_ENABLE (0)
    #define BOOT_XCP_SEED_KEY_ENABLE       (1)
The seed is { 0x55 } and the key is { 0x54 }.

----------------------------------------------------------------------------------------------------

You can program the BIN files using the following commands:

    export OPENOCD_HOME="<your_openocd_root_directory>"

    "$OPENOCD_HOME/bin/openocd"                                             \
        "-f" "$OPENOCD_HOME/scripts/interface/stlink.cfg"                   \
        "-c" "set CPUTAPID 0"                                               \
        "-c" "transport select hla_swd"                                     \
        "-c" "adapter speed 2000"                                           \
        "-f" "$OPENOCD_HOME/scripts/target/stm32f1x.cfg"                    \
        "-c" "program <firmware_file_name.bin> 0x8000000 verify reset exit"

====================================================================================================

