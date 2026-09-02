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
# NOTE: deliberately NOT using [Parameter()] attributes here (which would
# make this an "advanced" script and enable PowerShell's common parameters
# -Debug/-Verbose/etc, with unique-prefix matching). javac flags like -d
# and -verbose would then collide: "-d" uniquely prefix-matches "-Debug"
# and gets silently consumed as a switch instead of reaching $CompileArgs.
# Using plain param()/$args keeps binding literal.
param()
$CompileArgs = $args

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ── Includes ──────────────────────────────────────────────────────────────────

. "$PSScriptRoot\_port.ps1"
. "$PSScriptRoot\_javac_args.ps1"

# ── Configuration ─────────────────────────────────────────────────────────────
$TmpDir = $env:TEMP

# ── Sentinel constants ────────────────────────────────────────────────────────
# PowerShell strings are UTF-16 and can contain US (char 0x1F) natively
$US         = [char]0x1F
$SEN_ENDINP = "${US}ENDINP${US}"

# ── Locate JDK ────────────────────────────────────────────────────────────────

$ScriptDir = $PSScriptRoot

# Prefer the JDK that contains this script (installed in bin dir),
# then JAVA_HOME, then whatever java is on PATH
if (Test-Path (Join-Path $ScriptDir 'java.exe')) {
    $JdkBin = $ScriptDir
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $JdkBin = Join-Path $env:JAVA_HOME 'bin'
} else {
    $JavaExe = Get-Command java -ErrorAction SilentlyContinue
    if (-not $JavaExe) {
        [Console]::Error.WriteLine('ERROR: Cannot locate JDK. Set JAVA_HOME or install a JDK.')
        exit 1
    }
    $JdkBin = Split-Path -Parent $JavaExe.Source
}

$JavaBin   = Join-Path $JdkBin 'java.exe'
$JavacReal = Join-Path $JdkBin 'javac.real.exe'
$JarPath   = Join-Path $ScriptDir 'compile-server.jar'

# ── Port derivation ───────────────────────────────────────────────────────────

$Port = Get-JavaMajorPort -JavaBin $JavaBin

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
            return $true   # Daemon is alive
        } catch {
            Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        }
    }

    if (Test-PortOpen $Port) { return $true }

    # Start daemon as a detached process.
    # Deliberately NOT setting RedirectStandardOutput/Error/Input: doing so
    # forces .NET's CreateProcess call to pass bInheritHandles=TRUE, which
    # inherits *every* inheritable handle this process holds into the
    # daemon - not just the ones we explicitly redirect. Several hops up
    # a CI runner's process chain (cmd.exe -> this powershell.exe -> java),
    # one of those inherited handles is the pipe the runner uses to capture
    # this very step's output; the daemon outliving the step then holds
    # that pipe's write end open forever, so the runner's reader never sees
    # EOF and the step hangs indefinitely even after every real command has
    # finished. CompileServer.java writes its own log file directly, so we
    # don't need OS-level output piping here at all.
    $psi                 = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName        = $JavaBin
    $psi.Arguments       = "-Djava.io.tmpdir=`"$TmpDir`" -jar `"$JarPath`" $Port"
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow  = $true
    $proc                = [System.Diagnostics.Process]::Start($psi)

    # Wait up to 5 s for the daemon to open the port
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Milliseconds 500
        if (Test-PortOpen $Port) {
            $proc.Id |
                Set-Content $PidFile
            return $true
        }
    }

    [Console]::Error.WriteLine("WARNING: javac daemon failed to start on port $Port.")
    [Console]::Error.WriteLine("         Check $LogFile")
    return $false
}

# ── Absolutize path arguments relative to client's working directory ──────────
# The server daemon runs in a fixed CWD; convert all relative paths to absolute
# so the server can locate source files, output dirs, and classpath entries

$ClientCwd = $PWD.Path

$ProcessedArgs = Process-JavacArgs $CompileArgs

# ── Compilation ───────────────────────────────────────────────────────────────

function Invoke-ViaDaemon {
    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect('127.0.0.1', $Port)
        $stream           = $client.GetStream()
        # [System.Text.Encoding]::UTF8 (the static instance) includes a UTF-8 BOM
        # preamble, which StreamWriter silently writes as the first 3 bytes onto
        # the socket -- corrupting whichever argument lands first in the stream
        # into something the daemon reads back as an unrecognized flag. Use a
        # UTF8Encoding instance with encoderShouldEmitUTF8Identifier = $false.
        $enc              = New-Object System.Text.UTF8Encoding($false)
        $writer           = New-Object System.IO.StreamWriter($stream, $enc)
        $writer.AutoFlush = $true
        $reader           = New-Object System.IO.StreamReader($stream, $enc)

        foreach ($arg in $ProcessedArgs) { $writer.WriteLine($arg) }
        $writer.WriteLine($SEN_ENDINP)
        $writer.Flush()
        $client.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)

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

        foreach ($l in $stdoutLines) { [Console]::Out.WriteLine($l) }
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
