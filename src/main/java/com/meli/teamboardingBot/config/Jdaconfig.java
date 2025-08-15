package com.meli.teamboardingBot.config;


import com.meli.teamboardingBot.discord.listener.ComponentInteractionListener;
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
                       ComponentInteractionListener componentInteractionListener) throws Exception {

        return JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(/* adicionar os Listeners */)
                .build()
                .awaitReady();


    }
}
