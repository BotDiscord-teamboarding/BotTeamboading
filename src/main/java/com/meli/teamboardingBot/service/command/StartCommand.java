package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements SlashCommandHandler {
    
    private final DiscordUserAuthenticationService authService;

    public StartCommand(DiscordUserAuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        // Verifica se já está autenticado
        if (authService.isUserAuthenticated(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Você já está autenticado!")
                .setDescription("Você já está autenticado no sistema.\n\n" +
                              "Use os comandos disponíveis:\n" +
                              "• `/squad-log` - Gerenciar squad logs\n" +
                              "• `/squad-log-lote` - Criar squad logs em lote")
                .setColor(0x00FF00);
            
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // Mostra opções de autenticação
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🚀 Bem-vindo ao Squad Log Bot!")
            .setDescription("Para começar a usar o bot, você precisa fazer a autenticação.\n\n" +
                          "**Escolha o método de autenticação:**\n\n" +
                          "🔐 **Manual** - Digite seu e-mail e senha\n" +
                          "🌐 **Google** - Autentique com sua conta Google")
            .setColor(0x5865F2)
            .setFooter("Selecione uma opção abaixo");
        
        event.replyEmbeds(embed.build())
            .setActionRow(
                Button.primary("auth-manual", "🔐 Manual"),
                Button.success("auth-google", "🌐 Google")
            )
            .setEphemeral(true)
            .queue();
    }
}
