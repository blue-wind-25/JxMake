/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.List;

import jxm.*;
import jxm.xb.*;


public class CommandHistory {

    private       String            _cmdHistFPath   = null;
    private       int               _commandMaxSave = 200;
    private final ArrayList<String> _commandHistory = new ArrayList<>();
    private       int               _commandIndex   = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public CommandHistory(final String cmdHistFPath, final int commandMaxSave)
    {
        _cmdHistFPath   = cmdHistFPath;
        _commandMaxSave = commandMaxSave;
    }

    public CommandHistory(final String cmdHistFPath)
    { _cmdHistFPath = cmdHistFPath; }

    public CommandHistory(final int commandMaxSave)
    { _commandMaxSave = commandMaxSave; }

    public void setFilePath(final String cmdHistFPath)
    { _cmdHistFPath = cmdHistFPath; }

    public void setFilePath(final int commandMaxSave)
    { _commandMaxSave = commandMaxSave; }

    public void save()
    {
        try {
            final OutputStreamWriter writer = new OutputStreamWriter( new FileOutputStream(_cmdHistFPath), SysUtil._CharEncoding );
            final int                size   = _commandHistory.size();
            final int                start  = Math.max(0, size - _commandMaxSave); // Only keep the last '_commandMaxSave' commands
            for(int i = start; i < size; ++i) {
                writer.write( _commandHistory.get(i) );
                writer.write( "\n" );
            }
            writer.close();
        }
        catch(final Exception e) {
            if( !(e instanceof FileNotFoundException) ) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }
    }

    public void load()
    {
        try {
            final InputStreamReader reader  = new InputStreamReader( new FileInputStream(_cmdHistFPath), SysUtil._CharEncoding );
            final BufferedReader    breader = new BufferedReader(reader);
                  String            line;
            while( (line = breader.readLine() ) != null ) {
                _commandHistory.add(line);
            }
            reader.close();
            _commandIndex = _commandHistory.size();
        }
        catch(final Exception e) {
            if( !(e instanceof FileNotFoundException) ) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void put(final String text)
    {
        // Remove the text if it is already in the command history
        if( _commandHistory.contains(text) ) _commandHistory.remove(text);

        // Add the text to the end of the command history
        _commandHistory.add(text);
        _commandIndex = _commandHistory.size();
    }

    public boolean hasPrev()
    { return !_commandHistory.isEmpty() && _commandIndex > 0; }

    public boolean hasNext()
    { return !_commandHistory.isEmpty() && _commandIndex < _commandHistory.size(); }

    public String getPrev()
    {
        if( !hasPrev() ) return null;

        // Decrement the command history index
        --_commandIndex;

        // Return the text from the command history
        return _commandHistory.get(_commandIndex);
    }

    public String getNext()
    {
        if( !hasNext() ) return null;

        // Increment the command history index
        ++_commandIndex;
        if( _commandIndex >= _commandHistory.size() ) return null;

        // Return the text from the command history
        return _commandHistory.get(_commandIndex);
    }

    public List<String> getAll()
    { return _commandHistory; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void clearAll()
    {
        _commandHistory.clear();
        _commandIndex = 0;
    }

    public void deleteFirstN(final int N)
    {
        if(N <= 0) return;

        final int count = Math.min( N, _commandHistory.size() );

        for(int i = 0; i < count; ++i) _commandHistory.remove(0);

        _commandIndex = Math.max(0, _commandIndex - count);
    }

    public void deleteLastN(final int N)
    {
        if(N <= 0) return;

        final int count = Math.min( N, _commandHistory.size() );

        for(int i = 0; i < count; ++i) _commandHistory.remove( _commandHistory.size() - 1 );

        if( _commandIndex >= _commandHistory.size() ) _commandIndex = _commandHistory.size() - 1;
    }

} // class CommandHistory
