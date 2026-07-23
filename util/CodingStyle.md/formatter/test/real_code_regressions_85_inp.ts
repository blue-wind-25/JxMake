/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
import { join } from 'path';

const workspaceRoot = join(__dirname, '../../..');
const localPackageResolver = join(
  workspaceRoot,
  'integration/_support/register-local-packages.ts',
);
