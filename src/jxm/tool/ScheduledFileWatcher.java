/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.nio.file.attribute.FileTime;

import jxm.*;
import jxm.xb.*;


public abstract class ScheduledFileWatcher {

    public static FileTime FallbackFileTime = FileTime.fromMillis(0);

    public enum Reason {
        Modified, Deleted, Created, Overflow
    }

    private volatile boolean                     _running       = true;
    private final    ScheduledExecutorService    _scheduler;
    private final    Map< Path, Set<FileEntry> > _monitoredDirs = new ConcurrentHashMap<>();
    private final    WatchService                _watchService;

    private static class FileEntry {

        public final    Path    filePath;
        public final    Object  userObject;
        public volatile boolean suspended = false;

        public FileEntry(final Path filePath, final Object userObject)
        {
            this.filePath   = filePath;
            this.userObject = userObject;
        }

    } // class FileEntry

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    public ScheduledFileWatcher(final int poolRate_mS) throws IOException
    {
        this._watchService = FileSystems.getDefault().newWatchService();
        this._scheduler    = Executors.newSingleThreadScheduledExecutor();

        this._scheduler.scheduleAtFixedRate( this::_pollEvents, 0, poolRate_mS, TimeUnit.MILLISECONDS );
    }

    public synchronized void shutdown()
    {
        unregisterAll();

        _running = false;

        _scheduler.shutdownNow();

        try {
            _watchService.close();
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    protected abstract void fileChanged(final String absFilePath, final Object userObject, final FileTime lastModifiedTime, final Reason reason);
    protected abstract void postFileChanges();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void registerFile(final String absFilePath, final Object userObject) throws IOException
    {
        if( !SysUtil.pathIsValid(absFilePath) ) return;

        /*
        SysUtil.stdDbg().printf("registerFile(%s)\n", absFilePath);
        //*/

        final Path file = Paths.get(absFilePath);
        final Path dir  = file.getParent();

        if( !_monitoredDirs.containsKey(dir) ) {
            dir.register(
                _watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            );
            _monitoredDirs.put( dir, Collections.synchronizedSet( new HashSet<>() ) );
        }

        final Set<FileEntry> files = _monitoredDirs.get(dir);

        for(final FileEntry entry : files) {
            if( entry.filePath.equals(file) /*&& entry.userObject == userObject*/ ) return; // Already exists
        }
        files.add( new FileEntry(file, userObject) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void unregisterFile(final String absFilePath)
    {
        final Path file = Paths.get(absFilePath);

        _monitoredDirs.values().forEach( set -> set.removeIf( entry -> entry.filePath.equals(file) ) );
    }

    public synchronized void unregisterFile(final Object userObject)
    { _monitoredDirs.values().forEach( set -> set.removeIf( entry -> entry.userObject == userObject ) ); }

    public synchronized void unregisterDir(final String absDirPath)
    {
        final Path dir = Paths.get(absDirPath);

        _monitoredDirs.remove(dir);
    }

    public synchronized void unregisterAll()
    { _monitoredDirs.clear(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void suspendFile(final String absFilePath)
    {
        final Path file = Paths.get(absFilePath);

        _monitoredDirs.values().forEach(
            set -> set.stream()
                      .filter ( entry -> entry.filePath.equals(file) )
                      .forEach( entry -> entry.suspended = true      )
        );
    }

    public synchronized void suspendFile(final Object userObject)
    {
        _monitoredDirs.values().forEach(
            set -> set.stream()
                      .filter ( entry -> entry.userObject == userObject )
                      .forEach( entry -> entry.suspended = true         )
        );
    }

    public synchronized void suspendDir(final String absDirPath)
    {
        final Path           dir   = Paths.get(absDirPath);
        final Set<FileEntry> files = _monitoredDirs.get(dir);

        if(files != null) files.forEach(entry -> entry.suspended = true);
    }

    public synchronized void suspendAll()
    { _monitoredDirs.values().forEach( set -> set.forEach(entry -> entry.suspended = true) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void resumeFile(final String absFilePath)
    {
        final Path file = Paths.get(absFilePath);

        _monitoredDirs.values().forEach(
            set -> set.stream()
                      .filter ( entry -> entry.filePath.equals(file) )
                      .forEach( entry -> entry.suspended = false     )
        );
    }

    public synchronized void resumeFile(final Object userObject)
    {
        _monitoredDirs.values().forEach(
            set -> set.stream()
                      .filter ( entry -> entry.userObject == userObject )
                      .forEach( entry -> entry.suspended = false        )
        );
    }

    public synchronized void resumeDir(final String absDirPath)
    {
        final Path            dir  = Paths.get(absDirPath);
        final Set<FileEntry> files = _monitoredDirs.get(dir);

        if(files != null) files.forEach(entry -> entry.suspended = false);
    }

    public synchronized void resumeAll()
    { _monitoredDirs.values().forEach( set -> set.forEach(entry -> entry.suspended = false) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void clearEvents()
    {
        WatchKey key;

        while( ( key = _watchService.poll() ) != null ) {
            key.pollEvents(); // Drain events
            key.reset();      // Re-arm the key for future events
        }
    }

    private synchronized void _pollEvents()
    {
        if(!_running) return;

        WatchKey key;
        boolean  any = false;

        while( ( key = _watchService.poll() ) != null ) {

            final Path           dir   = (Path) key.watchable();
            final Set<FileEntry> files = _monitoredDirs.get(dir);

            if(files == null) {
                key.reset();
                continue;
            }

            for( final WatchEvent<?> event : key.pollEvents() ) {

                final Path   changed = dir.resolve( (Path) event.context() );
                      Reason reason;

                     if( event.kind() == StandardWatchEventKinds.ENTRY_CREATE ) reason = Reason.Created;
                else if( event.kind() == StandardWatchEventKinds.ENTRY_DELETE ) reason = Reason.Deleted;
                else if( event.kind() == StandardWatchEventKinds.ENTRY_MODIFY ) reason = Reason.Modified;
                else                                                            reason = Reason.Overflow;

                for(final FileEntry entry : files) {

                    /*
                    SysUtil.stdDbg().printf("CHANGED='%s' REF=['%s']'%s'\n", changed, dir, entry.filePath);
                    //*/

                    if( entry.filePath.equals(changed) && !entry.suspended ) {

                        FileTime lastModified = null;

                        try {
                            if(reason != Reason.Deleted) lastModified = Files.getLastModifiedTime(changed);
                        }
                        catch(final IOException e) {
                            // Print the stack trace if requested
                            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                            // Use the fallback time
                            lastModified = FallbackFileTime;
                        }

                        fileChanged( changed.toAbsolutePath().toString(), entry.userObject, lastModified, reason );
                        any = true;

                    }

                } // for

            } // for

            key.reset();

        } // while

        if(any) postFileChanges();
    }

} // class ScheduledFileWatcher

