package com.stratocloud.jpa.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.RoleType;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.PermissionNotGrantedException;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.utils.Assert;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.GenericTypeResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class AbstractRepository<E, ID, R extends JpaRepository<E, ID>> implements Repository<E, ID>{

    protected final R jpaRepository;

    protected AbstractRepository(R jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @SuppressWarnings("unchecked")
    protected Class<E> getEntityClass(){
        Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(getClass(), AbstractRepository.class);
        Objects.requireNonNull(typeArguments);
        Assert.isNotEmpty(typeArguments);
        return (Class<E>) typeArguments[0];
    }

    public void validatePermission(E entity){
        UserSession callingUser = CallContext.current().getCallingUser();
        if(callingUser.roleType() != RoleType.SUPER_ADMIN)
            throw new PermissionNotGrantedException(
                    "Calling user %s's role type %s can never have write permissions to %s entities.".formatted(
                            callingUser.loginName(), callingUser.roleType(), getEntityClass().getSimpleName()
                    )
            );
    }

    @Override
    @Transactional
    public E save(E entity) {
        EntityUtil.preSave(entity);
        validatePermission(entity);
        return jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public E saveWithSystemSession(E entity) {
        CallContext current = CallContext.current();
        CallContext.registerSystemSession();
        E e = save(entity);
        CallContext.registerBack(current);
        return e;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public E saveWithoutTransaction(E entity) {
        EntityUtil.preSave(entity);
        validatePermission(entity);
        return jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public List<E> saveAll(List<E> entities) {
        List<E> result = new ArrayList<>();
        entities.forEach(e -> result.add(save(e)));
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<E> saveAllIgnoreDuplicateKey(List<E> entities) {
        List<E> result = new ArrayList<>();
        entities.forEach(e -> result.add(saveIgnoreDuplicateKey(e)));
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public E saveIgnoreDuplicateKey(E entity) {
        try {
            return save(entity);
        }catch (DataIntegrityViolationException e){
            log.warn("Entity data integrity violated. Entity={}.", entity);
            return entity;
        }catch (ConstraintViolationException e){
            log.warn("Entity constraint violated. Entity={}.", entity);
            return entity;
        }catch (ObjectOptimisticLockingFailureException e){
            log.warn("Optimistic locking failure. Entity={}.", entity);
            return entity;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<E> findById(ID id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<E> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<E> findAllById(List<ID> ids) {
        return jpaRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        Optional<E> optional = findById(id);
        if(optional.isEmpty())
            return;
        delete(optional.get());
    }

    @Override
    @Transactional
    public void delete(E entity) {
        validatePermission(entity);
        jpaRepository.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAllById(List<ID> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    @Transactional
    public void deleteAll(List<E> entities) {
        entities.forEach(this::delete);
    }


    protected Specification<E> getSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and();
    }
}
