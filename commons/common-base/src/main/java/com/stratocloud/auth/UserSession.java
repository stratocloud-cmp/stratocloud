package com.stratocloud.auth;

import com.stratocloud.identity.RoleType;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.stratocloud.identity.BuiltInIds.SYSTEM_USER_ID;

@Builder
public record UserSession(Long userId,
                          String loginName,
                          String realName,
                          LocalDateTime lastLoginTime,
                          boolean passwordExpired,
                          String token,
                          Map<String, List<String>> permissions,
                          Long tenantId,
                          List<Long> grantedTenantIds,
                          List<Long> subTenantIds,
                          List<Long> inheritedTenantIds,
                          RoleType roleType,
                          Map<String, List<String>> grantedTags) implements Serializable {

    public boolean isSystemUser(){
        return Objects.equals(userId(), SYSTEM_USER_ID);
    }
}
