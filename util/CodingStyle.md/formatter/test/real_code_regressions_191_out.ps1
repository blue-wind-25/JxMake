Get-Something |
    ForEach-Object {
    ("a").ForEach( { $_ }) |
        Write-Output
}
