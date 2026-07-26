/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function f(): ts.server.PluginModule =>
{
  return null;
};

const g = (node: tss.Node) : node is tss.Node => true;

const h = (diag: unknown) : diag is ts.Diagnostic => true;

const bare = (n) => n + 1;
