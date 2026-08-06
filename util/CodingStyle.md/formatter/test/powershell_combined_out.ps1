#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

if ($x) {
    Write-Host "hello"
    if ($y) {
        Write-Host "world"
    }
}

$a  = 1
$bb = $a + 2

Get-ChildItem                           |
    Where-Object { $_.Length -gt 10MB } |
    Sort-Object Name

@{
    Name = "John"
    Age  = 20
}

$h = @{ Name = "Jane";Age = 30 }

switch ($x) {
    1  { "one" }
    22 { "two" }
}

# safety: pipe inside string and comment
Write-Host "a|b|c"
# Get-ChildItem|Sort-Object

# safety: here-string body left byte-identical
@"
if($z){
  Write-Host "inside"
}
"@
