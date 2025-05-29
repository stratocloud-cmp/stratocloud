package com.stratocloud.resource.monitor;

import java.time.LocalDateTime;

public record MetricDataPoint(double value, LocalDateTime time) {

    public MetricDataPoint(double value, LocalDateTime time) {
        this.value = ((double) Math.round(value*100))/100;
        this.time = time;
    }

}
