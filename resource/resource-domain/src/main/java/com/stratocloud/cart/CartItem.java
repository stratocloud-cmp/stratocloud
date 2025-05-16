package com.stratocloud.cart;

import com.stratocloud.job.JobHandler;
import com.stratocloud.job.JobHandlerRegistry;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.request.JobParameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends Controllable {
    @Column(nullable = false)
    private String jobType;
    @Column(nullable = false)
    private String jobTypeName;
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> jobParameters;
    @Column(columnDefinition = "TEXT")
    private String summary;

    public CartItem(String jobType, Map<String, Object> jobParameters) {
        setJobType(jobType);
        setJobParameters(jobParameters);
    }

    public void update(Map<String, Object> jobParameters){
        setJobParameters(jobParameters);
    }

    @SuppressWarnings("unchecked")
    public void setJobParameters(Map<String, Object> jobParameters) {
        this.jobParameters = jobParameters;
        JobHandler<JobParameters> jobHandler = (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(jobType);
        List<String> summaryData = jobHandler.collectSummaryData(jobHandler.toTypedJobParameters(jobParameters));
        this.summary = String.join("\n", summaryData);
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
        this.jobTypeName = JobHandlerRegistry.getJobHandler(jobType).getJobTypeName();
    }
}
