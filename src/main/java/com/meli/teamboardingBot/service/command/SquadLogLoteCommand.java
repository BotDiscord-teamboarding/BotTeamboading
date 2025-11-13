package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.handler.BatchCreationHandler;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class SquadLogLoteCommand implements SlashCommandHandler {
    
    @Autowired
    private BatchCreationHandler batchCreationHandler;
    
    @Autowired
    private DiscordUserAuthenticationService authService;
    
    @Autowired
    private PendingAuthMessageService pendingAuthMessageService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Override
    public String getName() {
        return "squad-log-lote";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        if (!authService.isUserAuthenticated(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + messageSource.getMessage("txt_autenticacao_necessaria", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("Create new scratch file from selection", null, formState.getLocale()))
                .setColor(0xFFA500);
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue(hook -> hook.retrieveOriginal().queue(
                    message -> pendingAuthMessageService.storePendingAuthMessage(userId, message)
                ));
            return;
        }
        
        batchCreationHandler.handleBatchCreationCommand(event);
    }
}
