package com.stratocloud.account;

import com.stratocloud.account.cmd.SynchronizeAccountsCmd;
import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobScheduler;
import org.springframework.stereotype.Component;

@Component
public class SynchronizeAccountsJobHandler implements AsyncJobHandler<SynchronizeAccountsCmd> {

    private final ExternalAccountService accountService;

    private final SynchronizeAccountsJobScheduler scheduler;

    public SynchronizeAccountsJobHandler(ExternalAccountService accountService,
                                         SynchronizeAccountsJobScheduler scheduler) {
        this.accountService = accountService;
        this.scheduler = scheduler;
    }

    @Override
    public String getJobType() {
        return "SYNCHRONIZE_ACCOUNTS";
    }

    @Override
    public String getJobTypeName() {
        return "同步云账号";
    }

    @Override
    public String getStartJobTopic() {
        return "SYNCHRONIZE_ACCOUNTS_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "SYNCHRONIZE_ACCOUNTS_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(SynchronizeAccountsCmd parameters) {
        accountService.synchronizeAccounts(parameters);
    }

    @Override
    public void onUpdateJob(SynchronizeAccountsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        accountService.synchronizeAccounts(parameters);
    }

    @Override
    public void onCancelJob(String message, SynchronizeAccountsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(SynchronizeAccountsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }

    @Override
    public boolean defaultWorkflowRequireOrder() {
        return false;
    }

    @Override
    public JobScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public boolean isTransientJob() {
        return true;
    }
}
