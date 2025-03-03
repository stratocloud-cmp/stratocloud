package com.stratocloud.jpa.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<E, ID> {
    E save(E entity);

    E saveWithSystemSession(E entity);

    E saveWithoutTransaction(E entity);

    List<E> saveAll(List<E> entities);

    List<E> saveAllIgnoreDuplicateKey(List<E> entities);


    E saveIgnoreDuplicateKey(E entity);

    Optional<E> findById(ID id);

    boolean existsById(ID id);

    List<E> findAll();

    List<E> findAllById(List<ID> ids);

    long count();

    void deleteById(ID id);

    void delete(E entity);

    void deleteAllById(List<ID> ids);

    void deleteAll(List<E> entities);

    default boolean isPublicVisibility(){
        return false;
    }
}
