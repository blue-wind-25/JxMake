// Translated using Claude Sonnet 4.6
package com.pcpp.preprocessor;

/**
 * Records the time taken to #include a file.
 * Mirrors FileInclusionTime in preprocessor.py.
 */
public class FileInclusionTime {
    public final String including_path;
    public final String included_path;
    public final String included_abspath;
    public final int depth;
    public double elapsed;

    public FileInclusionTime(String including_path, String included_path,
                             String included_abspath, int depth) {
        this.including_path = including_path;
        this.included_path = included_path;
        this.included_abspath = included_abspath;
        this.depth = depth;
        this.elapsed = 0.0;
    }
}
