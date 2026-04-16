/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.net.Authenticator;
import java.net.PasswordAuthentication;

import jxm.xb.*;


/*
 * The code in this class is developed based on the information from:
 *     https://gist.github.com/veysiertekin/763f4637c60368f199e4
 */
public class TLAuthenticator extends Authenticator {

    private static final XCom.Mutex          _tlaMutex       = new XCom.Mutex();
    private static       boolean             _installed      = false;
    private static final TLAuthenticator     _instance       = new TLAuthenticator();

    private static final ThreadLocal<String> _proxyUsername  = new ThreadLocal<>();
    private static final ThreadLocal<String> _proxyPassword  = new ThreadLocal<>();

    private static final ThreadLocal<String> _serverUsername = new ThreadLocal<>();
    private static final ThreadLocal<String> _serverPassword = new ThreadLocal<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TLAuthenticator()
    {}

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        String username = null;
        String password = null;

        if( super.getRequestorType() == RequestorType.PROXY ) {
            username = _proxyUsername.get();
            password = _proxyPassword.get();
        }
        else if( super.getRequestorType() == RequestorType.SERVER ) {
            username = _serverUsername.get();
            password = _serverPassword.get();
        }

        if(username == null || password == null) return null;

        return new PasswordAuthentication( username, password.toCharArray() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void setAsDefault()
    {
        _tlaMutex.lock();

        /*
         *  DEPRECATED since Java 9
         *
         *  https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.base/share/classes/sun/net/www/protocol/http/AuthCacheValue.java
         *  https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.base/share/classes/sun/net/www/protocol/http/AuthCacheImpl.java
         *
         *  import sun.net.www.protocol.http.AuthCacheValue;
         *  import sun.net.www.protocol.http.AuthCacheImpl;
         *  ....
         *  AuthCacheValue.setAuthCache(new AuthCacheImpl());
         *  Authenticator.setDefault(new URLAuthenticator(username, password));
         */
        /*
        try {
            final Class<?>  classAuthCacheValue = Class.forName("sun.net.www.protocol.http.AuthCacheValue");
            final Class<?>  classAuthCache      = Class.forName("sun.net.www.protocol.http.AuthCache"     );
            final Class<?>  classAuthCacheImpl  = Class.forName("sun.net.www.protocol.http.AuthCacheImpl" );
            classAuthCacheValue.getDeclaredMethod("setAuthCache", classAuthCache).invoke( classAuthCacheValue, classAuthCacheImpl.newInstance() );
        }
        catch(final Exception e) {
            e.printStackTrace();
        }
        //*/

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
    }

    public static void clrServerAuth()
    {
        _serverUsername.set(null);
        _serverPassword.set(null);
    }

} // class TLAuthenticator
