/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.io.Serializable;

import java.nio.file.Paths;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Pattern;

import javax.swing.ToolTipManager;

/*
// ### !!! DEPRECATED !!! ###
import sun.misc.Signal;
import sun.misc.SignalHandler;
//*/

/*
import java.net.InetAddress;
//*/

import jxm.*;
import jxm.tool.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;
import jxm.xb.*;


//
// Test class (the test application entry point)
//
public class GTest1 {

    public static void main(final String[] args)
    {
        /*
        // ### !!! DEPRECATED !!! ###
        final SignalHandler handler = new SignalHandler () {
            public void handle(Signal sig)
            { SysUtil.stdDbg().println("### ### ###"); }
        };
        Signal.handle(new Signal("INT"), handler);
        //*/

        try {

            // Initialize global
            JxMake.initializeGlobal();

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            /*
            final InetAddress address = InetAddress.getByName("nodemcu.local");
            System.out.println( address.getHostAddress() );
            //*/

            // Test 'TokenReader'
            if(!true) {
                final ArgParser ap = new ArgParser(args);

                SysUtil.stdDbg().println("####################################################################################################\n");
                SysUtil.setCWD( ap.jxmStartPath() );
                TokenReader tr = new TokenReader( SysUtil.resolveAbsolutePath( ap.jxmSpecFile() ), false );
                while(true) {
                    // Get the token
                    final TokenReader.Token token = tr.readToken();
                    if(token == null) break;
                    // Print the token
                    SysUtil.stdDbg().printf( "(%-3d, %-3d) # %s", token.lNum, token.cNum, token.isEOL() ? "<EOL>\n" : token.tStr );
                         if( token.mDQP != TokenReader.Token.DQPMode.None ) SysUtil.stdDbg().println(" [" + token.mDQP + "]");
                    else if( token.tRX1.isEmpty()                         ) SysUtil.stdDbg().println();
                    else                                                    SysUtil.stdDbg().printf (" # %s # %s\n", token.tRX1, token.tRX2);
                    token.printError();
                    // Split double-quoted string
                    final ArrayList<TokenReader.Token> tokens = TokenReader.splitDQString(token);
                    if(tokens != null) {
                        for(final TokenReader.Token t : tokens) {
                            SysUtil.stdDbg().printf("    %s", t.tStr);
                            if( t.tRX1.isEmpty() ) SysUtil.stdDbg().println();
                            else                   SysUtil.stdDbg().printf("    \"%s\" \"%s\"\n", t.tRX1, t.tRX2);
                            t.printError();
                        }
                    }
                }
                tr = null;
                SysUtil.stdDbg().println("####################################################################################################\n");
            } // if

            // Test 'JavaSP'
            if(!true) {
                final String src = "../test/src/java/lib/jsp_test.java.in";
                final String dst = SysUtil.getRootTmpDir() + "/jsp_test.java";
                final JavaSP jsp = new JavaSP(src, dst);

                final TreeSet<String> opts = new TreeSet<>( Arrays.asList( new String[] {
                                                 "__VALUE1__",
                                                 "__VALUE2__"
                                             } ) );

                @SuppressWarnings("serial")
                final TreeMap<String, String> svms = new TreeMap<String, String>() {{
                    put("REV_VAL_1", "\"abc\"");
                    put("REV_VAL_2", "\"def\"");
                }};

                jsp.process(opts, svms);

                JavaSP._dumpTextFile(dst);
            }

            // Test 'DocsBrowser'
            if(!true) ( new DocBrowser(false) ).run();

            // Test 'JxMakeEnvVarEditor'
            if(!true) {
                // NOTE : This dialog will not function properly when tested in isolation like this.
                final jxm.gcomp.se.JxMakeEnvVarEditor evEditor = new jxm.gcomp.se.JxMakeEnvVarEditor(false) {
                    @Override
                    public void onSaveDefault(final jxm.gcomp.JKeyValueTable.StateHashMap refTable, final jxm.gcomp.JKeyValueTable.StateHashMap usrTable)
                    {}
                    @Override
                    public void onLoadDefault()
                    {}
                };
                evEditor.initialize();
                @SuppressWarnings("serial")
                final jxm.gcomp.JKeyValueTable.StateHashMap testData = new jxm.gcomp.JKeyValueTable.StateHashMap() {{
                    for(int i = 0; i < 25; ++i) put( "KEY" + i, new jxm.gcomp.JKeyValueTable.State( (i % 5) != 0, "Value " + i ) );
                }};
                evEditor.initializeFromMap(
                    jxm.gcomp.JKeyValueTable.convertMap( SysUtil.getAllEnv() ),
                    testData
                );
                evEditor.waitUntilClosed(false);
            }

            // Test 'JxMakeCmdComposer'
            if(!true) {
                // NOTE : This dialog will not function properly when tested in isolation like this.
                final jxm.gcomp.se.JxMakeCmdComposer cmdComposer = new jxm.gcomp.se.JxMakeCmdComposer(false) {
                    @Override
                    public void onPaste(final String[] command)
                    {}
                    public void onPasteAndExecute(final String[] command)
                    {}
                };
                cmdComposer.initialize();
                final jxm.tool.CommandSpecifier.CommandRegistry registry = new jxm.tool.CommandSpecifier.CommandRegistry();
                    final jxm.tool.CommandSpecifier.CommandConfig lsCmd = new jxm.tool.CommandSpecifier.CommandConfig("ls", "list file");
                        lsCmd.addOption( "--color", false, jxm.tool.CommandSpecifier.AssignmentStyle.EQUALS, new String[] { "never", "auto", "always" }, "colorize the output"       );
                        lsCmd.addOption( "-l"     , false, jxm.tool.CommandSpecifier.AssignmentStyle.NONE                                              , "use a long listing format" );
                        lsCmd.addOption( "<path>" , true , jxm.tool.CommandSpecifier.AssignmentStyle.VALUE                                             , null                        );
                    registry.registerCommand(lsCmd);
                    final jxm.tool.CommandSpecifier.CommandConfig ceCmd = new jxm.tool.CommandSpecifier.CommandConfig(true, "cmdecho", "Change/get the command echo setting.");
                        ceCmd.addOption(
                            "<mode>", false, jxm.tool.CommandSpecifier.AssignmentStyle.VALUE,
                            new String[] { "off", "on", "state" },
                            new String[] {
                                "{ off | on | state }"         ,
                                "Disable command echo"         ,
                                "Enable command echo (default)",
                                "Query current echo state"
                            }
                        );
                    ceCmd.preselectValues(1);
                    registry.registerCommand(ceCmd);
                final jxm.tool.CommandSpecifier.CommandConfig dateCmd = new jxm.tool.CommandSpecifier.CommandConfig("date", "Print the current date and/or time");
                    dateCmd.addOption(
                        "<mode>", false, jxm.tool.CommandSpecifier.AssignmentStyle.VALUE,
                        new String[] { "-s", "-d", "-t", "-dt" },
                        new String[] {
                            "Output mode"                                            ,
                            "Seconds since Unix Epoch"                               ,
                            "Date in 'yyyy-MM-dd' format"                            ,
                            "Time in 'HH:mm:ss' format"                              ,
                            "Date and time in 'yyyy-MM-dd HH:mm:ss' format (default)"
                        }
                    );
                    dateCmd.addOption(
                        "<timezone>", false, jxm.tool.CommandSpecifier.AssignmentStyle.VALUE,
                        new String[] { "-u", "-l" },
                        new String[] {
                            "Time zone"                    ,
                            "Use UTC time zone"            ,
                            "Use local time zone (default)"
                        }
                    );
                    dateCmd.preselectOptions(0, 1);
                    dateCmd.preselectValues (3, 1);
                registry.registerCommand(dateCmd);
                cmdComposer.setCommandRegistry(registry);
                cmdComposer.waitUntilClosed(false);
            }

            // Test 'SysUtil'
            if(!true) {
                String path;

                SysUtil.stdDbg().println();

                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", SysUtil.osName(), SysUtil.osIsWindows(), SysUtil.osIsLinux(), SysUtil.osIsMac() );
                SysUtil.stdDbg().printf( "%08X\n", SysUtil.jxmVerValue() );

                SysUtil.stdDbg().println();

                SysUtil.cu_touch("a");
                SysUtil.cu_touch("b");
                SysUtil.cu_touch("c");
                SysUtil.cu_touch("d");

                SysUtil.stdDbg().printf( "a | b     : %-5b\n", SysUtil.isPathMoreRecent( "a", "b"                          ) );
                SysUtil.stdDbg().printf( "a | b c d : %-5b\n", SysUtil.isPathMoreRecent( "a", Arrays.asList("b", "c", "d") ) );

                SysUtil.sleepMS(5);
                SysUtil.cu_touch("a");
                SysUtil.cu_touch("c");
                SysUtil.stdDbg().printf( "a | b     : %-5b\n", SysUtil.isPathMoreRecent( "a", "b"                          ) );
                SysUtil.stdDbg().printf( "a | b c d : %-5b\n", SysUtil.isPathMoreRecent( "a", Arrays.asList("b", "c", "d") ) );

                SysUtil.sleepMS(5);
                SysUtil.cu_touch("a");
                SysUtil.stdDbg().printf( "a | b     : %-5b\n", SysUtil.isPathMoreRecent( "a", "b"                          ) );
                SysUtil.stdDbg().printf( "a | b c d : %-5b\n", SysUtil.isPathMoreRecent( "a", Arrays.asList("b", "c", "d") ) );

                SysUtil.cu_rmfile("a");
                SysUtil.cu_rmfile("b");
                SysUtil.cu_rmfile("c");
                SysUtil.cu_rmfile("d");

                SysUtil.stdDbg().println();

                path = "my_dir";
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.cu_mkdir(path);
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.cu_touch(path + "/my_file");
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.cu_rmfile(path + "/my_file");
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.cu_rmdir(path);
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.stdDbg().println();

                //path = "/tmp/my_dir";

                SysUtil.cu_mkdir(path                            );
                SysUtil.cu_touch(path + "/my_file"               );
                SysUtil.cu_mkdir(path + "/my_sub_dir"            );
                SysUtil.cu_touch(path + "/my_sub_dir/my_sub_file");
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.cu_rmdir_recursive(path);
                SysUtil.stdDbg().printf( "%-5b [%-5b]\n", SysUtil.pathIsEmptyDirectory(path), SysUtil.pathIsValid(path) );

                SysUtil.stdDbg().println();

                final String saveCWD = SysUtil.getCWD();

                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", SysUtil.getCWD(), SysUtil.pathIsAbsolute( SysUtil.getCWD() ) );
                SysUtil.setCWD("..");
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", SysUtil.getCWD(), SysUtil.pathIsAbsolute( SysUtil.getCWD() ) );

                SysUtil.stdDbg().println();

                path = "src/";
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", path, SysUtil.pathIsAbsolute(path) );
                path = SysUtil.resolveAbsolutePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", path, SysUtil.pathIsAbsolute(path) );
                path = SysUtil.resolveRelativePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", path, SysUtil.pathIsAbsolute(path) );

                SysUtil.stdDbg().println();

                SysUtil.setCWD("src");
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", SysUtil.getCWD(), SysUtil.pathIsAbsolute( SysUtil.getCWD() ) );

                SysUtil.stdDbg().println();

                path = "/dev/pts/";
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );
                path = SysUtil.resolveRelativePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );
                path = SysUtil.resolveAbsolutePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );

                SysUtil.stdDbg().println();

                path = "/dev/pts/ptmx";
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );
                path = SysUtil.resolveRelativePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );
                path = SysUtil.resolveAbsolutePath(path);
                SysUtil.stdDbg().printf( "%-50s [%-5b %-5b %-5b]\n", path, SysUtil.pathIsAbsolute(path), SysUtil.pathIsFile(path), SysUtil.pathIsDirectory(path) );

                SysUtil.stdDbg().println();

                SysUtil.setCWD("/dev/pts");
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", SysUtil.getCWD(), SysUtil.pathIsAbsolute( SysUtil.getCWD() ) );
                try {
                    SysUtil.setCWD("/dev/pts/ptmx");
                }
                catch(final Exception e) {
                    e.printStackTrace();
                }
                SysUtil.stdDbg().printf( "%-50s [%-5b]\n", SysUtil.getCWD(), SysUtil.pathIsAbsolute( SysUtil.getCWD() ) );

                SysUtil.stdDbg().println();

                path = "/dev/pts/ptmx";
                SysUtil.stdDbg().printf( "%-50s %-50s\n", SysUtil.getDirName (path), SysUtil.getFileName(path) );

                path = "/dev/pts";
                SysUtil.stdDbg().printf( "%-50s %-50s\n", SysUtil.getDirName (path), SysUtil.getFileName(path) );

                SysUtil.stdDbg().println();

                final ArgParser argParser = new ArgParser(args);
                SysUtil.setCWD( saveCWD                  );
                SysUtil.setCWD( argParser.jxmStartPath() );
                SysUtil.stdDbg().printf( "%-50s %-50s\n", SysUtil.getRootTmpDir(), SysUtil.getProjectTmpDir( argParser.jxmSpecFile() ) );

                SysUtil.getProjectTmpDir("dummy");

                SysUtil.delProjectTmpDir( argParser.jxmSpecFile(), true );
                SysUtil.delRootTmpDir();

                SysUtil.stdDbg().println();
            } // if

            // Test 'SysUtil.cu_file_*()'
            if(!true) {

                SysUtil.stdDbg().println( ".gz  = " + SysUtil.cu_file_mimetype("../0_excluded_directory/temporary/Z_DOUBLY.tar.gz.gz"   ) );
                SysUtil.stdDbg().println( ".xz  = " + SysUtil.cu_file_mimetype("../0_excluded_directory/temporary/Z_DOUBLY.tar.gz.gz.xz") );
                SysUtil.stdDbg().println( ".bz2 = " + SysUtil.cu_file_mimetype("/data/aloysius/mxe-20230701.tar.bz2"                    ) );
                SysUtil.stdDbg().println( ".exe = " + SysUtil.cu_file_mimetype("/data/aloysius/_ISO_/kicad-5.1.6_1-x86_64.exe"          ) );
                SysUtil.stdDbg().println( ".msu = " + SysUtil.cu_file_mimetype("/data/aloysius/_ISO_/Windows6.1-KB2999226-x64.msu"      ) );
                SysUtil.stdDbg().println( ".iso = " + SysUtil.cu_file_mimetype("/data/aloysius/_ISO_/lubuntu-16.04.1-desktop-amd64.iso" ) );
                SysUtil.stdDbg().println( ".img = " + SysUtil.cu_file_mimetype("/data/aloysius/_ISO_/nokia_1202_lcd_starfield.img"      ) );
                SysUtil.stdDbg().println( ".txt = " + SysUtil.cu_file_mimetype("/data/aloysius/_ISO_/usm-serial-number.txt"             ) );
                SysUtil.stdDbg().println( ".zip = " + SysUtil.cu_file_mimetype("../test/1-test.zip"                                     ) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println( ".gz  = " + SysUtil.cu_file_message ("../0_excluded_directory/temporary/Z_DOUBLY.tar.gz.gz"   ) );
                SysUtil.stdDbg().println( ".xz  = " + SysUtil.cu_file_message ("../0_excluded_directory/temporary/Z_DOUBLY.tar.gz.gz.xz") );
                SysUtil.stdDbg().println( ".bz2 = " + SysUtil.cu_file_message ("/data/aloysius/mxe-20230701.tar.bz2"                    ) );
                SysUtil.stdDbg().println( ".exe = " + SysUtil.cu_file_message ("/data/aloysius/_ISO_/kicad-5.1.6_1-x86_64.exe"          ) );
                SysUtil.stdDbg().println( ".msu = " + SysUtil.cu_file_message ("/data/aloysius/_ISO_/Windows6.1-KB2999226-x64.msu"      ) );
                SysUtil.stdDbg().println( ".iso = " + SysUtil.cu_file_message ("/data/aloysius/_ISO_/lubuntu-16.04.1-desktop-amd64.iso" ) );
                SysUtil.stdDbg().println( ".img = " + SysUtil.cu_file_message ("/data/aloysius/_ISO_/nokia_1202_lcd_starfield.img"      ) );
                SysUtil.stdDbg().println( ".txt = " + SysUtil.cu_file_message ("/data/aloysius/_ISO_/usm-serial-number.txt"             ) );
                SysUtil.stdDbg().println( ".zip = " + SysUtil.cu_file_message ("../test/1-test.zip"                                     ) );
                SysUtil.stdDbg().println();
            }

            // Test 'SerialPortUtil'
            if(!true) {
                SerialPortUtil.initialize();
                SerialPortUtil.dumpPortList();
                /*
                SerialPort[] ports = SerialPort.getCommPorts();
                for(int i = 0; i < ports.length; ++i) {
                    SysUtil.stdDbg().printf(
                        "[%d] %s (%s) : %s - %s @ %s\n",
                        i, ports[i].getSystemPortName(), ports[i].getSystemPortPath(), ports[i].getDescriptivePortName(), ports[i].getPortDescription(), ports[i].getPortLocation()
                    );
                }
                //*/
            }

            // Test 'XMLGUIEventHandler'
            if(!true) {
                SysUtil.stdDbg().println("###");
                for( final XMLFrame.Result res : XMLFrame.executeFromXMLFile("1-TestData/GTest1.GUI.xml") ) {
                    SysUtil.stdDbg().printf( "%-14s : '%s'\n", res.type, XMLFrame.escapeEntity(res.value) );
                }
                SysUtil.stdDbg().println("###\n");

                SysUtil.stdDbg().println();
            }

            /*
            // Test high level variable set-get
            if(!true) {
                XBExec    xbx = new XBExec(null);
                ExecState xst = xbx.getExecState();

                class TestVSG {
                    void apply(final XCom.ReadVarSpec rvs, final boolean evaluated) throws JXMException
                    {
                        final XCom.VariableValue varVal = xst.readVar(null, null, rvs, evaluated);

                        SysUtil.stdDbg().printf( "'%s' [%-5b] [%d]\n", rvs.getVarName(), evaluated, varVal.size() );

                        for(final XCom.VariableStore item : varVal) {
                            if(item.regexp != null) {
                                SysUtil.stdDbg().printf("%s /%s/ /%s/\n", item.value, item.regexp.pattern(), item.replacement);
                            }
                            else {
                                SysUtil.stdDbg().println(item.value);
                            }
                        }

                        SysUtil.stdDbg().println();
                    }
                }
                TestVSG tvsg = new TestVSG();

                //
                // A  = test.ref
                // B1 = ${A} ${A}
                // B2 = ${A/(.*)\.ref/$1.o/}
                // C  = "123 ${B1} ${B2} 456"
                // D  = "123 ${B2} ${B2/(.*)\.o/$1.o.o/} 456"
                //

                xst.setGlobalVar( "${A}" ,  new XCom.VariableStore(true , "test.ref"                    ), false, false );

                xst.setGlobalVar( "${B1}",  new XCom.VariableStore(false, "${A}"                        ), true , false );
                xst.setGlobalVar( "${B1}",  new XCom.VariableStore(false, "${A}"                        ), true , false );

                xst.setGlobalVar( "${B2}",  new XCom.VariableStore(false, "${A}", "(.*)\\.ref", "$1.o"  ), false, false );

                xst.setGlobalVar( "${C}" ,  new XCom.VariableStore(true , "123"                         ), true , false );
                xst.setGlobalVar( "${C}" ,  new XCom.VariableStore(false, "${B1}"                       ), true , false );
                xst.setGlobalVar( "${C}" ,  new XCom.VariableStore(false, "${B2}"                       ), true , false );
                xst.setGlobalVar( "${C}" ,  new XCom.VariableStore(true , "456"                         ), true , false );

                xst.setGlobalVar( "${D}" ,  new XCom.VariableStore(true , "123"                         ), true , false );
                xst.setGlobalVar( "${D}" ,  new XCom.VariableStore(false, "${B2}"                       ), true , false );
                xst.setGlobalVar( "${D}" ,  new XCom.VariableStore(false, "${B2}", "(.*)\\.o" , "$1.o.o"), true , false );
                xst.setGlobalVar( "${D}" ,  new XCom.VariableStore(true , "456"                         ), true , false );

                tvsg.apply( new XCom.ReadVarSpec(false, null, "${A}",  null,                          null    ), false );

                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B1}", null,                          null    ), false );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B1}", null,                          null    ), true  );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B1}", Pattern.compile("(.*)\\.ref"), "$1.cpp"), false );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B1}", Pattern.compile("(.*)\\.ref"), "$1.cpp"), true  );

                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B2}", null,                          null    ), false );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B2}", null,                          null    ), true  );

              //tvsg.apply( new XCom.ReadVarSpec(false, null, "${B2}", Pattern.compile("(.*)\\.o"),   "$1.o.o"), false ); // Uncommenting this will cause exception
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${B2}", Pattern.compile("(.*)\\.o"),   "$1.o.o"), true  );

                tvsg.apply( new XCom.ReadVarSpec(false, null, "${C}" , null,                          null    ), false );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${C}" , null,                          null    ), true  );

                tvsg.apply( new XCom.ReadVarSpec(false, null, "${D}" , null,                          null    ), false );
                tvsg.apply( new XCom.ReadVarSpec(false, null, "${D}" , null,                          null    ), true  );

                SysUtil.stdDbg().println();
            } //if
            //*/

            // Test find files and directories
            if(!true) {
                for( final String s : SysUtil.cu_find_file          ( "../test/src/cpp20", Pattern.compile("(?:.+)\\.cpp") ) ) SysUtil.stdDbg().println(s);
                SysUtil.stdDbg().println();
                for( final String s : SysUtil.cu_find_file_recursive( "../test/src/cpp20", Pattern.compile("(?:.+)\\.cpp") ) ) SysUtil.stdDbg().println(s);
                SysUtil.stdDbg().println();
                for( final String s : SysUtil.cu_find_dir           ( ".."               , Pattern.compile("^\\.svn$"    ) ) ) SysUtil.stdDbg().println(s);
                SysUtil.stdDbg().println();
                for( final String s : SysUtil.cu_find_dir_recursive ( "../test/src"      , Pattern.compile("(?:.+)sys$"  ) ) ) SysUtil.stdDbg().println(s);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println();
            }

            // Test 'TarXz'
            if(!true) {
                // tar --list -Jf ../test/test.tar.xz
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untxz/wrapper
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untxz_copy/wrapper
                final String tarFileName = "../test/test.tar.xz";
                final String tarAPDN     = "wrapper";
                final String tarInpDir   = "../test/1-tarinpdir";
                final String tarOutDir   = "../test/1-taroutdir_untxz";
                TarXz.compressDir  (tarFileName, tarInpDir, tarAPDN);
                TarXz.uncompressDir(tarFileName, tarOutDir         );
                //*
                SysUtil.cu_cpdir_rec(tarOutDir, tarOutDir + "_copy", true);
                SysUtil.stdDbg().println();
                //*/
                SysUtil.stdDbg().println();
            }

            // Test 'TarBz2'
            if(!true) {
                // tar --list -jf ../test/test.tar.bz2
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untbz2/wrapper
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untbz2_copy/wrapper
                final String tarFileName = "../test/test.tar.bz2";
                final String tarAPDN     = "wrapper";
                final String tarInpDir   = "../test/1-tarinpdir";
                final String tarOutDir   = "../test/1-taroutdir_untbz2";
                TarBz2.compressDir  (tarFileName, tarInpDir, tarAPDN);
                TarBz2.uncompressDir(tarFileName, tarOutDir         );
                //*
                SysUtil.cu_cpdir_rec(tarOutDir, tarOutDir + "_copy", true);
                SysUtil.stdDbg().println();
                //*/
                SysUtil.stdDbg().println();
            }

            // Test 'TarGz'
            if(!true) {
                // tar --list -zf ../test/test.tar.gz
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untgz
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untgz_copy
                final String tarFileName = "../test/test.tar.gz";
                final String tarInpDir   = "../test/1-tarinpdir";
                final String tarOutDir   = "../test/1-taroutdir_untgz";
                TarGz.compressDir  (tarFileName, tarInpDir, null);
                TarGz.uncompressDir(tarFileName, tarOutDir      );
                //*
                SysUtil.cu_cpdir_rec(tarOutDir, tarOutDir + "_copy", true);
                SysUtil.stdDbg().println();
                //*/
                SysUtil.stdDbg().println();
            }

            // Test 'TarZip'
            if(!true) {
                // unzip -l ../test/test.tar.zip
                // tar --list -zf ../test/test.tar.zip
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untzip
                // diff -ru ../test/1-tarinpdir ../test/1-taroutdir_untzip_copy
                final String tarFileName = "../test/test.tar.zip";
                final String tarInpDir   = "../test/1-tarinpdir";
                final String tarOutDir   = "../test/1-taroutdir_untzip";
                TarZip.compressDir  (tarFileName, tarInpDir, null);
                TarZip.uncompressDir(tarFileName, tarOutDir      );
                //*
                SysUtil.cu_cpdir_rec(tarOutDir, tarOutDir + "_copy", true);
                SysUtil.stdDbg().println();
                //*/
                SysUtil.stdDbg().println();
            }

            // Test 'UnZip'
            if(!true) {
                // unzip -l ../test/test.zip
                // diff -ru ../test/1-tarinpdir ../test/1-zipoutdir
                final String zipFileName = "../test/test.zip";
                final String zipInpDir   = "../test/1-tarinpdir";
                final String zipOutDir   = "../test/1-zipoutdir";
                SysUtil.cu_rmfile(zipFileName);
                SysUtil.stdDbg().println( SysUtil.execlp( "bash", "-c", "cd " + zipInpDir + " && zip -r ../test.zip *" ) );
                UnZip.uncompressDir(zipFileName, zipOutDir);
                SysUtil.stdDbg().println();
            }

            // Test 'JSONDecoder'
            if(!true) {
                final String jsonStr1 = "{                               "
                                      + "    \"key1\"  : null,           "
                                      + "    \"key2a\" : false,          "
                                      + "    \"key2b\" : true,           "
                                      + "    \"key3\"  : 123,            "
                                      + "    \"key4\"  : 456.789,        "
                                      + "    \"key5\"  : \"hello world!\""
                                      + "}                               ";
                JSONDecoder.decode(jsonStr1).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonStr2 = "[                   "
                                      + "    null,           "
                                      + "    false,          "
                                      + "    true,           "
                                      + "    123,            "
                                      + "    456.789,        "
                                      + "    \"hello world!\""
                                      + "]                   ";
                JSONDecoder.decode(jsonStr2).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonStr3 = "{                                      "
                                      + "    \"key1\"  : [ 1, 2, 3 ],           "
                                      + "    \"key2\"  : [ \"a\", \"b\", \"c\" ]"
                                      + "}                                      ";
                JSONDecoder.decode(jsonStr3).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonStr4 = "[                        "
                                      +  "   {                    "
                                      +  "       \"key1a\" : 0x01,"
                                      +  "       \"key1b\" : 0x02 "
                                      +  "   },                   "
                                      +  "   {                    "
                                      +  "       \"key2a\" : 0x10,"
                                      +  "       \"key2b\" : 0x20 "
                                      +  "   }                    "
                                      + "]                        ";
                JSONDecoder.decode(jsonStr4).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonFile1 = "../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-latest.json";
                JSONDecoder.decode( Paths.get(jsonFile1) ).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonFile2 = "../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-tags.json";
                JSONDecoder.decode( Paths.get(jsonFile2) ).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final String jsonFile3 = "../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-releases-empty-assets.json";
                JSONDecoder.decode( Paths.get(jsonFile3) ).dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();
            }

            /*
            final ProgSWIM.ConfigSTM8S config = new ProgSWIM.ConfigSTM8S();

            config.memoryFlash.address    = 0x8000;
            config.memoryFlash.totalSize  = 8192;
            config.memoryFlash.pageSize   =   64;
            config.memoryFlash.numPages   =  128;

            config.memoryEEPROM.address   = 0x4000;
            config.memoryEEPROM.totalSize = 640;
            config.memoryEEPROM.pageSize  =  64;
            config.memoryEEPROM.numPages  =  10;

            SysUtil.stdDbg().println( JSONEncoderLite.serialize(config) );
            //*/

            // Test 'JSONEncoderLite' and 'FWComposer.ELF_*SecRules'
            if(!true) {
                XCom.setEnableAllExceptionStackTrace(true);

                final ProgISP.Config ispConfigRef = new ProgISP.Config();
                final String         ispJSONStr   = JSONEncoderLite.serialize(ispConfigRef);
                SysUtil.stdDbg().println(ispJSONStr);
                JSONDecoder.decode(ispJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();

                final ProgTPI.Config tpiConfigRef = new ProgTPI.Config();
                final String         tpiJSONStr   = JSONEncoderLite.serialize(tpiConfigRef);
                SysUtil.stdDbg().println(tpiJSONStr);
                JSONDecoder.decode(tpiJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();

                final ProgUPDI.Config updiConfigRef = new ProgUPDI.Config();
                final String          updiJSONStr   = JSONEncoderLite.serialize(updiConfigRef);
                SysUtil.stdDbg().println(updiJSONStr);
                JSONDecoder.decode(updiJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();

                final ProgPDI.Config pdiConfigRef = new ProgPDI.Config();
                final String         pdiJSONStr   = JSONEncoderLite.serialize(pdiConfigRef);
                SysUtil.stdDbg().println(pdiJSONStr);
                JSONDecoder.decode(pdiJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                final ProgSWD.Config swdConfigRef = new ProgSWD.Config();
                final String         swdJSONStr   = JSONEncoderLite.serialize(swdConfigRef);
                SysUtil.stdDbg().println(swdJSONStr);
                JSONDecoder.decode(swdJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();

                final ProgSWIM.Config swimConfigRef = new ProgSWIM.ConfigSTM8S();
                final String          swimJSONStr   = JSONEncoderLite.serialize(swimConfigRef);
                SysUtil.stdDbg().println(swimJSONStr);
                JSONDecoder.decode(swimJSONStr);//.dump( SysUtil.stdDbg() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();

                final FWComposer.ELF_ISecRules isrRef     = FWComposer.example_ELF_ISecRules_SAMD21(0x00002000L);
                final String                   isrJSONStr = JSONEncoderLite.serialize(isrRef);
                SysUtil.stdDbg().println(isrJSONStr);
                SysUtil.stdDbg().println( isrJSONStr.equals( JSONEncoderLite.serialize( JSONDecoder.deserialize(isrJSONStr, FWComposer.ELF_ISecRules.class) ) ) );
                SysUtil.stdDbg().println();

                final FWComposer.ELF_ESecRules esrRef     = FWComposer.default_ELF_ESecRules();
                final String                   esrJSONStr = JSONEncoderLite.serialize(esrRef);
                SysUtil.stdDbg().println(esrJSONStr);
                SysUtil.stdDbg().println( esrJSONStr.equals( JSONEncoderLite.serialize( JSONDecoder.deserialize(esrJSONStr, FWComposer.ELF_ESecRules.class) ) ) );
                SysUtil.stdDbg().println();

                XCom.setEnableAllExceptionStackTrace(false);
                SysUtil.stdDbg().println();

                /*
                SysUtil.systemExit();
                //*/
            }

            // Test serialize and deserialize 'SerializableDeepClone<Prog*.Config>' classes with JSON
            if(!true) {
                XCom.setEnableAllExceptionStackTrace(true);

                final ProgISP.Config ispConfig1 = new ProgISP.Config();
                                                      ispConfig1.spiMode                     = USB2GPIO.SPIMode._1;
                                                      ispConfig1.readSignature.instruction   = new int[] { 0x28, 0x00, 0x30, 0x00, 0x28, 0x00, 0x31, 0x00 };
                                                      ispConfig1.readSignature.responseIndex = new int[] { 3, 7 };
                                                      ispConfig1.memoryFlash.totalSize       = 32768;
                                                      ispConfig1.memoryFlash.pageSize        =   128;
                                                      ispConfig1.memoryFlash.numPages        =   256;
                                                      ispConfig1.memoryEEPROM.totalSize      =  1024;
                final String         ispJSONStr = SerializableDeepClone.toJSON(ispConfig1);
                final ProgISP.Config ispConfig2 = SerializableDeepClone.fromJSON(ispJSONStr, ProgISP.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(ispJSONStr);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(ispConfig1);
                SysUtil.stdDbg().println(ispConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(ispConfig1, ispConfig2) );
                SysUtil.stdDbg().println( SerializableDeepClone.fromJSON(ispJSONStr, ProgISP.Config.class) );
                SysUtil.stdDbg().println();
                /*
                SysUtil.systemExit();
                //*/

                final ProgTPI.Config tpiConfig1 = new ProgTPI.Config();
                                                      tpiConfig1.memoryFlash.totalSize     = 1024;
                                                      tpiConfig1.memoryFlash.pageSize      =   16;
                                                      tpiConfig1.memoryFlash.numPages      =   64;
                                                      tpiConfig1.memoryFlash.numWordWrites =    1;
                final String         tpiJSONStr = SerializableDeepClone.toJSON(tpiConfig1);
                final ProgTPI.Config tpiConfig2 = SerializableDeepClone.fromJSON(tpiJSONStr, ProgTPI.Config.class);
                SysUtil.stdDbg().println(tpiConfig1);
                SysUtil.stdDbg().println(tpiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(tpiConfig1, tpiConfig2) );
                SysUtil.stdDbg().println();

                final ProgUPDI.Config updiConfig1 = new ProgUPDI.Config();
                                                        updiConfig1.memoryFlash.address    = 0x4000;
                                                        updiConfig1.memoryFlash.totalSize  = 49152;
                                                        updiConfig1.memoryFlash.pageSize   =   128;
                                                        updiConfig1.memoryFlash.numPages   =   384;
                                                        updiConfig1.memoryFuse.bitMask[5]  = 0xC9;
                                                        updiConfig1.memoryEEPROM.totalSize = 256;
                                                        updiConfig1.memoryEEPROM.pageSize  =  64;
                                                        updiConfig1.memoryEEPROM.numPages  =   4;
                final String          updiJSONStr = SerializableDeepClone.toJSON(updiConfig1);
                final ProgUPDI.Config updiConfig2 = SerializableDeepClone.fromJSON(updiJSONStr, ProgUPDI.Config.class);
                SysUtil.stdDbg().println(updiConfig1);
                SysUtil.stdDbg().println(updiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(updiConfig1, updiConfig2) );
                SysUtil.stdDbg().println();

                final ProgPDI.Config pdiConfig1 = new ProgPDI.Config();
                                                      pdiConfig1.memoryFlash.totalSize  = 16384;
                                                      pdiConfig1.memoryFlash.pageSize   =   256;
                                                      pdiConfig1.memoryFlash.numPages   =    64;
                                                      pdiConfig1.memoryEEPROM.totalSize = 1024;
                                                      pdiConfig1.memoryEEPROM.pageSize  =   32;
                                                      pdiConfig1.memoryEEPROM.numPages  =   32;
                final String         pdiJSONStr = SerializableDeepClone.toJSON(pdiConfig1);
                final ProgPDI.Config pdiConfig2 = SerializableDeepClone.fromJSON(pdiJSONStr, ProgPDI.Config.class);
                SysUtil.stdDbg().println(pdiConfig1);
                SysUtil.stdDbg().println(pdiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(pdiConfig1, pdiConfig2) );
                SysUtil.stdDbg().println();

                final ProgSWD.Config swdConfig1 = new ProgSWD.Config();
                                                      swdConfig1.memorySRAM.address     = ProgSWD.DefaultCortexM_SRAMStart;
                                                      swdConfig1.memorySRAM.totalSize   = 32768;
                                                      swdConfig1.memoryFlash.driverName = "atsamdx";
                                                      swdConfig1.memoryFlash.address    = 0x00000000L;
                                                      swdConfig1.memoryFlash.totalSize  = 262144;
                                                      swdConfig1.memoryFlash.pageSize   =     64;
                final String         swdJSONStr = SerializableDeepClone.toJSON(swdConfig1);
                final ProgSWD.Config swdConfig2 = SerializableDeepClone.fromJSON(swdJSONStr, ProgSWD.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(swdJSONStr);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(swdConfig1);
                SysUtil.stdDbg().println(swdConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swdConfig1, swdConfig2) );
                SysUtil.stdDbg().println();

                final ProgSWIM.Config swimConfigS1 = new ProgSWIM.ConfigSTM8S();
                                                        swimConfigS1.memoryFlash.address    = 0x8000;
                                                        swimConfigS1.memoryFlash.totalSize  = 8192;
                                                        swimConfigS1.memoryFlash.pageSize   =   64;
                                                        swimConfigS1.memoryFlash.numPages   =  128;

                                                        swimConfigS1.memoryEEPROM.address   = 0x4000;
                                                        swimConfigS1.memoryEEPROM.totalSize = 640;
                                                        swimConfigS1.memoryEEPROM.pageSize  =  64;
                                                        swimConfigS1.memoryEEPROM.numPages  =  10;
                final String          swimJSONStrS = SerializableDeepClone.toJSON(swimConfigS1);
                final ProgSWIM.Config swimConfigS2 = SerializableDeepClone.fromJSON(swimJSONStrS, ProgSWIM.ConfigSTM8S.class);
                SysUtil.stdDbg().println(swimConfigS1);
                SysUtil.stdDbg().println(swimConfigS2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swimConfigS1, swimConfigS2) );
                SysUtil.stdDbg().println();

                final ProgSWIM.Config swimConfigL1 = new ProgSWIM.ConfigSTM8L();
                                                        swimConfigL1.memoryFlash.address    = 0x8000;
                                                        swimConfigL1.memoryFlash.totalSize  = 8192;
                                                        swimConfigL1.memoryFlash.pageSize   =   64;
                                                        swimConfigL1.memoryFlash.numPages   =  128;

                                                        swimConfigL1.memoryEEPROM.address   = 0x1000;
                                                        swimConfigL1.memoryEEPROM.totalSize = 256;
                                                        swimConfigL1.memoryEEPROM.pageSize  =  64;
                                                        swimConfigL1.memoryEEPROM.numPages  =   4;
                final String          swimJSONStrL = SerializableDeepClone.toJSON(swimConfigL1);
                final ProgSWIM.Config swimConfigL2 = SerializableDeepClone.fromJSON(swimJSONStrL, ProgSWIM.ConfigSTM8L.class);
                SysUtil.stdDbg().println(swimConfigL1);
                SysUtil.stdDbg().println(swimConfigL2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swimConfigL1, swimConfigL2) );
                SysUtil.stdDbg().println();

                final ProgPIC18.Config picConfig1 = ProgPIC18.Config.PIC18F4520();
                final String           picJSONSt1 = SerializableDeepClone.toJSON(picConfig1);
                final ProgPIC18.Config picConfig2 = SerializableDeepClone.fromJSON(picJSONSt1, ProgPIC18.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picJSONSt1);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig1);
                SysUtil.stdDbg().println(picConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig1, picConfig2) );
                SysUtil.stdDbg().println();

                final ProgPIC16.Config picConfig3 = ProgPIC16.Config.PIC16F676();
                final String           picJSONSt3 = SerializableDeepClone.toJSON(picConfig3);
                final ProgPIC16.Config picConfig4 = SerializableDeepClone.fromJSON(picJSONSt3, ProgPIC16.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picJSONSt3);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig3);
                SysUtil.stdDbg().println(picConfig4);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig3, picConfig4) );
                SysUtil.stdDbg().println();

                final ProgPIC24.Config picConfig5 = ProgPIC24.Config.PIC24FJ64GB00x();
                final String           picJSONSt4 = SerializableDeepClone.toJSON(picConfig5);
                final ProgPIC24.Config picConfig6 = SerializableDeepClone.fromJSON(picJSONSt4, ProgPIC24.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picJSONSt4);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig5);
                SysUtil.stdDbg().println(picConfig6);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig5, picConfig6) );
                SysUtil.stdDbg().println();

                final ProgLGT8.Config lgt8Config1 = ProgLGT8.Config.LGT8F328P();
                final String          lgt8JSONStr = SerializableDeepClone.toJSON(lgt8Config1);
                final ProgLGT8.Config lgt8Config2 = SerializableDeepClone.fromJSON(lgt8JSONStr, ProgLGT8.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(lgt8JSONStr);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(lgt8Config1);
                SysUtil.stdDbg().println(lgt8Config2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(lgt8Config1, lgt8Config2) );
                SysUtil.stdDbg().println();

                /*
                SysUtil.stdDbg().println( ProgExec.getProgConfigBasicJSONStr("ISP:ATmega328P") );
                //*/

                XCom.setEnableAllExceptionStackTrace(false);
                SysUtil.stdDbg().println();
            }

            // Test serialize and deserialize 'SerializableDeepClone<Prog*.Config>' classes with INI
            if(!true) {
                XCom.setEnableAllExceptionStackTrace(true);

                final ProgISP.Config ispConfig1 = new ProgISP.Config();
                                                      ispConfig1.spiMode                     = USB2GPIO.SPIMode._1;
                                                      ispConfig1.readSignature.instruction   = new int[] { 0x28, 0x00, 0x30, 0x00, 0x28, 0x00, 0x31, 0x00 };
                                                      ispConfig1.readSignature.responseIndex = new int[] { 3, 7 };
                                                      ispConfig1.memoryFlash.totalSize       = 32768;
                                                      ispConfig1.memoryFlash.pageSize        =   128;
                                                      ispConfig1.memoryFlash.numPages        =   256;
                                                      ispConfig1.memoryEEPROM.totalSize      =  1024;
                final String         ispINIStr  = SerializableDeepClone.toINI(ispConfig1);
                final ProgISP.Config ispConfig2 = SerializableDeepClone.fromINI(ispINIStr, ProgISP.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(ispINIStr );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(ispConfig1);
                SysUtil.stdDbg().println(ispConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(ispConfig1, ispConfig2) );
                SysUtil.stdDbg().println( SerializableDeepClone.fromINI(ispINIStr, ProgISP.Config.class) );
                SysUtil.stdDbg().println();

                final ProgTPI.Config tpiConfig1 = new ProgTPI.Config();
                                                      tpiConfig1.memoryFlash.totalSize     = 1024;
                                                      tpiConfig1.memoryFlash.pageSize      =   16;
                                                      tpiConfig1.memoryFlash.numPages      =   64;
                                                      tpiConfig1.memoryFlash.numWordWrites =    1;
                final String         tpiINIStr  = SerializableDeepClone.toINI(tpiConfig1);
                final ProgTPI.Config tpiConfig2 = SerializableDeepClone.fromINI(tpiINIStr, ProgTPI.Config.class);
                SysUtil.stdDbg().println(tpiConfig1);
                SysUtil.stdDbg().println(tpiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(tpiConfig1, tpiConfig2) );
                SysUtil.stdDbg().println();

                final ProgUPDI.Config updiConfig1 = new ProgUPDI.Config();
                                                        updiConfig1.memoryFlash.address    = 0x4000;
                                                        updiConfig1.memoryFlash.totalSize  = 49152;
                                                        updiConfig1.memoryFlash.pageSize   =   128;
                                                        updiConfig1.memoryFlash.numPages   =   384;
                                                        updiConfig1.memoryFuse.bitMask[5]  = 0xC9;
                                                        updiConfig1.memoryEEPROM.totalSize = 256;
                                                        updiConfig1.memoryEEPROM.pageSize  =  64;
                                                        updiConfig1.memoryEEPROM.numPages  =   4;
                final String          updiINIStr  = SerializableDeepClone.toINI(updiConfig1);
                final ProgUPDI.Config updiConfig2 = SerializableDeepClone.fromINI(updiINIStr, ProgUPDI.Config.class);
                SysUtil.stdDbg().println(updiConfig1);
                SysUtil.stdDbg().println(updiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(updiConfig1, updiConfig2) );
                SysUtil.stdDbg().println();

                final ProgPDI.Config pdiConfig1 = new ProgPDI.Config();
                                                      pdiConfig1.memoryFlash.totalSize  = 16384;
                                                      pdiConfig1.memoryFlash.pageSize   =   256;
                                                      pdiConfig1.memoryFlash.numPages   =    64;
                                                      pdiConfig1.memoryEEPROM.totalSize = 1024;
                                                      pdiConfig1.memoryEEPROM.pageSize  =   32;
                                                      pdiConfig1.memoryEEPROM.numPages  =   32;
                final String         pdiINIStr  = SerializableDeepClone.toINI(pdiConfig1);
                final ProgPDI.Config pdiConfig2 = SerializableDeepClone.fromINI(pdiINIStr, ProgPDI.Config.class);
                SysUtil.stdDbg().println(pdiConfig1);
                SysUtil.stdDbg().println(pdiConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(pdiConfig1, pdiConfig2) );

                final ProgSWD.Config swdConfig1 = new ProgSWD.Config();
                                                      swdConfig1.memorySRAM.address     = ProgSWD.DefaultCortexM_SRAMStart;
                                                      swdConfig1.memorySRAM.totalSize   = 32768;
                                                    //swdConfig1.memoryFlash.driverName = "ats am \" \\ dx";
                                                      swdConfig1.memoryFlash.driverName = "atsamdx";
                                                      swdConfig1.memoryFlash.address    = 0x00000000L;
                                                      swdConfig1.memoryFlash.totalSize  = 262144;
                                                      swdConfig1.memoryFlash.pageSize   =     64;
                final String         swdINIStr  = SerializableDeepClone.toINI(swdConfig1);
                final ProgSWD.Config swdConfig2 = SerializableDeepClone.fromINI(swdINIStr, ProgSWD.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(swdINIStr );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(swdConfig1);
                SysUtil.stdDbg().println(swdConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swdConfig1, swdConfig2) );
                SysUtil.stdDbg().println();

                final ProgSWIM.Config swimConfigS1 = new ProgSWIM.ConfigSTM8S();
                                                        swimConfigS1.memoryFlash.address    = 0x8000;
                                                        swimConfigS1.memoryFlash.totalSize  = 8192;
                                                        swimConfigS1.memoryFlash.pageSize   =   64;
                                                        swimConfigS1.memoryFlash.numPages   =  128;

                                                        swimConfigS1.memoryEEPROM.address   = 0x4000;
                                                        swimConfigS1.memoryEEPROM.totalSize = 640;
                                                        swimConfigS1.memoryEEPROM.pageSize  =  64;
                                                        swimConfigS1.memoryEEPROM.numPages  =  10;
                final String          swimINIStrS  = SerializableDeepClone.toINI(swimConfigS1);
                final ProgSWIM.Config swimConfigS2 = SerializableDeepClone.fromINI(swimINIStrS, ProgSWIM.ConfigSTM8S.class);
                SysUtil.stdDbg().println(swimConfigS1);
                SysUtil.stdDbg().println(swimConfigS2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swimConfigS1, swimConfigS2) );
                SysUtil.stdDbg().println();

                final ProgSWIM.Config swimConfigL1 = new ProgSWIM.ConfigSTM8L();
                                                        swimConfigL1.memoryFlash.address    = 0x8000;
                                                        swimConfigL1.memoryFlash.totalSize  = 8192;
                                                        swimConfigL1.memoryFlash.pageSize   =   64;
                                                        swimConfigL1.memoryFlash.numPages   =  128;

                                                        swimConfigL1.memoryEEPROM.address   = 0x1000;
                                                        swimConfigL1.memoryEEPROM.totalSize = 256;
                                                        swimConfigL1.memoryEEPROM.pageSize  =  64;
                                                        swimConfigL1.memoryEEPROM.numPages  =   4;
                final String          swimINIStrL  = SerializableDeepClone.toINI(swimConfigL1);
                final ProgSWIM.Config swimConfigL2 = SerializableDeepClone.fromINI(swimINIStrL, ProgSWIM.ConfigSTM8L.class);
                SysUtil.stdDbg().println(swimConfigL1);
                SysUtil.stdDbg().println(swimConfigL2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(swimConfigL1, swimConfigL2) );
                SysUtil.stdDbg().println();

                final ProgPIC18.Config picConfig1 = ProgPIC18.Config.PIC18F4520();
                final String           picINISt1  = SerializableDeepClone.toINI(picConfig1);
                final ProgPIC18.Config picConfig2 = SerializableDeepClone.fromINI(picINISt1, ProgPIC18.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picINISt1 );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig1);
                SysUtil.stdDbg().println(picConfig2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig1, picConfig2) );
                SysUtil.stdDbg().println();

                final ProgPIC16.Config picConfig3 = ProgPIC16.Config.PIC16F676();
                final String           picINISt3  = SerializableDeepClone.toINI(picConfig3);
                final ProgPIC16.Config picConfig4 = SerializableDeepClone.fromINI(picINISt3, ProgPIC16.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picINISt3 );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig3);
                SysUtil.stdDbg().println(picConfig4);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig3, picConfig4) );
                SysUtil.stdDbg().println();

                final ProgPIC24.Config picConfig5 = ProgPIC24.Config.PIC24FJ64GB00x();
                final String           picINISt4  = SerializableDeepClone.toINI(picConfig5);
                final ProgPIC24.Config picConfig6 = SerializableDeepClone.fromINI(picINISt4, ProgPIC24.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picINISt4 );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(picConfig5);
                SysUtil.stdDbg().println(picConfig6);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(picConfig5, picConfig6) );
                SysUtil.stdDbg().println();

                final ProgLGT8.Config lgt8Config1 = ProgLGT8.Config.LGT8F328P();
                final String          lgt8INIStr  = SerializableDeepClone.toINI(lgt8Config1);
                final ProgLGT8.Config lgt8Config2 = SerializableDeepClone.fromINI(lgt8INIStr, ProgLGT8.Config.class);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(lgt8INIStr );
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println(lgt8Config1);
                SysUtil.stdDbg().println(lgt8Config2);
                SysUtil.stdDbg().println( SerializableDeepClone.equals(lgt8Config1, lgt8Config2) );
                SysUtil.stdDbg().println();

                //*
                if(true) {
                    final String filePath = "1-TestData/PTestF.ProgISP-ATmega328P.ini";
                    INIDecoder.decode     ( Paths.get(filePath)                       );
                    INIDecoder.deserialize( Paths.get(filePath), ProgISP.Config.class );
                    SysUtil.stdDbg().println();
                    SysUtil.stdDbg().println(filePath);
                    SysUtil.stdDbg().println();
                    SysUtil.stdDbg().println( SerializableDeepClone.fromPSpecStr( SysUtil.readTextFileAsString(filePath), ProgISP.Config.class ) );
                    SysUtil.stdDbg().println();
                }
                if(true) {
                    final String filePath = "1-TestData/PTestF.ProgPIC16-PIC16F54.ini";
                    INIDecoder.decode     ( Paths.get(filePath)                         );
                    INIDecoder.deserialize( Paths.get(filePath), ProgPIC16.Config.class );
                    SysUtil.stdDbg().println();
                    SysUtil.stdDbg().println(filePath);
                    SysUtil.stdDbg().println();
                    SysUtil.stdDbg().println( SerializableDeepClone.fromPSpecStr( SysUtil.readTextFileAsString(filePath), ProgPIC16.Config.class ) );
                    SysUtil.stdDbg().println();
                }
                //*/

                /*
                SysUtil.stdDbg().println( ProgExec.getProgConfigBasicINIStr("ISP:ATmega328P") );
                SysUtil.stdDbg().println( SerializableDeepClone.equals(
                    SerializableDeepClone.fromINI( ProgExec.getProgConfigBasicINIStr("ISP:ATmega328P"), ProgISP.Config.class ),
                    ProgISP.Config.ATmega328P()
                ) );
                //*/

                /*
                SysUtil.stdDbg().println( ProgExec.getProgConfigBasicINIStr("PIC:PIC16F54") );
                SysUtil.stdDbg().println( SerializableDeepClone.equals(
                    SerializableDeepClone.fromINI( ProgExec.getProgConfigBasicINIStr("PIC:PIC16F54"), ProgPIC16.Config.class ),
                    ProgPIC16.Config.PIC16F54()
                ) );
                //*/

                XCom.setEnableAllExceptionStackTrace(false);
                SysUtil.stdDbg().println();
            }

        } // try
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class GTest1
