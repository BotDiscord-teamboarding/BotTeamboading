package com.meli.teamboardingBot.adapters.handler;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.*;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@Order(9)
public class SummaryHandler extends AbstractInteractionHandler {

    private final MessageSource messageSource;


    public SummaryHandler(GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, GetFormStatePort getFormStatePort, SetBatchEntriesPort setBatchEntriesPort, SetBatchCurrentIndexPort setBatchCurrentIndexPort, GetBatchEntriesPort getBatchEntriesPort, GetBatchCurrentIndexPort getBatchCurrentIndexPort, ClearBatchStatePort clearBatchStatePort, DeleteFormStatePort deleteFormStatePort, ResetFormStatePort resetFormStatePort, LoggerApiPort loggerApiPort, MessageSource messageSource) {
        super(getOrCreateFormStatePort, putFormStatePort, getFormStatePort, setBatchEntriesPort, setBatchCurrentIndexPort, getBatchEntriesPort, getBatchCurrentIndexPort, clearBatchStatePort, deleteFormStatePort, resetFormStatePort, loggerApiPort);
        this.messageSource = messageSource;
    }
    @Override
    public boolean canHandle(String componentId) {
        return false;
    }
    public void showCreateSummary(ModalInteractionEvent event, FormState state) {
           loggerApiPort.info("Mostrando resumo de criação via modal");
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_squad_log", null, state.getLocale()), messageSource.getMessage("txt_confirme_os_dados_antes_de_criar", null, state.getLocale()) + ":");
        Button createButton = Button.success("confirmar-criacao", "✅ " + messageSource.getMessage("txt_criar", null, state.getLocale()));
        Button editButton = Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt-editar", null, state.getLocale()));
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(createButton, editButton))
            .queue();
    }

    public void showCreateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
           loggerApiPort.info("Mostrando resumo de criação");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_que_foi_preenchido", null, state.getLocale()), messageSource.getMessage("txt_verifique_todos_os_dados_antes_de_criar_o_log", null, state.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ "+ messageSource.getMessage("txt_criar", null, state.getLocale())),
                Button.secondary("editar-log", "✏️ "+ messageSource.getMessage("txt-editar", null, state.getLocale()))
            )
            .queue();
    }
    public void showUpdateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
           loggerApiPort.info("Mostrando resumo de atualização");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 "+messageSource.getMessage("txt_resumo_do_questionario_selecionado", null, state.getLocale()), messageSource.getMessage("txt_dados_atuais_do_questionario", null, state.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, state.getLocale())),
                Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, state.getLocale())),
                Button.primary("voltar-logs", "↩️ " + messageSource.getMessage("txt_voltar", null, state.getLocale()))
            )
            .queue();
    }
    public void showUpdateSummary(StringSelectInteractionEvent event, FormState state) {
           loggerApiPort.info("Mostrando resumo de atualização via select");
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_questionario_selecionado", null, state.getLocale()), messageSource.getMessage("txt_dados_atuais_do_questionario", null, state.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, state.getLocale())),
                Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, state.getLocale())),
                Button.primary("voltar-logs", "↩️ " + messageSource.getMessage("txt_voltar", null, state.getLocale()))
            ))
            .queue();
    }
    public void showSummary(ModalInteractionEvent event, FormState state) {
           loggerApiPort.info("Mostrando resumo após modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_atualizado", null, state.getLocale()), messageSource.getMessage("txt_dados_atualizados", null, state.getLocale()) + ":");
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, state.getLocale())),
                    Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, state.getLocale())),
                    Button.primary("voltar-logs", "↩️ " + messageSource.getMessage("txt_voltar", null, state.getLocale()))
                ))
                .queue();
        }
    }
    public void showSummary(StringSelectInteractionEvent event) {
           loggerApiPort.warn("Método showSummary(StringSelectInteractionEvent) chamado mas não implementado");
    }
    private EmbedBuilder buildSummaryEmbed(FormState state, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
           loggerApiPort.info("Construindo resumo - squadName: '{}', userName: '{}', userId: '{}'", 
                   squadName, userName, state.getUserId());
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        String categoryNames = (!state.getCategoryNames().isEmpty()) ? 
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        String description2 = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        String startDateText = state.getStartDate() != null ? 
            formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        String endDateText = state.getEndDate() != null ? 
            formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, state.getLocale() );
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, state.getLocale()), squadName, false);
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, state.getLocale()), userName, false);
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, state.getLocale()), typeName, false);
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, state.getLocale()), categoryNames, false);
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, state.getLocale()), description2, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_inicio", null, state.getLocale()), startDateText, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, state.getLocale()) , endDateText, false);
        return embed;
    }
    @Override
    public int getPriority() {
        return 9;
    }
}
