package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.usecase.auth.UserTokenAbstract;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.FormStateService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;


@Component
public class StatusCommand extends UserTokenAbstract implements SlashCommandHandler{
    private final GetIsUserAuthenticatedPort isUserAuthenticated;
    private final MessageSource messageSource;
    private final FormStateService formStateService;

    public StatusCommand(LoggerApiPort loggerApiPort, GetIsUserAuthenticatedPort isUserAuthenticated, MessageSource messageSource, FormStateService formStateService) {
        super(loggerApiPort);
        this.isUserAuthenticated = isUserAuthenticated;
        this.messageSource = messageSource;
        this.formStateService = formStateService;
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
        long userIdLong = event.getUser().getIdLong();
        FormState userFormState = formStateService.getOrCreateState(userIdLong);
        Locale locale = userFormState.getLocale();

        if (!isUserAuthenticated.isUserAuthenticated(userId)) {
            showUnauthenticatedStatus(event, locale);

        }else {
            showAuthenticatedStatus(event, userId, locale);
        }
    }

    private void showAuthenticatedStatus(SlashCommandInteractionEvent event, String userId, Locale locale) {
        String authMethod = getAuthMethod(userId);
        String authMethodText = "manual".equals(authMethod) ?
                messageSource.getMessage("status.auth.method.manual", null, locale) :
                messageSource.getMessage("status.auth.method.google", null, locale);

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
                        Button.secondary("status-close", "🚪 " + messageSource.getMessage("status.button.close", null, locale)),
                        Button.danger("status-logout", "🔓 " + messageSource.getMessage("status.button.logout", null, locale))
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
                        Button.primary("start-auth", "🔐 " + messageSource.getMessage("status.button.login", null, locale)),
                        Button.secondary("status-close", "🚪 " + messageSource.getMessage("status.button.close", null, locale))
                )
                .queue();
    }
}
