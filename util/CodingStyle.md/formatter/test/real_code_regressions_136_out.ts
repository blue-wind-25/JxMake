/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
async function loadDominoOrNull(): Promise<
  ( typeof import('../../../platform-server/third_party/domino/bundled-domino') )['default'] | null
>
{
  return null;
}
