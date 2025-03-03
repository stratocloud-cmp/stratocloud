package com.stratocloud.account.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class EnableAccountsCmd implements JobParameters {
    private List<Long> accountIds;
}
