package com.stratocloud.resource;

import com.stratocloud.job.TaskInputs;

public record ManageExternalResourceTaskInputs(ExternalResource externalResource) implements TaskInputs {
}
