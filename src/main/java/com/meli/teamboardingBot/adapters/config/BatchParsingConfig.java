package com.meli.teamboardingBot.adapters.config;

import com.meli.teamboardingBot.service.batch.TextParser;
import com.meli.teamboardingBot.service.batch.impl.IntelligentTextParsingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class BatchParsingConfig {

    @Bean
    @Primary
    public TextParser intelligentTextParser() {
        return new IntelligentTextParsingService();
    }
}
