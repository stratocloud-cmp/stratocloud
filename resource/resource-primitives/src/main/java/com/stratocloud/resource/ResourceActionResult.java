package com.stratocloud.resource;

import com.stratocloud.job.TaskState;

public record ResourceActionResult(TaskState taskState, String errorMessage) {
    public static ResourceActionResult finished(){
        return new ResourceActionResult(TaskState.FINISHED, null);
    }

    public static ResourceActionResult inProgress(){
        return new ResourceActionResult(TaskState.STARTED, null);
    }

    public static ResourceActionResult failed(String errorMessage) {
        return new ResourceActionResult(TaskState.FAILED, errorMessage);
    }
}
