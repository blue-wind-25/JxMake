The 'libusb0.sys' files below (one per architecture) were obtained from the libusb-win32 project's
binary release:

    libusb-win32-bin-1.4.0.2.zip
    https://github.com/mcuee/libusb-win32/releases

        x86/libusb0.sys
        amd64/libusb0.sys
        arm64/libusb0.sys

    Dual-licensed under the GNU GPL v3 or the GNU LGPL v3 - see
    '<JxMake_Source_Root>/3rd_party_library_licenses/LICENSE_GPLv3.txt' and 'LICENSE_LGPLv3.txt'
    (also shipped alongside this distribution as 'jxmake_dist/3rd_party_library_licenses/', see
    'make dist' in '<JxMake_Source_Root>/src/Makefile'). The release archive's own
    COPYING_GPL.txt/COPYING_LGPL.txt are text-identical to those, aside from an http->https FSF
    URL change, so they are not duplicated here.

The '.inf' that binds this driver to a specific VID/PID is generated at install time by
generateLibusb0Inf() in WindowsDriverInstaller.java - not the release archive's own template INF.
