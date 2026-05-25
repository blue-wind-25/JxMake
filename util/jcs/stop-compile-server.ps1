#Requires -Version 5.1
<#
.SYNOPSIS
    Stops a running javac compile daemon.
    PowerShell equivalent of stop-compile-server.sh.

.PARAMETER Port
    TCP port of the daemon to stop.  Use 0 to auto-derive from the active
    JDK major version.  Omit entirely to stop ALL running daemons.

.PARAMETER JavaBin
    Used only when Port is 0, to determine the Java major version.
    Defaults to java on PATH or JAVA_HOME.

.EXAMPLE
    stop-compile-server.cmd 21000
    stop-compile-server.cmd 0
    stop-compile-server.cmd          # stops all daemons
#>
param(
    [Parameter(Position=0)]
    [string]$Port = '',

    [Parameter(Position=1)]
    [string]$JavaBin = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Configuration ─────────────────────────────────────────────────────────────
$TmpDir = $env:TEMP

# ── Helpers ───────────────────────────────────────────────────────────────────

function Resolve-JavaPort {
    param([string]$Java)
    if (-not $Java) {
        $Java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
    }
    $verLines = & $Java '-version' 2>&1
    $verStr   = $verLines[0].ToString()
    if ($verStr -match '"1\.(\d+)') { return [int]$Matches[1] * 1000 }
    if ($verStr -match '"(\d+)')    { return [int]$Matches[1] * 1000 }
    throw "Cannot parse Java version from: $verStr"
}

function Stop-OneDaemon {
    param([int]$P)
    $PidFile = Join-Path $TmpDir "javac-daemon-$P.pid"

    if (-not (Test-Path $PidFile)) {
        Write-Host "No PID file found for port $P ($PidFile). Daemon may not be running."
        return $false
    }

    $daemonPid = [int](Get-Content $PidFile -ErrorAction SilentlyContinue)

    $proc = $null
    try {
        $proc = Get-Process -Id $daemonPid -ErrorAction Stop
    } catch {
        Write-Host "Daemon for port $P (PID $daemonPid) is not running. Removing stale PID file."
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return $false
    }

    Write-Host "Stopping javac daemon on port $P (PID $daemonPid)..."
    $proc.Kill()

    # Wait up to 3 s for clean exit
    if ($proc.WaitForExit(3000)) {
        Write-Host 'Stopped.'
    } else {
        Write-Host 'Daemon did not exit cleanly within 3 s; process was forcibly terminated.'
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    return $true
}

# ── Main ──────────────────────────────────────────────────────────────────────

if ($Port -eq '') {
    # Stop all daemons
    $pidFiles = Get-Item (Join-Path $TmpDir 'javac-daemon-*.pid') `
        -ErrorAction SilentlyContinue
    if (-not $pidFiles) {
        Write-Host 'No running javac daemons found.'
        exit 0
    }
    $stopped = 0
    foreach ($f in $pidFiles) {
        if ($f.Name -match 'javac-daemon-(\d+)\.pid') {
            $p = [int]$Matches[1]
            if (Stop-OneDaemon -P $p) { $stopped++ }
        }
    }
    if ($stopped -eq 0) { Write-Host 'No running javac daemons found.' }
} elseif ([int]$Port -eq 0) {
    $p = Resolve-JavaPort -Java $JavaBin
    Stop-OneDaemon -P $p
} else {
    Stop-OneDaemon -P ([int]$Port)
}
