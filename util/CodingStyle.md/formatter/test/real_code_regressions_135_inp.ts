/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
interface WaitCallback {
  doneCb: () => void;
  timeoutId: number;
  updateCb: () => void;
}

function f(cb: () => void, timeoutId: number, updateCb: () => void) {
  push(<WaitCallback>{doneCb: cb, timeoutId: timeoutId, updateCb: updateCb});
}
