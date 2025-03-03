package com.stratocloud.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class Controllable extends Tenanted {
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    public void transferToNewOwner(long ownerId) {
        setOwnerId(ownerId);
    }
}
