package com.stratocloud.utils.concurrent;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.RandomUtil;

import java.util.concurrent.TimeUnit;

public class SleepUtil {
    public static void sleep(int seconds){
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new StratoException(e);
        }
    }

    public static void sleepByMillis(int millis){
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new StratoException(e);
        }
    }

    public static void sleepRandomlyByMilliSeconds(int lowerBound, int upperBound){
        try {
            TimeUnit.MILLISECONDS.sleep(RandomUtil.generateRandomInteger(lowerBound, upperBound));
        } catch (InterruptedException e) {
            throw new StratoException(e);
        }
    }
}
