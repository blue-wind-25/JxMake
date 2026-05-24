// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.List;
import java.util.Map;

/**
 * Interface for pre-generated parse-table data.
 *
 * In Python PLY, pre-generated tables live in a parsetab.py module.
 * In Java, implement this interface in a generated or hand-written class and
 * pass it to {@link LRTable#readTable(ParseTabData)}.
 *
 * Each production row is an Object array with exactly 6 elements:
 *   [0] String  str   - "name -> sym1 sym2 ..."
 *   [1] String  name  - LHS non-terminal name
 *   [2] Integer len   - number of RHS symbols
 *   [3] String  func  - action function name (may be null)
 *   [4] String  file  - source file (may be null)
 *   [5] Integer line  - source line (may be null / 0)
 */
public interface ParseTabData {
    /** The action table: state → (token → action integer). */
    Map<Integer, Map<String, Integer> > getAction();

    /** The goto table: state → (non-terminal → next state). */
    Map<Integer, Map<String, Integer> > getGoto();

    /** Raw production tuples. */
    List<Object[]> getProductions();

    /** "LALR" or "SLR". */
    String getMethod();

    /** Grammar signature used to validate table freshness. */
    String getSignature();
} // interface ParseTabData
