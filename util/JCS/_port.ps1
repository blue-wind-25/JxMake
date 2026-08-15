#Requires -Version 5.1
<#
.SYNOPSIS
    Include file -- do not execute directly.
    Provides Get-JavaMajorPort: maps a Java binary's major version to its
    daemon port (major * 1000).  PowerShell equivalent of _port.sh.
    Dot-sourced by javac-client.ps1, javac-daemon-wrapper.ps1,
    start-compile-server.ps1, and stop-compile-server.ps1.

.NOTES
    Usage:
        . "$PSScriptRoot\_port.ps1"
        $port = Get-JavaMajorPort -JavaBin 'java'
#>

# Maps a java(.exe) binary to the daemon port for its major version.
# Returns an [int]; throws if the version string cannot be parsed.
function Get-JavaMajorPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaBin
    )
    $verLines = & $JavaBin '-version' 2>&1
    $verStr   = $verLines[0].ToString()
    if ($verStr -match '"1\.(\d+)') { return [int]$Matches[1] * 1000 }
    if ($verStr -match '"(\d+)') { return [int]$Matches[1] * 1000 }
    throw "Cannot parse Java version from: $verStr"
}
