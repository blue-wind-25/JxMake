/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public class Foo {

    final Validator noRecordPatterns = new TreeVisitorValidator(
        (node, reporter)-> { if(node instanceof RecordPatternExpr) reporter.report( node, new UpgradeJavaMessage("Record patterns are not supported.", ParserConfiguration.LanguageLevel.JAVA_21) ) ; }
    );

} // class Foo
