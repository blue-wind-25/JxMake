/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

type Instrumentations = Record<string | symbol, Function | number>;

function createInstrumentations(readonly: boolean, shallow: boolean): Instrumentations
{
  return {};
}

export const mutableCollectionHandlers: ProxyHandler<CollectionTypes> = {
  get: createInstrumentationGetter(false, false),
};
