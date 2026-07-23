/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

{
  //#7852
  type Steps = { step: '1' } | { step: '2' };
    const shallowUnionGenParam = shallowRef<Steps>({ step: '1' });
    const shallowUnionAsCast   = shallowRef({ step: '1' } as Steps);

  expectType<IsUnion<typeof shallowUnionGenParam>>(false);
  expectType<IsUnion<typeof shallowUnionAsCast>>(false);
}
