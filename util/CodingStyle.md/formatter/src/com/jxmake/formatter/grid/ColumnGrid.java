/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColumnGrid {

    private final List<String[]> buffer = new ArrayList<>();

    public void addRow(String[] cells) {
        buffer.add(cells);
    }

    public List<String[]> flush() {
        if (buffer.isEmpty()) {
            return Collections.emptyList();
        }

        int maxColumns = 0;
        for (String[] row : buffer) {
            maxColumns = Math.max(maxColumns, row.length);
        }

        int[] widths = new int[maxColumns];
        for (String[] row : buffer) {
            for (int c = 0; c < row.length; c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }

        List<String[]> result = new ArrayList<>(buffer.size());
        for (String[] row : buffer) {
            String[] padded = new String[row.length];
            for (int c = 0; c < row.length; c++) {
                boolean isLastInRow = c == row.length - 1;
                padded[c] = isLastInRow ? row[c] : padRight(row[c], widths[c]);
            }
            result.add(padded);
        }

        buffer.clear();
        return result;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
