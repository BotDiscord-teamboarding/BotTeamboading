package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.service.command.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LanguageInterceptorService {
    private static final Logger logger = LoggerFactory.getLogger(LanguageInterceptorService.class);
    
    private final UserLanguageService languageService;
    private final MessageSource messageSource;
    private final Map<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();
    
    public LanguageInterceptorService(UserLanguageService languageService, MessageSource messageSource) {
        this.languageService = languageService;
        this.messageSource = messageSource;
    }
    
    public static class PendingCommand {
        public final String commandName;
        public final SlashCommandHandler handler;
        public final SlashCommandInteractionEvent event;
        
        public PendingCommand(String commandName, SlashCommandHandler handler, SlashCommandInteractionEvent event) {
            this.commandName = commandName;
            this.handler = handler;
            this.event = event;
        }
    }
    
    public boolean shouldShowLanguageSelection(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        return !languageService.hasLanguagePreference(userId);
    }
    
    public void showLanguageSelection(SlashCommandInteractionEvent event, SlashCommandHandler handler) {
        String userId = event.getUser().getId();
        String commandName = event.getName();
        
        pendingCommands.put(userId, new PendingCommand(commandName, handler, event));
        logger.info("Storing pending command for user {}: {}", userId, commandName);
        
        Locale detectedLocale = languageService.detectUserLocale(event.getUserLocale().getLocale());
        languageService.saveUserLanguagePreference(userId, detectedLocale);
        
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
        
        String continueButtonText = messageSource.getMessage("txt_continuar", null, detectedLocale);
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.success("confirm-language-" + detectedLocale.toLanguageTag(), "✅ " + continueButtonText)
            )
            .setEphemeral(true)
            .queue();
    }
    
    public void showLanguageConfirmation(SlashCommandInteractionEvent event, SlashCommandHandler handler) {
        String userId = event.getUser().getId();
        String commandName = event.getName();
        
        pendingCommands.put(userId, new PendingCommand(commandName, handler, event));
        logger.info("Storing pending command for user {}: {}", userId, commandName);
        
        Locale userLocale = languageService.getUserLanguagePreference(userId);
        String languageName = languageService.getLanguageName(userLocale);
        
        String title = messageSource.getMessage("txt_idioma_confirmado_titulo", null, userLocale);
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_salvo_descricao", null, userLocale),
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ " + title)
            .setDescription(description)
            .setColor(0x00FF00);
        
        String continueButtonText = messageSource.getMessage("txt_continuar", null, userLocale);
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.primary("execute-pending-command", "▶️ " + continueButtonText)
            )
            .setEphemeral(true)
            .queue();
    }
    
    public PendingCommand getPendingCommand(String userId) {
        return pendingCommands.get(userId);
    }
    
    public void clearPendingCommand(String userId) {
        pendingCommands.remove(userId);
        logger.info("Cleared pending command for user: {}", userId);
    }
}
