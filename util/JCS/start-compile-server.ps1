#Requires -Version 5.1
<#
.SYNOPSIS
    Starts the javac compile daemon for a given port and JDK.
    PowerShell equivalent of start-compile-server.sh.

.PARAMETER Port
    TCP port for the daemon.  Use 0 to auto-derive from the JDK major
    version (major * 1000, e.g. JDK 21 -> 21000).

.PARAMETER JavaBin
    Path to the java.exe to use.  Defaults to java on PATH or JAVA_HOME.

.EXAMPLE
    start-compile-server.cmd 21000
    start-compile-server.cmd 0
    start-compile-server.cmd 0 "C:\jdk21\bin\java.exe"
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Port,

    [Parameter(Position=1)]
    [string]$JavaBin = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Includes ──────────────────────────────────────────────────────────────────

. "$PSScriptRoot\_port.ps1"

# ── Configuration ─────────────────────────────────────────────────────────────
$TmpDir = $env:TEMP

# ── Resolve java binary ───────────────────────────────────────────────────────

if (-not $JavaBin) {
    if ($env:JAVA_HOME) {
        $JavaBin = Join-Path $env:JAVA_HOME 'bin\java.exe'
    } else {
        $JavaBin = 'java'
    }
}

# ── Port resolution ───────────────────────────────────────────────────────────

$verStr       = (& $JavaBin '-version' 2>&1)[0].ToString()
$resolvedPort = [int]$Port
if ($resolvedPort -eq 0) {
    $resolvedPort = Get-JavaMajorPort -JavaBin $JavaBin
    Write-Host "Auto-derived port $resolvedPort from Java version: $verStr"
}

# ── Locate compile-server.jar ─────────────────────────────────────────────────

$ScriptDir = $PSScriptRoot
$JarPath   = Join-Path $ScriptDir 'compile-server.jar'

if (-not (Test-Path $JarPath)) {
    [Console]::Error.WriteLine("ERROR: $JarPath not found. Run build-server.sh first.")
    exit 1
}

$PidFile = Join-Path $TmpDir "javac-daemon-$resolvedPort.pid"
$LogFile = Join-Path $TmpDir "javac-daemon-$resolvedPort.log"

# ── Already running? ──────────────────────────────────────────────────────────

if (Test-Path $PidFile) {
    $existingPid = [int](Get-Content $PidFile -ErrorAction SilentlyContinue)
    try {
        $null = Get-Process -Id $existingPid -ErrorAction Stop
        Write-Host "Daemon already running on port $resolvedPort (PID $existingPid)"
        exit 0
    } catch {
        Write-Host 'Stale PID file found, removing.'
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
}

# Check if port is already in use by something else
try {
    $t = New-Object System.Net.Sockets.TcpClient
    $t.Connect('127.0.0.1', $resolvedPort)
    $t.Close()
    [Console]::Error.WriteLine("ERROR: Something is already listening on port $resolvedPort.")
    exit 1
} catch {
    # Port is free -- proceed
}

# ── Start daemon ──────────────────────────────────────────────────────────────

Write-Host "Starting javac daemon on port $resolvedPort using $verStr..."

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName        = $JavaBin
$psi.Arguments       = "-Djava.io.tmpdir=`"$TmpDir`" -jar `"$JarPath`" $resolvedPort"
$psi.UseShellExecute = $false
$psi.CreateNoWindow  = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError  = $true

$proc = [System.Diagnostics.Process]::Start($psi)

# Drain output asynchronously to log file
$logWriter = [System.IO.StreamWriter]::new($LogFile, $false,
    [System.Text.Encoding]::UTF8)
$logWriter.AutoFlush = $true
$proc.OutputDataReceived += { param($s,$e); if ($e.Data) { $logWriter.WriteLine($e.Data) } }
$proc.ErrorDataReceived  += { param($s,$e); if ($e.Data) { $logWriter.WriteLine($e.Data) } }
$proc.BeginOutputReadLine()
$proc.BeginErrorReadLine()

# Wait up to 5 s for the port to open
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Milliseconds 500
    try {
        $t = New-Object System.Net.Sockets.TcpClient
        $t.Connect('127.0.0.1', $resolvedPort)
        $t.Close()
        $proc.Id | Set-Content $PidFile
        Write-Host "Daemon started (PID $($proc.Id)), listening on port $resolvedPort"
        Write-Host "Log: $LogFile"
        exit 0
    } catch { }
}

[Console]::Error.WriteLine("ERROR: Daemon did not start within 5 seconds. Check $LogFile")
exit 1
