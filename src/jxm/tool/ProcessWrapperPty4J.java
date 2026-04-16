/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.lang.ReflectiveOperationException;

import java.lang.reflect.Method;

import java.util.Map;

import java.util.concurrent.TimeUnit;

import jxm.*;
import jxm.xb.*;


/*
 * Pty4J - Pseudo terminal (PTY) implementation in Java
 * https://github.com/JetBrains/pty4j
 *
 * Copyright (c) JetBrains and contributors
 * https://github.com/JetBrains/pty4j/graphs/contributors
 *
 * Licensed under the Eclipse Public License v1.0
 * https://www.eclipse.org/legal/epl-v10.html
 * https://www.eclipse.org/legal/epl/epl-v10.html
 *
 * For the full license text, see the '../../../3rd_party_library_licenses' folder.
 */
public class ProcessWrapperPty4J extends ProcessWrapperBase {

    private Object _pty4jPBInst                                            = null;
    private Method _pty4jPB_setEnvironment                                 = null;
    private Method _pty4jPB_setDirectory                                   = null;
    private Method _pty4jPB_setCommand                                     = null;
    private Method _pty4jPB_setConsole                                     = null;
    private Method _pty4jPB_setInitialRows                                 = null;
    private Method _pty4jPB_setInitialColumns                              = null;
    private Method _pty4jPB_setWindowsAnsiColorEnabled                     = null;
    private Method _pty4jPB_setUseWinConPty                                = null;
    private Method _pty4jPB_setUnixOpenTtyToPreserveOutputAfterTermination = null;
    private Method _pty4jPB_start                                          = null;

    private Method _pty4jPR_getOutputStream                                = null;
    private Method _pty4jPR_getInputStream                                 = null;
    private Method _pty4jPR_getErrorStream                                 = null;
    private Method _pty4jPR_waitFor                                        = null;
    private Method _pty4jPR_waitForWithTimeout                             = null;
    private Method _pty4jPR_exitValue                                      = null;
    private Method _pty4jPR_destroy                                        = null;
    private Method _pty4jPR_destroyForcibly                                = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setEnvironment(final Map<String, String> environment) throws ReflectiveOperationException
    { _pty4jPB_setEnvironment.invoke(_pty4jPBInst, environment); }

    @Override
    public void setDirectory(final String directory) throws ReflectiveOperationException
    { _pty4jPB_setDirectory.invoke(_pty4jPBInst, directory); }

    @Override
    public void setCommand(final String[] command) throws ReflectiveOperationException
    { _pty4jPB_setCommand.invoke( _pty4jPBInst, (Object) command ); }

    @Override
    public void setConsole(final boolean console) throws ReflectiveOperationException
    { _pty4jPB_setConsole.invoke(_pty4jPBInst, console); }

    @Override
    public void setInitialRows(final int initialRows) throws ReflectiveOperationException
    { _pty4jPB_setInitialRows.invoke(_pty4jPBInst, initialRows); }

    @Override
    public void setInitialColumns(final int initialColumns) throws ReflectiveOperationException
    { _pty4jPB_setInitialColumns.invoke(_pty4jPBInst, initialColumns); }

    @Override
    public void setWindowsAnsiColorEnabled(final boolean windowsAnsiColorEnabled) throws ReflectiveOperationException
    { _pty4jPB_setWindowsAnsiColorEnabled.invoke(_pty4jPBInst, windowsAnsiColorEnabled); }

    @Override
    public void setUseWinConPty(final boolean useWinConPty) throws ReflectiveOperationException
    { _pty4jPB_setUseWinConPty.invoke(_pty4jPBInst, useWinConPty); }

    @Override
    public void setUnixOpenTtyToPreserveOutputAfterTermination(final boolean unixOpenTtyToPreserveOutputAfterTermination) throws ReflectiveOperationException
    { _pty4jPB_setUnixOpenTtyToPreserveOutputAfterTermination.invoke(_pty4jPBInst, unixOpenTtyToPreserveOutputAfterTermination); }

    @Override
    public Object start() throws ReflectiveOperationException, IOException
    { return _pty4jPB_start.invoke(_pty4jPBInst); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public OutputStream getOutputStream(final Object pty4jProcess) throws ReflectiveOperationException
    { return (OutputStream) _pty4jPR_getOutputStream.invoke(pty4jProcess); }

    @Override
    public InputStream getInputStream(final Object pty4jProcess) throws ReflectiveOperationException
    { return (InputStream) _pty4jPR_getInputStream.invoke(pty4jProcess); }

    @Override
    public InputStream getErrorStream(final Object pty4jProcess) throws ReflectiveOperationException
    { return (InputStream) _pty4jPR_getErrorStream.invoke(pty4jProcess); }

    @Override
    public int waitFor(final Object pty4jProcess) throws ReflectiveOperationException
    { return (Integer) _pty4jPR_waitFor.invoke(pty4jProcess); }

    @Override
    public boolean waitFor(final Object pty4jProcess, final int timeoutMS) throws ReflectiveOperationException, InterruptedException
    { return (Boolean) _pty4jPR_waitForWithTimeout.invoke(pty4jProcess, timeoutMS, TimeUnit.MILLISECONDS); }

    @Override
    public int exitValue(final Object pty4jProcess) throws ReflectiveOperationException
    { return (Integer) _pty4jPR_exitValue.invoke(pty4jProcess); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isAlive(final Object pty4jProcess)
    { return ( (Process) pty4jProcess ).isAlive(); }

    @Override
    public void destroy(final Object pty4jProcess)
    { ( (Process) pty4jProcess ).destroy(); }

    @Override
    public void destroyForcibly(final Object pty4jProcess)
    { ( (Process) pty4jProcess ).destroyForcibly(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean initialize()
    {
        try {
            // Try to load the class dynamically
            final Class<?> builderClass = Class.forName("com.pty4j.PtyProcessBuilder");
            final Class<?> processClass = Class.forName("com.pty4j.PtyProcess"       );

            _pty4jPBInst                                            = builderClass.getDeclaredConstructor().newInstance();
            _pty4jPB_setEnvironment                                 = builderClass.getMethod("setEnvironment"                                , Map     .class                );
            _pty4jPB_setDirectory                                   = builderClass.getMethod("setDirectory"                                  , String  .class                );
            _pty4jPB_setCommand                                     = builderClass.getMethod("setCommand"                                    , String[].class                );
            _pty4jPB_setConsole                                     = builderClass.getMethod("setConsole"                                    , boolean .class                );
            _pty4jPB_setInitialRows                                 = builderClass.getMethod("setInitialRows"                                , Integer .class                );
            _pty4jPB_setInitialColumns                              = builderClass.getMethod("setInitialColumns"                             , Integer .class                );
            _pty4jPB_setWindowsAnsiColorEnabled                     = builderClass.getMethod("setWindowsAnsiColorEnabled"                    , boolean .class                );
            _pty4jPB_setUseWinConPty                                = builderClass.getMethod("setUseWinConPty"                               , boolean .class                );
            _pty4jPB_setUnixOpenTtyToPreserveOutputAfterTermination = builderClass.getMethod("setUnixOpenTtyToPreserveOutputAfterTermination", boolean .class                );
            _pty4jPB_start                                          = builderClass.getMethod("start"                                                                         );

            _pty4jPR_getOutputStream                                = processClass.getMethod("getOutputStream"                                                               );
            _pty4jPR_getInputStream                                 = processClass.getMethod("getInputStream"                                                                );
            _pty4jPR_getErrorStream                                 = processClass.getMethod("getErrorStream"                                                                );
            _pty4jPR_waitFor                                        = processClass.getMethod("waitFor"                                                                       );
            _pty4jPR_waitForWithTimeout                             = processClass.getMethod("waitFor"                                       , long    .class, TimeUnit.class);
            _pty4jPR_exitValue                                      = processClass.getMethod("exitValue"                                                                     );

            return true;
        }
        catch(final Throwable e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean available()
    { return _pty4jPBInst != null; }

} // class ProcessWrapperPty4J
