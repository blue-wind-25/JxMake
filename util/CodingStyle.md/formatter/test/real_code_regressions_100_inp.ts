/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
export interface ParserOptions
  extends ErrorHandlingOptions, CompilerCompatOptions {
  htmlMode?: boolean;
}

class Widget
  implements Renderable, Disposable {
  render(): void {}
}
