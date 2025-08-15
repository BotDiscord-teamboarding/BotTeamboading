package com.meli.teamboardingBot.discord.command;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandRegister {

    private final JDA jda;
    private final String guildId;

    public SlashCommandRegister(JDA jda, @Value("${discord.guild.id}") String guildId) {
        this.jda = jda;
        this.guildId = guildId;
    }

    @PostConstruct
    public void registerCommands() {
        jda.getGuildById(guildId).updateCommands().addCommands(
                /*Commands aqui*/
        ).queue();
    }
}