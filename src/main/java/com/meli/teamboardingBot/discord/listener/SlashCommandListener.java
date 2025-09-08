package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.discord.command.SquadLogCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandListener extends ListenerAdapter {
    @Autowired
    private SquadLogCommand squadLogCommand;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(squadLogCommand.getName())) {
            squadLogCommand.execute(event);
        }
    }
}