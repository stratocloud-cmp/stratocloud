package com.stratocloud.tag.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class AddTagValueCmd implements ApiCommand {
    private Long tagEntryId;

    private String tagValue;
    private String tagValueName;

    private int index;

    private String description;

}
