/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
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
