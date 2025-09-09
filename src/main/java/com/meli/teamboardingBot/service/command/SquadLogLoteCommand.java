package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.handler.BatchCreationHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SquadLogLoteCommand implements SlashCommandHandler {
    
    @Autowired
    private BatchCreationHandler batchCreationHandler;

    @Override
    public String getName() {
        return "squad-log-lote";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        batchCreationHandler.handleBatchCreationCommand(event);
    }
}
