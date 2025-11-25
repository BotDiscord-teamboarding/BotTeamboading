package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
import com.meli.teamboardingBot.service.UserLanguageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;

@Component
public class StartCommand implements SlashCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StartCommand.class);
    
    private final DiscordUserAuthenticationService authService;
    private final PendingAuthMessageService pendingAuthMessageService;
    private final UserLanguageService languageService;

    public StartCommand(DiscordUserAuthenticationService authService,
                       PendingAuthMessageService pendingAuthMessageService,
                       UserLanguageService languageService) {
        this.authService = authService;
        this.pendingAuthMessageService = pendingAuthMessageService;
        this.languageService = languageService;
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
        
        try {
            Locale userLocale = languageService.hasLanguagePreference(userId) 
                ? languageService.getUserLanguagePreference(userId)
                : languageService.detectUserLocale(event.getUserLocale().getLocale());
            
            if (authService.isUserAuthenticated(userId)) {
                logger.info("User {} is already authenticated", userId);
                showAlreadyAuthenticatedScreen(event, userLocale);
            } else if (languageService.hasLanguagePreference(userId)) {
                logger.info("User {} already has language preference: {}", userId, userLocale);
                showAuthenticationScreen(event, userId, userLocale);
            } else {
                logger.info("Detecting language for user {} from Discord locale: {}", userId, event.getUserLocale().getLocale());
                showLanguageConfirmation(event, userId, userLocale);
            }
        } catch (Exception e) {
            logger.error("Error detecting language for user {}: {}", userId, e.getMessage(), e);
            Locale fallbackLocale = languageService.getSpanishLocale();
            showLanguageError(event, fallbackLocale);
        }
    }
    
    private void showLanguageConfirmation(SlashCommandInteractionEvent event, String userId, Locale detectedLocale) {
        String languageName = languageService.getLanguageName(detectedLocale);
        Locale alternativeLocale = languageService.getAlternativeLocale(detectedLocale);
        String alternativeLanguageName = languageService.getLanguageName(alternativeLocale);
        
        String title = messageSource.getMessage("txt_idioma_detectado_titulo", null, detectedLocale);
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_detectado_descricao", null, detectedLocale),
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🌍 " + title)
            .setDescription(description)
            .setColor(0x5865F2);
        
        String continueButtonText = MessageFormat.format(
            messageSource.getMessage("txt_continuar_em", null, detectedLocale),
            languageName
        );
        String changeButtonText = MessageFormat.format(
            messageSource.getMessage("txt_mudar_para", null, detectedLocale),
            alternativeLanguageName
        );
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.success("confirm-language-" + detectedLocale.toLanguageTag(), "✅ " + continueButtonText),
                Button.primary("change-language-" + alternativeLocale.toLanguageTag(), "🔄 " + changeButtonText)
            )
            .setEphemeral(true)
            .queue();
    }
    
    private void showAlreadyAuthenticatedScreen(SlashCommandInteractionEvent event, Locale userLocale) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ " + messageSource.getMessage("txt_vc_ja_esta_autenticado", null, userLocale) + "!")
            .setDescription(messageSource.getMessage("txt_vc_ja_esta_autenticado", null, userLocale) + ".\n\n" +
                        messageSource.getMessage("txt_use_os_comandos_disponiveis", null, userLocale) + ":\n" +
                          "• `/squad-log` - " + messageSource.getMessage("txt_squad_log_gerenciar_squad_logs", null, userLocale) + "\n" +
                          "• `/squad-log-lote` - " + messageSource.getMessage("txt_squad_logs_em_log", null, userLocale) + "\n" +
                          "• `/status` - " + messageSource.getMessage("txt_verificar_seu_status_de_autenticacao", null, userLocale) + "\n" +
                          "• `/language` - " + messageSource.getMessage("txt_alterar_idioma", null, userLocale)+"\n" +
                          "• `/help` - " + messageSource.getMessage("txt_help", null, userLocale))
            .setColor(0x00FF00);
        
        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .queue();
    }
    
    private void showAuthenticationScreen(SlashCommandInteractionEvent event, String userId, Locale userLocale) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🚀 " + messageSource.getMessage("txt_bem_vindo_ao_squad_log_bot", null, userLocale) + "!")
            .setDescription(messageSource.getMessage("txt_para_comecar_a_usar_o_bot_vc_precisa_fazer_a_autenticacao", null, userLocale) + ".\n\n" +
                          "**" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, userLocale) + ":**\n\n" +
                          "🔐 **" + messageSource.getMessage("txt_manual", null, userLocale) + "** - " + messageSource.getMessage("txt_digite_seu_email_e_senha", null, userLocale) + "\n" +
                          "🌐 **Google** - " + messageSource.getMessage("txt_autentique_com_sua_conta_google", null, userLocale))
            .setColor(0x5865F2)
            .setFooter(messageSource.getMessage("txt_selecione_uma_opcao_abaixo", null, userLocale) + " • Use /language para mudar idioma");
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, userLocale)),
                Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, userLocale)),
                Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, userLocale))
            )
            .setEphemeral(true)
            .queue();
    }
    
    private void showLanguageError(SlashCommandInteractionEvent event, Locale fallbackLocale) {
        String title = messageSource.getMessage("txt_erro_detectar_idioma", null, fallbackLocale);
        String description = messageSource.getMessage("txt_erro_detectar_idioma_descricao", null, fallbackLocale);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚠️ " + title)
            .setDescription(description)
            .setColor(0xFFA500);
        
        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .queue();
    }
}
