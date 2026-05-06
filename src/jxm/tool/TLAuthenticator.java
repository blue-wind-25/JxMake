/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.net.Authenticator;
import java.net.PasswordAuthentication;

import java.nio.charset.StandardCharsets;

import java.util.Base64;

import jxm.*;
import jxm.xb.*;


/*
 * The code in this class is developed based on the information from:
 *     https://gist.github.com/veysiertekin/763f4637c60368f199e4
 *
 * This class provides a ThreadLocal-based Authenticator that allows each thread to have its own
 * HTTP server and proxy credentials. It also provides a snapshot mechanism for compatibility with
 * Java 11+ HttpClient worker threads.
 */
public class TLAuthenticator extends Authenticator {

    private static final XCom.Mutex          _tlaMutex       = new XCom.Mutex();
    private static       boolean             _installed      = false;
    private static final TLAuthenticator     _instance       = new TLAuthenticator();

    private static final ThreadLocal<String> _proxyUsername  = new ThreadLocal<>();
    private static final ThreadLocal<String> _proxyPassword  = new ThreadLocal<>();

    private static final ThreadLocal<String> _serverUsername = new ThreadLocal<>();
    private static final ThreadLocal<String> _serverPassword = new ThreadLocal<>();

    private static String _fallbackServerUsername = null;
    private static String _fallbackServerPassword = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TLAuthenticator()
    {}

    private static String _getSUser() {
        return (_serverUsername.get() != null) ? _serverUsername.get() : _fallbackServerUsername;
    }

    private static String _getSPass() {
        return (_serverPassword.get() != null) ? _serverPassword.get() : _fallbackServerPassword;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        final RequestorType rt = getRequestorType();

        String username = null;
        String password = null;

        if( rt == RequestorType.PROXY ) {
            username = _proxyUsername.get();
            password = _proxyPassword.get();
        }
        else {
            // Default to server credentials for SERVER or any other (null/unexpected) type
            username = _getSUser();
            password = _getSPass();
        }

        if(username == null || password == null) return null;

        return new PasswordAuthentication( username, password.toCharArray() );
    }

    /*
     * # Returns a stand-alone Authenticator whose credentials are a snapshot of the calling thread's current credentials.
     * # If the calling thread has no credentials set, the returned Authenticator returns null (no credentials).
     */
    public static Authenticator snapshot()
    {
        final String proxyUser  = _proxyUsername .get();
        final String proxyPass  = _proxyPassword .get();
        final String serverUser = _getSUser();
        final String serverPass = _getSPass();

        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                final RequestorType rt = getRequestorType();

                String username = null;
                String password = null;

                if( rt == RequestorType.PROXY ) {
                    username = proxyUser;
                    password = proxyPass;
                }
                else {
                    // Default to server credentials for SERVER or any other (null/unexpected) type
                    username = serverUser;
                    password = serverPass;
                }

                if(username == null || password == null) return null;

                return new PasswordAuthentication( username, password.toCharArray() );
            }

        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void setAsDefault()
    {
        _tlaMutex.lock();

        if(!_installed) {
            Authenticator.setDefault(_instance);
            _installed = true;
        }

        _tlaMutex.unlock();
    }

    public static void setProxyAuth(final String username, final String password)
    {
        _proxyUsername.set(username);
        _proxyPassword.set(password);
    }

    public static void clrProxyAuth()
    {
        _proxyUsername.set(null);
        _proxyPassword.set(null);
    }

    public static void setServerAuth(final String username, final String password)
    {
        _serverUsername.set(username);
        _serverPassword.set(password);

        // Also update the fallback
        _fallbackServerUsername = username;
        _fallbackServerPassword = password;
    }

    public static void clrServerAuth()
    {
        _serverUsername.set(null);
        _serverPassword.set(null);

        // Also clear the fallback
        _fallbackServerUsername = null;
        _fallbackServerPassword = null;
    }

    /*
     * # Returns the value of the 'Authorization' header for the current thread's server credentials,
     * # encoded as HTTP Basic authentication (RFC 7617), or null if no server credentials are set.
     * # Use this for preemptive authentication with java.net.http.HttpClient (Java 11+) because the
     *   Authenticator-based challenge/retry mechanism is unreliable across JDK versions and HTTP/2.
     */
    public static String getServerAuthorizationHeader()
    {
        final String u = _getSUser();
        final String p = _getSPass();

        if(u == null || p == null) return null;

        return "Basic " + Base64.getEncoder().encodeToString( (u + ":" + p).getBytes(StandardCharsets.UTF_8) );
    }

} // class TLAuthenticator
