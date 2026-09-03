The 'libusbK.sys' files below (one per architecture) were obtained from the libusbK project's
binary release:

    libusbK-3.1.0.0-bin-debug.7z
    https://github.com/mcuee/libusbk/releases

        x86/libusbK.sys    (from bin/sys/x86/libusbK.sys)
        amd64/libusbK.sys  (from bin/sys/amd64/libusbK.sys)

    No arm64 build is published for this driver, so libusbK install is x86/amd64 only.

    Licensed under the GNU Lesser General Public License (LGPL) - see
    '<JxMake_Source_Root>/3rd_party_library_licenses/LICENSE_LGPLv3.txt'. The release archive
    itself carries no separate license file; this is per the "(GNU LGPL)" notice in the project's
    own '.inf.in' template (libwdi, https://github.com/pbatard/libwdi, LGPL v3).

The '.inf' that binds this driver to a specific VID/PID is generated at install time by
generateLibusbKInf() in WindowsDriverInstaller.java - not the release archive's own template INF
(which this "-bin-debug" archive does not even include).
