/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct IMutableEnumValuesRegistry {
    virtual ~IMutableEnumValuesRegistry();

    virtual Detail::EnumInfo const& registerEnum( StringRef enumName, StringRef allEnums, std::vector<int> const& values ) = 0;

    template<typename E>
    Detail::EnumInfo const& registerEnum( StringRef enumName, StringRef allEnums, std::initializer_list<E> values ) {
        static_assert(sizeof(int) >= sizeof(E), "Cannot serialize enum to int");
        std::vector<int> intValues;
        intValues.reserve( values.size() );
        for( auto enumValue : values )
            intValues.push_back( static_cast<int>( enumValue ) );
        return registerEnum( enumName, allEnums, intValues );
    }
};
