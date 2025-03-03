package com.stratocloud.tag.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CreateTagEntryCmd implements ApiCommand {
    private String resourceCategory;
    private String tagKey;
    private String tagKeyName;

    private String description;

    private Boolean requiredWhenCreating = false;
    private Boolean requiredWhenFiltering = false;


    private Boolean userGroupTaggable = false;

}
