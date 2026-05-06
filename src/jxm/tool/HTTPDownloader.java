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

import java.lang.reflect.Method;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;

import java.nio.file.Paths;

import java.time.Duration;

import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

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

    // java.net.http.HttpClient support via reflection

    // True when java.net.http.HttpClient is available and should be used
    private static final boolean _USE_HTTP_CLIENT;

    // # Reflectively resolved java.net.http classes (null when _USE_HTTP_CLIENT is false).
    // # Stored as static fields because they are used in getMethod() calls below
    //   (as parameter-type arguments) and the compiler needs them to be accessible.
    private static Class<?> _clsHttpRequest;
    private static Class<?> _clsHttpRequestBuilder;
    private static Class<?> _clsBodyPublisher;
    private static Class<?> _clsBodyPublishers;
    private static Class<?> _clsBodyHandler;
    private static Class<?> _clsBodyHandlers;
    private static Class<?> _clsHttpResponse;
    private static Class<?> _clsHttpClient;
    private static Class<?> _clsHttpClientBuilder;
    private static Class<?> _clsHttpClientVersion;

    // HttpClient.Redirect.NORMAL enum constant
    private static Object   _redirectNormal;

    // HttpClient.Version enum constants
    private static Object   _versionHttp11;
    private static Object   _versionHttp2;

    // Reflectively resolved methods (null when _USE_HTTP_CLIENT is false)
    private static Method   _mHttpRequestNewBuilder;
    private static Method   _mBuilderUri;
    private static Method   _mBuilderMethod;
    private static Method   _mBuilderHeader;
    private static Method   _mBuilderTimeout;
    private static Method   _mBuilderVersion;
    private static Method   _mBuilderBuild;
    private static Method   _mPublishersNoBody;
    private static Method   _mHandlersOfInputStream;
    private static Method   _mHandlersOfByteArray;
    private static Method   _mClientNewBuilder;
    private static Method   _mClientBuilderFollowRedirects;
    private static Method   _mClientBuilderConnectTimeout;
    private static Method   _mClientBuilderSslContext;
    private static Method   _mClientBuilderSslParameters;
    private static Method   _mClientBuilderAuthenticator;
    private static Method   _mClientBuilderBuild;
    private static Method   _mClientSend;
    private static Method   _mResponseStatusCode;
    private static Method   _mResponseBody;
    private static Method   _mResponseHeaders;
    private static Method   _mHeadersMap;

    static {
        boolean avail = false;
        if( !com.intellectualsites.http.HttpClient.isLegacyHttpForced() ) {
            try {
                final Class<?> clsHttpHeaders  = Class.forName("java.net.http.HttpHeaders"        );
                final Class<?> clsRedirect     = Class.forName("java.net.http.HttpClient$Redirect");

                _clsHttpRequest                = Class.forName("java.net.http.HttpRequest"               );
                _clsHttpRequestBuilder         = Class.forName("java.net.http.HttpRequest$Builder"       );
                _clsBodyPublisher              = Class.forName("java.net.http.HttpRequest$BodyPublisher" );
                _clsBodyPublishers             = Class.forName("java.net.http.HttpRequest$BodyPublishers");
                _clsBodyHandler                = Class.forName("java.net.http.HttpResponse$BodyHandler"  );
                _clsBodyHandlers               = Class.forName("java.net.http.HttpResponse$BodyHandlers" );
                _clsHttpResponse               = Class.forName("java.net.http.HttpResponse"              );
                _clsHttpClient                 = Class.forName("java.net.http.HttpClient"                );
                _clsHttpClientBuilder          = Class.forName("java.net.http.HttpClient$Builder"        );
                _clsHttpClientVersion          = Class.forName("java.net.http.HttpClient$Version"        );

                _redirectNormal                = clsRedirect.getField("NORMAL").get(null);
                _versionHttp11                 = _clsHttpClientVersion.getField("HTTP_1_1").get(null);
                _versionHttp2                  = _clsHttpClientVersion.getField("HTTP_2"  ).get(null);

                _mHttpRequestNewBuilder        = _clsHttpRequest       .getMethod("newBuilder"                                                    );
                _mBuilderUri                   = _clsHttpRequestBuilder.getMethod("uri"            , URI.class                                    );
                _mBuilderMethod                = _clsHttpRequestBuilder.getMethod("method"         , String.class             , _clsBodyPublisher );
                _mBuilderHeader                = _clsHttpRequestBuilder.getMethod("header"         , String.class             , String.class      );
                _mBuilderTimeout               = _clsHttpRequestBuilder.getMethod("timeout"        , Duration.class                               );
                _mBuilderVersion               = _clsHttpRequestBuilder.getMethod("version"        , _clsHttpClientVersion                        );
                _mBuilderBuild                 = _clsHttpRequestBuilder.getMethod("build"                                                         );
                _mPublishersNoBody             = _clsBodyPublishers    .getMethod("noBody"                                                        );
                _mHandlersOfInputStream        = _clsBodyHandlers      .getMethod("ofInputStream"                                                 );
                _mHandlersOfByteArray          = _clsBodyHandlers      .getMethod("ofByteArray"                                                   );
                _mClientNewBuilder             = _clsHttpClient        .getMethod("newBuilder"                                                    );
                _mClientBuilderFollowRedirects = _clsHttpClientBuilder .getMethod("followRedirects", clsRedirect                                  );
                _mClientBuilderConnectTimeout  = _clsHttpClientBuilder .getMethod("connectTimeout" , Duration.class                               );
                _mClientBuilderSslContext      = _clsHttpClientBuilder .getMethod("sslContext"     , SSLContext.class                             );
                _mClientBuilderSslParameters   = _clsHttpClientBuilder .getMethod("sslParameters"  , SSLParameters.class                          );
                _mClientBuilderAuthenticator   = _clsHttpClientBuilder .getMethod("authenticator"  , java.net.Authenticator.class                 );
                _mClientBuilderBuild           = _clsHttpClientBuilder .getMethod("build");
                _mClientSend                   = _clsHttpClient        .getMethod("send"           , _clsHttpRequest             , _clsBodyHandler);
                _mResponseStatusCode           = _clsHttpResponse      .getMethod("statusCode"                                                    );
                _mResponseBody                 = _clsHttpResponse      .getMethod("body"                                                          );
                _mResponseHeaders              = _clsHttpResponse      .getMethod("headers"                                                       );
                _mHeadersMap                   =  clsHttpHeaders       .getMethod("map"                                                           );

                avail = true;
            }
            catch (final Throwable ignored) {
                // HttpClient unavailable (Java 8) or initialisation failed; fall back to legacy path
            }
        }

        _USE_HTTP_CLIENT = avail;
    }

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
                final int bytesRead = bis.read(buff, 0, DownloadBufferSize);
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

    // Create a java.net.http.HttpClient via reflection, optionally configured with the trust-all SSLContext
    // from SSLTrustAll when self-signed certificates must be accepted. Only called when _USE_HTTP_CLIENT is true
    private Object _createHttpClient() throws Exception
    {
        final Object builder = _mClientNewBuilder.invoke(null);
        _mClientBuilderFollowRedirects.invoke( builder, _redirectNormal             );
        _mClientBuilderConnectTimeout .invoke( builder, Duration.ofMillis(_timeout) );

        // Apply the trust-all SSLContext if SSLTrustAll is currently active
        final SSLContext sslCtx = SSLTrustAll.getTrustAllSSLContext();
        if(sslCtx != null) {
            _mClientBuilderSslContext.invoke(builder, sslCtx);
            // Disable hostname verification - getTrustAllSSLParameters() returns SSLParameters with the
            // endpoint-identification algorithm set to the empty string (not null) because Java 11+ HttpClient
            // silently promotes a null value to "HTTPS", whereas an empty string disables the check entirely
            _mClientBuilderSslParameters.invoke( builder, SSLTrustAll.getTrustAllSSLParameters() );
        }

        // # Use a per-request snapshot of the calling thread's credentials rather than the TLAuthenticator singleton.
        // # Java 11+ HttpClient invokes the Authenticator from its own thread pool, so ThreadLocal values set by the
        //   calling thread would not be visible there.
        // # The snapshot captures the credentials at call time.
        _mClientBuilderAuthenticator.invoke( builder, TLAuthenticator.snapshot() );

        return _mClientBuilderBuild.invoke(builder);
    }

    private boolean _begin() throws IOException, URISyntaxException, JXMException
    {
        // Check if the URL is not valid
        if(_url == null) return false;

        if(_USE_HTTP_CLIENT) {
            // ====================================================================================================
            // HttpClient path (Java 11+, via reflection)
            // ====================================================================================================
            try {
                final Object noBody = _mPublishersNoBody.invoke(null);

                // Preemptive authentication - capture the Authorization header once at the start of each _begin()
                // call rather than relying on the Authenticator challenge/retry mechanism, which is unreliable
                // in java.net.http.HttpClient across JDK versions (early Java 11 bugs, HTTP/2 path, custom SSLContext
                // interactions)
                final String authHdr = TLAuthenticator.getServerAuthorizationHeader();

                // Multi-step retry strategy for the initial file-info request:
                //   1. HEAD with manual Authorization (preemptive)
                //   2. HEAD without manual Authorization (rely on Authenticator)
                //   3. GET  with manual Authorization
                //   4. GET  without manual Authorization
                //   5. Force HTTP/1.1 for any of the above if 401 persists
                Object response = null;
                int    step     = 0;
                while(true) {
                    final Object client = _createHttpClient();
                    final Object b      = _mHttpRequestNewBuilder.invoke(null);
                    _mBuilderUri    .invoke( b, _url.toURI()                );
                    _mBuilderTimeout.invoke( b, Duration.ofMillis(_timeout) );

                    // Step logic:
                    // 0: HEAD,    manual auth
                    // 1: HEAD, no manual auth
                    // 2: GET ,    manual auth
                    // 3: GET , no manual auth
                    // 4: HEAD,    manual auth, HTTP/1.1
                    // 5: HEAD, no manual auth, HTTP/1.1
                    // 6: GET ,    manual auth, HTTP/1.1
                    // 7: GET , no manual auth, HTTP/1.1
                    final boolean useGet     = (step % 4) >= 2;
                    final boolean manualAuth = (step % 2) == 0;
                    final boolean forceHttp1 = step >= 4;

                    _mBuilderMethod .invoke( b, useGet ? "GET" : "HEAD", noBody        );
                    _mBuilderHeader .invoke( b, "User-Agent", SysUtil._JxMakeUserAgent );
                    _mBuilderTimeout.invoke( b, Duration.ofMillis(_timeout)            );
                    if(manualAuth && authHdr != null) _mBuilderHeader .invoke(b, "Authorization", authHdr);
                    if(forceHttp1)                    _mBuilderVersion.invoke(b, _versionHttp11);

                    final Object req     = _mBuilderBuild.invoke(b);
                    final Object handler = useGet ? _mHandlersOfInputStream.invoke(null) : _mHandlersOfByteArray.invoke(null);
                    response = _mClientSend.invoke(client, req, handler);

                    final int statusCode = (Integer) _mResponseStatusCode.invoke(response);
                    if(statusCode == HttpURLConnection.HTTP_OK) break;

                    // If it was a GET request, we need to close the input stream because we only wanted the headers
                    if(useGet) {
                        try { ( (InputStream) _mResponseBody.invoke(response) ).close(); } catch(final Exception ignored) {}
                    }

                    // If it's a 401 or 405 (Method Not Allowed for HEAD), try the next step
                    if( (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_BAD_METHOD) && ++step < 8 ) {
                        continue;
                    }

                    throw XCom.newIOException("HTTP info request error %d: %s", statusCode, _url);
                }

                // Extract Content-Length and Content-Disposition from response headers
                final Object headersObj = _mResponseHeaders.invoke(response);

                @SuppressWarnings("unchecked")
                final Map< String, List<String> > hMap = (Map< String, List<String> >) _mHeadersMap.invoke(headersObj);

                _fileSize = -1;
                String cd = null;

                for( final Map.Entry<String, List<String>> e : hMap.entrySet() ) {

                    if( e.getKey() == null || e.getValue().isEmpty() ) continue;

                    final String k = e.getKey();

                    if( k.equalsIgnoreCase("content-length") ) {
                        try { _fileSize = Long.parseLong( e.getValue().get(0) ); }
                        catch(final NumberFormatException ignored) {}
                    }
                    else if( k.equalsIgnoreCase("content-disposition") ) {
                        cd = e.getValue().get(0);
                    }

                } // for

                if(_fileSize == 0) _fileSize = -1;

                // Determine the output file name as needed
                if(_outFileName == null) {
                    if(cd != null) _outFileName = ReCache._reGetMatcher("(?i)^.*?; filename=\"?([^\"]+)\"?.*$", cd).replaceFirst("$1").trim();
                    if( _outFileName == null || _outFileName.isEmpty() ) _outFileName = Paths.get( _url.toURI().getPath() ).getFileName().toString();
                }

                // If it was a GET request, we need to close the input stream because we only wanted the headers
                if( ( (Integer) _mResponseStatusCode.invoke(response) ) == HttpURLConnection.HTTP_OK ) {
                    final Object body = _mResponseBody.invoke(response);
                    if(body instanceof InputStream) {
                        try { ( (InputStream) body ).close(); } catch(final Exception ignored) {}
                    }
                }

                // Get and check the output file size
                _outFilePath    = SysUtil.resolvePath(_outFileName, _outDirPath);
                _downloadedSize = SysUtil.pathIsValidFile(_outFilePath) ? SysUtil.pathGetFileSize(_outFilePath) : 0;

                if(_fileSize > 0) {
                    // Check only if the server sent the file size
                    if(_downloadedSize >= _fileSize) return (_downloadedSize == _fileSize);
                }
                else {
                    // Start from the beginning if the server did not send the file size
                    _downloadedSize = 0;
                }

                // ----- GET request (Main download) -----
                step = 0;
                while(true) {
                    final Object client = _createHttpClient();
                    final Object b      = _mHttpRequestNewBuilder.invoke(null);
                    _mBuilderUri    .invoke( b, _url.toURI()                );
                    _mBuilderTimeout.invoke( b, Duration.ofMillis(_timeout) );

                    final boolean manualAuth = (step % 2) == 0;
                    final boolean forceHttp1 = step >= 2;

                    _mBuilderMethod .invoke(b, "GET", noBody                         );
                    _mBuilderHeader .invoke(b, "User-Agent", SysUtil._JxMakeUserAgent);
                    if(manualAuth && authHdr != null) _mBuilderHeader .invoke(b, "Authorization", authHdr);
                    if(forceHttp1)                    _mBuilderVersion.invoke(b, _versionHttp11);

                    if(_downloadedSize > 0 && _fileSize > 0) {
                        _mBuilderHeader.invoke( b, "Range", "bytes=" + _downloadedSize + "-" + (_fileSize - 1) );
                    }


                    final Object req        = _mBuilderBuild.invoke(b);
                    final Object handler    = _mHandlersOfInputStream.invoke(null);
                    final Object res        = _mClientSend.invoke(client, req, handler);
                    final int    statusCode = (Integer) _mResponseStatusCode.invoke(res);

                    // If it's a 401, try the next step
                    if(statusCode == HttpURLConnection.HTTP_UNAUTHORIZED && ++step < 4) {
                        continue;
                    }

                    try {
                        // Prepare the output stream
                        if(_downloadedSize == 0) {
                            if(statusCode != HttpURLConnection.HTTP_OK) throw XCom.newIOException("HTTP GET error %d: %s", statusCode, _url);
                            _fos = new FileOutputStream(_outFilePath, false);
                        }
                        else {
                            if(statusCode != HttpURLConnection.HTTP_PARTIAL) throw XCom.newIOException("HTTP partial GET error %d (range bytes=%d-%d): %s", statusCode, _downloadedSize, _fileSize - 1, _url);
                            _fos = new FileOutputStream(_outFilePath, true);
                        }
                        // Prepare the input stream
                        _bis = new BufferedInputStream( (InputStream) _mResponseBody.invoke(res) );
                        break;
                    }
                    catch(final IOException e) {
                        // Close the streams
                        _closeStreams();
                        // Re-throw the exception
                        throw e;
                    }
                } // while true
            }
            catch(final Exception e) {
                // If it was a 401, try falling back to legacy path as a last resort
                String msg = (e.getMessage() != null) ? e.getMessage() : "";
                if( msg.contains("401") || msg.contains("Unauthorized") ) {
                    return _beginLegacy();
                }
                if(e instanceof IOException)       throw (IOException) e;
                if(e instanceof URISyntaxException) throw (URISyntaxException) e;
                if(e instanceof JXMException)      throw (JXMException) e;
                throw new IOException(e);
            }
        }
        else {
            return _beginLegacy();
        }

        // Done
        return true;
    }

    private boolean _beginLegacy() throws IOException, URISyntaxException, JXMException
    {
        // ====================================================================================================
        // Legacy path (Java 8, or when HttpClient is disabled via env-var) HttpsURLConnection is used
        // automatically for HTTPS URLs since it is a subclass of HttpURLConnection; SSLTrustAll continues to
        // configure the default SSL socket factory globally
        // ====================================================================================================

        // Send HEAD request
        HttpURLConnection httpConnection = (HttpURLConnection) _url.openConnection();

        httpConnection.setDoInput       (true    );
        httpConnection.setConnectTimeout(_timeout);
        httpConnection.setReadTimeout   (_timeout);
        httpConnection.setRequestMethod ("HEAD"  );

        // Preemptive authentication for legacy path
        final String authHdr = TLAuthenticator.getServerAuthorizationHeader();
        if(authHdr != null) httpConnection.setRequestProperty("Authorization", authHdr);

        if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) {
            // Try GET if HEAD fails with 401 or 405
            if( httpConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED || httpConnection.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD ) {
                httpConnection.disconnect();
                httpConnection = (HttpURLConnection) _url.openConnection();
                httpConnection.setDoInput       (true    );
                httpConnection.setConnectTimeout(_timeout);
                httpConnection.setReadTimeout   (_timeout);
                httpConnection.setRequestMethod ("GET"   );
                if(authHdr != null) httpConnection.setRequestProperty("Authorization", authHdr);
                if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) _throwHTTPError(httpConnection);
            }
            else {
                _throwHTTPError(httpConnection);
            }
        }

        // Get the file size
        _fileSize = httpConnection.getContentLengthLong();

        if(_fileSize == 0) _fileSize = -1; // Set the file size to -1 if the server does not sent the content length

        // Determine the output file name as needed
        if(_outFileName == null) {
            final String cd = httpConnection.getHeaderField("Content-Disposition");
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
        if(authHdr != null) httpConnection.setRequestProperty("Authorization", authHdr);

        try {
            // Prepare the output stream
            if(_downloadedSize == 0) {
                if( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) _throwHTTPError(httpConnection);
                _fos = new FileOutputStream(_outFilePath, false);
            }
            else {
                httpConnection.setRequestProperty( "Range", "bytes=" + _downloadedSize + "-" + (_fileSize - 1) );
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
