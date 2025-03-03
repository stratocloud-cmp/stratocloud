package com.stratocloud.repository;

import com.stratocloud.group.UserGroup;
import com.stratocloud.jpa.repository.TenantedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserGroupRepository extends TenantedRepository<UserGroup> {
    UserGroup findUserGroup(Long userGroupId);

    List<UserGroup> findByFilters(List<Long> userGroupIds, List<Long> userIds, String search);

    Page<UserGroup> page(List<Long> userGroupIds, List<Long> userIds, String search, Boolean allGroups, Pageable pageable);
}
