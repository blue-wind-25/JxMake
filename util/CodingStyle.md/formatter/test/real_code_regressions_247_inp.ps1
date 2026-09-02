function Get-JavaMajorPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaBin
    )
    # java -version writes to stderr; merging it via 2>&1 turns each line into
    # an ErrorRecord.
    $verLines = & $JavaBin '-version' 2>&1
    return $verLines
}
