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
import java.util.HashMap;

import jxm.*;


public class Function extends ContainerBlock implements XCom.LabelMapOwner {

    private final ArrayList<String> _parNames;
    private final boolean           _supersede;
    private final String            _depreFor;

    private       String            _origin   = null;

    private       XCom.LabelMap     _labelMap = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Function(final String path, final int lNum, final int cNum, final String name, final ArrayList<String> parNames, final boolean supersede, final String depreFor, final boolean allocExecBlocksElem)
    {
        super(path, lNum, cNum, name, allocExecBlocksElem ? 1 : -1);

        _parNames  = parNames;
        _supersede = supersede;
        _depreFor  = depreFor;
    }

    public Function(final String path, final int lNum, final int cNum, final String name, final ArrayList<String> parNames, final boolean supersede, final String depreFor)
    { this(path, lNum, cNum, name, parNames, supersede, depreFor, true); }

    public ArrayList<String> parNames()
    { return _parNames; }

    public String getDepreFor()
    { return _depreFor; }

    public String getOriginal()
    { return _origin; }

    @Override
    public void putLabel(final String label)
    { _labelMap = XCom.LabelMap.putLabel( _labelMap, label, getExecBlocks().size() ); }

    @Override
    public int getLabel(final String label)
    { return XCom.LabelMap.getLabel(_labelMap, label); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static HashMap<String, XCom.IntegerRef> _rfiCntMap = new HashMap<>();

    private static String _genOrgFName(final String fname)
    {
        final XCom.IntegerRef idxObj = _rfiCntMap.get(fname);
              int             idxNum = 0;

        if(idxObj == null) {
            _rfiCntMap.put( fname, new XCom.IntegerRef(idxNum) );
        }
        else {
            idxNum = idxObj.get();
            idxObj.set(++idxNum);
        }

        return fname + "[$org" + String.valueOf(idxNum) + ']';
    }

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // NOTE : Instead of executing the execution-blocks, this function registers this function to the function map

        // Check if a function with the same name already exists
        final String   fname = getBlockName();
        final Function chk  = execData.functionMap.get(fname);

        if(chk != null) {
            // Check if this function is a superseding function
            if(_supersede) {
                _origin = _genOrgFName(fname);
                execData.functionMap.remove(fname       );
                execData.functionMap.put   (_origin, chk);
                chk.setBlockName(_origin);
            }
            // This function is not a superseding function
            else {
                setErrorFromString( Texts.EMsg_UserFuncDefExist, fname, chk.jxmSpecFile_absPath(), chk.jxmSpecFileLNum(), chk.jxmSpecFileCNum() );
                return XCom.ExecuteResult.Error;
            }
        }
        else {
            if(_supersede) {
                setErrorFromString(Texts.EMsg_UserFuncDefNoPrevDef, fname);
                return XCom.ExecuteResult.Error;
            }
        }

        // Store to the function map
        execData.functionMap.put(fname, this);

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveArrayListString(dos, _parNames);

        dos.writeBoolean(_supersede);
        XSaver.saveString(dos, _depreFor);

        XSaver.saveLabelMap(dos, _labelMap);
    }

    public static Function loadFromStream(final DataInputStream dis) throws Exception
    {
        final ContainerBlock.LoadContainerData loadCD = loadCDFromStream(dis);

        if(loadCD.blockName == null || loadCD.arrayExecBlocks.length != 1) return null;

        final Function function = new Function(
            loadCD.loadPLC.path,
            loadCD.loadPLC.lNum,
            loadCD.loadPLC.cNum,
            loadCD.blockName,
            XLoader.loadArrayListString(dis),
            dis.readBoolean(),
            XLoader.loadString(dis),
            false
        );

        function._labelMap = XLoader.loadLabelMap(dis);

        function.setExecBlocks( 0, loadCD.arrayExecBlocks[0] );

        return function;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Function(final Function refFunction)
    {
        super( refFunction.getPath(), refFunction.getLNum(), refFunction.getCNum(), refFunction.getBlockName(), -1 );

        _parNames  = refFunction._parNames;
        _supersede = refFunction._supersede;
        _depreFor  = refFunction._depreFor;
        _origin    = refFunction._origin;    // The origin function will have already been deep-cloned when the 'XCom.ExecData' object is cloned
        _labelMap  = refFunction._labelMap;

        setExecBlocks( 0, refFunction.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is a subclass of the 'ContainerBlock' class, hence, its member execution blocks
        // may not be thread safe; therefore, return a clone of this instance
        return new Function(this);
    }

} // class Function
