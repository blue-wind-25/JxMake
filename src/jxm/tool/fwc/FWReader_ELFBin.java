/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.HashMap;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
class FWReader_ELFBin extends FWReader {

    private final FWComposer.ELF_ISecRules _includeSections;
    private final FWComposer.ELF_ESecRules _excludeSections;

    private final HashMap<String, Long>    _sectionBegs               = new HashMap<>();
    private final HashMap<String, Long>    _sectionEnds               = new HashMap<>();

    private       ByteArrayInputStream     _elfDataStream             = null;

    private       boolean                  _elf32                     = false;
    private       boolean                  _elf64                     = false;

    private       boolean                  _elfLE                     = false;
    private       boolean                  _elfBE                     = false;

    private       String[]                 _elf_sh_name               = null;
    private       long  []                 _elf_sh_offset             = null;
    private       long  []                 _elf_sh_size               = null;
    private       long  []                 _elf_sh_addr               = null;

    private       int                      _elf_e_shnum               = -1;
    private       int                      _elf_sh_cur                =  0;
    private       long                     _elf_sh_last_addr          = -1;

    private       long                     _thisDataBlockStartAddress = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _seek(final long absPos) throws JXMException
    {
        _elfDataStream.reset();
        if( _elfDataStream.skip(absPos) != absPos ) _throwInvalidBinFileEOF();
    }

    private byte[] _readBytes(final long len) throws JXMException
    {
        final byte[] res = FWUtil._readBytes( _elfDataStream, (int) len );
        if(res == null) _throwInvalidBinFileEOF();

        return res;
    }

    private int _rdUI08() throws JXMException
    {
        final int res = FWUtil._readUInt08(_elfDataStream);
        if(res < 0) _throwInvalidBinFileEOF();

        return res;
    }

    private int[] _rdUI08(final int len) throws JXMException
    {
        final int[] res = FWUtil._readUInt08(_elfDataStream, len);
        if(res == null) _throwInvalidBinFileEOF();

        return res;
    }

    private int _rdUI16() throws JXMException
    {
        final int res = FWUtil._readUInt16(_elfDataStream, _elfLE);
        if(res < 0) _throwInvalidBinFileEOF();

        return res;
    }

    private long _rdUI32() throws JXMException
    {
        final long res = FWUtil._readUInt32(_elfDataStream, _elfLE);
        if(res < 0) _throwInvalidBinFileEOF();

        return res;
    }

    private long _rdUI32BE() throws JXMException
    {
        final long res = FWUtil._readUInt32BE(_elfDataStream);
        if(res < 0) _throwInvalidBinFileEOF();

        return res;
    }

    private long _rdUI64() throws JXMException
    {
        final long res = FWUtil._readUInt64(_elfDataStream, _elfLE);
        if(res < 0) _throwInvalidBinFileEOF();

        return res;
    }

    private long _rdUIW() throws JXMException
    { return _elf32 ? _rdUI32() : _rdUI64(); }

    private String _rdString() throws JXMException
    {
        String str = "";

        while(true) {
            final int res = FWUtil._readUInt08(_elfDataStream);
            if(res <  0) _throwInvalidBinFileEOF();
            if(res == 0) break;
            str += (char) res;
        }

        return str;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_ELFBin(final String binFilePath, final FWComposer.ELF_ISecRules includeSections, final FWComposer.ELF_ESecRules excludeSections) throws IOException
    {
        super(binFilePath);

        _includeSections = includeSections;
        _excludeSections = excludeSections;
    }

    @Override
    protected String __L_RNAME()
    { return FWUtil.ELFB_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.ELFB_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    {
        // Read the ELF file if it has not been read previously
        if(_elfDataStream == null) {
            // ===== Read all the binary data =====
            _elfDataStream = new ByteArrayInputStream( _readAllBinaryData() );
            // ===== Process the file header =====
            // Check 'e_ident[EI_MAG]:4'
            if( _rdUI32BE() != 0x7F454C46L ) _throwInvalidBinFileFormat();
            // Parse 'e_ident[EI_CLASS]:1'
            if( _rdUI08() == 0x01 ) _elf32 = true;
            else                    _elf64 = true;
            // Parse 'e_ident[EI_DATA]:1'
            if( _rdUI08() == 0x01 ) _elfLE = true;
            else                    _elfBE = true;
            // Check 'e_ident[EI_VERSION]:1'
            if( _rdUI08() != 0x01 ) _throwInvalidBinFileFormat();
            // Ignore 'e_ident[EI_OSABI]:1' and 'e_ident[EI_ABIVERSION]:1' and 'e_ident[EI_PAD]:7'
            _rdUI08( );
            _rdUI08( );
            _rdUI08(7);
            // Check 'e_type:2'
            if( _rdUI16() != 0x02 ) _throwInvalidBinFileFormat();
            // Ignore 'e_machine:2'
            _rdUI16();
            // Check 'e_version:4'
            if( _rdUI32() != 0x01 ) _throwInvalidBinFileFormat();
            // Ignore 'e_entry:4|8'
            _rdUIW();
            // Read 'e_phoff:4|8' and 'e_shoff:4|8'
            final long e_phoff = _rdUIW();
            final long e_shoff = _rdUIW();
            // Ignore 'e_flags:4' and 'e_ehsize:2' and 'e_phentsize:2'
            _rdUI32();
            _rdUI16();
            _rdUI16();
            // Read 'e_phnum:2'
            final int e_phnum = _rdUI16();
            // Ignore 'e_shentsize:2'
            _rdUI16();
            // Read 'e_shnum:2' and 'e_shstrndx:2'
                      _elf_e_shnum    = _rdUI16();
            final int      e_shstrndx = _rdUI16();
            // ===== Process the program header =====
            _seek(e_phoff);
            for(int i = 0; i < e_phnum; ++i) {
                // Ignore 'p_type:4'
                _rdUI32();
                // Ignore 'p_flags:4' as needed
                if(_elf64) _rdUI32();
                // Ignore 'p_offset:4|8' and 'p_vaddr:4|8' and 'p_paddr:4|8' and 'p_filesz:4|8' and 'p_memsz:4|8'
                _rdUIW();
                _rdUIW();
                _rdUIW();
                _rdUIW();
                _rdUIW();
                // Ignore 'p_flags:4' as needed
                if(_elf32) _rdUI32();
                // Ignore 'p_align:4|8'
                _rdUIW();
            }
            // ===== Process the section header =====
            final long[]     sh_name   = new long[_elf_e_shnum];
                        _elf_sh_addr   = new long[_elf_e_shnum];
                        _elf_sh_offset = new long[_elf_e_shnum];
                        _elf_sh_size   = new long[_elf_e_shnum];
            _seek(e_shoff);
            for(int i = 0; i < _elf_e_shnum; ++i) {
                // Read 'sh_name:4'
                sh_name[i] = _rdUI32();
                // Ignore 'sh_type:4' and 'sh_flags:4|8'
                _rdUI32();
                _rdUIW ();
                // Read 'sh_addr:4|8' and 'sh_offset:4|8' and 'sh_size:4|8'
                _elf_sh_addr  [i] = _rdUIW();
                _elf_sh_offset[i] = _rdUIW();
                _elf_sh_size  [i] = _rdUIW();
                // Ignore 'sh_link:4' and 'sh_info:4' and 'sh_addralign:4|8' and 'sh_entsize:4|8'
                _rdUI32();
                _rdUI32();
                _rdUIW ();
                _rdUIW ();
            }
            // ===== Process the section names =====
            _elf_sh_name = new String[_elf_e_shnum];
            for(int i = 0; i < _elf_e_shnum; ++i) {
                _seek( _elf_sh_offset[e_shstrndx] + sh_name[i] );
                _elf_sh_name[i] = _rdString();
            }
        }

        // Get the index to the next section to be included
        int    idx     = -1;
        long   aofs    =  0;
        String secName = null;

        if( _includeSections != null && !_includeSections.isEmpty() ) {
            // Get the index to the next section to be included
            while(true) {
                // Get and check the section index
                idx = _elf_sh_cur++;
                if( _elf_sh_cur > _includeSections.size() ) return null; // There are no more unprocessed sections
                // Find the section
                boolean included = false;
                for(int i = 0; i < _elf_e_shnum; ++i) {
                    // Skip if the section is empty
                    if(_elf_sh_size[i] <= 0) continue;
                    // Filter using the exclusion filter as needed
                    if( _excludeSections != null ) {
                        boolean excluded = false;
                        for(final FWComposer.ELF_ESecRule esr : _excludeSections) {
                            if( esr != null && esr.reName != null && esr.reName.matcher( _elf_sh_name[i] ).matches() ) {
                                excluded = true;
                                break;
                            }
                        }
                        if(excluded) continue;
                    }
                    // Filter using the inclusion filter and translate the section address
                    final FWComposer.ELF_ISecRule isr = _includeSections.get(idx);
                    if( _elf_sh_size[i] > 0 && isr != null && isr.reName != null && isr.reName.matcher( _elf_sh_name[i] ).matches() ) {
                        included = true;
                        idx      = i; // Replace the index with the real section index
                        aofs     = isr.addressOffset - _elf_sh_addr[idx];
                        secName  = (isr.compositeName != null) ? isr.compositeName : _elf_sh_name[idx];
                        if( isr.appendAfter != null ) aofs += _sectionEnds.get(isr.appendAfter);
                        break;
                    }
                }
                if(included) break;
            }
        }
        else {
            // Get the index
            while(true) {
                // Get and check the section index
                idx = _elf_sh_cur++;
                if(_elf_sh_cur > _elf_e_shnum) return null; // There are no more unprocessed sections
                // Skip if the section is empty
                if(_elf_sh_size[idx] <= 0) continue;
                // Filter using the exclusion filter as needed
                if( _excludeSections != null ) {
                    boolean excluded = false;
                    for(final FWComposer.ELF_ESecRule esr : _excludeSections) {
                        if( esr != null && esr.reName != null && esr.reName.matcher( _elf_sh_name[idx] ).matches() ) {
                            excluded = true;
                            break;
                        }
                    }
                    if(excluded) continue;
                }
                // We have got a section
                secName = _elf_sh_name[idx];
                break;
            }
            // Translate the section address
            if(_elf_sh_last_addr < 0) {
                for(int i = 0; i < _elf_e_shnum; ++i) {
                    if( !".text".equals(_elf_sh_name[i]) ) continue;
                    _elf_sh_last_addr = _elf_sh_addr[i];
                    break;
                }
            }
            aofs               = _elf_sh_last_addr - _elf_sh_addr[idx];
            _elf_sh_last_addr += _elf_sh_size[idx];
        }

        /*
        SysUtil.stdDbg().printf( "%36s [%08X->%08X] %08X (%08d) %s\n", filePath(), _elf_sh_addr[idx], aofs + _elf_sh_addr[idx], _elf_sh_size[idx], _elf_sh_size[idx], _elf_sh_name[idx] );
        //*/

        // Update/put the section begin and end addresses
        final long bAddr = aofs  + _elf_sh_addr[idx];
        final long eAddr = bAddr + _elf_sh_size[idx];

        if( _sectionBegs.containsKey(secName) ) {
            // Update the values
            _sectionBegs.replace( secName, Math.min( bAddr, _sectionBegs.get(secName) ) );
            _sectionEnds.replace( secName, Math.max( eAddr, _sectionEnds.get(secName) ) );
        }
        else {
            // Put using the original name or the composite name
            _sectionBegs.put(secName, bAddr);
            _sectionEnds.put(secName, eAddr);
            // Put using the original name as needed
            if( !secName.equals(_elf_sh_name[idx]) ) {
                _sectionBegs.put(_elf_sh_name[idx], bAddr);
                _sectionEnds.put(_elf_sh_name[idx], eAddr);
            }
        }

        // Read and return the section data
        _seek(_elf_sh_offset[idx]);

        _thisDataBlockStartAddress = aofs + _elf_sh_addr[idx];

        return _readBytes(_elf_sh_size[idx]);
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_ELFBin
