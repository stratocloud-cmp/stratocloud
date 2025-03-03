package com.stratocloud.ip;

import com.stratocloud.exceptions.AllocatingIpReachableException;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.resource.Resource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Objects;

@Entity
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedIp extends Auditable {
    @ManyToOne
    private IpRange range;

    @Column(nullable = false, updatable = false)
    private Long ipPoolId;

    @Column(nullable = false)
    @Convert(converter = IpAttributeConverter.class)
    private IpAddress address;

    @Column(nullable = false, updatable = false)
    private BigInteger toBigInteger;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private ManagedIpState state;

    @Column
    private Long resourceId;
    @Column
    private String resourceName;
    @Column
    private String resourceCategory;
    @Column
    private String allocationReason;

    public ManagedIp(IpRange range, IpAddress address) {
        this.range = range;
        this.address = address;

        this.ipPoolId = range.getIpPool().getId();
        this.toBigInteger = address.toBigInteger();
        this.state = ManagedIpState.AVAILABLE;
    }

    public void allocate(Resource resource, String allocationReason){
        if(state != ManagedIpState.AVAILABLE && !Objects.equals(this.resourceId, resource.getId()))
            throw new BadCommandException("IP已被使用");

        if(address.isReachable())
            throw new AllocatingIpReachableException("IP["+address+"]可连通，无法分配");

        doAllocate(resource, allocationReason);
    }

    public void doAllocate(Resource resource, String allocationReason) {
        this.state = ManagedIpState.ALLOCATED;
        this.resourceId = resource.getId();
        this.resourceName = resource.getName();
        this.resourceCategory = resource.getCategory();
        this.allocationReason = allocationReason;
    }

    public void release(){
        this.state = ManagedIpState.AVAILABLE;
        this.resourceId = null;
        this.resourceName = null;
        this.allocationReason = null;
    }

    public void markExcluded(String reason){
        this.state = ManagedIpState.EXCLUDED;
        this.allocationReason = reason;
    }

}
