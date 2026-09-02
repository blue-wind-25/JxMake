# Shared detached-process helper.
#
# .NET's [System.Diagnostics.Process]::Start / CreateProcess-based launch can
# leak inherited handles from further up a CI runner's process chain into a
# long-lived detached daemon (see git history on this file for the earlier,
# unsuccessful hand-rolled Win32 CreateProcess P/Invoke attempt - it hit a
# persistent, unexplained ERROR_INVALID_NAME (123) on real Windows CI across
# several independently-plausible fixes, and wasn't worth chasing further
# blind against a Windows target from a Linux dev box).
#
# Start-Process, when none of its -RedirectStandard* parameters are used,
# launches via UseShellExecute = $true (ShellExecuteExW) rather than raw
# CreateProcess. That is a different codepath that does not pass
# bInheritHandles = TRUE, so it sidesteps the handle-inheritance hang
# without needing manual Win32 interop at all.

# Spawns $FilePath $Arguments fully detached and hidden. Returns the new
# process's PID (int). The caller is responsible for the child's own output
# (e.g. the child writing its own log file), since no stdio is redirected.
function Start-DetachedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string]$Arguments
    )

    $proc = Start-Process -FilePath $FilePath -ArgumentList $Arguments `
        -WindowStyle Hidden -PassThru

    return $proc.Id
}
