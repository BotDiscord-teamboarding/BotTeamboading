package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class LoginCommand implements SlashCommandHandler {
    private final DiscordUserAuthenticationService authService;

    public LoginCommand(DiscordUserAuthenticationService authService) {
        this.authService = authService;
    }

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("login", "Fazer login manual no sistema");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        if (authService.isUserAuthenticated(userId)) {
            event.reply("✅ " +  messageSource.getMessage("txt_vc_ja_esta_autenticado_use_o_comando_para_comecar", null, formState.getLocale()) + ".")
                .setEphemeral(true)
                .queue();
            return;
        }

        TextInput username = TextInput.create("username", messageSource.getMessage("txt_email", null, formState.getLocale()), TextInputStyle.SHORT)
                .setPlaceholder(messageSource.getMessage("txt_digite_seu_email", null, formState.getLocale()))
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(100)
                .build();

        TextInput password = TextInput.create("password", messageSource.getMessage("txt_senha", null, formState.getLocale()), TextInputStyle.SHORT)
                .setPlaceholder(messageSource.getMessage("txt_digite_sua_senha", null, formState.getLocale()))
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("login-modal", "🔐 " + messageSource.getMessage("txt_login_squad_log", null, formState.getLocale()))
                .addActionRow(username)
                .addActionRow(password)
                .build();

        event.replyModal(modal).queue();
    }
}