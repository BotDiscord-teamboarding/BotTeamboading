package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.config.MessageConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StopCommand implements SlashCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);
    private final FormStateService formStateService;

    public StopCommand(FormStateService formStateService) {
        this.formStateService = formStateService;
    }

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stop", "Cancelar o fluxo de criação ou edição atual");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        
        logger.info("🛑 Comando /stop executado por usuário: {}", userId);
        
        var state = formStateService.getState(userId);
        
        if (state == null || (!state.isCreating() && !state.isEditing())) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ " + messageSource.getMessage("txt_nenhum_fluxo_ativo", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_vc_n_esta_em_nenhum_processo_de_criacao_ou_edicao_no_momento", null, formState.getLocale()) + ".\n\n" +
                              messageSource.getMessage("txt_use_squad_log_para_iniciar_um_novo_fluxo", null, formState.getLocale()))
                .setColor(0x3498db);
            
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue(hook -> hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
            
            logger.info("ℹ️ Usuário {} não tinha fluxo ativo", userId);
            return;
        }
        
        String fluxoTipo = state.isCreating() 
            ? messageSource.getMessage("txt_criacao", null, formState.getLocale()) 
            : messageSource.getMessage("txt_edicao", null, formState.getLocale());
        formStateService.removeState(userId);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🛑 "  +messageSource.getMessage("txt_fluxo_de", null, formState.getLocale()) + " " + fluxoTipo + " " + messageSource.getMessage("txt_encerrado", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_o_processo_foi_cancelado_com_sucesso", null, formState.getLocale()) + ".\n\n" +
                    messageSource.getMessage("txt_todos_os_dados_nao_salvos_foram_descartados", null, formState.getLocale()) + ".\n\n" +
                    messageSource.getMessage("txt_use_squad_log_quando_quiser_comecar_novamente", null, formState.getLocale()) + ".")
            .setColor(0xe74c3c)
            .setFooter(messageSource.getMessage("txt_processo_cancelado_pelo_usuario", null, formState.getLocale()));
        
        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .queue();
        
        logger.info("✅ Fluxo de {} encerrado para usuário {}", fluxoTipo, userId);
    }
}
