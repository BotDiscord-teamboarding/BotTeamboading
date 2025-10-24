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
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);
        event.editMessageEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-pessoa", "👤 Pessoa"),
                    Button.secondary("edit-tipo", "📝 Tipo"),
                    Button.secondary("edit-categorias", "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary("edit-descricao", "📄 Descrição"),
                    Button.secondary("edit-datas", "📅 Datas"),
                    Button.primary("voltar-resumo", "↩️ Voltar ao resumo")
                )
            )
            .queue();
    }
    private void showSquadSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook) {
    }
    private void showLogSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, String userId) {
        // Delegated to LogSelectionHandler
    }
    private void exitBot(net.dv8tion.jda.api.interactions.InteractionHook hook, Long userId) {
        formStateService.removeState(userId);
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("👋 Até logo!")
            .setDescription("Obrigado por usar o Squad Log Bot. Use /squad-log quando quiser voltar!")
            .setColor(0x0099FF);
        hook.editOriginalEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    private void showCreateSummary(ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do que foi preenchido", "Verifique todos os dados antes de criar o log:");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("editar-log", "✏️ Editar")
            )
            .queue();
    }
    private void showUpdateSummary(ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do Questionário Selecionado", "Dados atuais do questionário:");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "💾 Salvar"),
                Button.secondary("editar-log", "✏️ Alterar"),
                Button.primary("voltar-logs", "↩️ Voltar")
            )
            .queue();
    }
    private EmbedBuilder buildSummaryEmbed(FormState state, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : "Não informado";
        String userName = state.getUserName() != null ? state.getUserName() : "Não informado";
        String typeName = state.getTypeName() != null ? state.getTypeName() : "Não informado";
        String categoryNames = (!state.getCategoryNames().isEmpty()) ? 
            String.join(", ", state.getCategoryNames()) : "Não informado";
        String description2 = state.getDescription() != null ? state.getDescription() : "Não informado";
        String startDateText = state.getStartDate() != null ? 
            formatToBrazilianDate(state.getStartDate()) : "Não informado";
        String endDateText = state.getEndDate() != null ? 
            formatToBrazilianDate(state.getEndDate()) : "Não informada";
        embed.addField("🏢 Squad", squadName, false);
        embed.addField("👤 Pessoa", userName, false);
        embed.addField("📝 Tipo", typeName, false);
        embed.addField("🏷️ Categorias", categoryNames, false);
        embed.addField("📄 Descrição", description2, false);
        embed.addField("📅 Data de Início", startDateText, false);
        embed.addField("📅 Data de Fim", endDateText, false);
        return embed;
    }
    @Override
    public int getPriority() {
        return 7;
    }
}
