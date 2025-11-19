package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.UserLanguageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;

@Component
public class LanguageSelectionHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LanguageSelectionHandler.class);
    
    private final UserLanguageService languageService;
    private final MessageSource messageSource;
    private final FormStateService formStateService;
    
    public LanguageSelectionHandler(UserLanguageService languageService, MessageSource messageSource, FormStateService formStateService) {
        this.languageService = languageService;
        this.messageSource = messageSource;
        this.formStateService = formStateService;
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        if (buttonId.startsWith("confirm-language-")) {
            handleConfirmLanguage(event, buttonId);
        } else if (buttonId.startsWith("change-language-")) {
            handleChangeLanguage(event, buttonId);
        } else if (buttonId.equals("continue-to-auth")) {
            handleContinueToAuth(event);
        }
    }
    
    private void handleConfirmLanguage(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        String localeTag = buttonId.replace("confirm-language-", "");
        Locale selectedLocale = Locale.forLanguageTag(localeTag);
        
        logger.info("User {} confirmed language: {}", userId, selectedLocale);
        
        languageService.saveUserLanguagePreference(userId, selectedLocale);
        
        FormState formState = formStateService.getOrCreateState(event.getUser().getIdLong());
        formState.setLocale(selectedLocale);
        logger.info("FormState locale updated to: {}", selectedLocale);
        
        String languageName = languageService.getLanguageName(selectedLocale);
        String title = messageSource.getMessage("txt_idioma_confirmado_titulo", null, selectedLocale);
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_confirmado_descricao", null, selectedLocale),
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ " + title)
            .setDescription(description)
            .setColor(0x00FF00);
        
        String continueButtonText = messageSource.getMessage("txt_continuar", null, selectedLocale);
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.primary("continue-to-auth", "▶️ " + continueButtonText)
            )
            .queue();
    }
    
    private void handleChangeLanguage(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        String localeTag = buttonId.replace("change-language-", "");
        Locale newLocale = Locale.forLanguageTag(localeTag);
        
        logger.info("User {} changed language to: {}", userId, newLocale);
        
        languageService.saveUserLanguagePreference(userId, newLocale);
        
        FormState formState = formStateService.getOrCreateState(event.getUser().getIdLong());
        formState.setLocale(newLocale);
        logger.info("FormState locale updated to: {}", newLocale);
        
        String languageName = languageService.getLanguageName(newLocale);
        String title = messageSource.getMessage("txt_idioma_alterado_titulo", null, newLocale);
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_alterado_descricao", null, newLocale),
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🔄 " + title)
            .setDescription(description)
            .setColor(0x5865F2);
        
        String continueButtonText = messageSource.getMessage("txt_continuar", null, newLocale);
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.primary("continue-to-auth", "▶️ " + continueButtonText)
            )
            .queue();
    }
    
    private void handleContinueToAuth(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        Locale userLocale = languageService.getUserLanguagePreference(userId);
        
        if (userLocale == null) {
            logger.warn("User {} has no language preference, using Spanish as default", userId);
            userLocale = languageService.getSpanishLocale();
            languageService.saveUserLanguagePreference(userId, userLocale);
        }
        
        logger.info("User {} continuing to authentication screen with locale: {}", userId, userLocale);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🚀 " + messageSource.getMessage("txt_bem_vindo_ao_squad_log_bot", null, userLocale) + "!")
            .setDescription(messageSource.getMessage("txt_para_comecar_a_usar_o_bot_vc_precisa_fazer_a_autenticacao", null, userLocale) + ".\n\n" +
                          "**" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, userLocale) + ":**\n\n" +
                          "🔐 **" + messageSource.getMessage("txt_manual", null, userLocale) + "** - " + messageSource.getMessage("txt_digite_seu_email_e_senha", null, userLocale) + "\n" +
                          "🌐 **Google** - " + messageSource.getMessage("txt_autentique_com_sua_conta_google", null, userLocale))
            .setColor(0x5865F2)
            .setFooter(messageSource.getMessage("txt_selecione_uma_opcao_abaixo", null, userLocale));
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, userLocale)),
                Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, userLocale))
            )
            .queue();
    }
}
