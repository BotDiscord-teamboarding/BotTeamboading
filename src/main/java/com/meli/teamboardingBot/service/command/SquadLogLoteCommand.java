package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.handler.BatchCreationHandler;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import com.meli.teamboardingBot.service.FormStateService;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class SquadLogLoteCommand implements SlashCommandHandler {
    
    private final BatchCreationHandler batchCreationHandler;
    private final DiscordUserAuthenticationService authService;
    private final PendingAuthMessageService pendingAuthMessageService;
    private final MessageSource messageSource;
    private final FormStateService formStateService;

    public SquadLogLoteCommand(BatchCreationHandler batchCreationHandler,
                               DiscordUserAuthenticationService authService,
                               PendingAuthMessageService pendingAuthMessageService,
                               MessageSource messageSource,
                               FormStateService formStateService) {
        this.batchCreationHandler = batchCreationHandler;
        this.authService = authService;
        this.pendingAuthMessageService = pendingAuthMessageService;
        this.messageSource = messageSource;
        this.formStateService = formStateService;
    }

    @Override
    public String getName() {
        return "squad-log-lote";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("squad-log-lote", "Criar múltiplos squad logs de uma vez usando texto livre");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        long userIdLong = event.getUser().getIdLong();
        FormState userFormState = formStateService.getOrCreateState(userIdLong);
        Locale locale = userFormState.getLocale();
        
        if (!authService.isUserAuthenticated(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + messageSource.getMessage("txt_autenticacao_necessaria", null, locale))
                .setDescription(messageSource.getMessage("txt_faca_login_para_usar_os_comandos", null, locale) + 
                    "\n\n💡 " + messageSource.getMessage("txt_use_comando_start_ou_clique_botao", null, locale))
                .setColor(0xFFA500);
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .addActionRow(
                    Button.primary("btn-autenticar", "🔐 " + messageSource.getMessage("txt_fazer_login", null, locale)),
                    Button.secondary("status-close", "🚪 " + messageSource.getMessage("txt_fechar", null, locale))
                )
                .queue(hook -> hook.retrieveOriginal().queue(
                    message -> pendingAuthMessageService.storePendingAuthMessage(userId, message)
                ));
            return;
        }
        
        batchCreationHandler.handleBatchCreationCommand(event);
    }
}
