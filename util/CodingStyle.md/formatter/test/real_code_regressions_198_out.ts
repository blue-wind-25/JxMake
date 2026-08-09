export interface JSDocAugmentsTag extends JSDocTag {

    readonly kind: SyntaxKind.JSDocAugmentsTag;
    readonly class: ExpressionWithTypeArguments & { readonly expression: Identifier | PropertyAccessEntityNameExpression; };

} // interface JSDocAugmentsTag
