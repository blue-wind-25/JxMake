/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;

import jxm.*;


public class DepReader_C extends DepReader {

    private static final Pattern     _pmInclude        = Pattern.compile("#include\\s*(?:(<)\\s*|(\"))(.+?)(?:\"|\\s*>)"                                   ); // Plain header file - #include <...>
                                                                                                                                                              //                   - #include "..."

    private static final Pattern     _pmCons_HdrU      = Pattern.compile("import\\s*(?:(<)\\s*|(\"))(.+?)(?:\"|\\s*>)\\s*;"                                ); // Consumer          -  header_unit
    private static final Pattern     _pmCons_RMod      = Pattern.compile("(?<!export)\\s*module\\s+([^:\\r\\n<\"]+)\\s*;"                                  ); // Consumer          -  module
    private static final Pattern     _pmCons_OMod_RPar = Pattern.compile("import\\s+([^:\\r\\n<\"]+)?((?:\\s*):(?:\\s*)[^:\\r\\n<\"]+)?\\s*;"              ); // Consumer          - [module] :partition
    private static final Pattern _pmConsProd_OMod_OPar = Pattern.compile("export\\s+import\\s+([^:\\r\\n<\"]+)?((?:\\s*):(?:\\s*)[^:\\r\\n<\"]+)?\\s*;"    ); // Consumer/Producer - [module][:partition]
    private static final Pattern     _pmProd_RMod_OPar = Pattern.compile("(?:export\\s+)?module\\s+([^:\\r\\n<\"]+)((?:\\s*):(?:\\s*)[^:\\r\\n<\"]+)?\\s*;"); //          Producer -  module [:partition]
    private static final Pattern     _pmProd_RMod_RPar = Pattern.compile("module\\s+([^:\\r\\n<\"]+)((?:\\s*):(?:\\s*)[^:\\r\\n<\"]+)\\s*;"                ); //          Producer -  module  :partition

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static enum ModType {
        ExportPrimaryInterface,
        ExportModuleImplementation,
        ExportImport,
        Import
    }

    private static final class ModSpec implements Comparable<ModSpec> {
        public final ModType type;

        public final String  name;
        public final String  partition;

        public final boolean headerLocal;
        public final String  headerUnit;

        public ModSpec(final ModType type_, final String name_, final String partition_)
        {
            type        = type_;

            name        = (name_      == null) ? "" : name_     ;
            partition   = (partition_ == null) ? "" : partition_.substring(1);

            headerLocal =  false;
            headerUnit  =  "";
        }

        public ModSpec(final boolean headerLocal_, final String headerUnit_)
        {
            type        = ModType.Import;

            name        = "";
            partition   = "";

            headerLocal = headerLocal_;
            headerUnit  = headerUnit_;
        }

        public ModSpec(final ModSpec refModSpec, final String name_)
        {
            type        = refModSpec.type;

            name        = name_;
            partition   = refModSpec.partition;

            headerLocal = refModSpec.headerLocal;
            headerUnit  = refModSpec.headerUnit;
        }

        public ModSpec toImportSpec(final ModType modType)
        {
            if( !headerUnit.isEmpty() ) return this;

            return new ModSpec(modType, this.name, ':' + this.partition);
        }

        @Override
        public int compareTo(final ModSpec ref)
        {
            int c;

            c = type.compareTo(ref.type);
            if(c != 0) return c;

            c = name.compareTo(ref.name);
            if(c != 0) return c;

            c = partition.compareTo(ref.partition);
            if(c != 0) return c;

            if(!headerLocal &&  ref.headerLocal) return -1;
            if( headerLocal && !ref.headerLocal) return  1;

            return headerUnit.compareTo(ref.headerUnit);
        }

        @Override
        public String toString()
        {
            if( !headerUnit.isEmpty() ) {
                if(headerLocal) return String.format("[I-HU] %-15s", '"' + headerUnit + '"');
                else            return String.format("[I-HU] %-15s", '<' + headerUnit + '>');
            }

            String typeStr = null;

            switch(type) {
                case ExportPrimaryInterface     : typeStr = "E-PI"; break;
                case ExportModuleImplementation : typeStr = "E-MI"; break;
                case ExportImport               : typeStr = "E-IM"; break;
                case Import                     : typeStr = "I-MO"; break;
            }

            return String.format("[%s] %-20s : %-20s", typeStr, name, partition);
        }
    }

    @SuppressWarnings("serial")
    private static class SpecModN extends TreeMap<String, String> {}; // NOTE : Sorted

    @SuppressWarnings("serial")
    private static class Consumer extends TreeMap< String, TreeSet<ModSpec> > { // NOTE : Sorted
        public void put(final String path, final ModSpec modSpec)
        {
            // Add to the current set if the key exist
            TreeSet<ModSpec> entry = this.get(path);

            if(entry != null) {
                entry.add(modSpec);
                return;
            }

            // Otherwise, create and store a new entry
            entry = new TreeSet<>();

            entry.add(modSpec);
            this.put(path, entry);
        }

        public void dump()
        {
            for( final Map.Entry< String, TreeSet<ModSpec> > entry : this.entrySet() ) {
                boolean first = true;
                SysUtil.stdOut().printf( "%70s => ", entry.getKey() );
                for( final ModSpec modSpec : entry.getValue() ) {
                    if(!first) SysUtil.stdOut().printf( "%70s => ", "" );
                    first = false;
                    SysUtil.stdOut().printf( "%s\n", modSpec.toString() );
                }
                SysUtil.stdOut().println();
            }
            SysUtil.stdOut().println();
        }
    };

    @SuppressWarnings("serial")
    private static class Producer extends TreeMap< ModSpec, TreeSet<String> > { // NOTE : Sorted
        public void put(final ModSpec modSpec, final String path)
        {
            // Add to the current set if the key exist
            TreeSet<String> entry = this.get(modSpec);

            if(entry != null) {
                entry.add(path);
                return;
            }

            // Otherwise, create and store a new entry
            entry = new TreeSet<>();

            entry.add(path);
            this.put(modSpec, entry);
        }

        public void dump()
        {
            for( final Map.Entry< ModSpec, TreeSet<String> > entry : this.entrySet() ) {
                boolean first = true;
                SysUtil.stdOut().printf( "%s => ", entry.getKey().toString() );
                for( final String path : entry.getValue() ) {
                    if(!first) SysUtil.stdOut().printf( "%50s => ", "" );
                    first = false;
                    SysUtil.stdOut().printf( "%s\n", path );
                }
                SysUtil.stdOut().println();
            }
            SysUtil.stdOut().println();
        }
    };

    private static void _cpp20GenDepend_impl(final String depOutFilePath, final String sourceDirPath, final String objFileExt) throws IOException
    {
        // List the source directory path
        final List<String> files = SysUtil.cu_lsfile_rec( SysUtil.resolveAbsolutePath(sourceDirPath), _pmCppSrcExt );

        // Instantiate the producer and consumer maps
        final SpecModN smod = new SpecModN();
        final Producer prod = new Producer();
        final Consumer cons = new Consumer();

        // Process the files
        for(final String file : files) {

            // Use a 'DepReader_C' instance to read the lines
            final DepReader_C dr = new DepReader_C(file, null);
                  Matcher     m = null;

            // Process the line
            while(true) {

                // Read one line
                String line = dr._readLine_CppJava();
                if(line == null) break;

                // Flag
                boolean gotMatch = false;

                // NOTE : Do not change the if blocks order/location !!!

                // Check for: Consumer - import <|" header_unit "|>
                if(!gotMatch) {
                    m = _pmCons_HdrU.matcher(line);
                    if( m.matches() ) {
                        // Matches 'import <...>;'
                        if( m.group(1) != null && m.group(1).charAt(0) == '<' ) {
                            cons.put( file, new ModSpec( false, m.group(3) ) );
                        }
                        // Matches 'import "...";'
                        else if( m.group(2) != null && m.group(2).charAt(0) == '"' ) {
                            cons.put( file, new ModSpec( true, m.group(3) ) );
                        }
                        gotMatch = true;
                    }
                }

                // Check for: Consumer - module
                if(!gotMatch) {
                    m = _pmCons_RMod.matcher(line);
                    if( m.matches() ) {
                        cons.put( file, new ModSpec( ModType.ExportModuleImplementation, m.group(1), null ) );
                        gotMatch = true;
                    }
                }

                // Check for: Consumer - import [module]:partition
                if(!gotMatch) {
                    m = _pmCons_OMod_RPar.matcher(line);
                    if( m.matches() ) {
                        if( m.group(1) == null ) cons.put( file, new ModSpec( ModType.Import, smod.get(file), m.group(2) ) );
                        else                     cons.put( file, new ModSpec( ModType.Import, m.group(1)    , m.group(2) ) );
                        gotMatch = true;
                    }
                }

                // Check for: Consumer/Producer - export import [module][:partition]
                if(!gotMatch) {
                    m = _pmConsProd_OMod_OPar.matcher(line);
                    if( m.matches() ) {
                        // Skip if both the module and partition names are empty
                        if( m.group(1) == null && m.group(2) == null ) continue;
                        // Store the entry
                        final ModSpec modeSpec = ( m.group(1) == null )
                                               ? new ModSpec( ModType.ExportImport, smod.get(file), m.group(2) )
                                               : new ModSpec( ModType.ExportImport, m.group(1)    , m.group(2) );
                        cons.put(file    , modeSpec);
                        prod.put(modeSpec, file    );
                        gotMatch = true;
                    }
                }

                // Check for: Producer -  [export] module[:partition]
                if(!gotMatch) {
                    m = _pmProd_RMod_OPar.matcher(line);
                    if( m.matches() ) {
                        final boolean hasExport = ( line.indexOf("export") == 0 );
                        smod.put( file, m.group(1) );
                        prod.put( new ModSpec( hasExport ? ModType.ExportPrimaryInterface : ModType.ExportModuleImplementation, m.group(1), m.group(2) ), file );
                        gotMatch = true;
                    }
                }

                // Check for: Producer -  module:partition
                if(!gotMatch) {
                    m = _pmProd_RMod_RPar.matcher(line);
                    if( m.matches() ) {
                        prod.put( new ModSpec( ModType.ExportModuleImplementation, m.group(1), m.group(2) ), file );
                        gotMatch = true;
                    }
                }

                // Terminate the file read if the token '(' or '{' is found
                if(!gotMatch) {
                    if( line.indexOf('(') >= 0 || line.indexOf('{') >= 0 ) break;
                }

            } // while true

        } // for file

        // Resolve empty module names
        for( final Map.Entry< String, TreeSet<ModSpec> > entry : cons.entrySet() ) {
            // Prepare the flag
            boolean repeat = false;
            // Loop until all empty module names in the current entry are resolved
            do {
                // Reset the flag
                repeat = false;
                // Walk through the specifications
                final TreeSet<ModSpec> modSpecs = entry.getValue();
                for(final ModSpec modSpec : modSpecs) {
                    // Skip if it is a header unit
                    if( !modSpec.headerUnit.isEmpty() ) continue;
                    // Only process if the module name is empty but the partition name is not empty
                    if( modSpec.name.isEmpty() && !modSpec.partition.isEmpty() ) {
                        // Get the specified module name
                        final String modName = smod.get( entry.getKey() );
                        if(modName != null) {
                            // Replace the entry with a new entry with now resolved module name
                            modSpecs.remove(modSpec);
                            modSpecs.add( new ModSpec(modSpec, modName) );
                            // Set flag and break for now
                            repeat = true;
                            break;
                        }
                    }
                }
            } while(repeat);
        } // for

        /*
        // Dump the consumer and producer maps
        cons.dump();
        prod.dump();
        //*/

        /*
         * Header units can have various file extensions such as '.h.ifc', '.h.gcm', '.pcm', etc.; therefore,
         * we will use the 'system_header.sys.unit' and 'user_header.h.usr.unit' meta targets and let the users specify
         * the actual implementation details that match the compiler's need.
         */

        // Generate the dependency data that can be loaded using 'depload' and 'sdepload'
        final StringBuilder sb = new StringBuilder();

        // Put the dependency list type
        sb.append(DepReader._smCpp20Module);
        sb.append('\n');
        sb.append('\n');

        // Process the consumer entries
        for( final Map.Entry< String, TreeSet<ModSpec> > entry : cons.entrySet() ) {

            // Get and transform the target relative path
            final String targetAbsPath = entry.getKey();
            final String targetRelPath = _transformCppModulePath( SysUtil.resolveRelativePath(targetAbsPath), objFileExt );

            // Store the target
            sb.append(targetRelPath);
            sb.append(':'          );

            // Process the dependencies
            for( final ModSpec modSpec : entry.getValue() ) {
                // Check if it is a header unit
                if( !modSpec.headerUnit.isEmpty() ) {
                    // Transform the header unit relative path
                    final String unitRelPath = _transformCppModulePath(modSpec.headerUnit, true, modSpec.headerLocal);
                    // Store the header unit
                    sb.append(' '        );
                    sb.append(unitRelPath);
                }
                // It is a normal module
                else {
                    // Generate the list of dependency paths
                    final TreeSet<String> depList = new TreeSet<>();
                    if(modSpec.type == ModType.ExportModuleImplementation) {
                        final TreeSet<String> g = prod.get( modSpec.toImportSpec(ModType.ExportPrimaryInterface) );
                        if(g != null) depList.addAll(g);
                    }
                    else if(modSpec.type == ModType.ExportImport) {
                        final TreeSet<String> g = prod.get( modSpec.toImportSpec(ModType.ExportPrimaryInterface) );
                        if(g != null) depList.addAll(g);
                    }
                    else if(modSpec.type == ModType.Import) {
                        if(true) {
                            final TreeSet<String> g = prod.get( modSpec.toImportSpec(ModType.ExportPrimaryInterface) );
                            if(g != null) depList.addAll(g);
                        }
                        if(true) {
                            final TreeSet<String> g = prod.get( modSpec.toImportSpec(ModType.ExportModuleImplementation) );
                            if(g != null) depList.addAll(g);
                        }
                        /*
                        final Map< ModSpec, TreeSet<String> > sm = prod.subMap(
                                                                       prod.  floorKey( modSpec.toImportSpec(ModType.ExportModuleImplementation) ), true,
                                                                       prod.ceilingKey( modSpec.toImportSpec(ModType.ExportModuleImplementation) ), true
                                                                   );
                        SysUtil.stdDbg().println(targetRelPath);
                        for( final Map.Entry< ModSpec, TreeSet<String> > e : sm.entrySet() ) {
                            SysUtil.stdDbg().println( e.getValue() );
                        }
                        //*/
                    }
                    // Process the dependency paths
                    for(final String preqAbsPath : depList) {
                        // Skip if it is actually the same with the target
                        if( preqAbsPath.equals(targetAbsPath) ) continue;
                        // Get and transform the dependency relative path
                        final String preqRelPath = _transformCppModulePath( SysUtil.resolveRelativePath(preqAbsPath), objFileExt );
                        // Store the dependency
                        sb.append(' '        );
                        sb.append(preqRelPath);
                    } // for
                } // if
            } // for

            // Add newlines
            sb.append('\n');
            sb.append('\n');

        } // for

        // Test and save the result
        _testAndSaveDepDataStr( sb.toString(), depOutFilePath );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected DepReader_C(final String sourceFilePath, final ArrayList<String> searchPaths) throws IOException
    { super(sourceFilePath, searchPaths); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void gdlCppInclude(final String depOutFilePath, final String sourceDirPath, final ArrayList<String> searchPaths, final String objFileExt, final String buildDir) throws IOException
    { _gdlSimple_impl( DepReader._smCppInclude, depOutFilePath, sourceDirPath, searchPaths, _pmCppSrcExt, (ps, ft) -> { return _transformCppIncludePath(ps, ft, objFileExt); }, buildDir ); }

    public static void gdlCppModule(final String depOutFilePath, final String sourceDirPath, final String objFileExt) throws IOException
    { _cpp20GenDepend_impl(depOutFilePath, sourceDirPath, objFileExt); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String readOneDepPath() throws IOException
    {
        while(true) {

            // Read one line
            final String line = _readLine_CppJava();
            if(line == null) return null;

            // Check for '#include <...>' and '#include "..."'
            final Matcher incMatcher = _pmInclude.matcher(line);
            if( incMatcher.matches() ) {

                // Matches '#include <...>'
                if( incMatcher.group(1) != null && incMatcher.group(1).charAt(0) == '<' ) {
                    final String absDepPath = _resolveAbsDepFilePath( incMatcher.group(3) );
                    if(absDepPath != null) return absDepPath;
                }

                // Matches '#include "..."'
                else if( incMatcher.group(2) != null && incMatcher.group(2).charAt(0) == '"' ) {
                    final String relDepPath = _resolveRelDepFilePath( incMatcher.group(3) );
                    if(relDepPath != null) return relDepPath;
                }

            }

            /*
            // Terminate the file read if the token '(' or '{' is found
            else {
                if( line.indexOf('(') >= 0 || line.indexOf('{') >= 0 ) return null;
            } // if
            */

        } // while
    }

} // class DepReader_C





/*
module;
module syslib1;
export module syslib1;

export module syslib2:one;
export module syslib2:two;
module syslib2:three;
export module syslib2;
export import :one;
export import :two;
import :three;

module syslib3:one;
module syslib3:two;
module syslib3:zero;
import :one;
import :two;
export module syslib3;
import :zero;

import;
import userlib;

export import aaa:one;
export import    :two;
export import;
*/
