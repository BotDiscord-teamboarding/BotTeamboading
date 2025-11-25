package com.meli.teamboardingBot.adapters.config;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
@Configuration
@ComponentScan(basePackages = {
    "com.meli.teamboardingBot.handler",
    "com.meli.teamboardingBot.service",
    "com.meli.teamboardingBot.listener"
})
public class InteractionHandlerConfiguration {
}
