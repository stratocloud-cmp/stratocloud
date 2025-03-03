package com.stratocloud.workflow;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SequenceFlow extends Auditable {
    @ManyToOne
    private Node source;
    @ManyToOne
    private Node target;

    public SequenceFlow(Node source, Node target) {
        this.source = source;
        this.target = target;
    }
}
