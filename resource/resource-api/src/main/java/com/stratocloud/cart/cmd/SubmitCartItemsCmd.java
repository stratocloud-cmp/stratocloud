package com.stratocloud.cart.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class SubmitCartItemsCmd implements ApiCommand {
    private List<Long> cartItemIds;
}
