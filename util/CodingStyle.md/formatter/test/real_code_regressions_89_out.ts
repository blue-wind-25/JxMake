/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

type MergedHook<T = () => void> = T | T[];

export type InjectToObject<T extends ComponentInjectOptions> =
  T extends string[]
    ? {
        [ K in T[number] ]?: unknown
}
    : never;

export type MergedComponentOptions = ComponentOptions &
  MergedComponentOptionsOverride;
