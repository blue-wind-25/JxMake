    function isConstAssertion(location: Node): location is AssertionExpression {
        return isAssertionExpression(location) && isConstTypeReference(location.type);
    }

    function relativeType(node: Node): InferenceResult {
        if (isParameter(node)) {
            return emptyInferenceResult;
        }
        if (isShorthandPropertyAssignment(node)) {
            return {
                typeNode: createTypeOfFromEntityNameExpression(node.name),
                mutatedTarget: false,
            };
        }
        if (isEntityNameExpression(node)) {
            return {
                typeNode: createTypeOfFromEntityNameExpression(node),
                mutatedTarget: false,
            };
        }
        if (isConstAssertion(node)) {
            return relativeType(node.expression);
        }
        if (isArrayLiteralExpression(node)) {
            const variableDecl = findAncestor(node, isVariableDeclaration);
            const partName = variableDecl && isIdentifier(variableDecl.name) ? variableDecl.name.text : undefined;
            return typeFromArraySpreadElements(node, partName);
        }
        if (isObjectLiteralExpression(node)) {
            const variableDecl = findAncestor(node, isVariableDeclaration);
            const partName = variableDecl && isIdentifier(variableDecl.name) ? variableDecl.name.text : undefined;
            return typeFromObjectSpreadAssignment(node, partName);
        }
        if (isVariableDeclaration(node) && node.initializer) {
            return relativeType(node.initializer);
        }
        if (isConditionalExpression(node)) {
            const { typeNode: trueType, mutatedTarget: mTrue } = relativeType(node.whenTrue);
            if (!trueType) return emptyInferenceResult;
            const { typeNode: falseType, mutatedTarget: mFalse } = relativeType(node.whenFalse);
            if (!falseType) return emptyInferenceResult;
            return {
                typeNode: factory.createUnionTypeNode([trueType, falseType]),
                mutatedTarget: mTrue || mFalse,
            };
        }

        return emptyInferenceResult;
    }

    function typeToTypeNode(type: Type, enclosingDeclaration: Node, flags = NodeBuilderFlags.None): TypeNode | undefined {
        let isTruncated = false;
        const minimizedTypeNode = typeToMinimizedReferenceType(typeChecker, type, enclosingDeclaration, declarationEmitNodeBuilderFlags | flags, declarationEmitInternalNodeBuilderFlags, {
            moduleResolverHost: program,
            trackSymbol() {
                return true;
            },
            reportTruncationError() {
                isTruncated = true;
            },
        });
        if (!minimizedTypeNode) {
            return undefined;
        }
        const result = typeNodeToAutoImportableTypeNode(minimizedTypeNode, importAdder, scriptTarget);
        return isTruncated ? factory.createKeywordTypeNode(SyntaxKind.AnyKeyword) : result;
    }

    function typePredicateToTypeNode(typePredicate: TypePredicate, enclosingDeclaration: Node, flags = NodeBuilderFlags.None): TypeNode | undefined {
        let isTruncated = false;
        const result = typePredicateToAutoImportableTypeNode(typeChecker, importAdder, typePredicate, enclosingDeclaration, scriptTarget, declarationEmitNodeBuilderFlags | flags, declarationEmitInternalNodeBuilderFlags, {
            moduleResolverHost: program,
            trackSymbol() {
                return true;
            },
            reportTruncationError() {
                isTruncated = true;
            },
        });
        return isTruncated ? factory.createKeywordTypeNode(SyntaxKind.AnyKeyword) : result;
    }

    function addTypeToVariableLike(decl: ParameterDeclaration | VariableDeclaration | PropertyDeclaration): DiagnosticOrDiagnosticAndArguments | undefined {
        const { typeNode } = inferType(decl);
        if (typeNode) {
            if (decl.type) {
                changeTracker.replaceNode(getSourceFileOfNode(decl), decl.type, typeNode);
            }
            else {
                changeTracker.tryInsertTypeAnnotation(getSourceFileOfNode(decl), decl, typeNode);
            }
            return [Diagnostics.Add_annotation_of_type_0, typeToStringForDiag(typeNode)];
        }
    }

    function typeToStringForDiag(node: Node) {
        setEmitFlags(node, EmitFlags.SingleLine);
        const result = typePrinter.printNode(EmitHint.Unspecified, node, sourceFile);
        if (result.length > defaultMaximumTruncationLength) {
            return result.substring(0, defaultMaximumTruncationLength - "...".length) + "...";
        }
