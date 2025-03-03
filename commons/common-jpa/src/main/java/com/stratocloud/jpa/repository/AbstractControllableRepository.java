package com.stratocloud.jpa.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.RoleType;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.PermissionNotGrantedException;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Objects;

public abstract class AbstractControllableRepository<E extends Controllable, R extends ControllableJpaRepository<E>>
        extends AbstractTenantedRepository<E,R> implements ControllableRepository<E>{

    @Override
    public void validatePermission(E entity) {
        UserSession callingUser = CallContext.current().getCallingUser();
        if(callingUser.roleType()== RoleType.NORMAL_USER){
            if(!Objects.equals(callingUser.userId(), entity.getOwnerId()))
                throw new PermissionNotGrantedException("Sorry, you cannot control entities owned by others.");
        }else {
            super.validatePermission(entity);
        }
    }

    protected AbstractControllableRepository(R jpaRepository) {
        super(jpaRepository);
    }


    protected Specification<E> getCallingOwnerSpec(){
        if(CallContext.current().isAdmin())
            return getSpec();

        Long userId = CallContext.current().getCallingUser().userId();
        return getOwnerSpec(List.of(userId));
    }

    protected Specification<E> getOwnerSpec(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> root.get("ownerId").in(userIds);
    }

    @Override
    public List<E> findByOwnerIds(List<Long> ownerIds) {
        return jpaRepository.findByOwnerIdIn(ownerIds);
    }
}
