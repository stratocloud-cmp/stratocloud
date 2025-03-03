package com.stratocloud.jpa.entities;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.identifier.SnowflakeId;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.identity.SimpleUserCollector;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EntityUtil {
    public static void forceSetId(Auditable auditable, Long id){
        try {
            Field idField = Auditable.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(auditable, id);
        } catch (Exception e) {
            throw new StratoException(e.getMessage(), e);
        }
    }


    public static void copyBasicFields(Auditable auditable, NestedAuditable nestedAuditable){
        Long id = auditable.getId();
        String createdBy = auditable.getCreatedBy();
        LocalDateTime createdAt = auditable.getCreatedAt();
        String lastModifiedBy = auditable.getLastModifiedBy();
        LocalDateTime lastModifiedAt = auditable.getLastModifiedAt();

        nestedAuditable.setId(id);
        nestedAuditable.setCreatedBy(createdBy);
        nestedAuditable.setCreatedAt(createdAt);
        nestedAuditable.setLastModifiedBy(lastModifiedBy);
        nestedAuditable.setLastModifiedAt(lastModifiedAt);

        if(auditable instanceof Tenanted tenanted){
            NestedTenanted nestedTenanted = (NestedTenanted) nestedAuditable;
            nestedTenanted.setTenantId(tenanted.getTenantId());

            if(tenanted instanceof Controllable controllable){
                NestedControllable nestedControllable = (NestedControllable) nestedTenanted;
                nestedControllable.setOwnerId(controllable.getOwnerId());
            }
        }
    }

    public static void preSave(Object o){
        if(!CallContext.exists()) {
            String message = "CallContext does not exist when saving %s.".formatted(o);
            log.error(message);
            throw new StratoException(message);
        }

        if(o instanceof Auditable auditable){
            if(auditable.getId() == null){
                auditable.setId(SnowflakeId.nextId());
            }

            UserSession callingUser = CallContext.current().getCallingUser();
            if(auditable.getCreatedAt() == null){
                auditable.setSoftDeleted(false);
                auditable.setCreatedAt(LocalDateTime.now());
                auditable.setCreatedBy(callingUser.realName());
            }

            auditable.setLastModifiedAt(LocalDateTime.now());
            auditable.setLastModifiedBy(callingUser.realName());

            if(auditable instanceof Tenanted tenanted){
                if(tenanted.getTenantId() == null){
                    tenanted.setTenantId(callingUser.tenantId());
                }

                if(tenanted instanceof Controllable controllable){
                    if(controllable.getOwnerId() == null){
                        controllable.setOwnerId(callingUser.userId());
                    }
                }
            }
        }
    }



    public static void fillOwnerInfo(Iterable<? extends NestedControllable> controllableSet,
                                     SimpleUserCollector simpleUserCollector){
        if(controllableSet == null)
            return;

        Set<Long> userIds = new HashSet<>();

        for (NestedControllable controllable : controllableSet) {
            if(controllable.getOwnerId() == null)
                continue;
            userIds.add(controllable.getOwnerId());
        }

        List<SimpleUser> users = simpleUserCollector.findUsers(new ArrayList<>(userIds));

        Map<Long, SimpleUser> userMap;

        if(Utils.isEmpty(users))
            userMap = Map.of();
        else
            userMap = users.stream().collect(
                    Collectors.toMap(SimpleUser::userId, u -> u)
            );

        for (NestedControllable controllable : controllableSet) {
            Long ownerId = controllable.getOwnerId();

            if(ownerId == null || !userMap.containsKey(ownerId))
                continue;

            SimpleUser simpleUser = userMap.get(ownerId);
            controllable.setOwnerLoginName(simpleUser.loginName());
            controllable.setOwnerRealName(simpleUser.realName());
        }
    }

}
