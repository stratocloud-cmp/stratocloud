package com.stratocloud.notification.email;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.notification.NotificationWayProperties;
import lombok.Data;

@Data
public class EmailNotificationWayProperties implements NotificationWayProperties {
    @InputField(label = "邮件服务器地址", regexMessage = RegexExpressions.IP_REGEX)
    private String host;
    @NumberField(label = "邮件服务端口", defaultValue = 25, min = 1, max = 65535)
    private Integer port;
    @InputField(label = "发送人邮箱")
    private String username;
    @InputField(label = "发送人密码", inputType = "password")
    private String password;
}
