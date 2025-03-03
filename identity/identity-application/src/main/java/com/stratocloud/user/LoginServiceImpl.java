package com.stratocloud.user;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.auth.AuthenticationInterceptor;
import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.StratoAuthenticationException;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.repository.TenantRepository;
import com.stratocloud.user.cmd.LoginCmd;
import com.stratocloud.user.response.LoginResponse;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    private final UserRepository repository;
    private final TenantRepository tenantRepository;
    private final UserSessionFactory userSessionFactory;
    private final CacheService cacheService;
    private final int maxLoginAttempts;

    public LoginServiceImpl(UserRepository repository,
                            TenantRepository tenantRepository,
                            UserSessionFactory userSessionFactory,
                            CacheService cacheService,
                            @Value("${strato.login.maxAttempts:5}") int maxLoginAttempts) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.userSessionFactory = userSessionFactory;
        this.cacheService = cacheService;
        this.maxLoginAttempts = maxLoginAttempts;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public LoginResponse login(LoginCmd cmd) {
        String loginName = cmd.getLoginName();
        String password = cmd.getPassword();

        User user = repository.findByLoginName(loginName).orElseThrow(
                ()->new StratoAuthenticationException("用户名或密码不正确")
        );

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        if(user.getLocked())
            throw new StratoAuthenticationException("账号已被锁定");

        if(user.getDisabled())
            throw new StratoAuthenticationException("账号已被禁用");

        Tenant tenant = tenantRepository.findTenant(user.getTenantId());

        if(tenant.getDisabled())
            throw new StratoAuthenticationException("租户已被禁用");

        try {
            UserAuthenticator authenticator = UserAuthenticatorRegistry.getAuthenticator(user.getAuthType());
            authenticator.authenticate(user, new Password(password));
            user.onLogin();
        }catch (StratoAuthenticationException e){
            log.error("Failed to authenticate user {}: {}.", user.getLoginName(), e.getMessage());
            user.onLoginFailed(maxLoginAttempts);
            throw e;
        }finally {
            CallContext.registerSystemSession();
            repository.saveWithoutTransaction(user);
            CallContext.unregister();
        }

        UserSession userSession = userSessionFactory.createUserSession(user);
        cacheService.set(userSession.token(), userSession, AuthenticationInterceptor.RENEW_MINUTES, ChronoUnit.MINUTES);

        AuditLogContext.current().setUserSession(userSession);

        return new LoginResponse(userSession);
    }
}
