/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import jxm.*;
import jxm.annotation.*;


@package_private
class FWReader_RawBin extends FWReader {

    public FWReader_RawBin(final String binFilePath) throws IOException
    { super(binFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.RBIN_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.RBIN_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    { return _readAllBinaryData(); }

    @Override
    public long dataBlockStartAddress()
    { return 0; }

} // class FWReader_RawBin
