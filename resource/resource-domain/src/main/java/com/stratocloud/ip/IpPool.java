package com.stratocloud.ip;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.NetworkUtil;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpPool extends Tenanted {
    @Column(nullable = false)
    private String name;
    @Column
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InternetProtocol protocol;
    @Column(nullable = false)
    @Convert(converter = CidrAttributeConverter.class)
    private Cidr cidr;
    @Column(nullable = false)
    @Convert(converter = IpAttributeConverter.class)
    private IpAddress gateway;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ipPool", orphanRemoval = true)
    private List<IpRange> ranges = new ArrayList<>();

    @OneToMany
    private List<Resource> attachedNetworkResources = new ArrayList<>();

    public IpPool(String name, String description, InternetProtocol protocol, Cidr cidr, IpAddress gateway) {
        validate(protocol, cidr, gateway);

        this.name = name;
        this.description = description;
        this.protocol = protocol;
        this.cidr = cidr;
        this.gateway = gateway;
    }

    private static void validate(InternetProtocol protocol, Cidr cidr, IpAddress gateway) {
        if(!NetworkUtil.isValidCidr(protocol, cidr.value()))
            throw new InvalidArgumentException("无效CIDR: %s".formatted(cidr.value()));

        if(!NetworkUtil.isValidIp(protocol, gateway.address()))
            throw new InvalidArgumentException("无效网关: %s".formatted(gateway.address()));
    }




    public void addRange(IpAddress startIp, IpAddress endIp){
        IpRange range = new IpRange(this, startIp, endIp);
        this.ranges.add(range);
    }

    public void update(String name, String description, Cidr cidr, IpAddress gateway) {
        validate(protocol, cidr, gateway);
        this.name = name;
        this.description = description;
        this.cidr = cidr;
        this.gateway = gateway;
    }

    public void updateAttachedNetworkResources(List<Resource> networks){
        if(Utils.isEmpty(networks)){
            this.attachedNetworkResources.clear();
            return;
        }

        for (Resource network : networks) {
            ResourceHandler resourceHandler = network.getResourceHandler();
            if(!resourceHandler.canAttachIpPool())
                throw new BadCommandException(
                        "Resource type %s cannot be attached to ip pools.".formatted(
                                resourceHandler.getResourceTypeId()
                        )
                );
        }

        this.attachedNetworkResources.clear();
        this.attachedNetworkResources.addAll(networks);
    }

    public void preDeleteCheck() {
        ranges.forEach(IpRange::preDeleteCheck);
    }

    public void removeRangeById(Long rangeId) {
        ranges.removeIf(ipRange -> Objects.equals(rangeId, ipRange.getId()));
    }
}
