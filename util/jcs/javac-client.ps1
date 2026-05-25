#Requires -Version 5.1
<#
.SYNOPSIS
    Drop-in javac replacement that routes compilation through a running
    compile-server daemon.  PowerShell equivalent of javac-client.sh.

.DESCRIPTION
    The daemon must already be running (use start-compile-server.ps1).
    Does NOT auto-start the server -- fails clearly if the daemon is down.

    Protocol: NUL-wrapped sentinel lines (NUL+TAG+NUL) separate stdout,
    stderr, and exit-code sections in the server response.  PowerShell
    strings handle NUL (U+0000) natively via [char]0.

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
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Port,

    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$CompileArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Sentinel constants ────────────────────────────────────────────────────────
# PowerShell strings are UTF-16 and can contain NUL (char 0) natively.
$NUL          = [char]0
$SEN_ENDINP   = "${NUL}ENDINP${NUL}"
$SEN_STDOUT   = "${NUL}STDOUT${NUL}"
$SEN_STDERR   = "${NUL}STDERR${NUL}"
$SEN_EXTCOD   = "${NUL}EXTCOD${NUL}"

# ── Port resolution ───────────────────────────────────────────────────────────

function Get-JavaMajorPort {
    param([string]$JavaBin = 'java')
    $verLines = & $JavaBin '-version' 2>&1
    $verStr   = $verLines[0].ToString()
    if ($verStr -match '"1\.(\d+)') {
        return [int]$Matches[1] * 1000
    } elseif ($verStr -match '"(\d+)') {
        return [int]$Matches[1] * 1000
    }
    throw "Cannot parse Java version from: $verStr"
}

$resolvedPort = [int]$Port
if ($resolvedPort -eq 0) {
    $javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
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

# ── Send request and receive framed response ──────────────────────────────────

$client = $null
try {
    $client  = New-Object System.Net.Sockets.TcpClient
    $client.Connect('127.0.0.1', $resolvedPort)
    $stream  = $client.GetStream()
    $enc     = [System.Text.Encoding]::UTF8
    $writer  = New-Object System.IO.StreamWriter($stream, $enc)
    $writer.AutoFlush = $true
    $reader  = New-Object System.IO.StreamReader($stream, $enc)

    # Send each javac argument on its own line, then the ENDINP sentinel
    foreach ($arg in $CompileArgs) {
        $writer.WriteLine($arg)
    }
    $writer.WriteLine($SEN_ENDINP)
    $writer.Flush()

    # Signal end of sending so the server's readLine() sees EOF-after-sentinel
    $client.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)

    # Read framed response sections
    $mode        = 'none'
    $stdoutLines = [System.Collections.Generic.List[string]]::new()
    $stderrLines = [System.Collections.Generic.List[string]]::new()
    $exitCode    = 1

    $line = $null
    while ($null -ne ($line = $reader.ReadLine())) {
        switch ($line) {
            $SEN_STDOUT { $mode = 'stdout'; break }
            $SEN_STDERR { $mode = 'stderr'; break }
            $SEN_EXTCOD { $mode = 'extcod'; break }
            default {
                switch ($mode) {
                    'stdout' { $stdoutLines.Add($line) }
                    'stderr' { $stderrLines.Add($line) }
                    'extcod' { $exitCode = [int]$line  }
                }
            }
        }
    }
} finally {
    if ($client) { $client.Dispose() }
}

# ── Output — preserve javac's stdout/stderr separation ───────────────────────

foreach ($l in $stdoutLines) { [Console]::Out.WriteLine($l)   }
foreach ($l in $stderrLines) { [Console]::Error.WriteLine($l) }

exit $exitCode
