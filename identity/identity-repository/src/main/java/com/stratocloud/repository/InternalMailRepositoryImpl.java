package com.stratocloud.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.notification.internal.InternalMail;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InternalMailRepositoryImpl
        extends AbstractAuditableRepository<InternalMail, InternalMailJpaRepository>
        implements InternalMailRepository{
    public InternalMailRepositoryImpl(InternalMailJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<InternalMail> page(String search, Boolean read, Pageable pageable) {
        Specification<InternalMail> spec = getSpec();

        spec = spec.and(getReceiverIdSpec());

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(read != null)
            spec = spec.and(getReadSpec(read));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<InternalMail> getReadSpec(Boolean read) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(root.get("read"), read);
    }

    private Specification<InternalMail> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("message"), "%"+search+"%");
            Predicate p2 = criteriaBuilder.like(root.get("eventId"), "%"+search+"%");
            return criteriaBuilder.or(p1, p2);
        };
    }

    private Specification<InternalMail> getReceiverIdSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("receiverUserId"),
                CallContext.current().getCallingUser().userId()
        );
    }
}
