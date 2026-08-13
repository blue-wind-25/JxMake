// Translated using Claude Sonnet 4.6
// ply: YGen.java
//
// This is a support program that auto-generates different versions of the YACC parsing
// function with different features removed for the purposes of performance.
//
// Users should edit the method LRParser.parsedebug() in yacc.py. The source code
// for that method is then used to create the other methods. See the comments in
// yacc.py for further details.
//
// Translated from ygen.py (PLY project).
package com.pcpp.ply;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class YGen {

    /**
     * Finds the line range [startIndex+1, endIndex) for the block tagged with {@code tag}.
     * The start line matches "#--! {tag}-start" and end line matches "#--! {tag}-end".
     *
     * @param lines all lines of the source file
     * @param tag   the tag name to search for
     * @return an int array of length 2: {startIndex + 1, endIndex}
     */
    public static int[] getSourceRange(List<String> lines, String tag)
    {
        String startTag = "#--! " + tag + "-start";
        String endTag   = "#--! " + tag + "-end";

        int startIndex = -1;
        int endIndex   = -1;

        for( int i = 0; i < lines.size(); ++i )
            if( lines.get(i).trim().startsWith(startTag) ) {
                startIndex = i;
                break;
        }

        for( int i = startIndex + 1; i < lines.size(); ++i )
            if( lines.get(i).trim().endsWith(endTag) ) {
                endIndex = i;
                break;
        }

        return new int[] {
                   startIndex + 1, endIndex
        };
    }

    /**
     * Filters out sections of lines that are enclosed between pairs of lines
     * starting with "#--! {tag}". The first occurrence toggles inclusion off,
     * the second toggles it back on, and so forth (like a comment-block toggle).
     *
     * @param lines source lines to filter
     * @param tag   the tag name to filter
     * @return the filtered list of lines
     */
    public static List<String> filterSection(List<String> lines, String tag)
    {
        List<String> filteredLines = new ArrayList<>();
        boolean      include       = true;
        String       tagText       = "#--! " + tag;

        for(String line : lines) {
                 if( line.trim().startsWith(tagText) ) include = !include;
            else if(include)                           filteredLines.add(line);
        }

        return filteredLines;
    }

    public static void main(String[] args) throws IOException
    {
        // Determine the directory containing this class's source (or use CWD as fallback).
        // In Python, os.path.dirname(__file__) gives the directory of the script.
        // Here we use the directory of yacc.py relative to YGen.java if run from source,
        // or fall back to the working directory. The conventional approach is to accept
        // the yacc.py path as an argument, or search alongside this file.
        String dirName;
        if(args.length > 0) {
            dirName = args[0];
        }
        else {
            // Default: same directory as this source file would be.
            // At runtime we use the working directory as a reasonable default.
            dirName = Paths.get( YGen.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath() )
                      .getParent().toString();
        }

        Path yaccPath    = Paths.get(dirName, "yacc.py");
        Path yaccBakPath = Paths.get(dirName, "yacc.py.bak");

        // Back up the original yacc.py
        Files.copy(
            yaccPath, yaccBakPath, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        );

        // Read all lines
        List<String> lines = Files.readAllLines(yaccPath, StandardCharsets.UTF_8);
        // ReadAllLines strips line terminators; add them back to match Python behaviour
        List<String> rawLines = new ArrayList<>( lines.size() );
        for(String l : lines) rawLines.add(l + "\n");
        lines = rawLines;

        int[] parseRange           = getSourceRange(lines, "parsedebug");
        int[] parseoptRange        = getSourceRange(lines, "parseopt");
        int[] parseoptNotrackRange = getSourceRange(lines, "parseopt-notrack");

        int parseStart           = parseRange[0];
        int parseEnd             = parseRange[1];
        int parseoptStart        = parseoptRange[0];
        int parseoptEnd          = parseoptRange[1];
        int parseoptNotrackStart = parseoptNotrackRange[0];
        int parseoptNotrackEnd   = parseoptNotrackRange[1];

        // Get the original source block
        List<String> origLines = new ArrayList<>( lines.subList(parseStart, parseEnd) );

        // Filter the DEBUG sections out
        List<String> parseoptLines = filterSection(origLines, "DEBUG");

        // Filter the TRACKING sections out
        List<String> parseoptNotrackLines = filterSection(parseoptLines, "TRACKING");

        // Replace the parser source sections with updated versions.
        // We must apply changes in reverse order so earlier indices remain valid.
        // notrack section comes after parseopt section in the file.
        lines.subList(parseoptNotrackStart, parseoptNotrackEnd).clear();
        lines.addAll(parseoptNotrackStart, parseoptNotrackLines);

        // Recalculate parseopt indices after the notrack replacement (no shift needed
        // if notrack is after parseopt, but we re-read them for safety).
        // The original Python replaced in-place using slice assignment in sequence;
        // since notrack is always after parseopt, we apply parseopt second.
        lines.subList(parseoptStart, parseoptEnd).clear();
        lines.addAll(parseoptStart, parseoptLines);

        // Normalise line endings (strip trailing whitespace, re-add newline)
        List<String> normalised = new ArrayList<>( lines.size() );
        for(String line : lines) normalised.add( rtrim(line) + "\n" );

        Files.write(yaccPath, normalised, StandardCharsets.UTF_8);

        System.out.println("Updated yacc.py");
    }

    private static String rtrim(String s)
    {
        int end = s.length();
        while( end > 0 && s.charAt(end - 1) <= ' ' ) end--;

        return s.substring(0, end);
    }

} // class YGen
