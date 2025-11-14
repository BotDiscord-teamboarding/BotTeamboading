package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;


@Component
public class StatusCommand implements SlashCommandHandler{
    private final DiscordUserAuthenticationService authService;
    private final MessageSource messageSource;

    public StatusCommand(DiscordUserAuthenticationService authService, MessageSource messageSource) {
        this.authService = authService;
        this.messageSource = messageSource;
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("status", "Verifica seu status de autenticação");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Locale locale = Locale.forLanguageTag("es-ES");

        if (!authService.isUserAuthenticated(userId)) {
            showUnauthenticatedStatus(event, locale);

        }else {
            showAuthenticatedStatus(event, userId, locale);
        }
    }

    private void showAuthenticatedStatus(SlashCommandInteractionEvent event, String userId, Locale locale) {
        String authMethod = authService.getAuthMethod(userId);
        String authMethodText = "manual".equals(authMethod) ?
                messageSource.getMessage("status.authMethod.manual", null, locale) :
                messageSource.getMessage("status.authMethod.google", null, locale);

        String userName = event.getUser().getName();
        String description = messageSource.getMessage("status.authenticated.description",
                new Object[]{userName, authMethodText}, locale);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageSource.getMessage("status.authenticated.title", null, locale))
                .setDescription(description)
                .setColor(0x00FF00);

        event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .addActionRow(
                        Button.secondary("status-close", messageSource.getMessage("Status.button.close", null, locale)),
                        Button.danger("status-logout", messageSource.getMessage("Status.button.logout", null, locale))
                )
                .queue();
    }

    private void showUnauthenticatedStatus(SlashCommandInteractionEvent event, Locale locale) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageSource.getMessage("status.unauthenticated.title", null, locale))
                .setDescription(messageSource.getMessage("status.unauthenticated.description", null, locale))
                .setColor(0xFF0000);

        event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .addActionRow(
                        Button.primary("start-auth", messageSource.getMessage("status.button.login", null, locale)),
                        Button.secondary("status-close", messageSource.getMessage("status.button.close", null, locale))
                )
                .queue();
        }

    }
