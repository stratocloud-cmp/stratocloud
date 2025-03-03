package com.stratocloud.job;

import com.stratocloud.constant.CronExpressions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TestScheduledTrigger {
    @Test
    public void test() throws InterruptedException {
        ScheduledTrigger trigger = new ScheduledTrigger(
                null, "123", CronExpressions.EVERY_TEN_SECONDS, null
        );

        assert trigger.generateNextTriggerTime().isPresent();
        assert trigger.generateNextTriggerTime().isEmpty();
        assert trigger.generateNextTriggerTime().isEmpty();
        TimeUnit.SECONDS.sleep(15L);
        assert trigger.generateNextTriggerTime().isPresent();
        assert trigger.generateNextTriggerTime().isEmpty();
        assert trigger.generateNextTriggerTime().isEmpty();
    }
}
