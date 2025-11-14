package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import com.meli.teamboardingBot.config.MessageConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements SlashCommandHandler {
    
    private final DiscordUserAuthenticationService authService;
    private final PendingAuthMessageService pendingAuthMessageService;

    public StartCommand(DiscordUserAuthenticationService authService,
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
        return "start";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("start", "Iniciar e fazer autenticação no bot");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        pendingAuthMessageService.clearPendingAuthMessage(userId);
        
        if (authService.isUserAuthenticated(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_vc_ja_esta_autenticado", null, formState.getLocale()) + "!")
                .setDescription(messageSource.getMessage("txt_vc_ja_esta_autenticado", null, formState.getLocale()) + ".\n\n" +
                            messageSource.getMessage("txt_use_os_comandos_disponiveis", null, formState.getLocale()) + ":\n" +
                              "• " + messageSource.getMessage("txt_squad_log_gerenciar_squad_logs", null, formState.getLocale()) + "\n" +
                              "• " + messageSource.getMessage("txt_squad_logs_em_log", null, formState.getLocale()))
                .setColor(0x00FF00);
            
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🚀 " + messageSource.getMessage("txt_bem_vindo_ao_squad_log_bot", null, formState.getLocale()) + "!")
            .setDescription(messageSource.getMessage("txt_para_comecar_a_usar_o_bot_vc_precisa_fazer_a_autenticacao", null, formState.getLocale()) + ".\n\n" +
                          "**" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, formState.getLocale()) + ":**\n\n" +
                          "🔐 **" + messageSource.getMessage("txt_manual", null, formState.getLocale()) + "** - " + messageSource.getMessage("txt_digite_seu_email_e_senha", null, formState.getLocale()) + "\n" +
                          "🌐 **Google** - " + messageSource.getMessage("txt_autentique_com_sua_conta_google", null, formState.getLocale()))
            .setColor(0x5865F2)
            .setFooter(messageSource.getMessage("txt_selecione_uma_opcao_abaixo", null, formState.getLocale()));
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, formState.getLocale())),
                Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, formState.getLocale()))
            )
            .setEphemeral(true)
            .queue();
    }
}
