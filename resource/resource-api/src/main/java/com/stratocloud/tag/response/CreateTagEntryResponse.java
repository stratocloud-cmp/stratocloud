package com.stratocloud.tag.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateTagEntryResponse extends ApiResponse {
    private Long tagEntryId;
}
