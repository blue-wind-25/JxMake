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

} // class GitHubUtil
