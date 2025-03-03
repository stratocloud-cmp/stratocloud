package com.stratocloud.tag.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class RemoveTagValuesCmd implements ApiCommand {
    private Long tagEntryId;
    private List<Long> tagValueIds;
}
