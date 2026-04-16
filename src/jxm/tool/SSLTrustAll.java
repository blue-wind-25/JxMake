/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jxm.xb.*;


/*
 * The code in this class is developed based on the information from:
 *     https://stackoverflow.com/a/13032654
 *     https://stackoverflow.com/a/2893932
 */
public class SSLTrustAll {

    private static final XCom.Mutex       _sslTrustAllMutex    = new XCom.Mutex();

    private static       HostnameVerifier _orgHostnameVerifier = null;
    private static       SSLSocketFactory _orgSSLSocketFactory = null;

    private static final HostnameVerifier _trustAllHosts       = new HostnameVerifier() {
        @Override public boolean verify(final String hostname, final SSLSession session) { return true; }
    };

    private static final TrustManager[]   _trustAllCerts       = new TrustManager[] { new X509TrustManager() {
        @Override public X509Certificate[] getAcceptedIssuers(                                                    ) { return new X509Certificate[0]; }
        @Override public void              checkClientTrusted(final X509Certificate[] chain, final String authType) {}
        @Override public void              checkServerTrusted(final X509Certificate[] chain, final String authType) {}
    } };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _enaSSLTrustAll() throws GeneralSecurityException
    {
        // Check if it is already enabled
        if(_orgHostnameVerifier != null || _orgSSLSocketFactory != null) return;

        // Save the original instances
        _orgHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        _orgSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

        // Install the all-trusting host name verifier
        HttpsURLConnection.setDefaultHostnameVerifier(_trustAllHosts);

        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init( null, _trustAllCerts, new SecureRandom() );
        HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );
    }

    private static void _disSSLTrustAll() throws GeneralSecurityException
    {
        // Check if it is not yet enabled
        if(_orgHostnameVerifier == null || _orgSSLSocketFactory == null) return;

        // Restore the original instances
         HttpsURLConnection.setDefaultHostnameVerifier(_orgHostnameVerifier);
         HttpsURLConnection.setDefaultSSLSocketFactory(_orgSSLSocketFactory);

        // Clear the saved instances
        _orgHostnameVerifier = null;
        _orgSSLSocketFactory = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void setSSLTrustAll(final boolean enabled) throws GeneralSecurityException
    {
        _sslTrustAllMutex.lock();

        try {
            if(enabled) _enaSSLTrustAll();
            else        _disSSLTrustAll();
        }
        catch(final GeneralSecurityException e) {
            // Unlock mutex
            _sslTrustAllMutex.unlock();
            // Re-throw the exception
            throw e;
        }

        _sslTrustAllMutex.unlock();
    }

} // class SSLTrustAll
