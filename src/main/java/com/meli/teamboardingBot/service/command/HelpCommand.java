package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.model.FormState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements SlashCommandHandler {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("help", "Exibe a lista de comandos disponíveis");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📚 Comandos Disponíveis")
            .setDescription("Aqui está a lista de todos os comandos disponíveis no bot:")
            .setColor(0x00AE86)
            .addField(
                "📋 `/squad-log`",
                "Gerenciar squad logs - criar ou atualizar registros de atividades da squad",
                false
            )
            .addField(
                "📦 `/squad-log-lote`",
                "Criar múltiplos squad logs de uma vez usando texto livre",
                false
            )
            .addField(
                "🚀 `/start`",
                "Iniciar e fazer autenticação no bot",
                false
            )
            .addField(
                "📊 `/status`",
                "Verificar o status da sua autenticação",
                false
            )
            .addField(
                "🛑 `/stop`",
                "Encerrar sua sessão e fazer logout",
                false
            )
            .addField(
                "❓ `/help`",
                "Exibir esta mensagem de ajuda",
                false
            )
            .setFooter("Use os comandos acima para interagir com o bot", null);

        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .addActionRow(
                Button.danger("help-close", "🚪 Sair")
            )
            .queue();
    }
}
