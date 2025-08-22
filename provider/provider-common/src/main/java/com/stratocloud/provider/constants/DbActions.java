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
    public static final ResourceAction MODIFY_TIME_WINDOW = new ResourceAction(
            "MODIFY_TIME_WINDOW",
            "更新维护窗口",
            405
    );
    public static final ResourceAction MODIFY_EXPAND_STRATEGY = new ResourceAction(
            "MODIFY_EXPAND_STRATEGY",
            "弹性扩容",
            406
    );



    public static final ResourceAction OPEN_DMC = new ResourceAction(
            "OPEN_DMC",
            "DMC控制台",
            420
    );

}
