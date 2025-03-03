package com.stratocloud.resource;

import com.stratocloud.job.TaskState;

public record RelationshipActionResult(TaskState taskState, String errorMessage) {
    public static RelationshipActionResult finished(){
        return new RelationshipActionResult(TaskState.FINISHED, null);
    }

    public static RelationshipActionResult inProgress(){
        return new RelationshipActionResult(TaskState.STARTED, null);
    }

    public static RelationshipActionResult failed(String errorMessage){
        return new RelationshipActionResult(TaskState.FAILED, errorMessage);
    }
}
