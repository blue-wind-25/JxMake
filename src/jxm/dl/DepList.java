/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayList;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jxm.*;


public class DepList {

    private       String          _depFilePath = null;
    private final TreeSet<String> _depFileList = new TreeSet<>(); // NOTE : Sorted

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public DepList()
    {}

    public void addDepFile(final String absPath)
    { _depFileList.add(absPath); }

    public boolean hasDepFile(final String absPath)
    { return _depFileList.contains(absPath); }

    public ArrayList<String> getDepFiles()
    { return new ArrayList<String>(_depFileList); }

    public void addDepFilesTo(final ArrayList<String> depFileList)
    { for(final String item : _depFileList) depFileList.add(item); }

    public void getDepFilesTo(final ArrayList<String> depFileList)
    {
        depFileList.clear();
        addDepFilesTo(depFileList);
    }

    public void clear()
    {
        _depFileList.clear();
        _depFilePath = null;
    }

    public boolean isEmpty()
    { return _depFileList.isEmpty(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void saveToFile(final String absPath) throws IOException
    {
        // Open the file
        final BufferedWriter bfw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(absPath), SysUtil._CharEncoding ) );

        // Write the data
        for(final String line : _depFileList) {
            bfw.write(line);
            bfw.newLine();
        }

        // Flush and close the file
        bfw.flush();
        bfw.close();
    }

    public void loadFromFile(final String absPath) throws IOException
    {
        // Clear first
        clear();

        // Open the file
        final BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream(absPath), SysUtil._CharEncoding ) );

        // Read the data
        while(true) {

            // Read one line
            final String line = bfr.readLine();
            if(line == null) break;

            // Store the line is it is not empty
            if( line.length() != 0 ) _depFileList.add(line);

        } // while true

        // Close the file
        bfr.close();

        // Save the file name
        _depFilePath = absPath;
    }

    public boolean needsRebuilt()
    {
        if(_depFilePath == null) return true;

        return !SysUtil.isPathMoreRecent(_depFilePath, _depFileList);
    }

} // class DepList
