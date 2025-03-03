package com.stratocloud.resource;

import com.stratocloud.job.TaskInputs;

import java.util.Map;

public record ConnectTaskInputs(Map<String, Object> properties) implements TaskInputs {
}
