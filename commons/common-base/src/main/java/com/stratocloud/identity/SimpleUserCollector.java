package com.stratocloud.identity;

import java.util.List;
import java.util.Optional;

public interface SimpleUserCollector {
    List<SimpleUser> findUsers(List<Long> userIds);

    default Optional<SimpleUser> findUser(Long userId){
        return findUsers(List.of(userId)).stream().findAny();
    }
}
