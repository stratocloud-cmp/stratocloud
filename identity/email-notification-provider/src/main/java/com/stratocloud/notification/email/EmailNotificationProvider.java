package com.stratocloud.notification.email;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.notification.NotificationProvider;
import com.stratocloud.notification.NotificationReceiver;
import com.stratocloud.notification.NotificationWay;
import com.stratocloud.notification.NotificationWayProperties;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.user.User;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EmailNotificationProvider implements NotificationProvider {

    private final UserRepository userRepository;

    public EmailNotificationProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getId() {
        return "EMAIL";
    }

    @Override
    public String getName() {
        return "Email";
    }

    @Override
    public void sendNotification(NotificationReceiver receiver) {
        User user = userRepository.findUser(receiver.getReceiverUserId());

        if(Utils.isBlank(user.getEmailAddress()))
            throw new StratoException("用户%s未设置邮箱".formatted(user.getRealName()));

        JavaMailSender sender = getSender(receiver.getNotification().getNotificationWay());

        try {
            MimeMessage mimeMessage = sender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setTo(user.getEmailAddress());

            mimeMessageHelper.setSubject(receiver.getNotification().getPolicy().getEventType().getEventTypeName());

            mimeMessageHelper.setText(receiver.getRenderedHtmlMessage(), true);

            sender.send(mimeMessage);
        }catch (Exception e){
            throw new StratoException(e.getMessage(), e);
        }
    }

    private JavaMailSenderImpl getSender(NotificationWay notificationWay) {
        var properties = JSON.convert(
                notificationWay.getProperties(),
                EmailNotificationWayProperties.class
        );

        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

        javaMailSender.setHost(properties.getHost());
        javaMailSender.setPort(properties.getPort());
        javaMailSender.setUsername(properties.getUsername());
        javaMailSender.setPassword(properties.getPassword());
        javaMailSender.setProtocol(JavaMailSenderImpl.DEFAULT_PROTOCOL);
        javaMailSender.setDefaultEncoding(StandardCharsets.UTF_8.displayName());

        return javaMailSender;
    }

    @Override
    public Class<? extends NotificationWayProperties> getPropertiesClass() {
        return EmailNotificationWayProperties.class;
    }

    @Override
    public void validateConnection(NotificationWay way) {
        try {
            getSender(way).testConnection();
        } catch (MessagingException e) {
            throw new StratoException("邮件服务连接异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {
        if(Utils.isEmpty(properties))
            return;

        properties.remove("password");
    }
}
