@echo off
setlocal

if not defined PYTHON set "PYTHON=python"

where %PYTHON% >nul 2>nul
if errorlevel 1 (
    echo ERROR: Python executable "%PYTHON%" not found on PATH. >&2
    echo Install Python, or set the PYTHON environment variable to the interpreter to use. >&2
    exit /b 1
)

set "DIR=%~dp0"

%PYTHON% -c "import markdown_it, mdit_py_plugins, pygments" >nul 2>nul
if errorlevel 1 (
    echo Installing dependencies from requirements.txt ...
    %PYTHON% -m pip install -r "%DIR%requirements.txt"
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies from requirements.txt >&2
        exit /b 1
    )
)

%PYTHON% "%DIR%mdx_server.py" %*
exit /b %errorlevel%
