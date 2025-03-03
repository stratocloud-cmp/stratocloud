package com.stratocloud.repository;

import com.stratocloud.cart.CartItem;
import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CartItemJpaRepository
        extends ControllableJpaRepository<CartItem>, JpaSpecificationExecutor<CartItem> {
}
