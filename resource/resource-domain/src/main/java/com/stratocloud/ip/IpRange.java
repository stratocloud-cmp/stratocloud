package com.stratocloud.ip;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpRange extends Auditable {
    @ManyToOne
    private IpPool ipPool;

    @Column(nullable = false)
    @Convert(converter = IpAttributeConverter.class)
    private IpAddress startIp;
    @Column(nullable = false)
    @Convert(converter = IpAttributeConverter.class)
    private IpAddress endIp;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "range", orphanRemoval = true)
    private List<ManagedIp> managedIps = new ArrayList<>();

    public IpRange(IpPool ipPool, IpAddress startIp, IpAddress endIp) {
        this.ipPool = ipPool;
        this.startIp = startIp;
        this.endIp = endIp;

        this.managedIps.addAll(createIps());
    }

    private List<ManagedIp> createIps(){
        validateRange(startIp, endIp);

        BigInteger start = startIp.toBigInteger();
        BigInteger end = endIp.toBigInteger();

        List<ManagedIp> result = new ArrayList<>();

        InternetProtocol protocol = startIp.getProtocol();

        for(BigInteger i=start; i.compareTo(end)<=0; i = i.add(BigInteger.ONE)){
            IpAddress ipAddress = IpAddress.fromBigInteger(i, protocol);
            ManagedIp managedIp = new ManagedIp(this, ipAddress);
            result.add(managedIp);
        }

        return result;
    }

    private static void validateRange(IpAddress startIp, IpAddress endIp) {
        if(startIp.getProtocol()!=endIp.getProtocol())
            throw new InvalidArgumentException("起始IP与结束IP版本不一致");

        long maxRange = 500L;

        BigInteger startNumber = startIp.toBigInteger();
        BigInteger endNumber = endIp.toBigInteger();

        BigInteger number = endNumber.subtract(startNumber).add(BigInteger.ONE);
        BigInteger min = BigInteger.ONE;
        BigInteger max = BigInteger.valueOf(maxRange);


        if(number.compareTo(min)<=0)
            throw new InvalidArgumentException("IP范围有误");

        if(number.compareTo(max)>0)
            throw new InvalidArgumentException("IP范围不得含有超过"+maxRange+"个IP");
    }

    public void preDeleteCheck() {
        if(managedIps.stream().anyMatch(ip->ip.getState() == ManagedIpState.ALLOCATED))
            throw new BadCommandException("存在已使用IP");
    }
}
