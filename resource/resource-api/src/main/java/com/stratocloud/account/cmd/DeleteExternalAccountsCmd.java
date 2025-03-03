package com.stratocloud.account.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteExternalAccountsCmd implements ApiCommand {
    private List<Long> externalAccountIds;
}
