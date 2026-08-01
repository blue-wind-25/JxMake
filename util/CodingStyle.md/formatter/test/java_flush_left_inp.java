/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on */

package com.example.core;

import java.util.List;
import java.util.ArrayList;

// A file with every line flushed to column 0 (no leading indentation at all),
// exercising reindentation from a fully unindented starting point.
public class FlushLeft {
private int count;
private List<String> names = new ArrayList<String>();

public FlushLeft(int count) {
this.count = count;
}

public int total() {
int sum = 0;
for (int i = 0; i < count; i++) {
if (i % 2 == 0) {
sum += i;
} else {
sum -= i;
}
}
return sum;
}

public void addName(String name) {
names.add(name);
}
}
