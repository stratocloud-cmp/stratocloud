package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.user.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserJpaRepository extends TenantedJpaRepository<User>, JpaSpecificationExecutor<User> {
    Optional<User> findByLoginName(String loginName);
}
