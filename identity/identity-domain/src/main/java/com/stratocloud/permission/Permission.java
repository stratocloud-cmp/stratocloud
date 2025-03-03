package com.stratocloud.permission;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_index_target_action", columnNames = {"target", "action"}))
public class Permission extends Auditable {
    @Column(nullable = false)
    private String target;
    @Column(nullable = false)
    private String targetName;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String actionName;

    public Permission(PermissionItem item) {
        this.target = item.target();
        this.targetName = item.targetName();
        this.action = item.action();
        this.actionName = item.actionName();
    }
}
