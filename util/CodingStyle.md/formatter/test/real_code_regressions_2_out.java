/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

public class RealCodeRegressions2 {

    static int classCode(final int mr)
    {
        return mr >>> 3;
    }

    private int _indentLevel = 0;

    public int  getIndentLevel(             ) { return _indentLevel;             }
    public void setIndentLevel(final int lvl) { _indentLevel = Math.max(0, lvl); }

    private java.util.List<Integer> _selected = new java.util.ArrayList<>();

    void report(final Object parent)
    {
        _getOperandFromField(
            parent,
            "value"
        );
    }

    private Object _getOperandFromField(final Object parent, final String field)
    {
        return null;
    }

    public void actionDelete()    { _deleteSelected(); }
    public void actionEdit()
    { if( _selected.size() == 1 ) _editBlock( _selected.iterator().next() ); }
    public void actionCut () { _cutSelected();  }
    public void actionCopy() { _copySelected(); }

    private void _deleteSelected(              ) {  }
    private void _editBlock     (final Object o) {  }
    private void _cutSelected   (              ) {  }
    private void _copySelected  (              ) {  }

} // class RealCodeRegressions2
