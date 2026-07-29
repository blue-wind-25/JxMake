/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class M {
    static class Inner {
        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            ExpressionStatement newExpression = withExpression(expression.withType(type));
            return (T) (newExpression == expression ? this : newExpression);
        }
    }
}
