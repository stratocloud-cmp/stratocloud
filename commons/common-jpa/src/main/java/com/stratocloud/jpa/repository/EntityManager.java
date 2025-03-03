package com.stratocloud.jpa.repository;

import java.io.Serializable;

public interface EntityManager {

    <T> boolean existById(Class<T> entityType, Serializable id);
    <T> T findById(Class<T> entityType, Serializable id);
}
