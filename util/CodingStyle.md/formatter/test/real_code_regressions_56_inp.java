/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public class SwitchCaseLineBreaking {
    public int getTokenType(Node node, String text, String tokenText) {
        switch (property) {
            case TYPE: {
                String expectedImage = "\"" + text.toLowerCase() + "\"";
                for (int i = 0; i < GeneratedJavaParserConstants.tokenImage.length; i++) {
                    if (GeneratedJavaParserConstants.tokenImage[i].equals(expectedImage)) {
                        return i;
                    }
                }
                throw new RuntimeException("no match");
            }
            default:
                return -1;
        }
    }
}
