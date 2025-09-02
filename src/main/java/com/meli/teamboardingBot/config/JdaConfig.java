package com.meli.teamboardingBot.config;

import com.meli.teamboardingBot.discord.listener.RefactoredComponentInteractionListener;
import com.meli.teamboardingBot.discord.listener.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdaConfig {
    @Bean
    public JDA jda(@Value("${discord.token}") String token,
                   RefactoredComponentInteractionListener refactoredComponentInteractionListener,
                   SlashCommandListener slashCommandListener) throws Exception {

        return JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(refactoredComponentInteractionListener, slashCommandListener)
                .build()
                .awaitReady();
    }
}