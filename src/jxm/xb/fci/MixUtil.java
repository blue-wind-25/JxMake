/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import jxm.*;
import jxm.dl.*;
import jxm.tool.*;
import jxm.ugc.*;
import jxm.xb.*;


public class MixUtil {

    public static void _execute_add_target(final ArrayList<XCom.VariableValue> evalVals, final XCom.ExecData execData) throws JXMException
    {
        // Get the reference name, new name, new prerequisites, and new extra dependencies
        final XCom.VariableValue _newPreqs = FuncCall._getOptParam(evalVals, 2);
        final XCom.VariableValue _newXDeps = FuncCall._getOptParam(evalVals, 3);

        final String             refName   = XCom.flatten( evalVals.get(0), "" );
        final String             newName   = XCom.flatten( evalVals.get(1), "" );
        final ArrayList<String>  newPreqs  = (_newPreqs != null) ? new ArrayList<>() : null;
        final ArrayList<String>  newXDeps  = (_newXDeps != null) ? new ArrayList<>() : null;

        if( refName.isEmpty() || newName.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNameEmpty);

        if(_newPreqs != null) {
            for(final XCom.VariableStore item : _newPreqs) {
                if( !item.value.isEmpty() ) newPreqs.add(item.value);
            }
        }

        if(_newXDeps != null) {
            for(final XCom.VariableStore item : _newXDeps) {
                if( !item.value.isEmpty() ) newXDeps.add(item.value);
            }
        }

        // Get the target instance
        final Target refTarget = execData.targetMap.get(refName);

        if(refTarget == null) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNotDefined, refName);

        // Execute the target to create a new one based on the original one
        refTarget.execute(execData, newName, newPreqs, newXDeps);
    }

    public static void _execute_add_extradep(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the target name, extra dependencies, and flag
        final String             trgName  = XCom.flatten( evalVals.get(0), "" );
        final XCom.VariableValue addXDeps =               evalVals.get(1);
        final boolean            globalFB = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "false") );

        if( trgName.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNameEmpty);

        // Get the target instance
        final Target refTarget = execData.targetMap.get(trgName);

        if(refTarget == null) {
            // If the flag is set, act as if it were a 'depload' statement
            if(globalFB) {
                for(final XCom.VariableStore item : addXDeps) {
                    GlobalDepLoad.addDepFor( SysUtil.resolveAbsolutePath(trgName), SysUtil.resolveAbsolutePath(item.value) );
                }
                return;
            }
            // Otherwise throw error
            throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNotDefined, trgName);
        }

        // Add the for-all marker
        refTarget.addExtraDep( new XCom.ReadVarSpec(true, null, Target.DepForMarker, null, null) );
        refTarget.addExtraDep( new XCom.ReadVarSpec(true, null, Target.DepForAll   , null, null) );

        // Add the extra dependencies
        for(final XCom.VariableStore item : addXDeps) {
            refTarget.addExtraDep(item.value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_has_var(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final XCom.ExecData execData) throws JXMException
    {
        for( final XCom.VariableStore chk : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, execData.execState.hasVarExt(chk.value) ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_resolve_ip(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String hn = item.value.trim();
                if( hn.isEmpty() ) retVal.add( new XCom.VariableStore( true, ""                    ) );
                else               retVal.add( new XCom.VariableStore( true, SysUtil.resolveIP(hn) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_mcu_prog_json(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the programmer configuration name
        final String configName = XCom.flatten( evalVals.get(0), "" );

        // Process it
        try {
            retVal.add( new XCom.VariableStore( true, ProgExec.getProgConfigBasicJSONStr(configName) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_mcu_prog_ini(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the programmer configuration name
        final String configName = XCom.flatten( evalVals.get(0), "" );

        // Process it
        try {
            retVal.add( new XCom.VariableStore( true, ProgExec.getProgConfigBasicINIStr(configName) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_mcu_prog_exec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the backend specification data
        final String[] backendSpec = new String[ evalVals.get(0).size() ];

        int idx = 0;
        for( final XCom.VariableStore item : evalVals.get(0) ) backendSpec[idx++] = item.value;

        // Get the progammer specification data
        final String[] programmerSpec = new String[ evalVals.get(1).size() ];

        idx = 0;
        for( final XCom.VariableStore item : evalVals.get(1) ) programmerSpec[idx++] = item.value;

        // Get the MCU signature bytes
        final String mcuSignatureBytes = XCom.flatten( evalVals.get(2), "," );

        // Get the commands
        final String[] commands = new String[ evalVals.get(3).size() ];

        idx = 0;
        for( final XCom.VariableStore item : evalVals.get(3) ) commands[idx++] = item.value;

        // Get the firmware composer handle
        final String fwcHandle = XCom.flatten( evalVals.get(4), "" );

        // Get the flash memory empty value
        final int fmEmptyValue = XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(5), "" ) ).intValue();

        // Process it
        ProgExec progExec = null;

        try {
            // Instantiate the programmer executor
            progExec = new ProgExec(backendSpec, programmerSpec, mcuSignatureBytes);
            progExec.setProgressOutputStream( SysUtil.stdDbg() );
            // Execute the command(s)
            final long[] res = progExec.execute( commands, fwcHandle, FWCList.fwcGet(fwcHandle), fmEmptyValue );
            // Store the result(s) as needed
            for(final long r : res) retVal.add( new XCom.VariableStore( true, String.valueOf(r) ) );
            // Shutdown
            progExec.shutdown();
        }
        catch(final Exception e) {
            // Reset and shutdown as needed
            if(progExec != null) progExec.resetAndShutdown();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_esp_st_dec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the arguments
        final String addr2line = XCom.flatten( evalVals.get(0), ""   );
        final String elf_file  = XCom.flatten( evalVals.get(1), ""   );
        final String content   = XCom.flatten( evalVals.get(2), "\n" );

        // Decode and store
        try {
            for( final String line : ESP_STDecoder.decode(addr2line, elf_file, content) ) {
                retVal.add( new XCom.VariableStore(true, line + "\n") );
            }
        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_pp_java_scf(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            // Get the arguments
            final String                  srcFilePath   = SysUtil.resolveAbsolutePath( XCom.flatten  ( evalVals.get(0), "" ) );
            final String                  dstFilePath   = SysUtil.resolveAbsolutePath( XCom.flatten  ( evalVals.get(1), "" ) );
            final HashSet<String>         definedCFlags =                              XCom.toHashSet( evalVals.get(2)     )  ;
            final HashMap<String, String> definedSVals  = MapList.mapGetUnordered    ( XCom.flatten  ( evalVals.get(3), "" ) );
            // Execute the process
            final JavaSP jsp = new JavaSP(srcFilePath, dstFilePath);
            jsp.process(definedCFlags, definedSVals);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.getMessage() );
        }
    }

} // class MixUtil
