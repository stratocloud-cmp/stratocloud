package com.stratocloud.repository;

import com.stratocloud.cart.CartItem;
import com.stratocloud.jpa.repository.ControllableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CartItemRepository extends ControllableRepository<CartItem> {
    Page<CartItem> page(String search, Pageable pageable);

    List<CartItem> findByFilters(String search);
}
