/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

export class Metadata {

    private static readonly _undefinedValue = {};
    private _map : { [key: string]: any; };
    private _version = 0;
    private _size = -1;

    public set(key: string, value: any): this {
        this._map[ Metadata._escapeKey(
            key
        ) ] = value === undefined ? Metadata._undefinedValue : value;
        this._size = -1;
        this._version++;
        return this;
    }

} // class Metadata
