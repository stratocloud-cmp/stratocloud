package com.stratocloud.kubernetes.volume;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.common.NamespacedRef;

public record PodVolumeId(NamespacedRef podRef, String volumeName) {
    @Override
    public String toString() {
        return "%s@%s".formatted(volumeName, podRef.toString());
    }

    public static PodVolumeId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new PodVolumeId(
                    NamespacedRef.fromString(arr[1]),
                    arr[0]
            );
        }catch (Exception e){
            throw new StratoException("Failed to parse PodVolumeId: " + s, e);
        }
    }
}
