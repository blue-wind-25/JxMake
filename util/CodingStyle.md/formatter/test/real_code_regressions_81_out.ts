/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {

  private static createAsyncProviders(
      options: ConfigurableModuleAsyncOptions<ModuleOptions>
  ): Provider[]
  {
    if(options.inject && options.provideInjectionTokensFrom) return [ this.createAsyncOptionsProvider(
        options
    ), ...getInjectionProviders(options.provideInjectionTokensFrom, options.inject), ];
    return [ this.createAsyncOptionsProvider(options) ];
  }

} // class Foo
