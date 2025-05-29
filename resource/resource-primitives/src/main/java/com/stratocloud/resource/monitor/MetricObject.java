package com.stratocloud.resource.monitor;

import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MetricObject(List<MetricDimension> dimensions) {
    public Optional<MetricDimension> getDimension(String dimensionName) {
        if(Utils.isEmpty(dimensions))
            return Optional.empty();

        return dimensions.stream().filter(d -> Objects.equals(dimensionName, d.name())).findAny();
    }
}
