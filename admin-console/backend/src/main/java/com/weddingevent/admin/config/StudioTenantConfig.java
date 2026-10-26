package com.weddingevent.admin.config;

import com.weddingevent.admin.security.CurrentStudio;
import com.weddingevent.common.tenant.StudioTenantResolver;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Studio rows (@TenantId) are filtered by the signed-in user's studio; anonymous requests see none (ADR-0001). */
@Configuration
public class StudioTenantConfig {

    @Bean
    HibernatePropertiesCustomizer studioTenantResolver() {
        return props -> props.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new StudioTenantResolver(CurrentStudio::fromToken));
    }
}
