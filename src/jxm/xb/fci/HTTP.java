/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.lang.reflect.InvocationTargetException;

import java.io.IOException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

import java.util.ArrayList;

import com.intellectualsites.http.*;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


public class HTTP {

    private static String DefaultTimeout_Seconds = "5";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum RMethod {
        HEAD,
        GET,
        POST
    }

    public static class ReqExWrapper {
        Throwable e;

        public ReqExWrapper()
        { e = null; }

        public void set(final Throwable e_)
        { e = e_; }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static HttpClient.WrappedRequestBuilder _newHttpRequest(final String url, final RMethod rm, final ArrayList<String> headerKeyValPairs)
    {
        // Check if the URL is empty
        if( url.isEmpty() ) return null;

        // Separate the base URL and path
        final int    protSIdx = url.indexOf("://"            );
        final int    pathSIdx = url.indexOf("/", protSIdx + 3);

        final String baseURL  = (pathSIdx < protSIdx) ? url : url.substring(0, pathSIdx);
        final String path     = (pathSIdx < protSIdx) ? "/" : url.substring(pathSIdx   );

        // Create a new HTTP client instance
        final HttpClient cli = HttpClient.newBuilder().withBaseURL     ( baseURL                    )
                                                      .withEntityMapper( EntityMapper.newInstance() )
                                                      .build           (                            );

        // Create and return a new HTTP request instance
        HttpClient.WrappedRequestBuilder wrp = null;

        switch(rm) {
            case HEAD : wrp = cli.head(path); break;
            case GET  : wrp = cli.get (path); break;
            case POST : wrp = cli.post(path); break;
        }

        // Add the header as needed
        if(wrp != null) {
            for(int i = 0; i < headerKeyValPairs.size();) {
                final String key = headerKeyValPairs.get(i++);
                final String val = headerKeyValPairs.get(i++);
                wrp.withHeader(key, val);
            }
        }

        // Return the new HTTP request instance
        return wrp;
    }

    public static void _processHTTPResponse(final XCom.VariableValue retVal, final ReqExWrapper rew, final HttpResponse res, final boolean getHeader) throws Exception
    {
        // Check if the request is successful (there is no exception)
        if(rew.e == null) {
            final int statusCode = res.getStatusCode();

            // If it is an unauthorized error and no credentials were provided, throw an exception to match modern HttpClient behavior
            if(statusCode == 401 && TLAuthenticator.getServerAuthorizationHeader() == null) {
                throw new IOException("No credentials provided");
            }

            // Store the status
            retVal.add( new XCom.VariableStore( true, String.valueOf( statusCode ) ) );
            retVal.add( new XCom.VariableStore( true,              res.getStatus() ) );
            // Store the headers
            if(getHeader) {
                for( final String key : res.getHeaders().getHeaders() ) {
                    final StringBuilder sb = new StringBuilder();
                    for( final String val : res.getHeaders().getHeaders(key) ) {
                        if( sb.length() != 0 ) sb.append("; ");
                        sb.append(val);
                    }
                    sb.insert(0, '=');
                    sb.insert(0, key);
                    retVal.add( new XCom.VariableStore( true, sb.toString() ) );
                }
            }
            // Store the response content
            else {
                retVal.add( new XCom.VariableStore( true, new String( res.getRawResponse(), SysUtil._CharEncoding ) ) );
            }
        }

        // The request is failed (there is an exception)
        else {
            if(rew.e instanceof Exception) throw (Exception) rew.e;
            throw new RuntimeException(rew.e);
        }
    }

    private static void _processException(final XCom.VariableValue retVal, final Throwable e, final boolean isDownload) throws JXMException
    {
        // Check if this is an IOException, InvocationTargetException, or SSLHandshakeException hiding anywhere in the cause chain
        Throwable cause       = e;
        Throwable iteFallback = null;

        while(cause != null) {
            final String clsNm = cause.getClass().getName();
            String suErr = null;

                 if( cause instanceof ConnectException             ) suErr = "ConnectException";
            else if( cause instanceof NoRouteToHostException       ) suErr = "NoRouteToHostException";
            else if( cause instanceof UnknownHostException         ) suErr = "UnknownHostException";
            else if( cause instanceof SocketTimeoutException       ) suErr = "SocketTimeoutException";
            else if( cause instanceof CertificateException         ) suErr = "CertificateException";
            else if( cause instanceof SSLHandshakeException        ) suErr = "SSLHandshakeException";
            else if( clsNm.contains("HttpConnectTimeoutException") ) suErr = "HttpConnectTimeoutException"; // Map modern java.net.http timeouts to 503 as well
            else if( clsNm.contains("HttpTimeoutException"       ) ) suErr = "HttpTimeoutException";

            // If we found a 503-mappable error
            if(suErr != null) {
                if(isDownload) {
                    SysUtil.printError( String.format("503 Service Unavailable: %s", suErr) );
                    retVal.add( new XCom.VariableStore(true, ""                   ) );
                }
                else {
                    retVal.add( new XCom.VariableStore(true, "503"                ) );
                    retVal.add( new XCom.VariableStore(true, "Service Unavailable") );
                    retVal.add( new XCom.VariableStore(true, suErr                ) );
                }
                return;
            }

            // If we find the error, use it immediately and exit
            if(cause instanceof IOException) {
                if(isDownload) {
                    SysUtil.printError( cause.toString() );
                    retVal.add( new XCom.VariableStore(true, ""               ) );
                }
                else {
                    retVal.add( new XCom.VariableStore(true, cause.toString() ) );
                }
                return;
            }

            // If we find the reflection wrapper, remember it in case we do not find an SSL error
            if(cause instanceof InvocationTargetException) iteFallback = cause;
            // Get the next cause
            cause = cause.getCause();

        } // while

        // Use the reflection wrapper if we have one
        if(iteFallback != null) {
            if(isDownload) {
                SysUtil.printError( iteFallback.toString() );
                retVal.add( new XCom.VariableStore(true, ""                     ) );
            }
            else {
                retVal.add( new XCom.VariableStore(true, iteFallback.toString() ) );
            }
            return;
        }

        // Print the stack trace if requested
        if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        
        // Throw as a different exception
        throw XCom.newJXMRuntimeError( e.toString() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_http_headget(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean getReq) throws JXMException
    {
        try {

            // Get the URL, headers, and timeout
            final String             url     = XCom.flatten( evalVals.get(0), "" );
            final XCom.VariableValue hkvp    =                                    FuncCall._getOptParam        (evalVals, 1                        );
            final int                timeout = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, DefaultTimeout_Seconds) ).intValue() * 1000;

            // Extract the headers
            final ArrayList<String> headerKeyValPairs = new ArrayList<>();

            if( hkvp != null && !( hkvp.size() == 1 && hkvp.get(0).value.equals("") ) ) {
                for(final XCom.VariableStore item : hkvp) headerKeyValPairs.add(item.value);
                if( ( headerKeyValPairs.size() & 1 ) == 1 ) headerKeyValPairs.add("");
            }

            // Create a new HTTP request instance
            final HttpClient.WrappedRequestBuilder req = _newHttpRequest(url, getReq ? RMethod.GET : RMethod.HEAD, headerKeyValPairs);

            if(req == null) return;

            // Perform the HTTP HEAD/GET request
            final ReqExWrapper rew = new ReqExWrapper();
            final HttpResponse res = req.onException( e -> { rew.set(e); } )
                                        .execute    ( timeout              );

            // Process the response
            _processHTTPResponse(retVal, rew, res, !getReq);

        } // try
        catch(final Exception e) {
            _processException(retVal, e, false);
        }
    }

    public static void _execute_http_post(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        try {

            // Get the URL, body, headers, and timeout
            final String             url     = XCom.flatten( evalVals.get(0), "" );
            final String             body    = XCom.flatten( evalVals.get(1), "" );
            final XCom.VariableValue hkvp    =                                    FuncCall._getOptParam        (evalVals, 2                        );
            final int                timeout = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 3, DefaultTimeout_Seconds) ).intValue() * 1000;

            // Extract the headers
            final ArrayList<String> headerKeyValPairs = new ArrayList<>();

            if( hkvp != null && !( hkvp.size() == 1 && hkvp.get(0).value.equals("") ) ) {
                for(final XCom.VariableStore item : hkvp) headerKeyValPairs.add(item.value);
                if( ( headerKeyValPairs.size() & 1 ) == 1 ) headerKeyValPairs.add("");
            }

            // Create a new HTTP request instance
            final HttpClient.WrappedRequestBuilder req = _newHttpRequest(url, RMethod.POST, headerKeyValPairs);

            if(req == null) return;

            // Perform the HTTP POST request
            final ReqExWrapper rew = new ReqExWrapper();
            final HttpResponse res = req.withInput  ( ()-> body            )
                                        .onException( e -> { rew.set(e); } )
                                        .execute    ( timeout              );

            // Process the response
            _processHTTPResponse(retVal, rew, res, false);

        } // try
        catch(final Exception e) {
            _processException(retVal, e, false);
        }
    }

    public static void _execute_http_download(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        String result = null;

        try {

            // Get the URL, output directory path, output name, and retry counts
            final String url     = XCom.flatten( evalVals.get(0), "" );
            final String outDir  =                                    FuncCall._readFlattenOptParam(evalVals, 1, null);
            final String outName =                                    FuncCall._readFlattenOptParam(evalVals, 2, null);
            final int    rcCons  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 3, "-1") ).intValue();
            final int    rcTotal = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 4, "-1") ).intValue();

            // Create a new HTTP downloader instance
            final HTTPDownloader htd = new HTTPDownloader(url, outDir, outName);

            // Perform download
            if( htd.download(rcCons, rcTotal) ) result = htd.getOutFilePath();

        } // try
        catch(final Exception e) {
            _processException(retVal, e, true);
            return;
        }

        // Set the result
        retVal.add( new XCom.VariableStore( true, (result != null) ? result : "" ) );
    }

    public static void _execute_http_set_auth(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        TLAuthenticator.setAsDefault();

        TLAuthenticator.setServerAuth(
            XCom.flatten( evalVals.get(0), "" ),
            XCom.flatten( evalVals.get(1), "" )
        );
    }

    public static void _execute_http_clr_auth(final ArrayList<XCom.VariableValue> evalVals)
    {
        TLAuthenticator.setAsDefault();

        TLAuthenticator.clrServerAuth();
    }

    public static void _execute_ssl_trust_all(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        try {
            SSLTrustAll.setSSLTrustAll( XCom.toBoolean( execBlock, execData, XCom.flatten( evalVals.get(0), "" ) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_gh_get_tags(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            retVal.add( new XCom.VariableStore( true, GitHubUtil.extractTagsFromString( XCom.flatten( evalVals.get(0), "" ) ) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_gh_get_assets(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            retVal.add( new XCom.VariableStore( true, GitHubUtil.extractAssetsFromString( XCom.flatten( evalVals.get(0), "" ) ) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class HTTP
