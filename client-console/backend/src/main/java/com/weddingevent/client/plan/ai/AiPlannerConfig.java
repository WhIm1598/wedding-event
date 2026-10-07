package com.weddingevent.client.plan.ai;

import com.weddingevent.client.config.AppProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPlannerConfig {

    private static final Logger log = LoggerFactory.getLogger(AiPlannerConfig.class);

    @Bean
    AiPlannerClient aiPlannerClient(AppProperties props) {
        AppProperties.AiPlanner cfg = props.aiPlanner();
        if (cfg == null || cfg.baseUrl() == null || cfg.baseUrl().isBlank()) {
            log.warn("app.ai-planner.base-url is not set: using the built-in rule-based planner instead of the AI service");
            return new RuleBasedAiPlanner();
        }
        return new HttpAiPlannerClient(cfg.baseUrl(), cfg.timeout() == null ? Duration.ofSeconds(30) : cfg.timeout());
    }
}
