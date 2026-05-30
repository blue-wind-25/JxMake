#Requires -Version 5.1
<#
.SYNOPSIS
    Include file -- do not execute directly.
    Provides path-absolutization helpers and Process-JavacArgs for converting
    relative paths in javac arguments to absolute paths before forwarding to
    the compile-server daemon.  PowerShell equivalent of _javac_args.sh.
    Dot-sourced by javac-client.ps1 and javac-daemon-wrapper.ps1.

.NOTES
    Usage:
        . "$PSScriptRoot\_javac_args.ps1"
        # $ClientCwd must be set before calling Process-JavacArgs.
        $ClientCwd = $PWD.Path
        $args = Process-JavacArgs $CompileArgs
#>

# Convert a single path to absolute, using $ClientCwd as the base.
function Abs-Path([string]$p) {
    if ([System.IO.Path]::IsPathRooted($p)) { return $p }
    return [System.IO.Path]::GetFullPath([System.IO.Path]::Combine($ClientCwd, $p))
}

# Convert a semicolon-separated classpath string to absolute paths.
function Abs-Cp([string]$cp) {
    return ($cp -split ';' | ForEach-Object { Abs-Path $_ }) -join ';'
}

# Rewrite all path-bearing javac arguments to absolute paths.
# Returns a [string[]] with the rewritten argument list.
function Process-JavacArgs([string[]]$javacArgs) {
    $out      = [System.Collections.Generic.List[string]]::new()
    $nextMode = ''
    $cpFlags  = @('-cp','-classpath','--class-path','-sourcepath','--source-path',
                  '-processorpath','--processor-path','-bootclasspath','--boot-class-path',
                  '-extdirs','-endorseddirs','-modulepath','--module-path',
                  '-mp','-p','--upgrade-module-path')
    $dirFlags = @('-d','-s','-h')

    foreach ($a in $javacArgs) {
        if ($nextMode -ne '') {
            if ($nextMode -eq 'cp')     { $out.Add((Abs-Cp   $a)) }
            else                         { $out.Add((Abs-Path $a)) }
            $nextMode = ''
        } elseif ($dirFlags -contains $a) {
            $out.Add($a); $nextMode = 'single'
        } elseif ($cpFlags -contains $a) {
            $out.Add($a); $nextMode = 'cp'
        } elseif ($a -match '^@(.+)') {
            $out.Add('@' + (Abs-Path $Matches[1]))
        } elseif ($a -match '^-') {
            $out.Add($a)
        } else {
            $out.Add((Abs-Path $a))
        }
    }
    return $out.ToArray()
}
