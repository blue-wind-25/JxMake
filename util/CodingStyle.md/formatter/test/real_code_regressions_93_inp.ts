/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
async function isInSyncWithRemote() {
  try {
    const branch = await getBranch()
    const res = await fetch(
      `https://api.github.com/repos/vuejs/core/commits/${branch}?per_page=1`,
    )
    const data = await res.json()
    return data.sha
  } catch {
    return false
  }
}

function get(target: object, key: string | symbol, receiver: object): unknown {
  if (data !== EMPTY_OBJ && isReservedPrefix(key[0]) && hasOwn(data, key)) {
    warn(
      `Property ${JSON.stringify(
        key,
      )} must be accessed via $data because it starts with a reserved ` +
        `character ("$" or "_") and is not proxied on the render context.`,
    )
  } else if (instance === currentRenderingInstance) {
    warn(
      `Property ${JSON.stringify(key)} was accessed during render ` +
        `but is not defined on instance.`,
    )
  }
}
