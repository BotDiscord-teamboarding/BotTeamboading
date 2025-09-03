package com.meli.teamboardingBot.service.command;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SlashCommandRegister {
    @Autowired
    private JDA jda;
    @Value("${discord.guild.id}")
    private String guildId;
    @PostConstruct
    public void registerCommands() {
        jda.getGuildById(guildId).updateCommands().addCommands(
                Commands.slash("squad-log", "Squad Log")
        ).queue();
    }
}