package com.stratocloud.provider.constants;

import com.stratocloud.resource.ResourceAction;

public class BucketActions {
    public static final ResourceAction EMPTY_BUCKET = new ResourceAction(
            "EMPTY_BUCKET", "清空存储桶", 402
    );

    public static final ResourceAction UPDATE_ACL = new ResourceAction(
            "UPDATE_BUCKET_ACL",
            "配置ACL",
            501
    );

    public static final ResourceAction UPDATE_POLICY = new ResourceAction(
            "UPDATE_BUCKET_POLICY",
            "配置访问策略",
            502
    );

    public static final ResourceAction UPDATE_LOGGING = new ResourceAction(
            "UPDATE_BUCKET_LOGGING",
            "配置日志转存",
            503
    );

    public static final ResourceAction UPDATE_LIFECYCLE = new ResourceAction(
            "UPDATE_BUCKET_LIFECYCLE",
            "配置生命周期",
            504
    );
    public static final ResourceAction UPDATE_WEBSITE = new ResourceAction(
            "UPDATE_BUCKET_WEBSITE",
            "配置静态网站",
            505
    );
    public static final ResourceAction UPDATE_CORS = new ResourceAction(
                "UPDATE_BUCKET_CORS",
                        "配置CORS",
                        506
    );
    public static final ResourceAction UPDATE_REFERER = new ResourceAction(
            "UPDATE_BUCKET_REFERER",
            "配置防盗链",
            507
    );

}
