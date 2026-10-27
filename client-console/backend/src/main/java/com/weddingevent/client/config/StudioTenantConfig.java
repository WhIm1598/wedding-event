package com.weddingevent.client.config;

import com.weddingevent.common.tenant.StudioTenant;
import com.weddingevent.common.tenant.StudioTenantResolver;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The mobile API serves one marketplace across every studio (ADR-0001), so it always runs as the root tenant: no
 * studio filter. It never writes the studio-only (@TenantId) tables.
 */
@Configuration
public class StudioTenantConfig {

    @Bean
    HibernatePropertiesCustomizer studioTenantResolver() {
        return props -> props.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new StudioTenantResolver(() -> StudioTenant.ROOT));
    }
}
