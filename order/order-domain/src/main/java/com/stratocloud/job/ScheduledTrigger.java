package com.stratocloud.job;

import com.stratocloud.utils.RandomUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledTrigger {
    @ManyToOne
    private JobDefinition jobDefinition;
    @Id
    private String triggerId;
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    @Column(nullable = false)
    private String cronExpression;
    @Column
    private LocalDateTime nextTriggerTime;
    @Column(nullable = false)
    private Boolean disabled = true;
    @Column
    private String description;

    public ScheduledTrigger(JobDefinition jobDefinition,
                            String triggerId,
                            String cronExpression,
                            String description) {
        this.jobDefinition = jobDefinition;
        this.triggerId = triggerId;
        this.cronExpression = cronExpression;
        this.description = description;
    }

    public Optional<LocalDateTime> generateNextTriggerTime(){
        if(nextTriggerTime != null && nextTriggerTime.isAfter(LocalDateTime.now()))
            return Optional.empty();


        int randomOffset = RandomUtil.generateRandomInteger(3000);
        nextTriggerTime = generateNextTriggerTime(cronExpression).plus(randomOffset, ChronoUnit.MILLIS);
        log.info("Next trigger time of {} is {}.", triggerId, nextTriggerTime);
        return Optional.of(nextTriggerTime);
    }

    private static LocalDateTime generateNextTriggerTime(String cronExpression) {
        ZoneId zoneId = ZoneId.systemDefault();

        CronTrigger cronTrigger = new CronTrigger(cronExpression, zoneId);

        SimpleTriggerContext triggerContext = new SimpleTriggerContext();

        Instant instant = cronTrigger.nextExecution(triggerContext);

        return LocalDateTime.ofInstant(Objects.requireNonNull(instant), zoneId);
    }

    public void disable() {
        this.disabled = true;
        this.nextTriggerTime = null;
    }


    public void enable() {
        this.disabled = false;
        generateNextTriggerTime();
    }

    public void update(String cronExpression, String description) {
        this.cronExpression = cronExpression;
        this.description = description;

        if(!disabled)
            generateNextTriggerTime();
    }
}


