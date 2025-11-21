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
        String userId = event.getUser().getId();
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📚 " + messageSource.getMessage("txt_help_titulo", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_help_descricao", null, formState.getLocale()))
            .setColor(0x00AE86)
            .addField(
                "📋 `/squad-log`",
                messageSource.getMessage("txt_help_squad_log_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "📦 `/squad-log-lote`",
                messageSource.getMessage("txt_help_squad_log_lote_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "🚀 `/start`",
                messageSource.getMessage("txt_help_start_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "📊 `/status`",
                messageSource.getMessage("txt_help_status_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "🛑 `/stop`",
                messageSource.getMessage("txt_help_stop_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "🌐 `/language`",
                messageSource.getMessage("txt_help_language_descricao", null, formState.getLocale()),
                false
            )
            .addField(
                "❓ `/help`",
                messageSource.getMessage("txt_help_help_descricao", null, formState.getLocale()),
                false
            )
            .setFooter(messageSource.getMessage("txt_help_footer", null, formState.getLocale()), null);

        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .addActionRow(
                Button.danger("help-close", "🚪 " + messageSource.getMessage("txt_sair", null, formState.getLocale()))
            )
            .queue();
    }
}
