package com.meli.teamboardingBot.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class SquadLogCommand implements SlashCommandHandler {
    @Override
    public String getName() {
        return "squad-log";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Escolha uma opção:")
                .addActionRow(
                        Button.primary("criar", "Criar"),
                        Button.secondary("atualizar", "Atualizar")
                )
                .setEphemeral(true)
                .queue();
    }
}