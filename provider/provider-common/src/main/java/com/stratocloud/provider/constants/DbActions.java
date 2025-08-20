package com.stratocloud.provider.constants;

import com.stratocloud.resource.ResourceAction;

public class DbActions {
    public static final ResourceAction ISOLATE = new ResourceAction(
            "ISOLATE",
            "隔离",
            402
    );

    public static final ResourceAction REMOVE_ISOLATION = new ResourceAction(
            "REMOVE_ISOLATION",
            "解除隔离",
            403
    );
    public static final ResourceAction UPGRADE_VERSION = new ResourceAction(
            "UPGRADE_VERSION",
            "升级版本",
            404
    );
}
