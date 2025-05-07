package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.notification.NotificationPolicy;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationPolicyRepositoryImpl extends AbstractTenantedRepository<NotificationPolicy, NotificationPolicyJpaRepository> implements NotificationPolicyRepository {

    public NotificationPolicyRepositoryImpl(NotificationPolicyJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPolicy findNotificationPolicy(Long notificationPolicyId) {
        return jpaRepository.findById(notificationPolicyId).orElseThrow(
                () -> new EntityNotFoundException("Notification policy not found")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationPolicy> page(String search, Pageable pageable) {
        Specification<NotificationPolicy> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<NotificationPolicy> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            Predicate p1 = criteriaBuilder.like(root.get("name"), s);
            Predicate p2 = criteriaBuilder.like(root.get("description"), s);
            return criteriaBuilder.or(p1, p2);
        };
    }
}
