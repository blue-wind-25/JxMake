/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 *
 * Made based on 'https://sourceforge.net/p/ecxx/code/HEAD/tree/trunk/tools/src/SerialConsole.java'
 */


package jxm;


import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.Socket;
import java.net.SocketException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.text.DefaultCaret;

import com.fazecast.jSerialComm.*;

import jxm.gcomp.*;
import jxm.tool.*;
import jxm.xb.*;


public class SerialConsole {

    // The default command history file name
    public static final String DEFAULT_CMD_HIST_F_NAME = "jxmake_serial_console_command_history";
    public static final int    COMMAND_HISTORY_SIZE    = 100;

    // The lowest possible baudrate that would still be supported on most platforms which is not the magic baudrate
    private static final int LOWEST_BAUDRATE_DEF = SerialPortUtil.LowestBaudrate;
    private static final int LOWEST_BAUDRATE_ALT = SerialPortUtil.LowestBaudrateAlt;

    private              int LOWEST_BAUDRATE_SEL =   0;

    // Reset pulse time
    private static final int RESET_PULSE_TIME    = SerialPortUtil.ResetPulseTimeMS;

    // Texts
    private static final String Str_Start = "⏴";
    private static final String Str_Stop  = "⏹";
    private static final String Stt_Start = Texts.SerCon_Stt_Start;
    private static final String Stt_Stop  = Texts.SerCon_Stt_Stop;

    // Communication-related variables
    private SerialPort        _ttyPort         = null;
    private BufferedReader    _ttyReader       = null;
    private BufferedWriter    _ttyWriter       = null;

    private Socket            _tcpSocket       = null;
    private BufferedReader    _tcpReader       = null;
    private BufferedWriter    _tcpWriter       = null;

    // Command history
    private CommandHistory    _commandHistory  = new CommandHistory(COMMAND_HISTORY_SIZE);

    // GUI-related variables
    private boolean           _serialPlotter   = false;
    private boolean           _showTextOutput  = false;
    private boolean           _scPause         = false;

    private JFileChooser      _jfcSaveImageAs  = null;
    private JFrame            _mainWindow      = null;
    private JScrollPane       _scpTextOutput   = null;
    private JTextArea         _txtOutput       = null;
    private JPanel            _pnlDrawChart    = null;
    private JDrawChart        _chtDrawChart    = null;
    private JButton           _btnReset        = null;
    private JTextFieldPH      _txtInput        = null;

    // Flags
    private boolean           _initialized     = false;
    private boolean           _inResetFunc     = false;

    private boolean           _windowClosed    = false;
    private boolean           _ttyDisconnected = false;
    private boolean           _tcpDisconnected = false;

    private boolean           _execError       = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SerialConsole()
    {}

    public SerialConsole(final boolean serialPlotterMode)
    { _serialPlotter = serialPlotterMode; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Reset MCU
    private void _resetMCU(final String device, final int baudrate)
    {
        // Set flag
        _inResetFunc = true;

        // Write some informational message
        _txtOutput.append(Texts.SerCon_ResetInfoMsg1);

        // Delay for a while
        SysUtil.sleepMS(RESET_PULSE_TIME * 2);

        // Send a reset pulse via DTR
        // --- With RTS high
        _ttyPort.setRTS  ();
        _ttyPort.clearDTR();
        SysUtil.sleepMS(RESET_PULSE_TIME);
        _ttyPort.setDTR();
        // --- With RTS low
        _ttyPort.clearRTS();
        _ttyPort.clearDTR();
        SysUtil.sleepMS(RESET_PULSE_TIME);
        _ttyPort.setDTR();

        // Restore the baudrate
        _ttyPort.setBaudRate​(LOWEST_BAUDRATE_SEL);
        _ttyPort.setBaudRate​(baudrate);

        // Write some informational message
        _txtOutput.append(Texts.SerCon_ResetInfoMsg2);

        // Reset flag
        _inResetFunc = false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _txtInput_originalText = "";

    // Disable controls in case the connection is no longer available
    private void _disableControlsCNA(final boolean tty)
    {
        // Set flag
        if(tty) _ttyDisconnected = true;
        else    _tcpDisconnected = true;

        // Disable some controls
        _btnReset.setEnabled(false);
        _txtInput.setEnabled(false);

        // Save the original text and show message using the input box
        _txtInput_originalText = _txtInput.getText();

        final String type = tty ? Texts.SerCon_CNA_SerPort : Texts.SerCon_CNA_TCPSerBrg;

        _txtInput.setText( String.format(Texts.SerCon_CNA_NotConn, type) );

        // Ensure the GUI is updated
        Thread.yield();
        SysUtil.sleepMS(100);
    }

    // Enable controls in case the connection is restored
    private void _enableControlsCNA(final boolean tty)
    {
        // Enable some controls
        _btnReset.setEnabled(tty );
        _txtInput.setEnabled(true);

        // Restore the text
        _txtInput.setText(_txtInput_originalText);

        // Reset flag
        if(tty) _ttyDisconnected = false;
        else    _tcpDisconnected = false;
    }

    private void _checkTCPDisconnection(final SocketException e) throws SocketException
    {
        // Re-throw if it is because of disconnection
        final String lcEMsg = e.toString().toLowerCase();

        /*
        SysUtil.stdDbg().println(lcEMsg);
        //*/

        // ##### !!! TODO : Does this work on systems with non-English languages? !!! #####
        // ##### !!! TODO : How about other operating systems? !!! #####
        if( lcEMsg.indexOf("forcibly closed" ) < 0 &&
            lcEMsg.indexOf("connection reset") < 0 &&
            lcEMsg.indexOf("broken pipe"     ) < 0
          ) throw e; // Re-throw the exception as needed

        // Print the stack trace if requested
        if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();

        // Disable some controls
        _disableControlsCNA(false);

        // NOTE : No auto reconnection for TCP <-> Serial Bridge because the remote-side
        //        baudrate would not be restored properly!
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Open the target
    private void _openTarget(final boolean tty, final String target, final int config) throws Exception
    {
        // Check if the target is empty
        if( target.isEmpty() ) throw XCom.newException(Texts.SerCon_Err_NoTarget);

        // Normal serial port
        if(tty) {
            // Select the lowest baudrate
            LOWEST_BAUDRATE_SEL = LOWEST_BAUDRATE_DEF;
            if(LOWEST_BAUDRATE_SEL == config) LOWEST_BAUDRATE_SEL = LOWEST_BAUDRATE_ALT;
            // Try to disable the hangup signal (POSIX only)
            // stty -F <device> cs8 <baudrate> ignbrk noflsh -brkint -crtscts -echo -echoctl -echoe -echok -echoke -hupcl -icanon -icrnl -iexten -imaxbel -isig -ixon -onlcr -opost
            if( SysUtil.osIsPOSIX() ) {
                try {
                  //final Process process = Runtime.getRuntime().exec( String.format("stty -F %s -hupcl", target) );
                    final Process process = Runtime.getRuntime().exec( new String[] { "stty", "-F", target, "-hupcl" } );
                    process.waitFor();
                    if( process.exitValue() != 0 ) {
                        final BufferedReader stdError = new BufferedReader(
                            new InputStreamReader( process.getErrorStream() )
                        );
                        if( XCom.enableAllExceptionStackTrace() ) XCom.newException(
                                                                      '\n' + stdError.lines().collect( Collectors.joining("\n") ) + '\n'
                                                                  ).printStackTrace();
                    }
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                }
            }
            // Open the port and set some settings
            _ttyPort = SerialPort.getCommPort(target);
            _ttyPort.setFlowControl​    (SerialPort.FLOW_CONTROL_DISABLED);
            _ttyPort.setBaudRate​       (LOWEST_BAUDRATE_SEL);
            _ttyPort.setNumDataBits​    (8);
            _ttyPort.setParity​         (SerialPort.NO_PARITY);
            _ttyPort.setNumStopBits​    (SerialPort.ONE_STOP_BIT);
            _ttyPort.setComPortTimeouts​(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 1000);
            _ttyPort.setRTS            ();
            _ttyPort.setDTR            ();
            _ttyPort.openPort          ();
            _ttyPort.setRTS            ();
            _ttyPort.setDTR            ();
            // Get the stream
            _ttyReader = new BufferedReader( new InputStreamReader ( _ttyPort.getInputStream​ (), SysUtil._CharEncoding ) );
            _ttyWriter = new BufferedWriter( new OutputStreamWriter( _ttyPort.getOutputStream(), SysUtil._CharEncoding ) );
            // Set the baudrate to the intended baudrate
            _ttyPort.setBaudRate(config);
            _ttyPort.setBaudRate​(LOWEST_BAUDRATE_SEL);
            _ttyPort.setBaudRate(config);
            // Add a data listener to catch the disconnection event
            _ttyPort.addDataListener​( new SerialPortDataListener() {
                public int getListeningEvents​() {
                    return SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
                }
                public void serialEvent​(SerialPortEvent event) {
                    // Check if the event is not the one we want
                    if( event.getEventType​() != SerialPort.LISTENING_EVENT_PORT_DISCONNECTED ) return;
                    // Disable some controls
                    _disableControlsCNA(true);
                    // Close the stream and serial port
                    try { _ttyReader.close    (); } catch(final Exception e) {}
                    try { _ttyWriter.close    (); } catch(final Exception e) {}
                          _ttyPort  .closePort();
                    // Initialize the auto reconnection thread
                    final Thread runARC = new Thread() {
                        @Override
                        public void run() {
                            while(!_windowClosed) {
                                // Yield
                                Thread.yield();
                                // Try to reconnect
                                try {
                                    // Open the port
                                    if( !_ttyPort.openPort() ) continue;
                                    // Set signal states
                                    _ttyPort.setRTS();
                                    _ttyPort.setDTR();
                                    // Get the stream
                                    _ttyReader = new BufferedReader( new InputStreamReader ( _ttyPort.getInputStream​ (), SysUtil._CharEncoding ) );
                                    _ttyWriter = new BufferedWriter( new OutputStreamWriter( _ttyPort.getOutputStream(), SysUtil._CharEncoding ) );
                                    // Enable some controls
                                    _enableControlsCNA(true);
                                    // Break
                                    break;
                                }
                                catch(final Exception e) {}
                            }
                        }
                    };
                    runARC.setDaemon(true);
                    runARC.start();
                }
            } );
        }

        // TCP <-> serial bridge
        else {
            // Open the socket
            _tcpSocket = new Socket(target, config);
            // Get the stream
            _tcpReader = new BufferedReader( new InputStreamReader ( _tcpSocket.getInputStream (), SysUtil._CharEncoding ) );
            _tcpWriter = new BufferedWriter( new OutputStreamWriter( _tcpSocket.getOutputStream(), SysUtil._CharEncoding ) );
        }
    }

    // Show text output (serial plotter mode only)
    private void _showTextOutput()
    {
        if(!_serialPlotter) return;

        _mainWindow.remove(_pnlDrawChart);
        _mainWindow.add(_scpTextOutput, BorderLayout.CENTER);

        _txtOutput.setText("");

        _mainWindow.revalidate();
        _mainWindow.repaint();

        Thread.yield();
        SysUtil.sleepMS(100);

        _showTextOutput = true;

        _spReset();
    }

    // Show draw chart (serial plotter mode only)
    private void _showDrawChart()
    {
        if(!_serialPlotter) return;

        _mainWindow.remove(_scpTextOutput);
        _mainWindow.add(_pnlDrawChart, BorderLayout.CENTER);

        _mainWindow.revalidate();
        _mainWindow.repaint();

        Thread.yield();
        SysUtil.sleepMS(100);

        _showTextOutput = false;
    }

    // Initialize all
    private void _initializeAll(final boolean tty, final String target, final int config, final int initLEMode) throws Exception
    {
        // Initialize the save image file selector
        _jfcSaveImageAs = new JFileChooser( SysUtil.getCWD() );
        _jfcSaveImageAs.setAcceptAllFileFilterUsed(false);
        _jfcSaveImageAs.setFileFilter( new FileNameExtensionFilter(Texts.SerCon_PNGImages, "png") );
        _jfcSaveImageAs.setDialogTitle(Texts.SerCon_SaveImageAs);

        // Open the target
        _openTarget(tty, target, config);

        // Create the GUI - main window
        final String SConfig   = String.format("%d", config);
        final String ModeTitle = (_serialPlotter ? Texts.SerCon_ModeTitleP : Texts.SerCon_ModeTitleC);

        if(_ttyPort != null)
            _mainWindow = new JFrame( String.format(Texts.SerCon_WinTitleTTY, ModeTitle) + " - " + target + ":" + SConfig + "/8N1" );
        else
            _mainWindow = new JFrame( String.format(Texts.SerCon_WinTitleTCP, ModeTitle) + " - " + target + ":" + SConfig          );

        _mainWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        _mainWindow.setLayout( new BorderLayout() );
        _mainWindow.getRootPane().setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );

        // Create the GUI - output area
        _txtOutput = new JTextArea(20, 100);
        _txtOutput.setEditable(false);
        _txtOutput.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12) );
        _txtOutput.setBorder( new CompoundBorder( _txtOutput.getBorder(), new EmptyBorder(5, 5, 5, 5) ) );

        final DefaultCaret caret = (DefaultCaret) _txtOutput.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        _scpTextOutput = new JScrollPane(_txtOutput);
        _scpTextOutput.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        _scpTextOutput.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        if(!_serialPlotter)  {
            _mainWindow.add(_scpTextOutput, BorderLayout.CENTER);
        }
        else {

            _pnlDrawChart = new JPanel();
            _pnlDrawChart.setLayout( new BorderLayout() );
            _pnlDrawChart.setBorder( new CompoundBorder( new EtchedBorder(EtchedBorder.RAISED), new EmptyBorder(5, 5, 5, 5) ) );

                final JPanel pnlChartTool = new JPanel();
                pnlChartTool.setLayout( new BorderLayout() );

                    final JToolBar tlbChartTool = new JToolBar("ChartTool", JToolBar.HORIZONTAL);
                    tlbChartTool.setBorder( new CompoundBorder( new BevelBorder(BevelBorder.RAISED), new EmptyBorder(3, 3, 3, 3) ) );
                    tlbChartTool.setFloatable(false);
                    tlbChartTool.setRollover (true );

                    final int       dimCell    = 35;
                    final Dimension dimBtnSize = new Dimension(dimCell, dimCell);

                    for(int i = 0; i < JDrawChart.NumOfChannels; ++i) {
                        final JToggleButton btnChn = new JToggleButton( String.valueOf(i + 1), true );
                            btnChn.setPreferredSize( dimBtnSize                                        );
                            btnChn.setMinimumSize  ( dimBtnSize                                        );
                            btnChn.setMaximumSize  ( dimBtnSize                                        );
                            btnChn.setFocusPainted ( false                                             );
                            btnChn.setToolTipText  ( String.format(Texts.SerCon_TurnOffChannel, i + 1) );
                            btnChn.addItemListener ( new ItemListener() {
                                @Override
                                public void itemStateChanged(final ItemEvent itemEvent) {
                                    final JToggleButton btn = (JToggleButton) itemEvent.getItem();
                                    final int           chn = Integer.valueOf( btn.getText() );
                                    final boolean       sel = btn.isSelected();
                                    _chtDrawChart.setChannelVisibility(chn - 1, sel);
                                    if(sel) btnChn.setToolTipText( String.format(Texts.SerCon_TurnOffChannel, chn) );
                                    else    btnChn.setToolTipText( String.format(Texts.SerCon_TurnOnChannel , chn) );
                                }
                            } );
                        tlbChartTool.add(btnChn);
                    }

                    final Dimension dimSeparator = new Dimension(dimCell / 5, dimCell);
                    tlbChartTool.addSeparator(dimSeparator);

                    final JSlider sldXWPos = new JSlider(0, _spXWindowCount, _spXWindowPos); // This control will be added to the toolbar after 'cmbScale
                    sldXWPos.setBorder          ( new CompoundBorder( new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1, 0, 0, 7) ) );
                    sldXWPos.setOpaque          ( false                                 );
                    sldXWPos.setPaintTicks      ( true                                  );
                    sldXWPos.setMajorTickSpacing( _spXWindowCount / SPXWindowPosRes * 2 );
                    sldXWPos.setMinorTickSpacing( _spXWindowCount / SPXWindowPosRes / 2 );
                    sldXWPos.setPreferredSize   ( new Dimension(dimCell * 5, dimCell)   );
                    sldXWPos.setMinimumSize     ( sldXWPos.getPreferredSize()           );
                    sldXWPos.setMaximumSize     ( sldXWPos.getPreferredSize()           );
                    sldXWPos.setToolTipText     ( Texts.SerCon_XAxisVWPos               );
                    sldXWPos.setEnabled         ( false                                 );
                    sldXWPos.addChangeListener  ( new ChangeListener() {
                        @Override
                        public void stateChanged(final ChangeEvent ignored) {
                            if( sldXWPos.isEnabled() ) _spXWindowPos = sldXWPos.getValue();
                        }
                    } );

                    final JComboBox<String> smbBuffSize = new JComboBox<>();
                    smbBuffSize.addItem("512");
                    smbBuffSize.addItem("256");
                    smbBuffSize.addItem("128");
                    smbBuffSize.addItem( "64");
                    smbBuffSize.addItem( "32");
                    smbBuffSize.setSelectedIndex(2); // NOTE : Ensure this selection matches the initial value of '_spXWindowSize' below
                    smbBuffSize.setPreferredSize( new Dimension(dimCell * 2, dimCell) );
                    smbBuffSize.setMinimumSize  ( smbBuffSize.getPreferredSize()      );
                    smbBuffSize.setMaximumSize  ( smbBuffSize.getPreferredSize()      );
                    smbBuffSize.setToolTipText  ( Texts.SerCon_XAxisVWSize            );
                    smbBuffSize.addItemListener ( new ItemListener() {
                        @Override
                        public void itemStateChanged(final ItemEvent itemEvent) {
                            if(itemEvent.getStateChange() != ItemEvent.SELECTED) return;
                            final int oldWindowCount = _spXWindowCount;
                            _spXWindowSize  = Integer.valueOf( (String) smbBuffSize.getSelectedItem() );
                            _spXWindowCount = SPBufferSize * SPXWindowPosRes / _spXWindowSize;
                            _spXWindowPos   = _spXWindowPos * _spXWindowCount / oldWindowCount;
                            sldXWPos.setEnabled         ( false                                 );
                            sldXWPos.setMaximum         ( _spXWindowCount                       );
                            sldXWPos.setValue           ( _spXWindowPos                         );
                            sldXWPos.setMajorTickSpacing( _spXWindowCount / SPXWindowPosRes * 2 );
                            sldXWPos.setMinorTickSpacing( _spXWindowCount / SPXWindowPosRes / 2 );
                            sldXWPos.setEnabled         ( _spPause                              );
                        }
                    } );
                    tlbChartTool.add(smbBuffSize);

                    tlbChartTool.addSeparator(dimSeparator);

                    final JComboBox<String> cmbScale = new JComboBox<>();
                    cmbScale.addItem("x 0.00001");
                    cmbScale.addItem("x 0.0001" );
                    cmbScale.addItem("x 0.001"  );
                    cmbScale.addItem("x 0.01"   );
                    cmbScale.addItem("x 0.1"    );
                    cmbScale.addItem("x 1"      );
                    cmbScale.addItem("x 10"     );
                    cmbScale.addItem("x 100"    );
                    cmbScale.addItem("x 1000"   );
                    cmbScale.addItem("x 10000"  );
                    cmbScale.addItem("x 100000" );
                    cmbScale.setSelectedIndex(5); // NOTE : Ensure this selection matches the initial value of '_spScale' below
                    cmbScale.setPreferredSize( new Dimension(dimCell * 3, dimCell) );
                    cmbScale.setMinimumSize  ( cmbScale.getPreferredSize()         );
                    cmbScale.setMaximumSize  ( cmbScale.getPreferredSize()         );
                    cmbScale.setToolTipText  ( Texts.SerCon_YAxisScale             );
                    cmbScale.addItemListener( new ItemListener() {
                        @Override
                        public void itemStateChanged(final ItemEvent itemEvent) {
                            if(itemEvent.getStateChange() != ItemEvent.SELECTED) return;
                            _spScale = Math.pow(10, cmbScale.getSelectedIndex() - 5 );
                        }
                    } );
                    tlbChartTool.add(cmbScale);

                    tlbChartTool.add( Box.createHorizontalGlue() );

                    tlbChartTool.add(sldXWPos);

                    tlbChartTool.addSeparator(dimSeparator);

                    final JButton btnSaveImg = new JButton("⎙");
                    btnSaveImg.setPreferredSize(dimBtnSize            );
                    btnSaveImg.setMinimumSize  (dimBtnSize            );
                    btnSaveImg.setMaximumSize  (dimBtnSize            );
                    btnSaveImg.setFocusPainted (false                 );
                    btnSaveImg.setToolTipText  (Texts.SerCon_SaveImage);
                    btnSaveImg.setEnabled      (false                 );
                    btnSaveImg.addActionListener( new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent ignored) {
                            // Show save dialog
                            _chtDrawChart.setNoDraw(true);
                            final int res = _jfcSaveImageAs.showSaveDialog(_mainWindow);
                            _chtDrawChart.setNoDraw(false);
                            // Check the user choice
                            if(res != JFileChooser.APPROVE_OPTION) return;
                            // Save image
                            try {
                                // Get the file path
                                String filePath = _jfcSaveImageAs.getSelectedFile().getPath().toString();
                                if( !SysUtil.getFileExtension(filePath).toLowerCase().equals("png") ) filePath += ".png";
                                // Save the image
                                SysUtil.showWaitCursor(_mainWindow);
                                _chtDrawChart.saveImage(filePath);
                                SysUtil.showDefaultCursor(_mainWindow);
                            }
                            catch(final Exception e) {
                                SysUtil.showDefaultCursor(_mainWindow);
                                _chtDrawChart.setNoDraw(true);
                                JOptionPane.showMessageDialog( _mainWindow, e.toString(), Texts.SerCon_ErrorTitle, JOptionPane.ERROR_MESSAGE );
                                _chtDrawChart.setNoDraw(false);
                            }
                        }
                    } );
                    tlbChartTool.add(btnSaveImg);

                    tlbChartTool.addSeparator(dimSeparator);

                    final JButton btnStartStop = new JButton(Str_Stop);
                    btnStartStop.setPreferredSize(dimBtnSize);
                    btnStartStop.setMinimumSize  (dimBtnSize);
                    btnStartStop.setMaximumSize  (dimBtnSize);
                    btnStartStop.setFocusPainted (false     );
                    btnStartStop.setToolTipText  (Stt_Stop  );
                    btnStartStop.addActionListener( new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent ignored) {
                            if( btnStartStop.getText().equals(Str_Stop) ) {
                                btnStartStop.setText       (Str_Start);
                                btnStartStop.setToolTipText(Stt_Start);
                                _spPause = true;
                            }
                            else {
                                btnStartStop.setText       (Str_Stop);
                                btnStartStop.setToolTipText(Stt_Stop);
                                _spPause = false;
                            }
                            sldXWPos  .setEnabled(_spPause);
                            btnSaveImg.setEnabled(_spPause);
                            if(!_spPause) {
                                _spXWindowPos = _spXWindowCount;
                                sldXWPos.setValue(_spXWindowPos);
                            }
                        }
                    } );
                    tlbChartTool.add(btnStartStop);

                pnlChartTool.add(tlbChartTool, BorderLayout.CENTER);

            _pnlDrawChart.add(pnlChartTool, BorderLayout.NORTH);

            final JPanel pnlChartInternal = new JPanel();
            pnlChartInternal.setLayout( new BorderLayout() );
            pnlChartInternal.setBorder( new EmptyBorder(10, 0, 0, 0) );

                _chtDrawChart = new JDrawChart(this);
                pnlChartInternal.add(_chtDrawChart, BorderLayout.CENTER);

            _pnlDrawChart.add(pnlChartInternal, BorderLayout.CENTER);

            _mainWindow.add(_pnlDrawChart, BorderLayout.CENTER);

        } // if !_serialPlotter

        // Create the GUI - input area
        final JPanel panelInMain = new JPanel();
        panelInMain.setLayout( new BorderLayout(5, 0) );
        panelInMain.setBorder( BorderFactory.createEmptyBorder(10, 0, 0, 0) );

            final JPanel panelLeft = new JPanel();
            panelLeft.setLayout( new BorderLayout() );

                final JPanel panelLeftSub = new JPanel();
                panelLeftSub.setLayout( new GridLayout(!_serialPlotter ? 3 : 1, 1, 0, 7) );

                    final JButton btnCopy = new JButton(Texts.SerCon_BtnCopy);
                    btnCopy.setBorder     ( new CompoundBorder( btnCopy.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );
                    btnCopy.setToolTipText( Texts.SerCon_BtnCopyTT                                                 );

                    final JButton btnClear = new JButton(Texts.SerCon_BtnClear);
                    btnClear.setBorder( new CompoundBorder( btnClear.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );
                    btnClear.setToolTipText( Texts.SerCon_BtnClearTT                                            );

                    _btnReset = new JButton(Texts.SerCon_BtnReset);
                    _btnReset.setBorder( new CompoundBorder( _btnReset.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );
                    _btnReset.setToolTipText( Texts.SerCon_BtnResetTT                                             );
                    if(!tty) _btnReset.setEnabled(false);

                if(!_serialPlotter) panelLeftSub.add( btnCopy );
                if(!_serialPlotter) panelLeftSub.add( btnClear);
                                    panelLeftSub.add(_btnReset);

            panelLeft.add( panelLeftSub, BorderLayout.PAGE_START );
            panelLeft.add( new JPanel(), BorderLayout.CENTER     );

            final JPanel panelMiddle = new JPanel();
            panelMiddle.setLayout( new BorderLayout() );

                final JPanel panelMiddleLayout = new JPanel();
                panelMiddleLayout.setLayout( new BorderLayout() );

                    _txtInput = new JTextFieldPH();
                    _txtInput.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12) );
                    _txtInput.setBorder( new CompoundBorder( _txtInput.getBorder(), new EmptyBorder(5, 5, 5, 5) ) );
                    _txtInput.setPlaceholder(Texts.SerCon_TxtInputPHText);

                    final JLabel lblPadding = new JLabel("");
                    lblPadding.setPreferredSize( new Dimension(7, 7) );
                    lblPadding.setMinimumSize  ( lblPadding.getPreferredSize()         );
                    lblPadding.setMaximumSize  ( lblPadding.getPreferredSize()         );

                    final JPanel panelMiddleTool = new JPanel();
                    panelMiddleTool.setLayout( new BorderLayout() );

                        final JPanel panelMiddleToolGrid = new JPanel();
                        panelMiddleToolGrid.setLayout( new GridLayout (1, 1, 5, 5) );
                        panelMiddleToolGrid.setBorder( new EmptyBorder(0, 0, 0, 1) );

                            final JButton btnStartStop = new JButton(Str_Stop);
                            btnStartStop.setBorder        ( new CompoundBorder( btnStartStop.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );
                            btnStartStop.setToolTipText   ( Stt_Stop                                                                    );
                            btnStartStop.addActionListener( new ActionListener() {
                                @Override
                                public void actionPerformed(final ActionEvent ignored) {
                                    // Start/stop the text output
                                    if( btnStartStop.getText().equals(Str_Stop) ) {
                                        btnStartStop.setText       (Str_Start);
                                        btnStartStop.setToolTipText(Stt_Start);
                                        _scPause = true;
                                    }
                                    else {
                                        btnStartStop.setText       (Str_Stop);
                                        btnStartStop.setToolTipText(Stt_Stop);
                                        _scPause = false;
                                    }
                                    // Set focus to the input text field
                                    _txtInput.requestFocusInWindow();
                                }
                            } );

                        panelMiddleToolGrid.add(btnStartStop);

                    panelMiddleTool.add(panelMiddleToolGrid, BorderLayout.LINE_END);

                                    panelMiddleLayout.add(_txtInput      , BorderLayout.NORTH );
                if(!_serialPlotter) panelMiddleLayout.add(lblPadding     , BorderLayout.CENTER);
                if(!_serialPlotter) panelMiddleLayout.add(panelMiddleTool, BorderLayout.SOUTH );

            panelMiddle.add(panelMiddleLayout, BorderLayout.PAGE_START);

            final JPanel panelRight = new JPanel();
            panelRight.setLayout( new BorderLayout() );

                final String[]      lineEndStr  = { "[none]", "LF", "CR", "CRLF", "LFCR" };
                final char[]        lineEndChr1 = { 0       , '\n', '\r', '\r'  , '\n'   };
                final char[]        lineEndChr2 = { 0       , 0   , 0   , '\n'  , '\r'   };
                final JList<String> lstLineEnd = new JList<String>(lineEndStr);
                lstLineEnd.setBorder       ( new CompoundBorder( new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 3, 3, 3) ) );
                lstLineEnd.setToolTipText  ( Texts.SerCon_SendMsgLnEMode                                                               );
                lstLineEnd.setSelectedIndex( initLEMode                                                                                );

            panelRight.add(lstLineEnd, BorderLayout.CENTER);

        panelInMain.add(panelLeft  , BorderLayout.LINE_START);
        panelInMain.add(panelMiddle, BorderLayout.CENTER    );
        panelInMain.add(panelRight , BorderLayout.LINE_END  );

        _mainWindow.add(panelInMain, BorderLayout.PAGE_END);

        // Add event listener to the copy button
        btnCopy.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored) {
                // Copy to clipboard
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection( _txtOutput.getText() ),
                    null
                );
                // Set focus to the input text field
                _txtInput.requestFocusInWindow();
            }
        } );

        // Add event listener to the clear button
        btnClear.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored) {
                // Clear the text field
                _txtOutput.setText("");
                // Set focus to the input text field
                _txtInput.requestFocusInWindow();
            }
        } );

        // Add event listeners to the input text field
        final Runnable _txtInput_VK_ENTER = new Runnable() {
            @Override
            public void run() {
                // Save the text to the command history list
                final String text = _txtInput.getText();
                if( !text.isEmpty() ) _commandHistory.put(text);
                // Get the line ending character(s)
                final char leChar1 = lineEndChr1[ lstLineEnd.getSelectedIndex() ];
                final char leChar2 = lineEndChr2[ lstLineEnd.getSelectedIndex() ];
                // Write data
                try {
                    // TTY
                    if(_ttyPort != null) {
                        _ttyWriter.write(text);
                        if(leChar1 != 0) _ttyWriter.write(leChar1);
                        if(leChar2 != 0) _ttyWriter.write(leChar2);
                        _ttyWriter.flush();
                    }
                    // TCP
                    else {
                        try {
                            _tcpWriter.write(text);
                            if(leChar1 != 0) _tcpWriter.write(leChar1);
                            if(leChar2 != 0) _tcpWriter.write(leChar2);
                            _tcpWriter.flush();
                        }
                        catch(final SocketException e) {
                            _checkTCPDisconnection(e);
                        }
                    }
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Exit the program
                    _execError = true;
                    _mainWindow.dispose();
                }
                // Clear the text field
                if(!_tcpDisconnected) _txtInput.setText("");
            }
        };

        _txtInput.addKeyListener( new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent keyEvent) {

                switch( keyEvent.getKeyCode() ) {

                    // Check if it is the enter key
                    case KeyEvent.VK_ENTER:
                        // Handle the event
                        _txtInput_VK_ENTER.run();
                        // Done
                        break;

                    // Check if it is the up-arrow key
                    case KeyEvent.VK_UP    : /* FALLTHROUGH */
                    case KeyEvent.VK_KP_UP :
                        // Get the previous text
                        if(true) {
                            final String text = _commandHistory.getPrev();
                            if(text != null) _txtInput.setText(text);
                        }
                        // Done
                        break;

                    // Check if it is the down-arrow key
                    case KeyEvent.VK_DOWN    : /* FALLTHROUGH */
                    case KeyEvent.VK_KP_DOWN :
                        // Get the next text
                        if(true) {
                            final String text = _commandHistory.getNext();
                            _txtInput.setText( (text != null) ? text : "" );
                        }
                        // Done
                        break;

                } // case

            } // keyPressed()
        } );

        _txtInput.registerKeyboardAction( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored) {
                try {
                    final String   string = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    final String[] lines  = string.split( System.lineSeparator() );

                    if(lines.length <= 1) {
                        _txtInput.paste();
                        return;
                    }

                    for(int i = 0; i < lines.length; ++i) {
                        _txtInput.setText( lines[i] );
                        if(i < lines.length - 1) _txtInput_VK_ENTER.run();
                    }
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                }
            } // actionPerformed()
            },
            "Paste",
            KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_FOCUSED
        );

        // Add event listener to the list box
        lstLineEnd.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent ignored) {
                // Set focus to the input text field
                _txtInput.requestFocusInWindow();
            }
        } );

        // Add event listener to the reset button
        _btnReset.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored) {
                // Reset the MCU using thread so the GUI does not froze
                ( new Thread() {
                    public void run() {
                        if(_serialPlotter) _showTextOutput();
                        _resetMCU(target, config);
                    }
                } ).start();
                // Set focus to the input text field
                _txtInput.requestFocusInWindow();
            }
        } );

        // Add event listeners to the main window
        _mainWindow.addWindowFocusListener( new WindowFocusListener_HandleAlwaysOnTop(_mainWindow) );

        _mainWindow.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ignored) {
                _commandHistory.save();
            }

            @Override
            public void windowClosed(final WindowEvent ignored) {
                _windowClosed = true;
            }
        } );

        _mainWindow.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentHidden(final ComponentEvent ignored) {
                // Nothing to do here
            }

            @Override
            public void componentShown(final ComponentEvent ignored) {
                // Set flag to indicate that application initialization has been completed
                _initialized = true;
            }
        } );

        // Set the window size
        _mainWindow.setSize       (               900, 600  );
        _mainWindow.setMinimumSize( new Dimension(900, 600) );

        // Set the window position
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        _mainWindow.setLocation( ( dim.width - _mainWindow.getSize().width ) / 2, ( dim.height - _mainWindow.getSize().height ) / 3 );
      //_mainWindow.setLocationByPlatform(true);

        // Show the window
        _mainWindow.setAlwaysOnTop(true);
        _mainWindow.setVisible    (true);

        if(_chtDrawChart != null) _chtDrawChart.repaint();

        // Set focus to the input text field
        _txtInput.requestFocusInWindow();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Show console
    public String showConsole(final String args[]) throws Exception
    {
        // Get the command history file path
        _commandHistory.setFilePath(args[4]);

        // Get the initial line ending mode
        String leStr = args[3];
        if( leStr.equals("native") ) {
                 if( SysUtil.osIsWindows  () ) leStr = "crlf";
            else if( SysUtil.osIsMacLegacy() ) leStr = "cr";
            else                               leStr = "lf";
        }

        // Check the initial line ending mode
        int initLEMode = 0;

             if( leStr.equals("none") ) initLEMode = 0;
        else if( leStr.equals("lf"  ) ) initLEMode = 1;
        else if( leStr.equals("cr"  ) ) initLEMode = 2;
        else if( leStr.equals("crlf") ) initLEMode = 3;
        else if( leStr.equals("lfcr") ) initLEMode = 4;

        // Get the other arguments
        final boolean tty    = !args[0].equals("tcp");
        final String  target =  args[1];                  // TTY : device   ; TCP : hostname/IP
        final int     config = Integer.parseInt(args[2]); // TTY : baudrate ; TCP : port
        final int     ilem   = initLEMode;

        // Load command history
        _commandHistory.load();

        // Initialize the application
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                try {
                    _initializeAll(tty, target, config, ilem);
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Exit the program
                    _execError    = true;
                    _windowClosed = true;
                }
            }
        } );

        // Initialize the reader thread
        final Thread runSC = new Thread() {
            private void _handleChar(final int c)
            {
                if(_serialPlotter) {
                    _spGatherAndParseData( String.valueOf( (char) c ) );
                    if(_showTextOutput) _showDrawChart();
                    _chtDrawChart.refresh();
                }
                else if(!_scPause && c != 0 && c != 13) { // Discard '\0' and '\r'
                    _txtOutput.append( String.valueOf( (char) c ) );
                }
                Thread.yield();
            }

            @Override
            public void run() {
                while(!_windowClosed) {
                    // Yield
                    Thread.yield();
                    // Skip if the application is not fully initialized yet
                    if(!_initialized) continue;
                    // Read data
                    try {
                        // TTY
                        if(_ttyPort != null) {
                            while( !_ttyDisconnected && _ttyReader.ready() ) {
                                int c = -1;
                                try {
                                    c = _ttyReader.read();
                                }
                                catch(final SerialPortTimeoutException e) {}
                                if(c < 0) break;
                                if(!_inResetFunc) _handleChar(c);
                            } // while
                        }
                        // TCP
                        else {
                            while( !_tcpDisconnected && _tcpReader.ready() ) {
                                int c = -1;
                                try {
                                    c = _tcpReader.read();
                                }
                                catch(final SocketException e) {
                                    _checkTCPDisconnection(e);
                                }
                                if(c < 0) break;
                                _handleChar(c);
                            } // while
                        }
                    }
                    catch(final Exception e) {
                        // Print the stack trace if requested
                        if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                        // Stop the program if the error is not because of disconnection
                        if(!_ttyDisconnected && !_tcpDisconnected) {
                            while(!_windowClosed) Thread.yield();
                            break;
                        }
                    }
                    /*
                    // Add a newline as needed
                    if( !_txtOutput.getText().endsWith("\n") && !_txtOutput.getText().endsWith("\r") ) {
                        _txtOutput.append("\n");
                    }
                    */
                } // while
            }
        };

        runSC.start();
        runSC.join();

        // Close the stream
        try { _ttyReader.close(); } catch(final Exception e) {}
        try { _ttyWriter.close(); } catch(final Exception e) {}

        // Close the port/socket
        if(_ttyPort   != null) _ttyPort  .closePort();
        if(_tcpSocket != null) _tcpSocket.close();

        // Check for execution error
        if(_execError) return null;

        // Return the text as needed
        if(_serialPlotter) return "";
        return (_txtOutput == null) ? "" : _txtOutput.getText();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int SPBufferSize    = 8192;
    private static final int SPXWindowPosRes = 8;

    private static class SPFieldSet {
        private       long              _index;
        private final ArrayList<String> _varName  = new ArrayList<>();
        private final ArrayList<Double> _varValue = new ArrayList<>();

        private SPFieldSet(final long index, final ArrayList<String> data)
        {
            _index = index;

            for(int i = 0; i < data.size(); i += 2) {
                double value = 0.0;
                try {
                    value = Double.parseDouble( data.get(i + 1) );
                }
                catch(final Exception e) {}

                _varName .add( data.get(i) );
                _varValue.add( value       );
            }
        }
    }

    public static class SPDataSets {
        public final ArrayList<           String  > names    =  new ArrayList<>();
        public final ArrayList< ArrayList<Double> > values   =  new ArrayList<>();
        public final ArrayList<           Long    > index    =  new ArrayList<>();

        public       double                         minValue =  Double.MAX_VALUE;
        public       double                         maxValue = -Double.MAX_VALUE;

        public boolean isEmpty()
        { return names.isEmpty(); }
    }

    private       long                  _spDataCounter   = 0;

    private       String                _spFieldRStr     = "";
    private final ArrayList<String>     _spFieldPart     = new ArrayList<>();
    private final ArrayList<SPFieldSet> _spFieldList     = new ArrayList<>();

    private       int                   _spXWindowSize   = 128;
    private       int                   _spXWindowCount  = SPBufferSize * SPXWindowPosRes / _spXWindowSize;
    private       int                   _spXWindowPos    = _spXWindowCount;
    private       double                _spScale         = 1.0;

    private       boolean               _spPause         = false;

    private synchronized void _spReset()
    {
        _spDataCounter = 0;
        _spFieldRStr   = "";

        _spFieldPart.clear();

        for(final SPFieldSet spFieldSet : _spFieldList) spFieldSet._index = -1;
    }

    private synchronized void _spGatherAndParseData(final String str)
    {
        /*
         * Arduino standard data format is:
         *     Variable_1:Value1,Variable_2:Value2, ...
         *     ...
         */

        for(int i = 0; i < str.length(); ++i) {

            final char ch = str.charAt(i);

            switch(ch) {

                case '\t' : /* FALLTHROUGH */
                case ' '  : /* FALLTHROUGH */
                case ','  : /* FALLTHROUGH */
                case ':'  :
                    if( _spFieldPart.size() < JDrawChart.NumOfChannels ) _spFieldPart.add(_spFieldRStr);
                    _spFieldRStr = "";
                    break;

                case '\r' : /* FALLTHROUGH */
                case '\n' :
                    // Store pending field string as needed
                    if( !_spFieldRStr.isEmpty() ) {
                        if( _spFieldPart.size() < JDrawChart.NumOfChannels ) _spFieldPart.add(_spFieldRStr);
                        _spFieldRStr = "";
                    }
                    // Store the field set as neede
                    if( !_spFieldPart.isEmpty() ) {
                        // An odd number of items means an error
                        if( ( _spFieldPart.size() & 1 ) == 0 ) {
                            // Remove an element as needed
                            if( _spFieldList.size() >= SPBufferSize ) _spFieldList.remove(0);
                            // Store a new element as needed
                            if(!_spPause) _spFieldList.add( new SPFieldSet(_spDataCounter, _spFieldPart) );
                            // Increment the index
                            ++_spDataCounter;
                        }
                        else {
                            // Ignore the data on error
                        }
                    }
                    _spFieldPart.clear();
                    break;

                default:
                    // Append the character to the field string
                    _spFieldRStr += ch;
                    break;

            } // switch

        } // for
    }

    public synchronized SPDataSets spGetDataSets()
    {
        // Create a new data sets instance
        final SPDataSets spDataSets = new SPDataSets();

        if( _spFieldList.isEmpty() || _spFieldList.get(0)._varName.isEmpty() ) return spDataSets;

        // Prepare the minimum and maximum values
        spDataSets.minValue =  Double.MAX_VALUE;
        spDataSets.maxValue = -Double.MAX_VALUE;

        // Get the last field index
        final int lastFieldSetIndex = _spFieldList.size() - 1;

        // Get the number of sets
        final int setCount = _spFieldList.get(lastFieldSetIndex)._varName.size();

        // Store the names and create array list instances
        for(int i = 0; i < setCount; ++i) {
            spDataSets.names.add ( _spFieldList.get(lastFieldSetIndex)._varName.get(i) );
            spDataSets.values.add( new ArrayList<>()                                   );
        }

        // Loop as many as the window size
        final int idxOffset = (_spXWindowCount - _spXWindowPos) * _spXWindowSize / SPXWindowPosRes + _spXWindowSize;
        final int idxStart  = _spFieldList.size() - idxOffset;
        final int idxEnd    = idxStart + _spXWindowSize;

        for(int w = idxStart; w < idxEnd; ++w) {

            // Get the data set
                  boolean    spFieldNull  = (w < 0);
            final SPFieldSet spFieldSet   = spFieldNull ? null : _spFieldList.get(w);
            final int        spValueCount = spFieldNull ? 0    :  spFieldSet._varValue.size();
            final boolean    spIdxInvalid = spFieldNull || (spFieldSet._index < 0);

            // Loop as many as the number of data sets
            for(int i = 0; i < setCount; ++i) {

                // Get the value
                final double val = (spIdxInvalid || i >= spValueCount) ? 0.0 : ( spFieldSet._varValue.get(i) * _spScale );

                // Find the minimum and maximum values
                if(val < spDataSets.minValue) spDataSets.minValue = val;
                if(val > spDataSets.maxValue) spDataSets.maxValue = val;

                // Store the value
                spDataSets.values.get(i).add(val);

            } // for

            // Store the index
            spDataSets.index.add(spFieldNull ? -1 : spFieldSet._index);

        } // for

        // Return the data sets instance
        return spDataSets;
    }

} // SerialConsole
