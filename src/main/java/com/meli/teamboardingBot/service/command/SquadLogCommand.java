package com.meli.teamboardingBot.service.command;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import com.meli.teamboardingBot.ui.Ui;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
@Component
public class SquadLogCommand implements SlashCommandHandler {
    private final DiscordUserAuthenticationService authService;
    private final PendingAuthMessageService pendingAuthMessageService;

    public SquadLogCommand(DiscordUserAuthenticationService authService,
                          PendingAuthMessageService pendingAuthMessageService) {
        this.authService = authService;
        this.pendingAuthMessageService = pendingAuthMessageService;
    }
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

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
        
        if (!authService.isUserAuthenticated(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + messageSource.getMessage("txt_autenticacao_necessaria", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_faca_login_para_usar_os_comandos", null, formState.getLocale()))
                .setColor(0xFFA500);
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue(hook -> hook.retrieveOriginal().queue(
                    message -> pendingAuthMessageService.storePendingAuthMessage(userId, message)
                ));
            return;
        }
        
        event.deferReply(true).queue(hook ->
                hook.editOriginalEmbeds(
                        Ui.info("Escolha uma opção").build()
                ).setActionRow(
                        Button.success("criar", "Criar"),
                        Button.secondary("atualizar", "Atualizar")
                ).queue()
        );
    }
}