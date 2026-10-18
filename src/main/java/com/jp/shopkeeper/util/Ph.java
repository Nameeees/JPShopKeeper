package com.jp.shopkeeper.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constructor corto de mapas de placeholders:
 * Ph.of("trader", id, "page", 1) -> {trader=id, page=1}
 */
public final class Ph {

    private Ph() {
    }

    public static Map<String, String> of(Object... pairs) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (pairs == null) {
            return out;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = String.valueOf(pairs[i]);
            Object value = pairs[i + 1];
            out.put(key, value == null ? "" : String.valueOf(value));
        }
        return out;
    }
}
