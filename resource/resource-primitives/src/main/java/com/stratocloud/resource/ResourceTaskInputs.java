package com.stratocloud.resource;

import com.stratocloud.job.TaskInputs;

import java.util.Map;

public record ResourceTaskInputs(ResourceAction resourceAction, Map<String, Object> parameters) implements TaskInputs {
}
