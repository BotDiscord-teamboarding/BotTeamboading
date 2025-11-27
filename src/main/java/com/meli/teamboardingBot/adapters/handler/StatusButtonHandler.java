package com.meli.teamboardingBot.adapters.handler;


import com.meli.teamboardingBot.core.ports.discorduserauthentication.LogoutDiscordUserPort;
import com.meli.teamboardingBot.service.UserLanguageService;
import com.meli.teamboardingBot.service.FormStateService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StatusButtonHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(StatusButtonHandler.class);
    private final LogoutDiscordUserPort authService;
    private final UserLanguageService languageService;
    private final MessageSource messageSource;
    private final FormStateService formStateService;

    private java.util.Locale getUserLocale(long userId) {
        return formStateService.getOrCreateState(userId).getLocale();
    }

    public StatusButtonHandler(LogoutDiscordUserPort authService,
                               UserLanguageService languageService,
                               MessageSource messageSource,
                               FormStateService formStateService) {
        this.authService = authService;
        this.languageService = languageService;
        this.messageSource = messageSource;
        this.formStateService = formStateService;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        String userId = event.getUser().getId();

        try {
            if ("status-close".equals(buttonId)) {
                handleClose(event);
                return;
            }

            if ("help-close".equals(buttonId)) {
                handleClose(event);
                return;
            }

            if ("status-logout".equals(buttonId)) {
                handleLogout(event, userId);
                return;
            }
        }catch (IllegalStateException e) {
            logger.warn("Interação já foi processada ou expirou para usuário {}: {}",
                    userId, e.getMessage());
        }
    }

    private void handleClose(ButtonInteractionEvent event) {
        logger.info("Usuário {} fechou a janela de status", event.getUser().getId());
        event.deferEdit().queue(hook -> hook.deleteOriginal().queue());
    }

    private void handleLogout(ButtonInteractionEvent event, String userId) {
       logger.info("Usuário {} solicitou logout", userId);

       authService.logoutUser(userId);
       languageService.clearUserLanguagePreference(userId);
       logger.info("Language preference cleared for user {} on logout", userId);

       Locale locale = getUserLocale(event.getUser().getIdLong());

       EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageSource.getMessage("status.logout.title", null, locale))
                .setDescription(messageSource.getMessage("status.logout.description", null, locale))
                .setColor(0x00FF00);

       event.deferEdit().queue(hook -> {
           hook.editOriginalEmbeds(embed.build())
                   .setComponents()
                   .queue(message -> {
                       try {
                           Thread.sleep(3000);
                           hook.deleteOriginal().queue();
                       } catch (InterruptedException e) {
                           Thread.currentThread().interrupt();
                       }
                   });
       });
    }
}
