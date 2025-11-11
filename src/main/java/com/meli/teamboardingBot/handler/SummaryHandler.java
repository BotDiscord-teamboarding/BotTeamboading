package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@Order(9)
public class SummaryHandler extends AbstractInteractionHandler {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    public SummaryHandler(FormStateService formStateService) {
        super(formStateService);
    }
    @Override
    public boolean canHandle(String componentId) {
        return false;
    }
    public void showCreateSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação via modal");
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_squad_log", null, state.getLocale()), messageSource.getMessage("txt_confirme_os_dados_antes_de_criar", null, formState.getLocale()) + ":");
        Button createButton = Button.success("confirmar-criacao", "✅ " + messageSource.getMessage("txt_criar", null, formState.getLocale()));
        Button editButton = Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt-editar", null, formState.getLocale()));
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(createButton, editButton))
            .queue();
    }

    public void showCreateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_que_foi_preenchido", null, formState.getLocale()), messageSource.getMessage("txt_verifique_todos_os_dados_antes_de_criar_o_log", null, formState.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ "+ messageSource.getMessage("txt_criar", null, formState.getLocale())),
                Button.secondary("editar-log", "✏️ "+ messageSource.getMessage("txt-editar", null, formState.getLocale()))
            )
            .queue();
    }
    public void showUpdateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 "+messageSource.getMessage("txt_resumo_do_questionario_selecionado", null, formState.getLocale()), messageSource.getMessage("txt_dados_atuais_do_questionario", null, formState.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "💾 r" + messageSource.getMessage("txt_salvar", null, formState.getLocale())),
                Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, formState.getLocale())),
                Button.primary("voltar-logs", "↩️ "+messageSource.getMessage("txt_voltar", null, formState.getLocale()))
            )
            .queue();
    }
    public void showUpdateSummary(StringSelectInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização via select");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 "+messageSource.getMessage("txt_resumo_do_questionario_selecionado", null, formState.getLocale()), messageSource.getMessage("txt_dados_atuais_do_questionario", null, formState.getLocale())+":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.success("criar-log", "💾 r" + messageSource.getMessage("txt_salvar", null, formState.getLocale())),
                Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, formState.getLocale())),
                Button.primary("voltar-logs", "↩️ "+messageSource.getMessage("txt_voltar", null, formState.getLocale()))
            ))
            .queue();
    }
    public void showSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo após modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_atualizado", null, formState.getLocale()), messageSource.getMessage("txt_dados_atualizados", null, formState.getLocale()) + ":");
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, formState.getLocale())),
                    Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, formState.getLocale())),
                    Button.primary("voltar-logs", "↩️ " +messageSource.getMessage("txt_voltar", null, formState.getLocale()))
                ))
                .queue();
        }
    }
    public void showSummary(StringSelectInteractionEvent event) {
        // Método vazio - considerar remoção se não for utilizado
        log.warn("Método showSummary(StringSelectInteractionEvent) chamado mas não implementado");
    }
    private EmbedBuilder buildSummaryEmbed(FormState state, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        log.info("Construindo resumo - squadName: '{}', userName: '{}', userId: '{}'", 
                   squadName, userName, state.getUserId());
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String categoryNames = (!state.getCategoryNames().isEmpty()) ? 
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String description2 = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String startDateText = state.getStartDate() != null ? 
            formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String endDateText = state.getEndDate() != null ? 
            formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 Squad"+ messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        embed.addField("👤 Pessoa"+ messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        embed.addField("📝 Tipo"+ messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        embed.addField("🏷️ Categorias"+ messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        embed.addField("📄 Descrição"+ messageSource.getMessage("txt_descricao", null, formState.getLocale()), description2, false);
        embed.addField("📅 Data de Início"+ messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDateText, false);
        embed.addField("📅 Data de Fim"+ messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) , endDateText, false);
        return embed;
    }
    @Override
    public int getPriority() {
        return 9;
    }
}
