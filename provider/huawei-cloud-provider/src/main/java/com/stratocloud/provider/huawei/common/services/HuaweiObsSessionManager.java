package com.stratocloud.provider.huawei.common.services;

import com.obs.services.IObsClient;
import com.obs.services.ObsClient;
import com.stratocloud.utils.RandomUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HuaweiObsSessionManager implements DisposableBean {

    public record ObsSessionKey(String regionId,
                                String accessKey,
                                String secretKey){
    }

    private static final Map<ObsSessionKey, IObsClient> sessionsMap = new ConcurrentHashMap<>();

    public static synchronized IObsClient getSession(ObsSessionKey key) {
        IObsClient obsClient = sessionsMap.get(key);

        if(obsClient == null){
            if(sessionsMap.size() >= 20)
                releaseRandomly();

            obsClient = new ObsClient(
                    key.accessKey,
                    key.secretKey,
                    "obs.%s.myhuaweicloud.com".formatted(key.regionId)
            );
            sessionsMap.put(key, obsClient);
        }

        return obsClient;
    }

    private static synchronized void releaseRandomly() {
        Optional<ObsSessionKey> releasingKey = RandomUtil.selectRandomly(
                new ArrayList<>(sessionsMap.keySet())
        );

        if(releasingKey.isPresent()) {
            IObsClient releasingSession = sessionsMap.remove(releasingKey.get());

            if(releasingSession != null) {
                try {
                    releasingSession.close();
                    log.warn("Too many huawei obs sessions, released random one: {}.", releasingKey.get());
                }catch (Exception e){
                    log.warn("Failed to shutdown oss session: {}", releasingKey.get(), e);
                }
            }
        }
    }


    @Override
    public void destroy() throws Exception {
        Collection<IObsClient> sessions = sessionsMap.values();

        if(Utils.isEmpty(sessions))
            return;

        for (IObsClient session : sessions) {
            try {
                session.close();
            }catch (Exception e){
                log.warn(e.getMessage());
            }
        }
    }
}
