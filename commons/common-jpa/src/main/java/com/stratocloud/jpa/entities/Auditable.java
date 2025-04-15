package com.stratocloud.jpa.entities;

import com.stratocloud.identifier.SnowflakeId;
import com.stratocloud.messaging.Message;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OptimisticLock;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class Auditable {
    @Id
    @Setter(AccessLevel.PROTECTED)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    @Column(nullable = false)
    private boolean softDeleted;
    @Column
    private Long deleteTimestamp;
    @Column(nullable = false, updatable = false)
    private String createdBy;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @OptimisticLock(excluded = true)
    @Column
    private String lastModifiedBy;
    @OptimisticLock(excluded = true)
    @Column
    private LocalDateTime lastModifiedAt;

    public Long getId() {
        if(id == null){
            id = SnowflakeId.nextId();
        }
        return id;
    }

    protected void publish(Message message){
        EntityMessagesHolder.offer(getId(), message);
    }

    protected boolean doesPublishWithSystemSession(){
        return false;
    }

    @PostPersist
    @PostUpdate
    protected void afterSave(){
        EntityMessagesHolder.flushEntityMessages(getId(), doesPublishWithSystemSession());
    }

    @PostRemove
    protected void afterDeleted(){
        EntityMessagesHolder.flushEntityMessages(getId(), doesPublishWithSystemSession());
    }

    @PrePersist
    @PreUpdate
    protected void preSave(){
        EntityUtil.preSave(this);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName()+"{" +
                "id=" + id +
                ", version=" + version +
                '}';
    }
}
