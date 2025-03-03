package com.stratocloud.limit;

import com.stratocloud.job.TaskInputs;

public record SynchronizeResourceUsageLimitInputs(Long limitId) implements TaskInputs {
}
