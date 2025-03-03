package com.stratocloud.utils;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class SelfMonitorWorker {
    private static final Set<SelfMonitorTarget> monitorTargets = new HashSet<>();

    public static void register(SelfMonitorTarget target){
        monitorTargets.add(target);
    }

    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES, initialDelay = 0L)
    public void logAllStats(){
        monitorTargets.forEach(SelfMonitorTarget::logStats);
    }
}
