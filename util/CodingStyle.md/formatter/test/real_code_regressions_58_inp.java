/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public class Foo
{
    public enum JsonTokenRange {

        BEGIN_TOKEN("beginToken"), END_TOKEN("endToken")

        ;

        final String propertyKey;

        JsonTokenRange(String p)
        {
            this.propertyKey = p;
        }

        public String toString()
        {
            return this.propertyKey;
        }

    } // enum JsonTokenRange
}
