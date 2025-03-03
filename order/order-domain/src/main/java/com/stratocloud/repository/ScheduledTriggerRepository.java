package com.stratocloud.repository;

import com.stratocloud.job.ScheduledTrigger;
import com.stratocloud.jpa.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTriggerRepository extends Repository<ScheduledTrigger, String> {
    List<ScheduledTrigger> findByNextTriggerTimeBefore(LocalDateTime dateTime);

    Page<ScheduledTrigger> page(String search, Pageable pageable);

    ScheduledTrigger findTrigger(String triggerId);
}
