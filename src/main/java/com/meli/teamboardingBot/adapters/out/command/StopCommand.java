package com.meli.teamboardingBot.adapters.out.command;

import com.meli.teamboardingBot.adapters.out.language.ActiveFlowMessageService;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.DeleteFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.GetFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StopCommand implements SlashCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);
    private final GetOrCreateFormStatePort getOrCreateFormStatePort;
    private final GetFormStatePort getFormStatePort;
    private final DeleteFormStatePort deleteFormStatePort;
    private final MessageSource messageSource;
    private final ActiveFlowMessageService activeFlowMessageService;

    public StopCommand(GetOrCreateFormStatePort getOrCreateFormStatePort, GetFormStatePort getFormStatePort, DeleteFormStatePort deleteFormStatePort, MessageSource messageSource, ActiveFlowMessageService activeFlowMessageService) {
        this.getOrCreateFormStatePort = getOrCreateFormStatePort;
        this.getFormStatePort = getFormStatePort;
        this.deleteFormStatePort = deleteFormStatePort;
        this.messageSource = messageSource;
        this.activeFlowMessageService = activeFlowMessageService;
    }

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
        FormState userFormState = getOrCreateFormStatePort.getOrCreateState(userId);
        java.util.Locale locale = userFormState.getLocale();
        
        logger.info("🛑 Comando /stop executado por usuário: {}", userId);
        
        var state = getFormStatePort.getState(userId);
        
        if (state == null || (!state.isCreating() && !state.isEditing())) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ " + messageSource.getMessage("txt_nenhum_fluxo_ativo", null, locale))
                .setDescription(messageSource.getMessage("txt_vc_n_esta_em_nenhum_processo_de_criacao_ou_edicao_no_momento", null, locale) + ".\n\n" +
                              messageSource.getMessage("txt_use_squad_log_para_iniciar_um_novo_fluxo", null, locale))
                .setColor(0x3498db);
            
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue(hook -> hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
            
            logger.info("ℹ️ Usuário {} não tinha fluxo ativo", userId);
            return;
        }
        
        String fluxoTipo = state.isCreating() 
            ? messageSource.getMessage("txt_criacao", null, locale) 
            : messageSource.getMessage("txt_edicao", null, locale);
        
        InteractionHook flowHook = activeFlowMessageService.getFlowHook(userId);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🛑 " + messageSource.getMessage("txt_fluxo_de", null, locale) + " " + fluxoTipo + " " + messageSource.getMessage("txt_encerrado", null, locale))
            .setDescription(messageSource.getMessage("txt_o_processo_foi_cancelado_com_sucesso", null, locale) + ".\n\n" +
                    messageSource.getMessage("txt_todos_os_dados_nao_salvos_foram_descartados", null, locale) + ".\n\n" +
                    messageSource.getMessage("txt_use_squad_log_quando_quiser_comecar_novamente", null, locale) + ".")
            .setColor(0xe74c3c)
            .setFooter(messageSource.getMessage("txt_processo_cancelado_pelo_usuario", null, locale) + " • " + messageSource.getMessage("txt_esta_mensagem_sera_excluida_automaticamente", null, locale));
        
        if (flowHook != null) {
            event.deferReply(true).queue(stopHook -> {
                stopHook.deleteOriginal().queue(
                    success -> logger.info("👁️ Resposta do /stop deletada imediatamente"),
                    error -> logger.warn("⚠️ Erro ao deletar resposta do /stop: {}", error.getMessage())
                );
            });
            
            deleteFormStatePort.removeState(userId);
            activeFlowMessageService.clearFlowHook(userId);
            try {
                flowHook.editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue(
                        success -> {
                            logger.info("✅ Mensagem do fluxo editada com sucesso para usuário {}", userId);
                            flowHook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS,
                                deleteSuccess -> logger.info("🗑️ Mensagem deletada após 10 segundos para usuário {}", userId),
                                deleteError -> logger.warn("⚠️ Erro ao deletar mensagem para usuário {}: {}", userId, deleteError.getMessage())
                            );
                        },
                        error -> logger.warn("⚠️ Erro ao editar mensagem do fluxo para usuário {}: {}", userId, error.getMessage())
                    );
            } catch (Exception e) {
                logger.error("❌ Erro ao processar edição da mensagem para usuário {}: {}", userId, e.getMessage());
            }
        } else {
            deleteFormStatePort.removeState(userId);
            activeFlowMessageService.clearFlowHook(userId);
            
            logger.warn("⚠️ Nenhum hook ativo encontrado para usuário {}, criando nova mensagem", userId);
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue(hook -> hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        
        logger.info("✅ Fluxo de {} encerrado para usuário {}", fluxoTipo, userId);
    }
}
