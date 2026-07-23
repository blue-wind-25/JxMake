/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
import { InjectionToken } from 'example-lib';

type CommonEdgeMetadata = {
    sourceModuleName : string;
    targetModuleName : string;
}; // type CommonEdgeMetadata

type ClassToClassEdgeMetadata = {
  type: 'class-to-class';
  sourceClassName: string;
  targetClassName: string;
  sourceClassToken: InjectionToken;
  targetClassToken: InjectionToken;
  injectionType: 'constructor' | 'property' | 'decorator';
  keyOrIndex?: string | number | symbol;
  /**
   * If true, indicates that this edge represents an internal providers connection
   */
  internal?: boolean;
} & CommonEdgeMetadata;

export interface Edge {

    id       : string;
    source   : string;
    target   : string;
    metadata : ClassToClassEdgeMetadata;

} // interface Edge
