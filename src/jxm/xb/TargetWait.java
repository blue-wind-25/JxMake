/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jxm.*;


public class TargetWait extends ExecBlock {

                                                   // ##### ??? TODO : Does this always return the maximum number of logical cores ??? #####
    private static final ExecutorService           _executor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
    private static final ArrayList< Future<Void> > _taskList = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected synchronized static void _submitTask(final Callable<Void> task)
    { _taskList.add( _executor.submit(task) ); }

    private synchronized static void _joinThreads()
    {
        for(final Future<Void> task : _taskList) {
            while( !task.isDone() ) {
                Thread.yield();
                try {
                    task.get();
                }
                catch(final InterruptedException e) {
                    // Restore state
                    Thread.currentThread().interrupt();
                    // Something happened, do not wait again for this task
                    break;
                }
                catch(final CancellationException | ExecutionException e) {
                    // Task is finished (cancelled or crashed), while loop will exit
                }
            }
        }

        _taskList.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public TargetWait(final String path, final int lNum, final int cNum)
    { super(path, lNum, cNum); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Wait until all the threads are terminated
        _joinThreads();

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    { super.saveToStream(dos); }

    public static TargetWait loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new TargetWait(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public TargetWait deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class TargetWait
