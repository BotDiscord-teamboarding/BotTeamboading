package com.meli.teamboardingBot.service.command;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import com.meli.teamboardingBot.service.FormStateService;
import java.util.Locale;
import com.meli.teamboardingBot.adapters.out.ui.Ui;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
@Component
public class SquadLogCommand implements SlashCommandHandler {
    private final GetIsUserAuthenticatedPort isUserAuthenticated;
    private final PendingAuthMessageService pendingAuthMessageService;
    private final MessageSource messageSource;
    private final FormStateService formStateService;

    public SquadLogCommand(GetIsUserAuthenticatedPort isUserAuthenticated,
                          PendingAuthMessageService pendingAuthMessageService,
                          MessageSource messageSource,
                          FormStateService formStateService) {
        this.isUserAuthenticated = isUserAuthenticated;
        this.pendingAuthMessageService = pendingAuthMessageService;
        this.messageSource = messageSource;
        this.formStateService = formStateService;
    }

    @Override
    public String getName() {
        return "squad-log";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("squad-log", "Gerenciar squad logs");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        long userIdLong = event.getUser().getIdLong();
        FormState userFormState = formStateService.getOrCreateState(userIdLong);
        Locale locale = userFormState.getLocale();
        
        if (!isUserAuthenticated.isUserAuthenticated(userId)) {
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
        
        event.deferReply(true).queue(hook ->
                hook.editOriginalEmbeds(
                        Ui.info(messageSource.getMessage("txt_escolha_uma_opcao", null, locale)).build()
                ).setActionRow(
                        Button.primary("criar", "✅ " + messageSource.getMessage("txt_criar", null, locale)),
                        Button.secondary("atualizar", "📝 " + messageSource.getMessage("txt_atualizar", null, locale)),
                        Button.danger("sair", "🚪 " + messageSource.getMessage("txt_sair", null, locale))
                ).queue()
        );
    }
}