# Shared native-process helper.
#
# .NET's [System.Diagnostics.Process]::Start always passes bInheritHandles
# for its underlying CreateProcess call in a way that can leak *every*
# inheritable handle the calling process holds into the child - not just
# ones explicitly redirected. For a long-lived detached daemon spawned deep
# in a CI runner's process chain (cmd.exe -> powershell.exe -> java.exe),
# one of those inherited handles can be the pipe the runner uses to capture
# a step's own output; the daemon outliving the step then holds that pipe's
# write end open forever, so the runner's reader never sees EOF and the
# step hangs indefinitely even after every real command has finished.
# This is worse under Windows PowerShell 5.1 (.NET Framework), which lacks
# the handle-inheritance-list restriction modern .NET Core / pwsh added.
#
# The only fully reliable fix is to call Win32 CreateProcess directly with
# bInheritHandles = FALSE, so the child inherits nothing, period.

if (-not ('JCS.Native' -as [type])) {
    Add-Type -Namespace JCS -Name Native -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)]
public struct PROCESS_INFORMATION {
    public IntPtr hProcess;
    public IntPtr hThread;
    public int dwProcessId;
    public int dwThreadId;
}

[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
public struct STARTUPINFO {
    public int cb;
    public string lpReserved;
    public string lpDesktop;
    public string lpTitle;
    public int dwX;
    public int dwY;
    public int dwXSize;
    public int dwYSize;
    public int dwXCountChars;
    public int dwYCountChars;
    public int dwFillAttribute;
    public int dwFlags;
    public short wShowWindow;
    public short cbReserved2;
    public IntPtr lpReserved2;
    public IntPtr hStdInput;
    public IntPtr hStdOutput;
    public IntPtr hStdError;
}

// lpCommandLine MUST be a mutable buffer, not a read-only string: when
// lpApplicationName is NULL, CreateProcess parses the executable name out
// of lpCommandLine itself and writes a null terminator into the buffer at
// the split point. Marshaling it as `string` hands CreateProcess a
// read-only/interned buffer, which corrupts that in-place parse and
// reliably fails with ERROR_INVALID_NAME (123) rather than crashing
// outright. StringBuilder marshals as a writable buffer, which is what
// CreateProcess actually requires here.
[DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
public static extern bool CreateProcess(
    string lpApplicationName,
    System.Text.StringBuilder lpCommandLine,
    IntPtr lpProcessAttributes,
    IntPtr lpThreadAttributes,
    bool bInheritHandles,
    uint dwCreationFlags,
    IntPtr lpEnvironment,
    string lpCurrentDirectory,
    ref STARTUPINFO lpStartupInfo,
    out PROCESS_INFORMATION lpProcessInformation);

[DllImport("kernel32.dll", SetLastError = true)]
public static extern bool CloseHandle(IntPtr hObject);
'@
}

# Spawns $CommandLine fully detached, with bInheritHandles = $false, so the
# new process cannot end up holding any of this process's inherited pipe
# or console handles open. Returns the new process's PID (int). The caller
# is responsible for redirecting the child's own output (e.g. the child
# writing its own log file), since no stdio handles are passed at all.
function Start-DetachedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string]$Arguments
    )

    $commandLine = New-Object System.Text.StringBuilder("`"$FilePath`" $Arguments")

    $si    = New-Object JCS.Native+STARTUPINFO
    $si.cb = [System.Runtime.InteropServices.Marshal]::SizeOf([type][JCS.Native+STARTUPINFO])
    $pi    = New-Object JCS.Native+PROCESS_INFORMATION

    # CREATE_NO_WINDOW = 0x08000000, DETACHED_PROCESS = 0x00000008
    $CREATE_NO_WINDOW = 0x08000000
    $DETACHED_PROCESS = 0x00000008
    $creationFlags     = $CREATE_NO_WINDOW -bor $DETACHED_PROCESS

    $ok = [JCS.Native]::CreateProcess(
        $null, $commandLine, [IntPtr]::Zero, [IntPtr]::Zero,
        $false, $creationFlags, [IntPtr]::Zero, $null,
        [ref]$si, [ref]$pi
    )

    if (-not $ok) {
        $err = [System.Runtime.InteropServices.Marshal]::GetLastWin32Error()
        throw "CreateProcess failed (Win32 error $err) for: $commandLine"
    }

    $childPid = $pi.dwProcessId
    [JCS.Native]::CloseHandle($pi.hProcess) | Out-Null
    [JCS.Native]::CloseHandle($pi.hThread)  | Out-Null

    return $childPid
}
