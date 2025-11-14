package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@Order(7)
public class NavigationHandler extends AbstractInteractionHandler {
    public NavigationHandler(FormStateService formStateService) {
        super(formStateService);
    }
    @Override
    public boolean canHandle(String componentId) {
        return "atualizar".equals(componentId) ||
               "editar-log".equals(componentId) ||
               "voltar-logs".equals(componentId) ||
               "criar-novo".equals(componentId) ||
               "atualizar-existente".equals(componentId) ||
               "sair".equals(componentId) ||
               "voltar-resumo".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        switch (buttonId) {
            case "atualizar" -> handleUpdateButton(event, state);
            case "editar-log" -> handleEditButton(event, state);
            case "voltar-logs" -> handleBackToLogsButton(event, state);
            case "criar-novo" -> handleCreateNewButton(event, state);
            case "atualizar-existente" -> handleUpdateExistingButton(event, state);
            case "sair" -> handleExitButton(event, state);
            case "voltar-resumo" -> handleBackToSummaryButton(event, state);
        }
    }
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    private void handleUpdateButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando fluxo de atualização");
        state.setCreating(false);
        state.setEditing(true);
        state.setStep(FormStep.LOG_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showLogSelection(event);
    }
    private void handleEditButton(ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando menu de edição");
        state.setEditing(true);
        state.setStep(FormStep.EDIT_MENU);
        updateFormState(event.getUser().getIdLong(), state);
        showEditFieldsMenu(event);
    }
    private void handleBackToLogsButton(ButtonInteractionEvent event, FormState state) {
        log.info("Voltando para seleção de logs");
        state.setStep(FormStep.LOG_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showLogSelection(event);
    }
    private void handleCreateNewButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando novo fluxo de criação");
        formStateService.resetState(event.getUser().getIdLong());
        FormState newState = formStateService.getOrCreateState(event.getUser().getIdLong());
        newState.setCreating(true);
        newState.setEditing(false);
        newState.setStep(FormStep.SQUAD_SELECTION);
        updateFormState(event.getUser().getIdLong(), newState);
        event.deferReply(true).queue();
        showSquadSelectionWithHook(event.getHook());
    }
    private void handleUpdateExistingButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando fluxo de atualização");
        state.setCreating(false);
        state.setEditing(true);
        state.setStep(FormStep.LOG_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferReply(true).queue();
        showLogSelectionWithHook(event.getHook(), event.getUser().getId());
    }
    private void handleExitButton(ButtonInteractionEvent event, FormState state) {
        log.info("Usuário saindo do bot");
        event.deferReply(true).queue();
        exitBot(event.getHook(), event.getUser().getIdLong());
    }
    private void handleBackToSummaryButton(ButtonInteractionEvent event, FormState state) {
        log.info("Voltando ao resumo - isEditing={}, isCreating={}", state.isEditing(), state.isCreating());
        if (state.isEditing() && !state.isCreating()) {
            log.info("Voltando ao resumo de atualização");
            showUpdateSummary(event, state);
        } else if (state.isCreating()) {
            log.info("Voltando ao resumo de criação");
            showCreateSummary(event, state);
        } else {
            log.info("Voltando ao resumo de atualização (padrão)");
            showUpdateSummary(event, state);
        }
    }
    private void showLogSelection(ButtonInteractionEvent event) {
    }
    private void showEditFieldsMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ " + messageSource.getMessage("txt_selecione_o_campo_para_editar", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_escolha_qual_campo_voce_deseja_modificar", null, formState.getLocale()) + ":")
            .setColor(0x0099FF);
        event.editMessageEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale())),
                    Button.secondary("edit-pessoa", "👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                    Button.secondary("edit-tipo", "📝 "+ messageSource.getMessage("txt_tipo", null, formState.getLocale())),
                    Button.secondary("edit-categorias", "🏷️ "+ messageSource.getMessage("txt_categorias", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.secondary("edit-descricao", "📄 "+ messageSource.getMessage("txt_descricao", null, formState.getLocale())),
                    Button.secondary("edit-datas", "📅 " +  messageSource.getMessage("txt_datas", null, formState.getLocale())),
                    Button.secondary("voltar-resumo", "↩️ "+ messageSource.getMessage("txt_voltar", null, formState.getLocale()))
                )
            )
            .queue();
    }
    private void showSquadSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook) {
    }
    private void showLogSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, String userId) {
    }
    private void exitBot(net.dv8tion.jda.api.interactions.InteractionHook hook, Long userId) {
        formStateService.removeState(userId);
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("👋 " + messageSource.getMessage("txt_ate_logo", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_obrigado_por_usar_o_squad_log_bot", null, formState.getLocale()))
            .setColor(0x0099FF);
        hook.editOriginalEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    private void showCreateSummary(ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 " + messageSource.getMessage("txt_resumo_do_que_foi_preenchido", null, formState.getLocale()), messageSource.getMessage("txt_verifique_todos_os_dados_antes_de_criar_o_log", null, formState.getLocale()) + ":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ "+ messageSource.getMessage("txt_criar_log", null, formState.getLocale())),
                Button.secondary("editar-log", "✏️ "+ messageSource.getMessage("txt_editar", null, formState.getLocale()))
            )
            .queue();
    }
    private void showUpdateSummary(ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 "+messageSource.getMessage("txt_resumo_do_questionario_selecionado", null, formState.getLocale()), messageSource.getMessage("txt_dados_atuais_do_questionario", null, formState.getLocale()) +":");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "💾 "+ messageSource.getMessage("txt_salvar", null, formState.getLocale())),
                Button.secondary("editar-log", "✏️ "+ messageSource.getMessage("txt_alterar", null, formState.getLocale())),
                Button.primary("voltar-logs", "↩️ "+ messageSource.getMessage("txt_voltar", null, formState.getLocale()))
            )
            .queue();
    }
    private EmbedBuilder buildSummaryEmbed(FormState state, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String categoryNames = (!state.getCategoryNames().isEmpty()) ? 
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String description2 = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String startDateText = state.getStartDate() != null ? 
            formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String endDateText = state.getEndDate() != null ? 
            formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        embed.addField("👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        embed.addField("📝 "+ messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        embed.addField("🏷️ "+ messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        embed.addField("📄 "+ messageSource.getMessage("txt_descricao", null, formState.getLocale()), description2, false);
        embed.addField("📅 "+ messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDateText, false);
        embed.addField("📅 "+ messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()), endDateText, false);
        return embed;
    }
    @Override
    public int getPriority() {
        return 7;
    }
}
