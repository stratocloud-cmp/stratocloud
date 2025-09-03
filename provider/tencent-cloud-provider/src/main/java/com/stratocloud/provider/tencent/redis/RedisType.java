package com.stratocloud.provider.tencent.redis;

import com.stratocloud.exceptions.StratoUnsupportedException;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum RedisType {
    REDIS_28_STANDARD(2L, Engine.Redis, "2.8", Architecture.Standard),
    CKV_32_STANDARD(3L, Engine.CKV, "3.2", Architecture.Standard),
    CKV_32_CLUSTER(4L, Engine.CKV, "3.2", Architecture.Cluster),
    REDIS_28_STANDALONE(5L, Engine.Redis, "2.8", Architecture.Standalone),
    REDIS_40_STANDARD(6L, Engine.Redis, "4.0", Architecture.Standard),
    REDIS_40_CLUSTER(7L, Engine.Redis, "4.0", Architecture.Cluster),
    REDIS_50_STANDARD(8L, Engine.Redis, "5.0", Architecture.Standard),
    REDIS_50_CLUSTER(9L, Engine.Redis, "5.0", Architecture.Cluster),
    REDIS_62_STANDARD(15L, Engine.Redis, "6.2", Architecture.Standard),
    REDIS_62_CLUSTER(16L, Engine.Redis, "6.2", Architecture.Cluster),
    REDIS_70_STANDARD(17L, Engine.Redis, "7.0", Architecture.Standard),
    REDIS_70_CLUSTER(18L, Engine.Redis, "7.0", Architecture.Cluster),
    MEMCACHED_16_CLUSTER(200L, Engine.Memcached, "1.6", Architecture.Cluster),
    UNKNOWN(-1L, Engine.Redis, "未知版本", Architecture.Standard)
    ;
    private final Long id;
    private final Engine engine;
    private final String version;
    private final Architecture architecture;

    RedisType(Long id,
              Engine engine,
              String version,
              Architecture architecture) {
        this.id = id;
        this.engine = engine;
        this.version = version;
        this.architecture = architecture;
    }

    public enum Engine {
        Redis, Memcached, CKV
    }

    public enum Architecture {
        Standalone("单机"),
        Standard("标准架构"),
        Cluster("集群架构");
        private final String label;

        Architecture(String label) {
            this.label = label;
        }
    }

    public String getLabel(){
        return "%s %s 内存版 (%s)".formatted(engine, version, architecture.label);
    }

    public static RedisType fromId(Long id){
        for (RedisType redisType : values()) {
            if(Objects.equals(redisType.id, id))
                return redisType;
        }

        return UNKNOWN;
    }

    public static RedisType findMatched(Engine engine, String version, Architecture architecture){
        for (RedisType redisType : values()) {
            if(redisType.engine == engine &&
                    redisType.version.equals(version) &&
                    redisType.architecture == architecture)
                return redisType;
        }

        throw new StratoUnsupportedException(
                "Unsupported tencent redis type %s %s %s".formatted(engine, version, architecture)
        );
    }
}
