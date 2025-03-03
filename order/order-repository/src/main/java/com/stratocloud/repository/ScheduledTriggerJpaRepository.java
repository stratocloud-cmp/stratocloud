package com.stratocloud.repository;

import com.stratocloud.job.ScheduledTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTriggerJpaRepository
        extends JpaRepository<ScheduledTrigger, String>, JpaSpecificationExecutor<ScheduledTrigger> {
    List<ScheduledTrigger> findByNextTriggerTimeBefore(LocalDateTime dateTime);
}
