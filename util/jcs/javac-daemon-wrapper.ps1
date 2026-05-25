#Requires -Version 5.1
<#
.SYNOPSIS
    Transparent javac replacement that auto-starts the compile daemon for
    the current JDK and routes compilation through it.
    PowerShell equivalent of javac-daemon-wrapper.sh.

.DESCRIPTION
    Installation (transparent drop-in on Windows):
      1. Rename the real compiler:
           Rename-Item "$env:JAVA_HOME\bin\javac.exe" javac.real.exe
      2. Copy this script and its .cmd launcher into the same directory:
           Copy-Item javac-daemon-wrapper.ps1 "$env:JAVA_HOME\bin\"
           Copy-Item javac-daemon-wrapper.cmd "$env:JAVA_HOME\bin\"
           # The .cmd file shadows javac.exe because cmd.exe resolves
           # .cmd before .exe when both are on PATH.
      3. Copy compile-server.jar into the same directory.

    When any tool invokes "javac", cmd.exe finds javac-daemon-wrapper.cmd
    first, which runs this script.  The script starts or reuses the daemon
    and forwards the compilation.  javac.real.exe is the fallback.

    Port is derived from the Java major version: major * 1000.

.PARAMETER CompileArgs
    Arguments passed directly to javac (captured from %* via the .cmd launcher).

.EXAMPLE
    # After installation, normal javac invocations are automatically proxied:
    javac -cp libs\*.jar -d build\classes src\Main.java
#>
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$CompileArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Configuration ─────────────────────────────────────────────────────────────
$TmpDir = $env:TEMP

# ── Sentinel constants ────────────────────────────────────────────────────────
# PowerShell strings are UTF-16 and can contain US (char 0x1F) natively.
$US         = [char]0x1F
$SEN_ENDINP = "${US}ENDINP${US}"

# ── Locate JDK ────────────────────────────────────────────────────────────────

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Prefer the JDK that contains this script (installed in bin dir),
# then JAVA_HOME, then whatever java is on PATH.
if (Test-Path (Join-Path $ScriptDir 'java.exe')) {
    $JdkBin  = $ScriptDir
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $JdkBin  = Join-Path $env:JAVA_HOME 'bin'
} else {
    $JavaExe = Get-Command java -ErrorAction SilentlyContinue
    if (-not $JavaExe) {
        [Console]::Error.WriteLine('ERROR: Cannot locate JDK. Set JAVA_HOME or install a JDK.')
        exit 1
    }
    $JdkBin = Split-Path -Parent $JavaExe.Source
}

$JavaBin    = Join-Path $JdkBin 'java.exe'
$JavacReal  = Join-Path $JdkBin 'javac.real.exe'
$JarPath    = Join-Path $ScriptDir 'compile-server.jar'

# ── Port derivation ───────────────────────────────────────────────────────────

function Get-MajorPort {
    param([string]$Java)
    $verLines = & $Java '-version' 2>&1
    $verStr   = $verLines[0].ToString()
    if ($verStr -match '"1\.(\d+)') { return [int]$Matches[1] * 1000 }
    if ($verStr -match '"(\d+)')    { return [int]$Matches[1] * 1000 }
    throw "Cannot parse Java version from: $verStr"
}

$Port = Get-MajorPort -Java $JavaBin

# ── Daemon management ─────────────────────────────────────────────────────────

function Test-PortOpen {
    param([int]$P)
    try {
        $t = New-Object System.Net.Sockets.TcpClient
        $t.Connect('127.0.0.1', $P)
        $t.Close()
        return $true
    } catch { return $false }
}

function Start-Daemon {
    if (-not (Test-Path $JarPath)) {
        [Console]::Error.WriteLine("ERROR: compile-server.jar not found at $JarPath")
        [Console]::Error.WriteLine('       Run build-server.sh first.')
        return $false
    }

    $PidFile = Join-Path $TmpDir "javac-daemon-$Port.pid"
    $LogFile = Join-Path $TmpDir "javac-daemon-$Port.log"

    # Check existing PID
    if (Test-Path $PidFile) {
        $existingPid = [int](Get-Content $PidFile -ErrorAction SilentlyContinue)
        try {
            $proc = Get-Process -Id $existingPid -ErrorAction Stop
            return $true   # daemon is alive
        } catch {
            Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        }
    }

    if (Test-PortOpen $Port) { return $true }

    # Start daemon as a detached process
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName         = $JavaBin
    $psi.Arguments        = "-Djava.io.tmpdir=`"$TmpDir`" -jar `"$JarPath`" $Port"
    $psi.UseShellExecute  = $false
    $psi.CreateNoWindow   = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError  = $true
    $proc = [System.Diagnostics.Process]::Start($psi)

    # Asynchronously drain output to the log file (fire-and-forget)
    $logStream = [System.IO.StreamWriter]::new($LogFile, $false,
        [System.Text.Encoding]::UTF8)
    $proc.OutputDataReceived += { param($s,$e); if ($e.Data) { $logStream.WriteLine($e.Data) } }
    $proc.ErrorDataReceived  += { param($s,$e); if ($e.Data) { $logStream.WriteLine($e.Data) } }
    $proc.BeginOutputReadLine()
    $proc.BeginErrorReadLine()

    # Wait up to 5 s for the daemon to open the port
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Milliseconds 500
        if (Test-PortOpen $Port) {
            $proc.Id | Set-Content $PidFile
            return $true
        }
    }

    [Console]::Error.WriteLine("WARNING: javac daemon failed to start on port $Port.")
    [Console]::Error.WriteLine("         Check $LogFile")
    return $false
}

# ── Compilation ───────────────────────────────────────────────────────────────

function Invoke-ViaDaemon {
    $client = $null
    try {
        $client  = New-Object System.Net.Sockets.TcpClient
        $client.Connect('127.0.0.1', $Port)
        $stream  = $client.GetStream()
        $enc     = [System.Text.Encoding]::UTF8
        $writer  = New-Object System.IO.StreamWriter($stream, $enc)
        $writer.AutoFlush = $true
        $reader  = New-Object System.IO.StreamReader($stream, $enc)

        foreach ($arg in $CompileArgs) { $writer.WriteLine($arg) }
        $writer.WriteLine($SEN_ENDINP)
        $writer.Flush()
        $client.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)

        $mode        = 'none'
        $stdoutLines = [System.Collections.Generic.List[string]]::new()
        $stderrLines = [System.Collections.Generic.List[string]]::new()
        $exitCode    = 1

        $line = $null
        while ($null -ne ($line = $reader.ReadLine())) {
            switch -Regex ($line) {
                "\u001FSTDOUT\u001F" { $mode = 'stdout'; break }
                "\u001FSTDERR\u001F" { $mode = 'stderr'; break }
                "\u001FEXTCOD\u001F" { $mode = 'extcod'; break }
                default {
                    switch ($mode) {
                        'stdout' { $stdoutLines.Add($line) }
                        'stderr' { $stderrLines.Add($line) }
                        'extcod' { $exitCode = [int]$line  }
                    }
                }
            }
        }

        foreach ($l in $stdoutLines) { [Console]::Out.WriteLine($l)   }
        foreach ($l in $stderrLines) { [Console]::Error.WriteLine($l) }
        exit $exitCode
    } finally {
        if ($client) { $client.Dispose() }
    }
}

function Invoke-FallbackReal {
    if (Test-Path $JavacReal) {
        & $JavacReal @CompileArgs
        exit $LASTEXITCODE
    }
    [Console]::Error.WriteLine("ERROR: Daemon unavailable and $JavacReal not found.")
    exit 1
}

# ── Main ──────────────────────────────────────────────────────────────────────

if (Start-Daemon) {
    Invoke-ViaDaemon
} else {
    [Console]::Error.WriteLine('Falling back to real javac...')
    Invoke-FallbackReal
}
