/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Outer {

  public createModuleReferenceType(): Type<ModuleRef>
  {
    const self = this;
    return class extends ModuleRef {

      constructor()
      {
        super(self.container);
      }

      public async create<T = any>(
        type: Type<T>,
        contextId?: ContextId,
      ): Promise<T> {
        return this.instantiateClass<T>(type, self, contextId);
      }

    }; // class ModuleRef
  }

} // class Outer
