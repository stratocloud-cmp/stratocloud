package com.stratocloud.account;

import com.stratocloud.account.cmd.SynchronizeAccountsCmd;
import com.stratocloud.constant.CronExpressions;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.job.TriggerParameters;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SynchronizeAccountsJobScheduler implements JobScheduler {

    private final ExternalAccountRepository accountRepository;

    public SynchronizeAccountsJobScheduler(ExternalAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Map<String, Object> createScheduledJobParameters() {
        List<ExternalAccount> accounts = accountRepository.findByDisabled(false);
        SynchronizeAccountsCmd cmd = new SynchronizeAccountsCmd();
        List<Long> accountIds = accounts.stream().map(ExternalAccount::getId).toList();
        cmd.setAccountIds(accountIds);
        return JSON.toMap(cmd);
    }

    @Override
    public TriggerParameters getTriggerParameters() {
        return new TriggerParameters(
                true,
                "SYNCHRONIZE_ACCOUNTS_TRIGGER",
                CronExpressions.EVERY_DAY_2_AM,
                false,
                "每天凌晨两点同步所有已启用的云账号"
        );
    }
}
