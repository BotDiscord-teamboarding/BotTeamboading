package com.meli.teamboardingBot.adapters.out.command;

import com.meli.teamboardingBot.core.domain.FormState;

import java.util.Locale;

import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements SlashCommandHandler {

    private final MessageSource messageSource;
    private final GetOrCreateFormStatePort getOrCreateState;

    public HelpCommand(MessageSource messageSource, GetOrCreateFormStatePort getOrCreateState) {
        this.messageSource = messageSource;
        this.getOrCreateState = getOrCreateState;
    }

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
        long userIdLong = event.getUser().getIdLong();
        FormState userFormState = getOrCreateState.getOrCreateState(userIdLong);
        Locale locale = userFormState.getLocale();
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📚 " + messageSource.getMessage("txt_help_titulo", null, locale))
            .setDescription(messageSource.getMessage("txt_help_descricao", null, locale))
            .setColor(0x00AE86)
            .addField(
                "📋 `/squad-log`",
                messageSource.getMessage("txt_help_squad_log_descricao", null, locale),
                false
            )
            .addField(
                "📦 `/squad-log-lote`",
                messageSource.getMessage("txt_help_squad_log_lote_descricao", null, locale),
                false
            )
            .addField(
                "🚀 `/start`",
                messageSource.getMessage("txt_help_start_descricao", null, locale),
                false
            )
            .addField(
                "📊 `/status`",
                messageSource.getMessage("txt_help_status_descricao", null, locale),
                false
            )
            .addField(
                "🛑 `/stop`",
                messageSource.getMessage("txt_help_stop_descricao", null, locale),
                false
            )
            .addField(
                "🌐 `/language`",
                messageSource.getMessage("txt_help_language_descricao", null, locale),
                false
            )
            .addField(
                "❓ `/help`",
                messageSource.getMessage("txt_help_help_descricao", null, locale),
                false
            )
            .setFooter(messageSource.getMessage("txt_help_footer", null, locale), null);

        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .addActionRow(
                Button.danger("help-close", "🚪 " + messageSource.getMessage("txt_sair", null, locale))
            )
            .queue();
    }
}
