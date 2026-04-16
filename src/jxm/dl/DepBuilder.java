/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.io.IOException;

import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class DepBuilder {

    private static void _buildDepList_impl(final DepList depList, final String sourceFilePath, final ArrayList<String> absCIncludePaths, final ArrayList<String> absJavaClassPaths) throws IOException
    {
        // Create a dependency reader instance
        final DepReader depReader = DepReader.newDepReader(sourceFilePath, absCIncludePaths, absJavaClassPaths);
        if(depReader == null) return;

        // Store the source file path as a dependency
        depList.addDepFile( depReader.filePath() );

        // Loop until no more dependency paths can be read
        ArrayList<String> recDepPaths = new ArrayList<>();

        while(true) {

            // Try to read one path
            final String depPath = depReader.readOneDepPath();
            if(depPath == null) break;

            // Skip if the path is already in the final list
            if( depList.hasDepFile(depPath) ) continue;

            // Add the path to the final list
            depList.addDepFile(depPath);

            // Add the path to the recursive list
            recDepPaths.add(depPath);

        } // while true

        // Recursively visit each dependency path
        for(final String path : recDepPaths) _buildDepList_impl(depList, path, absCIncludePaths, absJavaClassPaths);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static DepList buildDepList(final String sourceFilePath, final ArrayList<String> absCIncludePaths, final ArrayList<String> absJavaClassPaths) throws IOException
    {
        // Create a dependency list instance
        final DepList depList = new DepList();

        // Recursively build the dependency list
        _buildDepList_impl(depList, sourceFilePath, absCIncludePaths, absJavaClassPaths);

        // Done
        return depList;
    }

    public static DepList buildDepList(final ArrayList<String> sourceFilePaths, final ArrayList<String> absCIncludePaths, final ArrayList<String> absJavaClassPaths) throws IOException
    {
        // Create a dependency list instance
        final DepList depList = new DepList();

        // Recursively build the dependency list
        for(final String sourceFilePath : sourceFilePaths) {
            _buildDepList_impl(depList, sourceFilePath, absCIncludePaths, absJavaClassPaths);
        }

        // Done
        return depList;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void loadBuildDepList(final ArrayList<String> depFileList, final ArrayList<String> sourceFilePaths, final ArrayList<String> absCIncludePaths, final ArrayList<String> absJavaClassPaths)
    {
        // Clear the destination list first
        depFileList.clear();

        // Create a dependency list instance
        final DepList depList = new DepList();

        // Recursively build the dependency list
        for(final String sourceFilePath : sourceFilePaths) {
            // Try to load from the cache file first
            try {
                // Load from file
                depList.loadFromFile( SysUtil.getCacheFileName_dependencyList(sourceFilePath) );
                // Clear if the list is already out of date
                if( depList.needsRebuilt() ) depList.clear();
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Clear the dependency list
                depList.clear();
            }
            // (Re-)build the list as needed
            if( depList.isEmpty() ) {
                try {
                    // Build the list
                    _buildDepList_impl(depList, sourceFilePath, absCIncludePaths, absJavaClassPaths);
                    // Save to file
                    try {
                        depList.saveToFile( SysUtil.getCacheFileName_dependencyList(sourceFilePath) );
                    }
                    catch(final Exception e) {
                        // Print the stack trace if requested
                        if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    }
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Clear the dependency list
                    depList.clear();
                }
            }
            // Add the items to the destination list
            depList.addDepFilesTo(depFileList);
        } // for
    }

} // class DepBuilder
