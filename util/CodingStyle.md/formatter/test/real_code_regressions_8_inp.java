/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

public class RealCodeRegressions8 {

  public Void visitClass(ClassTree tree, Void unused) {
    switch (tree.getKind()) {
      case CLASS, INTERFACE -> visitClassDeclaration(tree);
      default -> throw new AssertionError(tree.getKind());
    }
    return null;
  }

}
