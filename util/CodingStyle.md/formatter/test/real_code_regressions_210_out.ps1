function Test-Thing
{
    param(
        [string]$Name,
        [int]$Count = 1
    )

    $result = Get-ChildItem -Path $Name `
        -Recurse `
        -Force

    return $result
}
