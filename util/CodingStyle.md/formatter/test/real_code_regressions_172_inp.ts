/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function checkAttrs(nodeAttrs: string[] | null, current: string, isProjectionMode: boolean) {
  if (nodeAttrs === null) {
    return false;
  } else {
    return isCssClassMatchingAttrsForTestingReallyLongNameHere(nodeAttrs, current, isProjectionMode);
  }
}

function checkFlag(nodeAttrs: string[] | null, current: boolean) {
  if (nodeAttrs === null) {
    return false;
  } else {
    return current && reallyLongBooleanFlagNameUsedOnlyForTestingPurposesHereIndeedYesTrulyVeryLongOne;
  }
}
