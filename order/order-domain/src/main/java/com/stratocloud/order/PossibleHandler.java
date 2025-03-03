package com.stratocloud.order;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PossibleHandler extends Auditable {
    @ManyToOne
    private Order order;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String userName;
    @Column(nullable = false)
    private Long nodeInstanceId;

    public PossibleHandler(Long userId, String userName, Long nodeInstanceId) {
        this.userId = userId;
        this.userName = userName;
        this.nodeInstanceId = nodeInstanceId;
    }
}
