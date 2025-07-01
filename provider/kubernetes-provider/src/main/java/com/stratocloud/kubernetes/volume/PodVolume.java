package com.stratocloud.kubernetes.volume;

import io.kubernetes.client.openapi.models.V1Volume;

public record PodVolume(PodVolumeId id, V1Volume volume) {

}
