package com.meli.teamboardingBot.service.command;
import com.meli.teamboardingBot.model.FormState;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
@Component
public class SlashCommandRegister {
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Autowired
    private JDA jda;
    @Value("${discord.guild.id}")
    private String guildId;
    @PostConstruct
    public void registerCommands() {
        jda.getGuildById(guildId).updateCommands().addCommands(
                Commands.slash("start",  messageSource.getMessage("txt_iniciar_e_fazer_autenticacao_no_bot", null, formState.getLocale())),
                Commands.slash("squad-log", messageSource.getMessage("txt_squad_log", null, formState.getLocale())),
                Commands.slash("squad-log-lote", messageSource.getMessage("txt_criar_multiplos_squad_logs_de_uma_vez_usando_texto_livre", null, formState.getLocale())),
                Commands.slash("status", messageSource.getMessage("txt_status", null, formState.getLocale())),
                Commands.slash("language", "Alterar idioma do bot / Cambiar idioma del bot"),
                Commands.slash("stop", messageSource.getMessage("txt_stop", null, formState.getLocale())),
                Commands.slash("help", "Exibe a lista de comandos disponíveis")
        ).queue();
    }
}