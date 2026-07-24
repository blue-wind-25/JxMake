/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
async function main()
{
  if( !( await isInSyncWithRemote() ) ) return;
  else console.log(`${pico.green(`✓`)} commit is up-to-date with remote.\n`);
}
