/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.io.IOException;

import java.util.ArrayList;

import jxm.*;
import jxm.dl.*;
import jxm.xb.*;


public class GDLUtil {

    public static enum DLFT {
        CppInclude,
        CppModule,
        JavaImport
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_gen_deplf(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final DLFT mode) throws JXMException
    {
        // Check the number of arguments
        final int dflCount = evalVals.get(0).size();
        final int srcCount = evalVals.get(1).size();

        if(dflCount != srcCount) throw XCom.newJXMRuntimeError(Texts.EMsg_gdl_NumFileDirNotSame, dflCount, srcCount);

        // Get the object file extension
        final String objExt = (mode == DLFT.CppInclude || mode == DLFT.CppModule)
                            ? FuncCall._readFlattenOptParam(evalVals, 2, null)
                            : null;

        // Prepare the search directory paths
        final XCom.VariableValue vvSearchPaths  = (mode == DLFT.CppInclude) ? execData.execState.getVar( execBlock, execData, XCom.genSVarName( XCom.SVarName.__include_paths__.name() ), true )
                                                : (mode == DLFT.JavaImport) ? execData.execState.getVar( execBlock, execData, XCom.genSVarName( XCom.SVarName.__class_paths__  .name() ), true )
                                                :                             null;

        final ArrayList<String>  absSearchPaths = (vvSearchPaths != null) ? new ArrayList<>() : null;

        if(vvSearchPaths != null) {
            for(final XCom.VariableStore item : vvSearchPaths) {
                final String p = SysUtil.resolveAbsolutePath(item.value);
                if( SysUtil.pathIsValidDirectory(p) ) absSearchPaths.add(p);
            }
        }

        // Get the build directory
        final String buildDir = (mode == DLFT.CppInclude) ? FuncCall._readFlattenOptParam(evalVals, 3, "")
                              : (mode == DLFT.JavaImport) ? FuncCall._readFlattenOptParam(evalVals, 2, "")
                              : "";

        // Create the depedency list file(s)
        try {
            for(int i = 0; i < dflCount; ++i) {
                final String dflName = evalVals.get(0).get(i).value;
                final String srcName = evalVals.get(1).get(i).value;
                switch(mode) {
                    case CppInclude : DepReader_C   .gdlCppInclude(dflName, srcName, absSearchPaths, objExt, buildDir); break;
                    case CppModule  : DepReader_C   .gdlCppModule (dflName, srcName,                 objExt          ); break;
                    case JavaImport : DepReader_Java.gdlJavaImport(dflName, srcName, absSearchPaths        , buildDir); break;
                }
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class GDLUtil
