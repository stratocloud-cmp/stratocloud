package com.stratocloud.jpa.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class DefaultEntityManager implements EntityManager {

    private final jakarta.persistence.EntityManager jpaEntityManager;


    public DefaultEntityManager(jakarta.persistence.EntityManager jpaEntityManager) {
        this.jpaEntityManager = jpaEntityManager;
    }

    @Override
    public <T> boolean existById(Class<T> entityType, Serializable id) {
        T t = jpaEntityManager.find(entityType, id);
        return t != null;
    }

    @Override
    public <T> T findById(Class<T> entityType, Serializable id) {
        T t = jpaEntityManager.find(entityType, id);

        if(t == null)
            throw new EntityNotFoundException("%s not found by id %s.".formatted(entityType, id));

        return t;
    }
}
