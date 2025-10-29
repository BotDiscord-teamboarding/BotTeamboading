package com.meli.teamboardingBot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(basePackages = {
    "com.meli.teamboardingBot.service",
    "com.meli.teamboardingBot.factory",
    "com.meli.teamboardingBot.client"
})
public class ClientBoardingConfiguration {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
