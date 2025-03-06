package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.tea.TeaException;
import com.aliyun.tea.TeaModel;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public abstract class AliyunAbstractService {
    protected final CacheService cacheService;
    protected final Config config;

    protected AliyunAbstractService(CacheService cacheService, Config config) {
        this.cacheService = cacheService;
        this.config = config;
    }


    protected interface Invoker<R> {
        R invoke() throws Exception;
    }

    private static class ErrorCodes {
        public static final String SERVICE_UNAVAILABLE = "ServiceUnavailable";

        public static final String THROTTLING = "Throttling";

        public static final String API_THROTTLING = "Throttling.Api";

        public static final String DRY_RUN_OPERATION = "DryRunOperation";

        public static final String TASK_CONFLICT = "TaskConflict";
    }

    protected static <R> R tryInvoke(Invoker<R> invoker){
        return doTryInvoke(invoker, 0);
    }

    private static <R> R doTryInvoke(Invoker<R> invoker, int triedTimes) {
        if(triedTimes >= 10)
            throw new StratoException("Max triedTimes exceeded: "+triedTimes);

        try {
            return invoker.invoke();
        } catch (TeaException e) {
            if(e.getCode() == null)
                throw new ProviderConnectionException(e.getMessage(), e);

            if(e.getCode().contains("AccessKey"))
                throw new ExternalAccountInvalidException(e.getMessage(), e);

            log.warn("ErrorCode: {}", e.getCode());

            if(Objects.equals(e.getStatusCode(), 404))
                throw new ExternalResourceNotFoundException(e.getMessage(), e);

            switch (e.getCode()) {
                case ErrorCodes.SERVICE_UNAVAILABLE -> throw new ProviderConnectionException(e.getMessage(), e);

                case ErrorCodes.THROTTLING, ErrorCodes.API_THROTTLING, ErrorCodes.TASK_CONFLICT -> {
                    log.warn("Retrying later: {}", e.getMessage());
                    SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                    return doTryInvoke(invoker, triedTimes+1);
                }
                case ErrorCodes.DRY_RUN_OPERATION -> {
                    log.info("Dry run operation passed: {}", e.getMessage());
                    return null;
                }
                default -> throw new StratoException(e.getMessage(), e);
            }
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    protected String buildCacheKey(String targetName, TeaModel queryRequest){
        return "Aliyun-%s-ofRegion-%s-andAccessKeyId-%s-andRequest-%s".formatted(
                targetName, config.getRegionId(), config.getAccessKeyId(), JSON.toJsonString(queryRequest)
        );
    }

    protected String buildCacheKey(String targetName){
        return "Aliyun-%s-ofRegion-%s-andAccessKeyId-%s".formatted(
                targetName, config.getRegionId(), config.getAccessKeyId()
        );
    }

    protected <E, R> List<E> queryAllByToken(Invoker<R> invoker,
                                             Function<R, List<E>> listGetter,
                                             Function<R, String> nextTokenGetter,
                                             Consumer<String> nextTokenSetter){
        try {
            R r = tryInvoke(invoker);

            List<E> result = new ArrayList<>();

            List<E> page = listGetter.apply(r);
            if(Utils.isNotEmpty(page))
                result.addAll(page);

            String nextToken = nextTokenGetter.apply(r);

            while (Utils.isNotBlank(nextToken)){
                nextTokenSetter.accept(nextToken);

                r = tryInvoke(invoker);

                page = listGetter.apply(r);
                if(Utils.isNotEmpty(page))
                    result.addAll(page);

                nextToken = nextTokenGetter.apply(r);
            }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return List.of();
        }
    }


    protected <E, R> List<E> queryAll(Invoker<R> invoker,
                                      Function<R, List<E>> listGetter,
                                      Function<R, Integer> totalCountGetter,
                                      Consumer<Integer> pageNumberSetter,
                                      Consumer<Integer> pageSizeSetter){
        try {
            List<E> result = new ArrayList<>();

            final int pageSize = 50;
            pageSizeSetter.accept(pageSize);

            int pageNumber = 1;

            int totalCount = 1;

            while (pageNumber <= (totalCount/pageSize + (totalCount%pageSize==0?0:1))){
                pageNumberSetter.accept(pageNumber);
                R response = tryInvoke(invoker);
                totalCount = totalCountGetter.apply(response);
                List<E> page = listGetter.apply(response);

                if(Utils.isNotEmpty(page))
                    result.addAll(page);

                pageNumber++;
            }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return List.of();
        }
    }
}
