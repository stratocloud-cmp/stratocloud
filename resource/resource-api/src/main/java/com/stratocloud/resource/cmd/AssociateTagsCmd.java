package com.stratocloud.resource.cmd;

import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class AssociateTagsCmd implements ApiCommand {
    private Long resourceId;
    private List<NestedResourceTag> tags;

    @Override
    public void validate() {
        if(Utils.isNotEmpty(tags)){
            for (NestedResourceTag tag : tags) {
                if(Utils.isBlank(tag.getTagKey()) || Utils.isBlank(tag.getTagKeyName()))
                    throw new InvalidArgumentException("标签键不能为空");
                if(Utils.isBlank(tag.getTagValue()) || Utils.isBlank(tag.getTagValueName()))
                    throw new InvalidArgumentException("标签值不能为空");
            }
        }
    }
}
