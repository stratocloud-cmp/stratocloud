package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.notification.Notification;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class NotificationRepositoryImpl
        extends AbstractTenantedRepository<Notification, NotificationJpaRepository>
        implements NotificationRepository {

    public NotificationRepositoryImpl(NotificationJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> page(String search, Pageable pageable) {
        Specification<Notification> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Notification> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("eventId"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("eventType"), "%" + search + "%");
            Predicate p3 = criteriaBuilder.like(root.get("eventTypeName"), "%" + search + "%");
            Predicate p4 = criteriaBuilder.like(root.get("eventObjectType"), "%" + search + "%");
            Predicate p5 = criteriaBuilder.like(root.get("eventObjectTypeName"), "%" + search + "%");
            Predicate p6 = criteriaBuilder.like(root.get("eventObjectName"), "%" + search + "%");
            Predicate p7 = criteriaBuilder.like(root.get("eventSummary"), "%" + search + "%");

            return criteriaBuilder.or(p1,p2,p3,p4,p5,p6,p7);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Notification findNotification(Long notificationId) {
        return jpaRepository.findById(notificationId).orElseThrow(
                () -> new EntityNotFoundException("Notification not found")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByPolicyIdAndSentCountLessThan(Long policyId, int sentCount) {
        Specification<Notification> spec = getSpec();

        spec = spec.and(getPolicyIdSpec(policyId));

        spec = spec.and(getSentCountLessThanSpec(sentCount));

        return jpaRepository.findAll(spec);
    }

    private Specification<Notification> getSentCountLessThanSpec(int sentCount) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThan(
                root.get("sentCount"), sentCount
        );
    }

    private Specification<Notification> getPolicyIdSpec(Long policyId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("policy").get("id"), policyId
        );
    }
}
