package com.meli.teamboardingBot.service.command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommandHandler {
    String getName();

    CommandData getCommandData();

    void  execute(SlashCommandInteractionEvent event);
}
