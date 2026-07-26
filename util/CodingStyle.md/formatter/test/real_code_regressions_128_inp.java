/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class RequiredArgsConstructor {
}

class J {
	public class Foo extends BarContext {
		public TerminalNode LOGICAL_OPERATOR() { return getToken(JsonPathParser.LOGICAL_OPERATOR, 0); }
	}

    @RequiredArgsConstructor(onConstructor_ = {@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)})
    public static final class Bar {
        int x;
    }
}
