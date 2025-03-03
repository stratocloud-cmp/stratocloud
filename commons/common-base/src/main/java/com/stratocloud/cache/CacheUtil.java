package com.stratocloud.cache;

import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.function.Supplier;

@Slf4j
public class CacheUtil {

    @SuppressWarnings("unchecked")
    public static <R> R queryWithCache(CacheService cacheService,
                                       String cacheKey,
                                       long secondsToLive,
                                       Supplier<R> queryFunction,
                                       R resultObject){
        try {
            R result = cacheService.get(cacheKey, (Class<R>) resultObject.getClass());

            if(result != null)
                return result;

        }catch (Exception e){
            log.warn("Failed to get cache: ", e);
        }

        R result = queryFunction.get();

        if(result!=null)
            setCache(cacheService, cacheKey, secondsToLive, result);


        return result;
    }

    private static <R> void setCache(CacheService cacheService, String cacheKey, long secondsToLive, R result) {
        try {
            if(result instanceof Collection<?> resultCollection)
                if(Utils.isEmpty(resultCollection))
                    return;

            cacheService.set(cacheKey, result, secondsToLive, ChronoUnit.SECONDS);
        }catch (Exception e){
            log.warn("Failed to set cache: ", e);
        }
    }

}
