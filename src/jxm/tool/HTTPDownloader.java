/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;

import java.nio.file.Paths;

import jxm.*;
import jxm.xb.*;
import jxm.xb.fci.*;


public class HTTPDownloader  {

    private static final int DownloadBufferSize                      =  8192;

    private static final int DefaultTimeoutMS                        = 10000;
    private static final int DefaultSleepTimeBeforeRetryMS           =  2500;

    private static final int DefaultMaxRetryInCaseOfConsecutiveError =     3;
    private static final int DefaultMaxTotalRetry                    = 65536;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private URL                 _url            = null;
    private String              _outDirPath     = null;
    private String              _outFileName    = null;
    private String              _outFilePath    = null;
    private int                 _timeout        = DefaultTimeoutMS;

    private long                _fileSize       = 0;
    private BufferedInputStream _bis            = null;
    private long                _downloadedSize = 0;
    private FileOutputStream    _fos            = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public interface ProgressCB {
        public boolean apply(long downloadedSize, long totalSize);
    }

    public class DefaultProgressCB implements ProgressCB {
        private static final int TotalTicks =  50;
        private static final int DivFactor  = 100 / TotalTicks;

        private              boolean _totalSizeUnknown = false;

        private              boolean _printedOBracket  = false;
        private              boolean _printedCBracket  = false;
        private              int     _printedTicks     = 0;

        private void _resetState()
        {
            _printedOBracket = false;
            _printedCBracket = false;
            _printedTicks    = 0;
        }

        public boolean apply(long downloadedSize, long totalSize)
        {
            // Calculate the number of ticks that should has been printed
            int ticks = 0;

            if(totalSize > 0) {
                final int percent = (int) ( ( downloadedSize * 100 + (totalSize / 2) ) / totalSize );
                          ticks   = percent / DivFactor;
            }
            else {
                _totalSizeUnknown = true;
                ticks             = (int) downloadedSize / 32768;
            }

            // Check for errpr
            if(downloadedSize < 0) {
                SysUtil.stdOut().printf(Texts.IMsg_HTTPDownloaderError, downloadedSize);
                _resetState();
                return false;
            }

            // Draw the opening bracket as needed
            if(!_printedOBracket) {
                _printedOBracket = true;
                                                            SysUtil.stdOut().println( "<<< " + getURL        ()                                      );
                                                            SysUtil.stdOut().println( ">>> " + getOutFilePath()                                      );
                if(totalSize > 0) {
                    if(downloadedSize == 0)                 SysUtil.stdOut().printf ( Texts.IMsg_HTTPDownloaderESizeBytes, totalSize                 );
                    else                                    SysUtil.stdOut().printf ( Texts.IMsg_HTTPDownloaderPSizeBytes, downloadedSize, totalSize );
                                                            SysUtil.stdOut().print  ( "[000"                                                         );
                    for(int i = 0; i < TotalTicks - 4; ++i) SysUtil.stdOut().print  ( ' '                                                            );
                                                            SysUtil.stdOut().println( "100]"                                                         );
                                                            SysUtil.stdOut().print  ( "[:"                                                           );
                }
                else {
                                                            SysUtil.stdOut().print  ( "[?"                                                           );
                }
            }

            // Draw the ticks
            if( !_totalSizeUnknown || downloadedSize != totalSize ) {
                while(_printedTicks < ticks) {
                    SysUtil.stdOut().print( (totalSize > 0) ? '.' : '-' );
                    ++_printedTicks;
                }
            }

            // Draw the closing bracket as needed
            if(!_printedCBracket && downloadedSize == totalSize) {
                _printedCBracket = true;
                SysUtil.stdOut().println("#]");
            }

            // [◨▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪◧]
            // [◧▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪◨]

            // [▶▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪◧]
            // [▶▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪◨]

            // Return 'true' so the process will be continued
            return true;
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public HTTPDownloader(final String url, final String outputDirPath, final String outputFileName) throws MalformedURLException, URISyntaxException
    {
        _url         = ( new URI(url) ).toURL();
        _outDirPath  = (outputDirPath != null) ? SysUtil.resolveAbsolutePath(outputDirPath) : SysUtil.getCWD();
        _outFileName = outputFileName;
    }

    public String getURL()
    { return _url.toString(); }

    public String getOutFileName()
    { return _outFileName; }

    public String getOutFilePath()
    { return _outFilePath; }

    public long getDownloadedSize()
    { return _downloadedSize; }

    public long getRemoteSize()
    { return _fileSize; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _throwHTTPError(final HttpURLConnection httpConnection) throws IOException
    {
        // Read the error message
        String errMsg = "";

        // Get the streams
        InputStream iserr = null;
        InputStream isout = null;

        try { iserr = httpConnection.getErrorStream(); } catch(final Exception e) {}
        try { isout = httpConnection.getInputStream(); } catch(final Exception e) {}

        // Read the error message as needed
        if(iserr != null || isout != null) {
            final byte                  buff[] = new byte[DownloadBufferSize];
            final BufferedInputStream   bis    = new BufferedInputStream( (iserr != null) ? iserr : isout );
            final ByteArrayOutputStream baos   = new ByteArrayOutputStream();
            while(true) {
                // Read bytes
                final int bytesRead = _bis.read(buff, 0, DownloadBufferSize);
                if(bytesRead == -1) break;
                // Write bytes
                baos.write(buff, 0, bytesRead);
            }
            // Convert the error message to string as needed
            if( baos.size() != 0 ) errMsg = baos.toString(SysUtil._CharEncoding);
            // Close the streams
            bis .close();
            baos.close();
        }

        // Disconnect
        httpConnection.disconnect();

        // Throw exception
        throw XCom.newIOException( httpConnection.getHeaderField(0) + errMsg );
    }

    private void _closeStreams()
    {
        try { if(_bis != null) _bis.close(); } catch(final IOException e) {}
        try { if(_fos != null) _fos.close(); } catch(final IOException e) {}

        _bis = null;
        _fos = null;
    }

    private boolean _begin() throws IOException, URISyntaxException, JXMException
    {
        // Check if the URL is not valid
        if(_url == null) return false;

        // Send HEAD request
        HttpURLConnection httpConnection = (HttpURLConnection) _url.openConnection();

        httpConnection.setDoInput       (true    );
        httpConnection.setConnectTimeout(_timeout);
        httpConnection.setReadTimeout   (_timeout);
        httpConnection.setRequestMethod ("HEAD"  );

        if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) _throwHTTPError(httpConnection);

        // Get the file size
        _fileSize = httpConnection.getContentLengthLong();

        if(_fileSize == 0) _fileSize = -1; // Set the file size to -1 if the server does not sent the content length

        // Determine the output file name as needed
        if(_outFileName == null) {
            final String cd = httpConnection.getHeaderField("Content-Disposition");
          //if(cd != null) _outFileName = cd.replaceFirst("(?i)^.*?; filename=\"?([^\"]+)\"?.*$", "$1").trim();
            if(cd != null) _outFileName = ReCache._reGetMatcher("(?i)^.*?; filename=\"?([^\"]+)\"?.*$", cd).replaceFirst("$1").trim();
            if( _outFileName == null || _outFileName.isEmpty() ) _outFileName = Paths.get( _url.toURI().getPath() ).getFileName().toString();
        }

        // Disconnect
        httpConnection.disconnect();

        // Get and check the output file size
        _outFilePath    = SysUtil.resolvePath(_outFileName, _outDirPath);
        _downloadedSize = SysUtil.pathIsValidFile(_outFilePath) ? SysUtil.pathGetFileSize(_outFilePath) : 0;

        if(_fileSize > 0) {
            // Check only if the server sent the file size
            if(_downloadedSize >= _fileSize) return (_downloadedSize == _fileSize); // Return 'true' if both sizes match
        }
        else {
            // Start from the beginning if the server did not send the file size
            _downloadedSize = 0;
        }

        // Prepare the input and output stream
        httpConnection = (HttpURLConnection) _url.openConnection();

        httpConnection.setDoInput       (true    );
        httpConnection.setConnectTimeout(_timeout);
        httpConnection.setReadTimeout   (_timeout);
        httpConnection.setRequestMethod ("GET"   );

        try {
            // Prepare the output stream
            if(_downloadedSize == 0) {
                if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) _throwHTTPError(httpConnection);
                _fos = new FileOutputStream(_outFilePath, false);
            }
            else {
                httpConnection.setRequestProperty("Range", "bytes=" + _downloadedSize + "-" + _fileSize);
                if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL ) _throwHTTPError(httpConnection);
                _fos = new FileOutputStream(_outFilePath, true );
            }
            // Prepare the input stream
            _bis = new BufferedInputStream( httpConnection.getInputStream() );
        }
        catch(final IOException e) {
            // Close the streams
            _closeStreams();
            // Re-throw the exception
            throw e;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int DRes_Error      = -1;
    private static int DRes_Incomplete =  0;
    private static int DRes_Done       =  1;

    private int _download(final ProgressCB progressCB)
    {
        // Check if the streams are not valid
        if(_fos == null || _bis == null) return DRes_Error;

        // Perform download
        boolean error = false;

        //int simErr = 0;

        try {
            final byte dataBuffer[] = new byte[DownloadBufferSize];
            while(true) {

                // Read bytes
                final int bytesRead = _bis.read(dataBuffer, 0, DownloadBufferSize);
                if(bytesRead == -1) break;

                // Write bytes
                _fos.write(dataBuffer, 0, bytesRead);
                _fos.flush();

                // Accumulate the number of downloaded bytes
                _downloadedSize += bytesRead;

                // Call the user progress callback if one is specified
                if(progressCB != null) {
                    if( !progressCB.apply(_downloadedSize, _fileSize) ) return DRes_Error;
                }

                //if(++simErr > 30) return DRes_Error;
                //if(++simErr > 30) break;

                // Check if the file is already fully downloaded
                if(_downloadedSize == _fileSize) break;

            } // while true
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error flag
            error = true;
        }

        // Close the streams
        _closeStreams();

        // Set the file size to the downloade size if the server did not send the file size
        if(_fileSize < 0) {
            _fileSize = _downloadedSize;
            if( !progressCB.apply(_downloadedSize, _fileSize) ) return DRes_Error;
        }

        // Done
        if(error || _downloadedSize > _fileSize) return DRes_Error;

        if(_downloadedSize < _fileSize) return DRes_Incomplete;

        return DRes_Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean download() throws IOException, URISyntaxException, JXMException
    { return download( new DefaultProgressCB(), DefaultMaxRetryInCaseOfConsecutiveError, DefaultMaxTotalRetry ); }

    public boolean download(final ProgressCB progressCB) throws IOException, URISyntaxException, JXMException
    { return download(progressCB, DefaultMaxRetryInCaseOfConsecutiveError, DefaultMaxTotalRetry); }

    public boolean download(final ProgressCB progressCB, final int numOfRetryInCaseOfConsecutiveError) throws IOException, URISyntaxException, JXMException
    { return download(progressCB, numOfRetryInCaseOfConsecutiveError, DefaultMaxTotalRetry); }

    public boolean download(final int numOfRetryInCaseOfConsecutiveError, final int maxNumOfTotalRetry) throws IOException, URISyntaxException, JXMException
    { return download( new DefaultProgressCB(), numOfRetryInCaseOfConsecutiveError, maxNumOfTotalRetry ); }

    public boolean download(final int numOfRetryInCaseOfConsecutiveError) throws IOException, URISyntaxException, JXMException
    { return download( new DefaultProgressCB() , numOfRetryInCaseOfConsecutiveError, DefaultMaxTotalRetry ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static long DErr_BeginError       = -101;
    private static long DErr_DownloadError    = -102;
    private static long DErr_ConsecutiveError = -201;
    private static long DErr_MaxTotalRetry    = -202;

    public boolean download(final ProgressCB progressCB, int numOfRetryInCaseOfConsecutiveError, int maxNumOfTotalRetry) throws IOException, URISyntaxException, JXMException
    {
        // Sanitize the parameters
        if(numOfRetryInCaseOfConsecutiveError <= 0) numOfRetryInCaseOfConsecutiveError = DefaultMaxRetryInCaseOfConsecutiveError;
        if(maxNumOfTotalRetry                 <= 0) maxNumOfTotalRetry                 = DefaultMaxTotalRetry;

        // Perform download with retry
        int consecutiveErrorCount = 0;
        int totalRetryCount       = 0;

        //int simErr = 0;

        while(true) {

            // Begin the download
            if( !_begin() /*|| (++simErr > 2)*/ ) {
                // Report the error
                if(progressCB != null) progressCB.apply(DErr_BeginError, DErr_BeginError);
                return false;
            }

            // Call the user progress callback if one is specified
            if(progressCB != null) {
                if( !progressCB.apply(_downloadedSize, _fileSize) ) return false;
            }

            // Check if the file is already fully downloaded
            if(_downloadedSize == _fileSize) return true;

            // Perform the download
            final int result = _download(progressCB);

            // In case of error increment and check the consecutive error counter
            if(result == DRes_Error) {
                // Report the error
                if(progressCB != null) progressCB.apply(DErr_DownloadError, DErr_DownloadError);
                // Increment and check the counter
                if(++consecutiveErrorCount > numOfRetryInCaseOfConsecutiveError) {
                    // Report the error
                    if(progressCB != null) progressCB.apply(DErr_ConsecutiveError, DErr_ConsecutiveError);
                    return false;
                }
                // Delay for a while and retry
                SysUtil.sleepMS(DefaultSleepTimeBeforeRetryMS * consecutiveErrorCount);
                continue;
            }

            // We have got some progress, so reset the consecutive error counter
            consecutiveErrorCount = 0;

            // Check if the file is already fully downloaded
            if(result == DRes_Done) break;

            // Increment and check the number of total retry counter
            if(++totalRetryCount > maxNumOfTotalRetry) {
                // Report the error
                if(progressCB != null) progressCB.apply(DErr_MaxTotalRetry, DErr_MaxTotalRetry);
                return false;
            }

            // Delay for a while and retry
            SysUtil.sleepMS(DefaultSleepTimeBeforeRetryMS);

        } // while true

        // Done
        return true;
    }

} // class HTTPDownloader
