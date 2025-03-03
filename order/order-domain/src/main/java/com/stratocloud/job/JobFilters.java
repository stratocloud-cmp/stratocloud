package com.stratocloud.job;

import java.util.List;

public record JobFilters(List<Long> jobIds, List<Long> tenantIds, List<Long> ownerIds, List<JobStatus> jobStatuses,
                         String search) {
}
