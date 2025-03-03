package com.stratocloud.request;

public interface BatchJobParameters extends JobParameters {
    void merge(BatchJobParameters other);
}
