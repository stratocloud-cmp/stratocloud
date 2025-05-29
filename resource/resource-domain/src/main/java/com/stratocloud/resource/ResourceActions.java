package com.stratocloud.resource;

public class ResourceActions {
    public static final ResourceAction BUILD_RESOURCE = new ResourceAction(
            "BUILD_RESOURCE", "创建", 0
    );


    public static final ResourceAction START = new ResourceAction("START", "启动", 1);


    public static final ResourceAction STOP = new ResourceAction("STOP", "停止", 2);
    public static final ResourceAction RESTART = new ResourceAction("RESTART", "重启", 3);

    public static final ResourceAction RESIZE = new ResourceAction(
            "RESIZE", "变更配置", 3
    );

    public static final ResourceAction DESTROY_RESOURCE = new ResourceAction(
            "DESTROY_RESOURCE", "销毁", 4
    );

    public static final ResourceAction UPDATE = new ResourceAction("UPDATE", "更新", 10);

    public static final ResourceAction MODIFY_CHARGE_TYPE = new ResourceAction(
            "MODIFY_CHARGE_TYPE", "变更付费方式", 20
    );


    public static final ResourceAction READ_PRIVATE_KEY = new ResourceAction(
            "READ_PRIVATE_KEY", "查看私钥", 99
    );


    public static final ResourceAction READ_CONSOLE_URL = new ResourceAction(
            "READ_CONSOLE_URL", "打开终端", 99
    );


    public static final ResourceAction UPDATE_GUEST_MGMT_INFO = new ResourceAction(
            "UPDATE_GUEST_MGMT_INFO", "更新连接信息", 200
    );
    public static final ResourceAction READ_GUEST_MGMT_PASSWORD = new ResourceAction(
            "READ_GUEST_MGMT_PASSWORD", "查看管理密码", 201
    );


    public static final ResourceAction READ_GUEST_MGMT_PRIVATE_KEY = new ResourceAction(
            "READ_GUEST_MGMT_PRIVATE_KEY", "查看管理私钥", 202
    );

    public static final ResourceAction EXECUTE_SCRIPT = new ResourceAction(
            "EXECUTE_SCRIPT", "执行脚本", 203
    );

    public static final ResourceAction RESET_PASSWORD = new ResourceAction(
            "RESET_PASSWORD", "重置密码", 301
    );
    public static final ResourceAction ROLLBACK_TO_SNAPSHOT = new ResourceAction(
            "ROLLBACK_TO_SNAPSHOT", "恢复快照", 302
    );
}
