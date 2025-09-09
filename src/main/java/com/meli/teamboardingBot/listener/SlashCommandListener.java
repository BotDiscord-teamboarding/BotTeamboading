package com.meli.teamboardingBot.listener;
import com.meli.teamboardingBot.service.command.SquadLogCommand;
<<<<<<< HEAD
=======
import com.meli.teamboardingBot.service.command.SquadLogLoteCommand;
>>>>>>> origin/pre-dev
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class SlashCommandListener extends ListenerAdapter {
    @Autowired
    private SquadLogCommand squadLogCommand;
<<<<<<< HEAD
=======
    @Autowired
    private SquadLogLoteCommand squadLogLoteCommand;
>>>>>>> origin/pre-dev
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(squadLogCommand.getName())) {
            squadLogCommand.execute(event);
        } else if (event.getName().equals(squadLogLoteCommand.getName())) {
            squadLogLoteCommand.execute(event);
        }
    }
}