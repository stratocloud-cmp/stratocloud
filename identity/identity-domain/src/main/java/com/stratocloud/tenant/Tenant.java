package com.stratocloud.tenant;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.identity.SimpleTenant;
import com.stratocloud.identity.TenantDeletedPayload;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.messaging.Message;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Tenant extends Auditable {
    @Column(nullable = false)
    private String name;
    @Column
    private String description;
    @Column(nullable = false)
    private Boolean disabled;
    @ManyToOne
    private Tenant parent;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
    private List<Tenant> children = new ArrayList<>();

    public Tenant(String name, String description, Tenant parent) {
        if(parent != null && parent.getDisabled())
            throw new BadCommandException("Parent tenant is disabled.");

        this.name = name;
        this.description = description;
        this.parent = parent;

        this.disabled = false;
    }

    public Tenant(String name, String description) {
        this(name, description, null);
    }


    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void disable() {
        this.disabled = true;
        this.children.forEach(Tenant::disable);
    }

    public void enable() {
        this.disabled = false;
        this.children.forEach(Tenant::enable);
    }

    public void onDelete() {
        SimpleTenant simpleTenant = new SimpleTenant(getId(), name);
        TenantDeletedPayload payload = new TenantDeletedPayload(simpleTenant);
        publish(Message.create(IdentityTopics.TENANT_DELETED_TOPIC, payload));
    }
}
