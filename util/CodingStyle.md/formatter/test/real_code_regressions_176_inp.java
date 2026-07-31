/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class M {
    void f(List<Wrapper> items) {
        List<JSONObject> results = items.stream()
            .filter( item -> {
                if( item == null ) return false;
                return item.isEnabled();
            } )
            .map( item -> {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name);
                obj.put("id", item.id);
                return obj;
            } )
            .collect( Collectors.toList() );
    }
}
