package com.stratocloud.kubernetes.common;

import com.stratocloud.exceptions.StratoException;

public record NamespacedRef(String namespace, String name) {
    @Override
    public String toString() {
        return "%s/%s".formatted(namespace, name);
    }

    public static NamespacedRef fromString(String s){
        try {
            String[] arr = s.split("/");
            return new NamespacedRef(
                    arr[0],
                    arr[1]
            );
        }catch (Exception e){
            throw new StratoException("Failed to parse namespaced ref: " + s, e);
        }
    }
}
