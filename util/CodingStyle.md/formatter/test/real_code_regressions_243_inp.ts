/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-by-operator-priority=on

function createApplicationInternalHelperFunctionNameHere(ngDevMode: boolean, validAppIdInitializer: unknown, appProviders: unknown[]): unknown[] {
  const allAppProviders = [
    provideZonelessChangeDetectionInternal(),
    errorHandlerEnvironmentInitializer,
    ...(ngDevMode ? [validAppIdInitializer] : []),
    ...(appProviders || []),
  ];
  return allAppProviders;
}
