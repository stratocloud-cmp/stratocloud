package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.SdkException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class HuaweiAbstractService {
    protected final CacheService cacheService;
    protected final ICredential credential;
    protected final String regionId;
    protected final String accessKeyId;

    protected HuaweiAbstractService(CacheService cacheService,
                                    ICredential credential,
                                    String regionId,
                                    String accessKeyId) {
        this.cacheService = cacheService;
        this.credential = credential;
        this.regionId = regionId;
        this.accessKeyId = accessKeyId;
    }


    private static class ErrorCodes {
        public static final String API_THROTTLED = "Common.1503";

        public static final String API_THROTTLED_2 = "Common.0024";

        public static final String ECS_TASK_CONFLICT = "Ecs.0603";
    }

    protected static <R> R tryInvoke(Supplier<R> supplier){
        return doTryInvoke(supplier, 0);
    }

    private static <R> R doTryInvoke(Supplier<R> supplier, int triedTimes) {
        if(triedTimes >= 10)
            throw new StratoException("Max triedTimes exceeded: "+triedTimes);

        try {
            return supplier.get();
        } catch (SdkException e) {
            if(!(e instanceof ServiceResponseException serviceResponseException))
                throw new ProviderConnectionException(e.getMessage(), e);

            validateStatusCode(serviceResponseException);

            String errorCode = serviceResponseException.getErrorCode();

            if(Utils.isBlank(errorCode))
                throw new StratoException(e.getMessage(), e);

            log.warn("ErrorCode: {}", errorCode);

            switch (errorCode) {
                case ErrorCodes.API_THROTTLED, ErrorCodes.API_THROTTLED_2, ErrorCodes.ECS_TASK_CONFLICT -> {
                    log.warn("Retrying later: {}.", e.getMessage());
                    SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                    return doTryInvoke(supplier, triedTimes+1);
                }
                default -> throw new StratoException(e.getMessage(), e);
            }
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private static void validateStatusCode(ServiceResponseException e) {
        int statusCode = e.getHttpStatusCode();
        if(statusCode == HttpStatus.UNAUTHORIZED.value())
            throw new ExternalAccountInvalidException(e.getMessage(), e);

        if(statusCode == HttpStatus.NOT_FOUND.value())
            throw new ExternalResourceNotFoundException(e.getMessage(), e);

        if(statusCode == HttpStatus.SERVICE_UNAVAILABLE.value())
            throw new ProviderConnectionException(e.getMessage(), e);
    }

    protected String buildCacheKey(String targetName, Object queryRequest){
        return "HuaweiCloud-%s-ofRegion-%s-andAccessKeyId-%s-andRequest-%s".formatted(
                targetName, regionId, accessKeyId, JSON.toJsonString(queryRequest)
        );
    }

    protected String buildCacheKey(String targetName){
        return "HuaweiCloud-%s-ofRegion-%s-andAccessKeyId-%s".formatted(
                targetName, regionId, accessKeyId
        );
    }

    protected <E> List<E> queryAll(Supplier<List<E>> supplier){
        List<E> response = tryInvoke(supplier);

        if(Utils.isEmpty(response))
            return new ArrayList<>();

        return new ArrayList<>(response);
    }

    protected <E> List<E> queryAll(Supplier<List<E>> supplier,
                                   Consumer<Integer> limitSetter,
                                   Consumer<String> markerSetter,
                                   Function<E, String> idGetter){
        int limit = 100;
        String marker;

        limitSetter.accept(limit);

        List<E> result = new ArrayList<>();
        List<E> response = tryInvoke(supplier);

        while (Utils.isNotEmpty(response)) {
            result.addAll(response);

            if(response.size() < limit)
                break;

            marker = idGetter.apply(response.get(response.size()-1));
            markerSetter.accept(marker);
            response = tryInvoke(supplier);
        }

        return result;
    }

    protected <E> List<E> queryAll(Supplier<List<E>> supplier,
                                   Consumer<Integer> limitSetter,
                                   Consumer<Integer> offsetSetter){
        int limit = 100;

        limitSetter.accept(limit);

        List<E> result = new ArrayList<>();
        offsetSetter.accept(0);
        List<E> response = tryInvoke(supplier);


        while (Utils.isNotEmpty(response)) {
            result.addAll(response);

            if(response.size() < limit)
                break;

            offsetSetter.accept(result.size());
            response = tryInvoke(supplier);
        }

        return result;
    }

    protected <T> Optional<T> queryOne(Supplier<T> supplier){
        try {
            return Optional.ofNullable(tryInvoke(supplier));
        }catch (StratoException e){
            if(e instanceof ExternalResourceNotFoundException){
                log.warn(e.toString());
                return Optional.empty();
            }
            throw e;
        }
    }
}
