/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LambdaTypeExtractor {
    void m() {
        ResolvedType actualType;

        if (lambdaExpr.getBody() instanceof ExpressionStmt) {
            actualType = facade.getType(((ExpressionStmt) lambdaExpr.getBody()).getExpression());
        } else if (lambdaExpr.getBody() instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt) lambdaExpr.getBody();

            List<ReturnStmt> returnStmts = blockStmt.findAll(ReturnStmt.class);

            if (returnStmts.size() > 0) {
                Set<ResolvedType> resolvedTypes = returnStmts.stream()
                        .map(returnStmt -> returnStmt
                                .getExpression()
                                .map(e -> facade.getType(e))
                                .orElse(ResolvedVoidType.INSTANCE))
                        .collect(Collectors.toSet());
                actualType = LeastUpperBoundLogic.of().lub(resolvedTypes);

            } else {
                actualType = ResolvedVoidType.INSTANCE;
            }

        } else {
            throw new UnsupportedOperationException("Cannot resolve the type of lambda expression body " + lambdaExpr.getBody());
        }
    }
}
