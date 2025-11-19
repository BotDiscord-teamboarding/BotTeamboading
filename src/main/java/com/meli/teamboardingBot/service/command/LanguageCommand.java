package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.service.UserLanguageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;

@Component
public class LanguageCommand implements SlashCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LanguageCommand.class);
    
    private final UserLanguageService languageService;
    private final MessageSource messageSource;
    
    public LanguageCommand(UserLanguageService languageService, MessageSource messageSource) {
        this.languageService = languageService;
        this.messageSource = messageSource;
    }
    
    @Override
    public String getName() {
        return "language";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("language", "Alterar idioma do bot / Cambiar idioma del bot");
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        try {
            Locale currentLocale = languageService.hasLanguagePreference(userId)
                ? languageService.getUserLanguagePreference(userId)
                : languageService.detectUserLocale(event.getUserLocale().getLocale());
            
            String currentLanguageName = languageService.getLanguageName(currentLocale);
            Locale alternativeLocale = languageService.getAlternativeLocale(currentLocale);
            String alternativeLanguageName = languageService.getLanguageName(alternativeLocale);
            
            logger.info("User {} opened language settings. Current: {}", userId, currentLocale);
            
            String title = messageSource.getMessage("txt_configuracao_idioma_titulo", null, currentLocale);
            String description = MessageFormat.format(
                messageSource.getMessage("txt_configuracao_idioma_descricao", null, currentLocale),
                currentLanguageName
            );
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌐 " + title)
                .setDescription(description)
                .setColor(0x5865F2)
                .setFooter("Idioma atual: " + currentLanguageName + " • Current language: " + currentLanguageName);
            
            String changeButtonText = MessageFormat.format(
                messageSource.getMessage("txt_mudar_para", null, currentLocale),
                alternativeLanguageName
            );
            
            event.replyEmbeds(embed.build())
                .addActionRow(
                    Button.primary("change-language-" + alternativeLocale.toLanguageTag(), "🔄 " + changeButtonText)
                )
                .setEphemeral(true)
                .queue();
                
        } catch (Exception e) {
            logger.error("Error in language command for user {}: {}", userId, e.getMessage(), e);
            
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("⚠️ Error / Erro")
                .setDescription("Ocorreu um erro ao processar sua solicitação.\nAn error occurred while processing your request.")
                .setColor(0xFF0000);
            
            event.replyEmbeds(errorEmbed.build())
                .setEphemeral(true)
                .queue();
        }
    }
}
