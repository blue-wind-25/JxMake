#Requires -Version 5.1
<#
.SYNOPSIS
    Drop-in javac replacement that routes compilation through a running
    compile-server daemon.  PowerShell equivalent of javac-client.sh.

.DESCRIPTION
    The daemon must already be running (use start-compile-server.ps1).
    Does NOT auto-start the server -- fails clearly if the daemon is down.

    Protocol: US-wrapped sentinel lines (US+TAG+US) separate stdout,
    stderr, and exit-code sections in the server response.  PowerShell
    strings handle US (U+001F) natively via [char]0x1F.

.PARAMETER Port
    TCP port the daemon is listening on.  Pass 0 to auto-derive from the
    active JDK major version (major * 1000).

.PARAMETER CompileArgs
    All remaining arguments are forwarded as-is to javac.

.EXAMPLE
    javac-client.cmd 21000 -cp libs\*.jar -d build\classes src\Main.java
    javac-client.cmd 0     -cp libs\*.jar -d build\classes src\Main.java
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Port,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CompileArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Includes ──────────────────────────────────────────────────────────────────

. "$PSScriptRoot\_port.ps1"
. "$PSScriptRoot\_javac_args.ps1"

# ── Sentinel constants ────────────────────────────────────────────────────────
# PowerShell strings are UTF-16 and can contain US (char 0x1F) natively
$US         = [char]0x1F
$SEN_ENDINP = "${US}ENDINP${US}"

# ── Port resolution ───────────────────────────────────────────────────────────

$resolvedPort = [int]$Port
if ($resolvedPort -eq 0) {
    $javaBin      = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
    $resolvedPort = Get-JavaMajorPort -JavaBin $javaBin
}

# ── Connectivity check ────────────────────────────────────────────────────────

$tcpTest = $null
try {
    $tcpTest = New-Object System.Net.Sockets.TcpClient
    $tcpTest.Connect('127.0.0.1', $resolvedPort)
    $tcpTest.Close()
} catch {
    [Console]::Error.WriteLine("ERROR: javac daemon is not running on port $resolvedPort.")
    [Console]::Error.WriteLine("       Start it with: start-compile-server.cmd $resolvedPort")
    exit 1
} finally {
    if ($tcpTest) { $tcpTest.Dispose() }
}

# ── Absolutize path arguments relative to client's working directory ──────────
# The server daemon runs in a fixed CWD; convert all relative paths to absolute
# so the server can locate source files, output dirs, and classpath entries

$ClientCwd = $PWD.Path

$processedArgs = Process-JavacArgs $CompileArgs

# ── Send request and receive framed response ──────────────────────────────────

$client = $null
try {
    $client = New-Object System.Net.Sockets.TcpClient
    $client.Connect('127.0.0.1', $resolvedPort)
    $stream           = $client.GetStream()
    $enc              = [System.Text.Encoding]::UTF8
    $writer           = New-Object System.IO.StreamWriter($stream, $enc)
    $writer.AutoFlush = $true
    $reader           = New-Object System.IO.StreamReader($stream, $enc)

    # Send each absolutized javac argument on its own line, then the ENDINP sentinel
    foreach ($arg in $processedArgs) {
        $writer.WriteLine($arg)
    }
    $writer.WriteLine($SEN_ENDINP)
    $writer.Flush()

    # Signal end of sending so the server's readLine() sees EOF-after-sentinel
    $client.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)

    # Read framed response sections.
    # switch -Regex: `u{001F} in a .NET regex pattern matches U+001F (Unit Separator).
    $mode        = 'none'
    $stdoutLines = [System.Collections.Generic.List[string]]::new()
    $stderrLines = [System.Collections.Generic.List[string]]::new()
    $exitCode    = 1

    $line = $null
    while ($null -ne ($line = $reader.ReadLine())) {
        switch -Exact ($line) {
            "`u{001F}STDOUT`u{001F}" { $mode = 'stdout'; break }
            "`u{001F}STDERR`u{001F}" { $mode = 'stderr'; break }
            "`u{001F}EXTCOD`u{001F}" { $mode = 'extcod'; break }
            default {
                switch ($mode) {
                    'stdout' { $stdoutLines.Add($line) }
                    'stderr' { $stderrLines.Add($line) }
                    'extcod' { $exitCode = [int]$line }
                }
            }
        }
    }
} finally {
    if ($client) { $client.Dispose() }
}

# ── Output -- preserve javac's stdout/stderr separation ───────────────────────

foreach ($l in $stdoutLines) { [Console]::Out.WriteLine($l) }
foreach ($l in $stderrLines) { [Console]::Error.WriteLine($l) }

exit $exitCode
