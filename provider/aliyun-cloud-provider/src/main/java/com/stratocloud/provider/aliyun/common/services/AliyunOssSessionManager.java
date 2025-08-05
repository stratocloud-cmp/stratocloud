package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
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
public class AliyunOssSessionManager implements DisposableBean {

    public record OssSessionKey(String regionId,
                                String accessKeyId,
                                String accessKeySecret){
    }

    private static final Map<OssSessionKey, OSS> sessionsMap = new ConcurrentHashMap<>();

    public static synchronized OSS getSession(OssSessionKey key) {
        OSS ossSession = sessionsMap.get(key);

        if(ossSession == null){
            if(sessionsMap.size() >= 20)
                releaseRandomly();

            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            ossSession = OSSClientBuilder.create()
                    .endpoint("oss-%s.aliyuncs.com".formatted(key.regionId()))
                    .region(key.regionId())
                    .credentialsProvider(
                            new DefaultCredentialProvider(
                                    key.accessKeyId(),
                                    key.accessKeySecret()
                            )
                    )
                    .clientConfiguration(clientBuilderConfiguration)
                    .build();
            sessionsMap.put(key, ossSession);
        }

        return ossSession;
    }

    private static synchronized void releaseRandomly() {
        Optional<OssSessionKey> releasingKey = RandomUtil.selectRandomly(
                new ArrayList<>(sessionsMap.keySet())
        );

        if(releasingKey.isPresent()) {
            OSS releasingSession = sessionsMap.remove(releasingKey.get());

            if(releasingSession != null) {
                try {
                    releasingSession.shutdown();
                    log.warn("Too many aliyun oss sessions, released random one: {}.", releasingKey.get());
                }catch (Exception e){
                    log.warn("Failed to shutdown oss session: {}", releasingKey.get(), e);
                }
            }
        }
    }


    @Override
    public void destroy() throws Exception {
        Collection<OSS> sessions = sessionsMap.values();

        if(Utils.isEmpty(sessions))
            return;

        for (OSS session : sessions) {
            try {
                session.shutdown();
            }catch (Exception e){
                log.warn(e.getMessage());
            }
        }
    }
}
