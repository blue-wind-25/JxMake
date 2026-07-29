/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class TestRunNodeBuilder {

    private fun buildJUnitDynamicNodes(
        testRunNodes: Collection<TreeNode<String>>
    ): Collection<String> =
    // This is the proper implementation that should be used instead:
//        buildList {
//            testRunNodes.forEach { testRunNode ->
//                testRunNode.items.mapTo(this) { testRun ->
//                    dynamicTest(testRun.displayName) { runTest(testRun) }
//                }
//            }
//        }
        buildList {
            testRunNodes.forEach { testRunNode ->
                testRunNode.items.mapTo(this) { testRun ->
                    dynamicTest(testRun.displayName) { runTest(testRun) }
                }
            }
        }

} // class TestRunNodeBuilder
