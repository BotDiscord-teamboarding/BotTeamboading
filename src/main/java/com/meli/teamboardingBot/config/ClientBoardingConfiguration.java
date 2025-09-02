package com.meli.teamboardingBot.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.meli.teamboardingBot.service",
    "com.meli.teamboardingBot.factory",
    "com.meli.teamboardingBot.client"
})
public class ClientBoardingConfiguration {
}
