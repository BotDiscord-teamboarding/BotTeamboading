package com.meli.teamboardingBot.config;

import com.meli.teamboardingBot.handler.LoginModalHandler;
import com.meli.teamboardingBot.handler.StatusButtonHandler;
import com.meli.teamboardingBot.listener.ComponentInteractionListener;
import com.meli.teamboardingBot.listener.MessageInputListener;
import com.meli.teamboardingBot.listener.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdaConfig {
    @Bean
    public JDA jda(
            @Value("${discord.token}") String token,
            ComponentInteractionListener componentInteractionListener,
            SlashCommandListener slashCommandListener,
            MessageInputListener messageInputListener,
            LoginModalHandler loginModalHandler,
            StatusButtonHandler statusButtonHandler
    ) throws Exception {
        return JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.MESSAGE_CONTENT, 
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS
                )
                .addEventListeners(
                    componentInteractionListener, 
                    slashCommandListener,
                    messageInputListener,
                    loginModalHandler,
                    statusButtonHandler
                )
                .build()
                .awaitReady();
    }
}