package com.stratocloud.resource;

import com.stratocloud.job.TaskInputs;

public record SynchronizeResourceTaskInputs(Long resourceId) implements TaskInputs {
}
