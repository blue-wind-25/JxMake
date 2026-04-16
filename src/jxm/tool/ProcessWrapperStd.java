/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.Map;

import java.util.concurrent.TimeUnit;


public class ProcessWrapperStd extends ProcessWrapperBase {

    private ProcessBuilder _stdPB = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setEnvironment(final Map<String, String> environment)
    {
        _stdPB.environment().clear();
        _stdPB.environment().putAll(environment);
    }

    @Override
    public void setDirectory(final String directory)
    { _stdPB.directory( new File(directory) ); }

    @Override
    public void setCommand(final String[] command)
    { _stdPB.command(command); }

    @Override
    public void setConsole(final boolean console)
    { /* Nothing to do here */ }

    @Override
    public void setInitialRows(final int initialRows)
    { /* Nothing to do here */ }

    @Override
    public void setInitialColumns(final int initialColumns)
    { /* Nothing to do here */ }

    @Override
    public void setWindowsAnsiColorEnabled(final boolean windowsAnsiColorEnabled)
    { /* Nothing to do here */ }

    @Override
    public void setUseWinConPty(final boolean useWinConPty)
    { /* Nothing to do here */ }

    @Override
    public void setUnixOpenTtyToPreserveOutputAfterTermination(final boolean unixOpenTtyToPreserveOutputAfterTermination)
    { /* Nothing to do here */ }

    @Override
    public Object start() throws IOException
    { return _stdPB.start(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isAlive(final Object stdProcess)
    { return ( (Process) stdProcess ).isAlive(); }

    @Override
    public void destroy(final Object stdProcess)
    { ( (Process) stdProcess ).destroy(); }

    @Override
    public void destroyForcibly(final Object stdProcess)
    { ( (Process) stdProcess ).destroyForcibly(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public OutputStream getOutputStream(final Object stdProcess)
    {  return ( (Process) stdProcess ).getOutputStream(); }

    @Override
    public InputStream getInputStream(final Object stdProcess)
    {  return ( (Process) stdProcess ).getInputStream(); }

    @Override
    public InputStream getErrorStream(final Object stdProcess)
    {  return ( (Process) stdProcess ).getErrorStream(); }

    @Override
    public int waitFor(final Object stdProcess) throws InterruptedException
    {  return ( (Process) stdProcess ).waitFor(); }

    @Override
    public boolean waitFor(final Object stdProcess, final int timeoutMS) throws InterruptedException
    {  return ( (Process) stdProcess ).waitFor(timeoutMS, TimeUnit.MILLISECONDS); }

    @Override
    public int exitValue(final Object stdProcess)
    {  return ( (Process) stdProcess ).exitValue(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean initialize()
    {
        _stdPB = new ProcessBuilder();

        return true;
    }

    @Override
    public boolean available()
    { return _stdPB != null; }

} // class ProcessWrapperStd

