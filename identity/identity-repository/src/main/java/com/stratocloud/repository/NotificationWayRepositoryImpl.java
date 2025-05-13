package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.notification.NotificationWay;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class NotificationWayRepositoryImpl
        extends AbstractTenantedRepository<NotificationWay, NotificationWayJpaRepository>
        implements NotificationWayRepository {

    public NotificationWayRepositoryImpl(NotificationWayJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationWay findNotificationWay(Long notificationWayId) {
        return findById(notificationWayId).orElseThrow(
                () -> new EntityNotFoundException("Notification way not found")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationWay> page(String search, List<Long> notificationWayIds, Pageable pageable) {
        Specification<NotificationWay> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(notificationWayIds))
            spec = spec.and(getIdSpec(notificationWayIds));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<NotificationWay> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            Predicate p1 = criteriaBuilder.like(root.get("name"), s);
            Predicate p2 = criteriaBuilder.like(root.get("description"), s);
            return criteriaBuilder.or(p1, p2);
        };
    }
}
