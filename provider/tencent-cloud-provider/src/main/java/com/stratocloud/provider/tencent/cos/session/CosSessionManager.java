package com.stratocloud.provider.tencent.cos.session;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
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
public class CosSessionManager implements DisposableBean {

    private static final Map<CosSessionKey, CosSession> sessionsMap = new ConcurrentHashMap<>();

    public static synchronized CosSession getSession(CosSessionKey key) {
        CosSession cosSession = sessionsMap.get(key);

        if(cosSession == null){
            if(sessionsMap.size() >= 20)
                releaseRandomly();

            COSClient cosClient = new COSClient(
                    new BasicCOSCredentials(
                            key.secretId(),
                            key.secretKey()
                    ),
                    new ClientConfig(
                            new Region(key.region())
                    )
            );
            cosSession = new CosSessionImpl(cosClient);
            sessionsMap.put(key, cosSession);
        }

        return cosSession;
    }

    private static synchronized void releaseRandomly() {
        Optional<CosSessionKey> releasingKey = RandomUtil.selectRandomly(
                new ArrayList<>(sessionsMap.keySet())
        );

        if(releasingKey.isPresent()) {
            CosSession releasingSession = sessionsMap.remove(releasingKey.get());

            if(releasingSession != null) {
                try {
                    releasingSession.shutdown();
                    log.warn("Too many tencent cos sessions, released random one: {}.", releasingKey.get());
                }catch (Exception e){
                    log.warn("Failed to shutdown cos session: {}", releasingKey.get(), e);
                }
            }
        }
    }


    @Override
    public void destroy() throws Exception {
        Collection<CosSession> sessions = sessionsMap.values();

        if(Utils.isEmpty(sessions))
            return;

        for (CosSession session : sessions) {
            try {
                session.shutdown();
            }catch (Exception e){
                log.warn(e.getMessage());
            }
        }
    }
}
