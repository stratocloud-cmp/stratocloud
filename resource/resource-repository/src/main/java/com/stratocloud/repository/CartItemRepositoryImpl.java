package com.stratocloud.repository;

import com.stratocloud.cart.CartItem;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartItemRepositoryImpl extends AbstractControllableRepository<CartItem, CartItemJpaRepository>
        implements CartItemRepository {

    public CartItemRepositoryImpl(CartItemJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    public Page<CartItem> page(String search, Pageable pageable) {
        Specification<CartItem> spec = createSpec(search);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<CartItem> createSpec(String search) {
        Specification<CartItem> spec = getCallingTenantSpec();
        spec = spec.and(getCallingOwnerSpec());

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return spec;
    }

    @Override
    public List<CartItem> findByFilters(String search) {
        return jpaRepository.findAll(createSpec(search));
    }

    private Specification<CartItem> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("jobTypeName"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("summary"), "%" + search + "%");
            return criteriaBuilder.or(p1, p2);
        };
    }
}
