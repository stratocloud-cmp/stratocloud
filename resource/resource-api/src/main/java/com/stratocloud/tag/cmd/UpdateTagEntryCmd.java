package com.stratocloud.tag.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateTagEntryCmd implements ApiCommand {
    private Long tagEntryId;

    private String resourceCategory;
    private String tagKeyName;

    private String description;

    private Boolean requiredWhenCreating = false;
    private Boolean requiredWhenFiltering = false;

    private Boolean userGroupTaggable = false;

}
