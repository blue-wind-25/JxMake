This directory contains third-party pre-built Windows kernel driver binaries ('*.sys') used by
'<JxMake_Source_Root>/src/jxm/WindowsDriverInstaller.java' to install generic USB access drivers
(libusbK, libusb0) for specific VID/PID hardware IDs. The '.inf' files that bind these drivers to a
particular VID/PID are generated at install time by WindowsDriverInstaller.java itself (see
generateLibusbKInf()/generateLibusb0Inf()) - only the driver binaries are stored here.

See each subdirectory's own README.txt for the exact source/version/license of its '.sys' files.

'make dist' (see '<JxMake_Source_Root>/src/Makefile') copies this whole directory into
'jxmake_dist/windows_driver' for distribution. At runtime, SysUtil.findWindowsDriverDir() searches
both the 'jxmake_dist/windows_driver' distribution location and this '3rd_party/windows_driver'
source-tree location (for test runs from the source tree) - see that method for the exact search order.
