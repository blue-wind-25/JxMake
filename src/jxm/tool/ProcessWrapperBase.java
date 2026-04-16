/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.Map;

import jxm.*;


public abstract class ProcessWrapperBase {

    public abstract boolean initialize();
    public abstract boolean available();

    public abstract boolean isAlive(final Object process);

    public abstract void destroy(final Object process);
    public abstract void destroyForcibly(final Object process);

    public abstract void setEnvironment(final Map<String,String> environment) throws Exception;
    public abstract void setDirectory(final String directory) throws Exception;
    public abstract void setCommand(final String[] command) throws Exception;
    public abstract void setConsole(final boolean console) throws Exception;
    public abstract void setInitialRows(final int initialRows) throws Exception;
    public abstract void setInitialColumns(final int initialColumns) throws Exception;
    public abstract void setWindowsAnsiColorEnabled(final boolean windowsAnsiColorEnabled) throws Exception;
    public abstract void setUseWinConPty(final boolean useWinConPty) throws Exception;
    public abstract void setUnixOpenTtyToPreserveOutputAfterTermination(final boolean unixOpenTtyToPreserveOutputAfterTermination) throws Exception;

    public abstract Object start() throws Exception;
    public abstract int waitFor(final Object process) throws Exception;
    public abstract boolean waitFor(final Object process, final int timeoutMS) throws Exception;
    public abstract int exitValue(final Object process) throws Exception;

    public abstract OutputStream getOutputStream(final Object process) throws Exception;
    public abstract InputStream getInputStream(final Object process) throws Exception;
    public abstract InputStream getErrorStream(final Object process) throws Exception;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public OutputStreamWriter getOutputStreamWriter(final Object stdProcess) throws Exception
    { return new OutputStreamWriter( getOutputStream(stdProcess), SysUtil._CharEncoding ); }

    public InputStreamReader getInputStreamReader(final Object stdProcess) throws Exception
    { return new InputStreamReader( getInputStream(stdProcess), SysUtil._CharEncoding ); }

    public InputStreamReader getErrorStreamReader(final Object stdProcess) throws Exception
    { return new InputStreamReader( getErrorStream(stdProcess), SysUtil._CharEncoding ); }

} // class ProcessWrapperBase

