/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import jxm.*;
import jxm.tool.*;


public class GitHubUtil {

    private static void _extractTag(final String mapHandle, final JSONDecoder.JSONObject jsonObject)
    {
        // Get and put the 'name'
        final String name = jsonObject.get("name").getString(true);
        MapList.mapPut(mapHandle, "__names__", name, true, true);

        // Get and put the 'zipball_url'
        final String zipball_url = jsonObject.get("zipball_url").getString(true);
        MapList.mapPut(mapHandle, name + ":zipball_url", zipball_url, true, true);

        // Get and put the 'tarball_url'
        final String tarball_url = jsonObject.get("tarball_url").getString(true);
        MapList.mapPut(mapHandle, name + ":tarball_url", tarball_url, true, true);

        // Get and put the 'commit.sha'
        final String commit_sha = jsonObject.get("commit").getObject().get("sha").getString(true);
        MapList.mapPut(mapHandle, name + ":commit.sha", commit_sha, true, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _extractAsset(final String mapHandle, final String id, final JSONDecoder.JSONObject jsonObject) throws Exception
    {
        // Get and put the 'size'
        final String size = jsonObject.get("size").getString(true);
        MapList.mapPut(mapHandle, id + ":size", size, true, true);

        // Get and put the 'updated_at'
        final String updated_at = jsonObject.get("updated_at").getString(true);
        MapList.mapPut(mapHandle, id + ":updated_at", updated_at, true, true);

        // Get and put the 'browser_download_url'
        final String browser_download_url = jsonObject.get("browser_download_url").getString(true);
        MapList.mapPut(mapHandle, id + ":browser_download_url", browser_download_url, true, true);
    }

    private static void _extractAssets(final String mapHandle, final JSONDecoder.JSONObject jsonObject) throws Exception
    {
        // Get and put the 'id'
        final String id = jsonObject.get("id").getString(true);
        MapList.mapPut(mapHandle, "__ids__", id, true, true);

        // Get and put the 'name'
        final String name = jsonObject.get("name").getString(true);
        MapList.mapPut(mapHandle, id + ":name", name, true, true);

        // Get and put the 'tag_name'
        final String tag_name = jsonObject.get("tag_name").getString(true);
        MapList.mapPut(mapHandle, id + ":tag_name", tag_name, true, true);

        // Get and put the 'prerelease'
        final String prerelease = jsonObject.get("prerelease").getString(true);
        MapList.mapPut(mapHandle, id + ":prerelease", prerelease, true, true);

        // Get and put the 'created_at'
        final String created_at = jsonObject.get("created_at").getString(true);
        MapList.mapPut(mapHandle, id + ":created_at", created_at, true, true);

        // Get and put the 'tarball_url'
        final String tarball_url = jsonObject.get("tarball_url").getString(true);
        MapList.mapPut(mapHandle, id + ":tarball_url", tarball_url, true, true);

        // Get and put the 'zipball_url'
        final String zipball_url = jsonObject.get("zipball_url").getString(true);
        MapList.mapPut(mapHandle, id + ":zipball_url", zipball_url, true, true);

        // Get and process the assets
        for( final JSONDecoder.JSONValue jsv : jsonObject.get("assets").getArray() ) {
            _extractAsset( mapHandle, id, jsv.getObject() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String extractTagsFromString(final String jsonStr) throws Exception
    {
        // Create a new map
        final String mapHandle = MapList.mapNew();

        try {

            // Decode the JSON string
            final JSONDecoder.JSONValue jsonValue = JSONDecoder.decode(jsonStr);
          //jsonValue.dump( SysUtil.stdDbg() );

            // Extract the tag(s)
            if( jsonValue.isArray() ) {
                for( final JSONDecoder.JSONValue jsv : jsonValue.getArray() ) {
                    _extractTag( mapHandle, jsv.getObject() );
                }
            }
            else {
                _extractTag( mapHandle, jsonValue.getObject() );
            }

        } // try
        catch(final Exception e) {
            // Delete the map
            MapList.mapDelete(mapHandle);
            // Re-throw the exception
            throw e;
        }

        // Return the new map handle
        return mapHandle;
    }

    public static String extractTagsFromFile(final String jsonFilePath) throws Exception
    { return extractTagsFromString( SysUtil.readTextFileAsString(jsonFilePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String extractAssetsFromString(final String jsonStr) throws Exception
    {
        // Create a new map
        final String mapHandle = MapList.mapNew();

        try {

            // Decode the JSON string
            final JSONDecoder.JSONValue jsonValue = JSONDecoder.decode(jsonStr);
          //jsonValue.dump( SysUtil.stdDbg() );

            // Extract the asset(s)
            if( jsonValue.isArray() ) {
                for( final JSONDecoder.JSONValue jsv : jsonValue.getArray() ) {
                    _extractAssets( mapHandle, jsv.getObject() );
                }
            }
            else {
                _extractAssets( mapHandle, jsonValue.getObject() );
            }

        } // try
        catch(final Exception e) {
            // Delete the map
            MapList.mapDelete(mapHandle);
            // Re-throw the exception
            throw e;
        }

        // Return the new map handle
        return mapHandle;
    }

    public static String extractAssetsFromFile(final String jsonFilePath) throws Exception
    { return extractAssetsFromString( SysUtil.readTextFileAsString(jsonFilePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * API VERSION MIGRATION: 2022-11-28 → 2026-03-10
     *
     * CHANGED FIELDS (observed via live API comparison and GitHub changelog):
     *
     *   Releases endpoint:
     *     - `id`         : JSON integer (was already int in 2022-11-28; getString(true) coerced it).
     *                      In 2026-03-10 the schema formally documents it as int64 — coercion is
     *                      no longer guaranteed to be stable across future parsers.
     *     - `prerelease` : JSON boolean (was already bool; getString(true) coerced to "true"/"false").
     *                      Same stability concern as above.
     *     - `immutable`  : NEW boolean field added in 2026-03-10.  Absent in 2022-11-28 responses.
     *
     *   Assets endpoint (each element of the `assets` array inside a release):
     *     - `size`       : JSON integer (was already int; getString(true) coerced it).
     *                      Same stability concern as `id` above.
     *     - `digest`     : NEW string field added in 2026-03-10 (sha256:<hex> checksum).
     *                      Absent in 2022-11-28 responses.
     *
     *   Tags endpoint: no removals or renames observed; structure is identical in both versions.
     *
     * TYPE CHANGES THAT REQUIRE CODE ADAPTATION:
     *   The original functions call getString(true) on `id`, `size`, and `prerelease`. While
     *   getString(true) performs automatic coercion, it is fragile: if JSONDecoder is replaced or
     *   tightened, those calls will return null for non-string nodes.  The _v2026 variants use
     *   getLong() / getBoolean() and convert explicitly, making the intent unambiguous.
     *
     * HOW TO AUTO-SELECT WHICH VARIANT TO CALL:
     *   Preferred — inspect the parsed JSON for sentinel fields that exist only in one version:
     *     Releases : if jsonObject.get("immutable") != null && !jsonObject.get("immutable").isNull()
     *                → call _extractAssets_v2026; otherwise call _extractAssets.
     *     Assets   : if jsonObject.get("digest")   != null && !jsonObject.get("digest").isNull()
     *                → call _extractAsset_v2026;  otherwise call _extractAsset.
     *   Alternative — pass the API version string down from the HTTP layer and branch on it:
     *     boolean useV2026 = "2026-03-10".compareTo(apiVersion) <= 0;
     */

    /*

    private static void _extractTag_v2026(final String mapHandle, final JSONDecoder.JSONObject jsonObject)
    {
        // Get and put the 'name'
        final String name = jsonObject.get("name").getString(true);
        MapList.mapPut(mapHandle, "__names__", name, true, true);

        // Get and put the 'zipball_url'
        final String zipball_url = jsonObject.get("zipball_url").getString(true);
        MapList.mapPut(mapHandle, name + ":zipball_url", zipball_url, true, true);

        // Get and put the 'tarball_url'
        final String tarball_url = jsonObject.get("tarball_url").getString(true);
        MapList.mapPut(mapHandle, name + ":tarball_url", tarball_url, true, true);

        // Get and put the 'commit.sha'
        final String commit_sha = jsonObject.get("commit").getObject().get("sha").getString(true);
        MapList.mapPut(mapHandle, name + ":commit.sha", commit_sha, true, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _extractAsset_v2026(final String mapHandle, final String id, final JSONDecoder.JSONObject jsonObject) throws Exception
    {
        // 'size' is an int64 in the 2026-03-10 schema — use getLong() instead of getString(true)
        final String size = Long.toString( jsonObject.get("size").getLong() );
        MapList.mapPut(mapHandle, id + ":size", size, true, true);

        // Get and put the 'updated_at'
        final String updated_at = jsonObject.get("updated_at").getString(true);
        MapList.mapPut(mapHandle, id + ":updated_at", updated_at, true, true);

        // Get and put the 'browser_download_url'
        final String browser_download_url = jsonObject.get("browser_download_url").getString(true);
        MapList.mapPut(mapHandle, id + ":browser_download_url", browser_download_url, true, true);

        // Get and put the 'digest' — new in 2026-03-10 (sha256:<hex>)
        final JSONDecoder.JSONValue digestVal = jsonObject.get("digest");
        if( digestVal != null && !digestVal.isNull() ) {
            final String digest = digestVal.getString(true);
            MapList.mapPut(mapHandle, id + ":digest", digest, true, true);
        }
    }

    private static void _extractAssets_v2026(final String mapHandle, final JSONDecoder.JSONObject jsonObject) throws Exception
    {
        // 'id' is an int64 in the 2026-03-10 schema — use getLong() instead of getString(true)
        final String id = Long.toString( jsonObject.get("id").getLong() );
        MapList.mapPut(mapHandle, "__ids__", id, true, true);

        // Get and put the 'name'
        final String name = jsonObject.get("name").getString(true);
        MapList.mapPut(mapHandle, id + ":name", name, true, true);

        // Get and put the 'tag_name'
        final String tag_name = jsonObject.get("tag_name").getString(true);
        MapList.mapPut(mapHandle, id + ":tag_name", tag_name, true, true);

        // 'prerelease' is a boolean in the 2026-03-10 schema — use getBoolean() instead of getString(true)
        final String prerelease = Boolean.toString( jsonObject.get("prerelease").getBoolean() );
        MapList.mapPut(mapHandle, id + ":prerelease", prerelease, true, true);

        // Get and put the 'created_at'
        final String created_at = jsonObject.get("created_at").getString(true);
        MapList.mapPut(mapHandle, id + ":created_at", created_at, true, true);

        // Get and put the 'tarball_url'
        final String tarball_url = jsonObject.get("tarball_url").getString(true);
        MapList.mapPut(mapHandle, id + ":tarball_url", tarball_url, true, true);

        // Get and put the 'zipball_url'
        final String zipball_url = jsonObject.get("zipball_url").getString(true);
        MapList.mapPut(mapHandle, id + ":zipball_url", zipball_url, true, true);

        // Get and put the 'immutable' — new boolean field in 2026-03-10
        final JSONDecoder.JSONValue immutableVal = jsonObject.get("immutable");
        if( immutableVal != null && !immutableVal.isNull() ) {
            final String immutable = Boolean.toString( immutableVal.getBoolean() );
            MapList.mapPut(mapHandle, id + ":immutable", immutable, true, true);
        }

        // Get and process the assets
        for( final JSONDecoder.JSONValue jsv : jsonObject.get("assets").getArray() ) {
            _extractAsset_v2026( mapHandle, id, jsv.getObject() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String extractTagsFromString_v2026(final String jsonStr) throws Exception
    {
        // Create a new map
        final String mapHandle = MapList.mapNew();

        try {

            // Decode the JSON string
            final JSONDecoder.JSONValue jsonValue = JSONDecoder.decode(jsonStr);
          //jsonValue.dump( SysUtil.stdDbg() );

            // Extract the tag(s)
            if( jsonValue.isArray() ) {
                for( final JSONDecoder.JSONValue jsv : jsonValue.getArray() ) {
                    _extractTag_v2026( mapHandle, jsv.getObject() );
                }
            }
            else {
                _extractTag_v2026( mapHandle, jsonValue.getObject() );
            }

        } // try
        catch(final Exception e) {
            // Delete the map
            MapList.mapDelete(mapHandle);
            // Re-throw the exception
            throw e;
        }

        // Return the new map handle
        return mapHandle;
    }

    public static String extractTagsFromFile_v2026(final String jsonFilePath) throws Exception
    { return extractTagsFromString_v2026( SysUtil.readTextFileAsString(jsonFilePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String extractAssetsFromString_v2026(final String jsonStr) throws Exception
    {
        // Create a new map
        final String mapHandle = MapList.mapNew();

        try {

            // Decode the JSON string
            final JSONDecoder.JSONValue jsonValue = JSONDecoder.decode(jsonStr);
          //jsonValue.dump( SysUtil.stdDbg() );

            // Extract the asset(s)
            if( jsonValue.isArray() ) {
                for( final JSONDecoder.JSONValue jsv : jsonValue.getArray() ) {
                    _extractAssets_v2026( mapHandle, jsv.getObject() );
                }
            }
            else {
                _extractAssets_v2026( mapHandle, jsonValue.getObject() );
            }

        } // try
        catch(final Exception e) {
            // Delete the map
            MapList.mapDelete(mapHandle);
            // Re-throw the exception
            throw e;
        }

        // Return the new map handle
        return mapHandle;
    }

    public static String extractAssetsFromFile_v2026(final String jsonFilePath) throws Exception
    { return extractAssetsFromString_v2026( SysUtil.readTextFileAsString(jsonFilePath) ); }

    //*/

} // class GitHubUtil
