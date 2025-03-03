package com.stratocloud.tag.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteTagEntriesCmd implements ApiCommand {
    private List<Long> tagEntryIds;
}
