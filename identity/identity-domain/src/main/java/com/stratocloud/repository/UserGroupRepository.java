package com.stratocloud.repository;

import com.stratocloud.group.UserGroup;
import com.stratocloud.jpa.repository.TenantedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserGroupRepository extends TenantedRepository<UserGroup> {
    UserGroup findUserGroup(Long userGroupId);

    List<UserGroup> findByFilters(List<Long> userGroupIds,
                                  List<Long> userIds,
                                  String search,
                                  Map<String, List<String>> tagsMap,
                                  boolean allGroups);

    Page<UserGroup> page(List<Long> userGroupIds,
                         List<Long> userIds,
                         String search,
                         Map<String, List<String>> tagsMap,
                         Boolean allGroups,
                         Pageable pageable);
}
