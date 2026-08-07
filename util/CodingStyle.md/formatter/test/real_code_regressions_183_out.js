/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function initCloneByTag(object, tag, isDeep)
{
    var Ctor = object.constructor;
  switch(tag) {

    case arrayBufferTag  : return cloneArrayBuffer(object)       ;

    case boolTag         : /* FALL-THROUGH */
    case dateTag         : return new Ctor(+object)              ;

    case dataViewTag     : return cloneDataView(object, isDeep)  ;

    case float32Tag      : /* FALL-THROUGH */ case float64Tag      : /* FALL-THROUGH */
    case int8Tag         : /* FALL-THROUGH */ case int16Tag        : /* FALL-THROUGH */ case int32Tag        : /* FALL-THROUGH */
    case uint8Tag        : /* FALL-THROUGH */ case uint8ClampedTag : /* FALL-THROUGH */ case uint16Tag       : /* FALL-THROUGH */ case uint32Tag       : return cloneTypedArray(
        object, isDeep
    );

    case mapTag          : return new Ctor                       ;

    case numberTag       : /* FALL-THROUGH */
    case stringTag       : return new Ctor(object)               ;

    case regexpTag       : return cloneRegExp(object)            ;

    case setTag          : return new Ctor                       ;

    case symbolTag       : return cloneSymbol(object)            ;

  } // switch
} // initCloneByTag
