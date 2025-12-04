package com.meli.teamboardingBot.adapters.in.listener;

import com.meli.teamboardingBot.adapters.out.command.SlashCommandHandler;
import com.meli.teamboardingBot.adapters.out.language.LanguageInterceptorService;
import com.meli.teamboardingBot.adapters.out.command.LanguageCommand;
import com.meli.teamboardingBot.adapters.out.command.LoginCommand;
import com.meli.teamboardingBot.adapters.out.command.StartCommand;
import com.meli.teamboardingBot.adapters.out.command.StatusCommand;
import com.meli.teamboardingBot.adapters.out.command.StopCommand;
import com.meli.teamboardingBot.adapters.out.command.SquadLogCommand;
import com.meli.teamboardingBot.adapters.out.command.SquadLogLoteCommand;
import com.meli.teamboardingBot.adapters.out.command.HelpCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SlashCommandListener.class);
    
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
    
    @Autowired
    private HelpCommand helpCommand;
    
    @Autowired
    private LanguageCommand languageCommand;
    
    @Autowired
    private LanguageInterceptorService languageInterceptor;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        if (commandName.equals("language")) {
            executeCommand(event);
            return;
        }
        
        if (languageInterceptor.shouldShowLanguageSelection(event)) {
            logger.info("User {} has no language preference, showing language selection for command: {}", 
                event.getUser().getId(), commandName);
            
            var handler = getCommandHandler(commandName);
            if (handler != null) {
                languageInterceptor.showLanguageSelection(event, handler);
            }
            return;
        }
        
        logger.info("User {} has language preference, executing command directly: {}", 
            event.getUser().getId(), commandName);
        
        executeCommand(event);
    }
    
    private SlashCommandHandler getCommandHandler(String commandName) {
        if (commandName.equals(squadLogCommand.getName())) {
            return squadLogCommand;
        } else if (commandName.equals(squadLogLoteCommand.getName())) {
            return squadLogLoteCommand;
        } else if (commandName.equals(loginCommand.getName())) {
            return loginCommand;
        } else if (commandName.equals(startCommand.getName())) {
            return startCommand;
        } else if (commandName.equals(stopCommand.getName())) {
            return stopCommand;
        } else if (commandName.equals(statusCommand.getName())) {
            return statusCommand;
        } else if (commandName.equals(helpCommand.getName())) {
            return helpCommand;
        } else if (commandName.equals(languageCommand.getName())) {
            return languageCommand;
        }
        return null;
    }
    
    public void executeCommand(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        var handler = getCommandHandler(commandName);
        if (handler != null) {
            handler.execute(event);
        }
    }
}