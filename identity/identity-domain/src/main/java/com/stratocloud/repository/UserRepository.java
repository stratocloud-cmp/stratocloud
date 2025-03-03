package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.user.User;
import com.stratocloud.user.UserFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends TenantedRepository<User> {
    List<User> findAllByFilters(UserFilters userFilters);


    Page<User> page(UserFilters userFilters, Pageable pageable);

    User findUser(Long userId);

    Optional<User> findByLoginName(String loginName);
}
