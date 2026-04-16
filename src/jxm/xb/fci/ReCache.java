/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class ReCache {

    private static final HashMap<String, Pattern> _reCache = new HashMap<>();
    private static final XCom.Mutex               _reMutex = new XCom.Mutex();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Pattern _reGetPattern(final String regexpStr) throws JXMException
    {
        // Lock mutex
        _reMutex.lock();

        // Try to get the compiled pattern from the cache first
        Pattern pattern = _reCache.get(regexpStr);

        // Compile the pattern string if a compiled version of it does not exist in the cache
        if(pattern == null) {
            // Compile the pattern
            try {
                pattern = Pattern.compile(regexpStr);
            }
            catch(final Exception e) {
                // Unlock mutex
                _reMutex.unlock();
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Throw as a different exception
                throw XCom.newJXMRuntimeError( e.toString() );
            }
            // Store the compiled pattern to the cache
            _reCache.put(regexpStr, pattern);
        }

        // Unlock mutex
        _reMutex.unlock();

        // Return the pattern instance
        return pattern;
    }

    public static Matcher _reGetMatcher(final String regexpStr, final String subject) throws JXMException
    { return _reGetPattern(regexpStr).matcher(subject); }

} // class ReCache
