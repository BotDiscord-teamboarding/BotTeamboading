package com.meli.teamboardingBot.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface SlashCommandHandler {
    String getName();

    void  execute(SlashCommandInteractionEvent event);

}
