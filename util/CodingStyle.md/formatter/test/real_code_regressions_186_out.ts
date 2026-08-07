/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Sample {

    effect   : AnimationEffect | null                                                           = null;
    finished : Promise<Animation>                                                               = Promise.resolve(
        {} as any
    );
    onremove : ( (this: Animation, ev: AnimationPlaybackEventWithExtraLongName) => any ) | null = null;

} // class Sample

class Widget {

  @Input( { transform: (value: string | number, _: any) => value ? 1 : 0 } )
  inlineFunctionInput: any;

} // class Widget
