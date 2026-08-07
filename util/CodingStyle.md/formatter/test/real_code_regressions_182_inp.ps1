# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT.

$oneCharName = "PSOneChar"
$directory = Split-Path -Parent $MyInvocation.MyCommand.Path
$invoke = Invoke-ScriptAnalyzer $directory\AvoidUsingReservedCharOneCharNames.ps1 | Where-Object {$_.RuleName -eq $oneCharName}
$noViolations = Invoke-ScriptAnalyzer $directory\GoodCmdlet.ps1 | Where-Object {$_.RuleName -eq $oneCharName}

function Remove-Build
{
    $profileDir = [System.IO.Path]::Combine($PSScriptRoot, 'profiles')
    Copy-Item -Force $profileDir/* $targetProfileDir
    Get-Content -LiteralPath $profileDir/README.md
}
