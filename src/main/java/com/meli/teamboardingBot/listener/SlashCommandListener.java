package com.meli.teamboardingBot.listener;

import com.meli.teamboardingBot.service.command.LoginCommand;
import com.meli.teamboardingBot.service.command.StartCommand;
import com.meli.teamboardingBot.service.command.StatusCommand;
import com.meli.teamboardingBot.service.command.StopCommand;
import com.meli.teamboardingBot.service.command.SquadLogCommand;
import com.meli.teamboardingBot.service.command.SquadLogLoteCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandListener extends ListenerAdapter {
    @Autowired
    private SquadLogCommand squadLogCommand;

    @Autowired
    private SquadLogLoteCommand squadLogLoteCommand;

    @Autowired
    private LoginCommand loginCommand;
    
    @Autowired
    private StartCommand startCommand;
    
    @Autowired
    private StopCommand stopCommand;
    
    @Autowired
    private StatusCommand statusCommand;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(squadLogCommand.getName())) {
            squadLogCommand.execute(event);
        } else if (event.getName().equals(squadLogLoteCommand.getName())) {
            squadLogLoteCommand.execute(event);
        } else if (event.getName().equals(loginCommand.getName())) {
            loginCommand.execute(event);
        } else if (event.getName().equals(startCommand.getName())) {
            startCommand.execute(event);
        } else if (event.getName().equals(stopCommand.getName())) {
            stopCommand.execute(event);
        } else if (event.getName().equals(statusCommand.getName())) {
            statusCommand.execute(event);
        }
    }
}