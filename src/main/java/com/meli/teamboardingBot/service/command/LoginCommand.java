package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

@Component
public class LoginCommand implements SlashCommandHandler {
    private final DiscordUserAuthenticationService authService;

    public LoginCommand(DiscordUserAuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        if (authService.isUserAuthenticated(userId)) {
            event.reply("✅ Você já está autenticado! Use o comando `/squad-log` para começar.")
                .setEphemeral(true)
                .queue();
            return;
        }

        TextInput username = TextInput.create("username", "E-mail", TextInputStyle.SHORT)
                .setPlaceholder("Digite seu e-mail")
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(100)
                .build();

        TextInput password = TextInput.create("password", "Senha", TextInputStyle.SHORT)
                .setPlaceholder("Digite sua senha")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("login-modal", "🔐 Login - Squad Log")
                .addActionRow(username)
                .addActionRow(password)
                .build();

        event.replyModal(modal).queue();
    }
}