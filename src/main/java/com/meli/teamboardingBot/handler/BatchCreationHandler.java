package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.model.batch.BatchLogEntry;
import com.meli.teamboardingBot.model.batch.BatchParsingResult;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import com.meli.teamboardingBot.service.batch.BatchValidator;
import com.meli.teamboardingBot.service.batch.PreviewNavigator;
import com.meli.teamboardingBot.service.batch.TextParser;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.json.JSONObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(10)
public class BatchCreationHandler extends AbstractInteractionHandler {
    
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    private final TextParser intelligentTextParser;
    private final BatchValidator batchValidator;
    private final PreviewNavigator previewNavigator;
    private final SquadLogService squadLogService;

    public BatchCreationHandler(FormStateService formStateService, 
                               TextParser intelligentTextParser,
                               BatchValidator batchValidator, 
                               PreviewNavigator previewNavigator,
                               SquadLogService squadLogService) {
        super(formStateService);
        this.intelligentTextParser = intelligentTextParser;
        this.batchValidator = batchValidator;
        this.previewNavigator = previewNavigator;
        this.squadLogService = squadLogService;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean canHandle(String componentId) {
        return componentId.startsWith("batch-") || componentId.equals("batch-creation-modal") ||
               componentId.equals("batch-edit-entry") || componentId.equals("batch-edit-modal") ||
               componentId.equals("batch-edit-squad") || componentId.equals("batch-edit-person") ||
               componentId.equals("batch-edit-type") || componentId.equals("batch-edit-categories") ||
               componentId.equals("batch-edit-description") || componentId.equals("batch-edit-dates") ||
               componentId.equals("batch-back-to-preview") ||
               componentId.equals("batch-edit-squad-modal") || componentId.equals("batch-edit-person-modal") ||
               componentId.equals("batch-edit-type-modal") || componentId.equals("batch-edit-categories-modal") ||
               componentId.equals("batch-edit-description-modal") || componentId.equals("batch-edit-dates-modal");
    }

    public void handleBatchCreationCommand(SlashCommandInteractionEvent event) {
        log.info("Iniciando comando /squad-log-lote para usuário: {}", event.getUser().getId());
        
        TextInput textInput = TextInput.create("batch-text", "Digite os squad logs", TextInputStyle.PARAGRAPH)
                .setPlaceholder("squad - pessoa - tipo - categoria - data (ex: nati - rafael - issue - tech - 10-09-2025)")
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 Criar Squad Logs em Lote")
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleBatchCreationModal(ModalInteractionEvent event) {
        log.info("Processando modal de criação em lote");
        
        event.deferReply(true).queue();
        
        String inputText = event.getValue("batch-text").getAsString();
        
        if (!intelligentTextParser.canParse(inputText)) {
            showParsingError(event);
            return;
        }

        List<BatchLogEntry> parsedEntries = intelligentTextParser.parseText(inputText);
        
        if (parsedEntries.isEmpty()) {
            showNoEntriesError(event);
            return;
        }

        log.info("Parsed entries before validation:");
        for (int i = 0; i < parsedEntries.size(); i++) {
            BatchLogEntry entry = parsedEntries.get(i);
            log.info("Entry {}: Squad='{}', Person='{}', Type='{}'", 
                i, entry.getSquadName(), entry.getPersonName(), entry.getLogType());
        }
        
        BatchParsingResult validationResult;
        try {
            log.info("Iniciando validação com API...");
            validationResult = batchValidator.validateEntries(parsedEntries);
            log.info("Validação com API concluída");
        } catch (RuntimeException e) {
            log.error("Erro durante validação com API: {}", e.getMessage());
            if (e.getMessage().contains("Timeout") || e.getMessage().contains("timeout")) {
                showApiTimeoutError(event);
            } else {
                showApiConnectionError(event, e.getMessage());
            }
            return;
        }
        
        if (!validationResult.hasValidEntries()) {
            showValidationErrors(event, validationResult);
            return;
        }
        
        log.info("Valid entries after validation:");
        List<BatchLogEntry> validEntries = validationResult.getValidEntries();
        for (int i = 0; i < validEntries.size(); i++) {
            BatchLogEntry entry = validEntries.get(i);
            log.info("Valid Entry {}: Squad='{}', Person='{}', Type='{}'", 
                i, entry.getSquadName(), entry.getPersonName(), entry.getLogType());
        }

        initializeBatchState(event.getUser().getId(), validEntries);
        showFirstPreview(event, validationResult);
    }

    public void handleBatchNavigation(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        String buttonId = event.getComponentId();
        
        if (buttonId.equals("batch-create-more")) {
            showCreateMoreModal(event);
            return;
        }
        
        if (buttonId.equals("batch-edit-bulk") || buttonId.equals("batch-exit") || 
            buttonId.equals("batch-back-to-menu") || buttonId.equals("batch-conclude")) {
            
            event.deferEdit().queue();
            
            switch (buttonId) {
                case "batch-edit-bulk":
                    showBulkEditNotReady(event);
                    return;
                case "batch-exit":
                    showExitMessage(event);
                    return;
                case "batch-back-to-menu":
                    showPostCreationMenu(event);
                    return;
                case "batch-conclude":
                    showPostCreationMenu(event);
                    return;
            }
        }
        
        List<BatchLogEntry> entries = getBatchEntries(userId);
        int currentIndex = getCurrentIndex(userId);
        
        if (entries == null || entries.isEmpty()) {
            event.deferEdit().queue();
            showSessionExpiredError(event);
            return;
        }

        if (buttonId.equals("batch-edit-squad") || buttonId.equals("batch-edit-person") ||
            buttonId.equals("batch-edit-type") || buttonId.equals("batch-edit-categories") ||
            buttonId.equals("batch-edit-description") || buttonId.equals("batch-edit-dates") ||
            buttonId.equals("batch-edit-entry")) {
            
            switch (buttonId) {
                case "batch-edit-squad":
                    showSquadEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-person":
                    showPersonEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-type":
                    showTypeEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-categories":
                    showCategoriesEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-description":
                    showDescriptionEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-dates":
                    showDatesEditModal(event, entries, currentIndex);
                    return;
                case "batch-edit-entry":
                    showEditEntryModal(event, entries, currentIndex);
                    return;
            }
        }
        
        event.deferEdit().queue();

        switch (buttonId) {
            case "batch-previous":
                currentIndex = previewNavigator.getPreviousIndex(currentIndex);
                setCurrentIndex(userId, currentIndex);
                updatePreview(event, entries, currentIndex);
                break;
                
            case "batch-next":
                currentIndex = previewNavigator.getNextIndex(currentIndex, entries.size());
                setCurrentIndex(userId, currentIndex);
                updatePreview(event, entries, currentIndex);
                break;
                
            case "batch-create-all":
                createAllLogs(event, entries);
                break;
                
            case "batch-cancel":
                cancelBatchCreation(event, userId);
                break;
                
            case "batch-back-to-preview":
                updatePreview(event, entries, currentIndex);
                break;
        }
    }

    private void showParsingError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Erro no Formato")
                .setDescription("Não foi possível interpretar o texto fornecido.\n\n" +
                               "**Formato esperado:**\n" +
                               "`Squad - Pessoa - Tipo - Categorias - Data início [a Data fim] [- Descrição]`\n\n" +
                               "**Exemplo:**\n" +
                               "`Squad Alpha - João Silva - Daily - Backend, Frontend - 15-01-2025 a 20-01-2025 - Revisão de código`")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showNoEntriesError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Nenhum Log Encontrado")
                .setDescription("Não foi possível extrair nenhum squad log do texto fornecido.")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showValidationErrors(ModalInteractionEvent event, BatchParsingResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Erros de Validação")
                .setDescription("Foram encontrados os seguintes erros:")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField("Erros Encontrados", errorText.toString(), false);
        embed.setFooter(String.format("Total processado: %d | Válidos: %d | Erros: %d", 
                                     result.getTotalProcessed(), 
                                     result.getValidCount(), 
                                     result.getErrorCount()));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showApiTimeoutError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ Timeout da API")
                .setDescription("A API demorou muito para responder e a operação foi cancelada.\n\n" +
                               "**Possíveis causas:**\n" +
                               "• Conectividade lenta com a API\n" +
                               "• API temporariamente indisponível\n" +
                               "• Sobrecarga no servidor\n\n" +
                               "**Tente novamente em alguns minutos.**")
                .setColor(Color.ORANGE)
                .setFooter("Timeout configurado: 15 segundos");

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showApiConnectionError(ModalInteractionEvent event, String errorMessage) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Erro de Conexão com a API")
                .setDescription("Não foi possível conectar com a API para validar os dados.\n\n" +
                               "**Erro técnico:**\n" +
                               "```" + errorMessage + "```\n\n" +
                               "**Tente novamente mais tarde ou contate o administrador.**")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showFirstPreview(ModalInteractionEvent event, BatchParsingResult result) {
        List<BatchLogEntry> entries = result.getValidEntries();
        BatchLogEntry firstEntry = entries.get(0);
        
        log.info("Creating first preview for entry: Squad='{}', Person='{}', Type='{}'", 
            firstEntry.getSquadName(), firstEntry.getPersonName(), firstEntry.getLogType());
        
        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(firstEntry, 0, entries.size());
        
        List<Button> buttons = createNavigationButtons(0, entries.size());
        
        EmbedBuilder summaryEmbed = new EmbedBuilder()
                .setTitle("✅ Logs Processados com Sucesso")
                .setDescription(String.format("**%d logs** foram extraídos e validados com sucesso!", entries.size()))
                .setColor(Color.GREEN);

        if (result.hasErrors()) {
            summaryEmbed.addField("⚠️ Avisos", 
                                String.format("%d linhas foram ignoradas devido a erros", result.getErrorCount()), 
                                false);
        }

        event.getHook().editOriginalEmbeds(summaryEmbed.build(), previewEmbed)
             .setComponents(ActionRow.of(buttons))
             .queue();
    }

    private void updatePreview(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        log.info("Updating preview for entry {}: Squad='{}', Person='{}', Type='{}'", 
            currentIndex, entry.getSquadName(), entry.getPersonName(), entry.getLogType());
        
        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(entry, currentIndex, entries.size());
        
        List<Button> buttons = createNavigationButtons(currentIndex, entries.size());
        
        event.getHook().editOriginalEmbeds(previewEmbed)
             .setComponents(ActionRow.of(buttons))
             .queue();
    }

    private List<Button> createNavigationButtons(int currentIndex, int totalCount) {
        List<Button> buttons = new ArrayList<>();
        
        Button previousButton = Button.secondary("batch-previous", "⬅️ Anterior")
                .withDisabled(!previewNavigator.hasPrevious(currentIndex));
        buttons.add(previousButton);
        
        Button nextButton = Button.secondary("batch-next", "Próximo ➡️")
                .withDisabled(!previewNavigator.hasNext(currentIndex, totalCount));
        buttons.add(nextButton);
        
        buttons.add(Button.primary("batch-edit-entry", "✏️ Editar"));
        buttons.add(Button.success("batch-create-all", "✅ Criar Todos"));
        buttons.add(Button.danger("batch-cancel", "❌ Cancelar"));
        
        return buttons;
    }

    private void createAllLogs(ButtonInteractionEvent event, List<BatchLogEntry> entries) {
        log.info("Iniciando criação em lote de {} logs", entries.size());
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏳ Criando Squad Logs...")
                .setDescription(String.format("Processando %d logs...", entries.size()))
                .setColor(Color.YELLOW);

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents()
             .queue();

        List<String> successes = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (BatchLogEntry entry : entries) {
            try {
                JSONObject payload = createSquadLogPayload(entry);
                squadLogService.createSquadLog(payload.toString());
                successes.add(String.format("✅ %s - %s", entry.getSquadName(), entry.getPersonName()));
                log.info("Log criado com sucesso: Squad {} - {}", entry.getSquadName(), entry.getPersonName());
            } catch (Exception e) {
                failures.add(String.format("❌ %s - %s: %s", entry.getSquadName(), entry.getPersonName(), e.getMessage()));
                log.error("Erro ao criar log: Squad {} - {}", entry.getSquadName(), entry.getPersonName(), e);
            }
        }

        showCreationResults(event, successes, failures);
    }

    private JSONObject createSquadLogPayload(BatchLogEntry entry) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", entry.getSquadId());
        payload.put("user_id", entry.getUserId());
        payload.put("squad_log_type_id", entry.getTypeId());
        payload.put("skill_category_ids", entry.getCategoryIds());
        payload.put("description", entry.getDescription());
        payload.put("start_date", entry.getStartDate().format(ISO_DATE_FORMAT));
        
        if (entry.getEndDate() != null) {
            payload.put("end_date", entry.getEndDate().format(ISO_DATE_FORMAT));
        }
        
        return payload;
    }

    private void showCreationResults(ButtonInteractionEvent event, List<String> successes, List<String> failures) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Resultado da Criação em Lote")
                .setColor(failures.isEmpty() ? Color.GREEN : Color.ORANGE);

        if (!successes.isEmpty()) {
            String successText = String.join("\n", successes.subList(0, Math.min(successes.size(), 10)));
            if (successes.size() > 10) {
                successText += String.format("\n... e mais %d logs criados", successes.size() - 10);
            }
            embed.addField(String.format("✅ Sucessos (%d)", successes.size()), successText, false);
        }

        if (!failures.isEmpty()) {
            String failureText = String.join("\n", failures.subList(0, Math.min(failures.size(), 5)));
            if (failures.size() > 5) {
                failureText += String.format("\n... e mais %d erros", failures.size() - 5);
            }
            embed.addField(String.format("❌ Falhas (%d)", failures.size()), failureText, false);
        }

        embed.setFooter(String.format("Total: %d | Sucessos: %d | Falhas: %d", 
                                     successes.size() + failures.size(), 
                                     successes.size(), 
                                     failures.size()));

        Button concludeButton = Button.primary("batch-conclude", "✅ Concluir");
        
        event.getHook().editOriginalEmbeds(embed.build())
             .setActionRow(concludeButton)
             .queue();
    }

    private void cancelBatchCreation(ButtonInteractionEvent event, String userId) {
        clearBatchState(userId);
        
        EmbedBuilder cancelingEmbed = new EmbedBuilder()
                .setTitle("⏳ Cancelando...")
                .setDescription("Cancelando criação em lote...")
                .setColor(Color.YELLOW);

        event.getHook().editOriginalEmbeds(cancelingEmbed.build())
             .setComponents()
             .queue();
             
        event.getHook().editOriginalEmbeds(createPostCancelMenu().build())
             .setComponents(createPostCancelButtons())
             .queueAfter(2, TimeUnit.SECONDS);
    }
    
    private EmbedBuilder createPostCancelMenu() {
        return new EmbedBuilder()
                .setTitle("🤖 O que você deseja fazer?")
                .setDescription("Escolha uma das opções abaixo:")
                .setColor(Color.BLUE);
    }
    
    private ActionRow createPostCancelButtons() {
        Button createBatchButton = Button.primary("batch-create-more", "📝 Criar Logs em Lote");
        Button editBulkButton = Button.secondary("batch-edit-bulk", "✏️ Editar Logs em Lote");
        Button exitButton = Button.danger("batch-exit", "🚪 Sair");
        
        return ActionRow.of(createBatchButton, editBulkButton, exitButton);
    }

    private void showSessionExpiredError(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ Sessão Expirada")
                .setDescription("A sessão de criação em lote expirou. Use o comando `/criar-em-lote` novamente.")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents()
             .queue();
    }

    private void initializeBatchState(String userId, List<BatchLogEntry> entries) {
        formStateService.setBatchEntries(userId, entries);
        formStateService.setBatchCurrentIndex(userId, 0);
    }

    private List<BatchLogEntry> getBatchEntries(String userId) {
        return formStateService.getBatchEntries(userId);
    }

    private int getCurrentIndex(String userId) {
        return formStateService.getBatchCurrentIndex(userId);
    }

    private void setCurrentIndex(String userId, int index) {
        formStateService.setBatchCurrentIndex(userId, index);
    }

    private void clearBatchState(String userId) {
        formStateService.clearBatchState(userId);
    }

    private void showEditEntryModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput squadInput = TextInput.create("edit-squad", "Squad", TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setRequiredRange(1, 100)
                .build();
                
        TextInput personInput = TextInput.create("edit-person", "Pessoa", TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setRequiredRange(1, 100)
                .build();
                
        TextInput typeInput = TextInput.create("edit-type", "Tipo", TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setRequiredRange(1, 50)
                .build();
                
        TextInput categoriesInput = TextInput.create("edit-categories", "Categorias", TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setRequiredRange(1, 200)
                .build();
                
        TextInput descriptionInput = TextInput.create("edit-description", "Descrição", TextInputStyle.SHORT)
                .setValue(entry.getDescription())
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-modal", "✏️ Editar Squad Log")
                .addActionRow(squadInput)
                .addActionRow(personInput)
                .addActionRow(typeInput)
                .addActionRow(categoriesInput)
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleEditEntryModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        List<BatchLogEntry> entries = getBatchEntries(userId);
        int currentIndex = getCurrentIndex(userId);
        
        if (entries == null || entries.isEmpty()) {
            showSessionExpiredError(event);
            return;
        }
        
        event.deferEdit().queue();
        
        BatchLogEntry entry = entries.get(currentIndex);
        
        String newSquad = event.getValue("edit-squad").getAsString().trim();
        String newPerson = event.getValue("edit-person").getAsString().trim();
        String newType = event.getValue("edit-type").getAsString().trim();
        String newCategoriesStr = event.getValue("edit-categories").getAsString().trim();
        String newDescription = event.getValue("edit-description").getAsString().trim();
        
        entry.setSquadName(newSquad);
        entry.setPersonName(newPerson);
        entry.setLogType(newType);
        entry.setCategories(Arrays.asList(newCategoriesStr.split(",\\s*")));
        entry.setDescription(newDescription);
        
        BatchParsingResult validationResult = batchValidator.validateEntries(Arrays.asList(entry));
        
        if (!validationResult.hasValidEntries()) {
            showEditValidationError(event, validationResult);
            return;
        }
        
        BatchLogEntry validatedEntry = validationResult.getValidEntries().get(0);
        entries.set(currentIndex, validatedEntry);
        
        updatePreviewFromModal(event, entries, currentIndex);
    }
    
    private void showEditValidationError(ModalInteractionEvent event, BatchParsingResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Erro na Edição")
                .setDescription("Foram encontrados erros nos dados editados:")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField("Erros Encontrados", errorText.toString(), false);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showSessionExpiredError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ Sessão Expirada")
                .setDescription("A sessão de criação em lote expirou. Use o comando `/criar-em-lote` novamente.")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void updatePreviewFromModal(ModalInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(entry, currentIndex, entries.size());
        
        List<Button> buttons = createNavigationButtons(currentIndex, entries.size());
        
        event.getHook().editOriginalEmbeds(previewEmbed)
             .setComponents(ActionRow.of(buttons))
             .queue();
    }

    private void showEditInstructions(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        showBatchEditSummary(event, entry, currentIndex);
    }

    private void showBatchEditSummary(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ Editar Squad Log")
                .setDescription("**Dados atuais do log:**")
                .setColor(Color.BLUE);

        embed.addField("🏢 Squad", entry.getSquadName(), false);
        embed.addField("👤 Pessoa", entry.getPersonName(), false);
        embed.addField("📝 Tipo", entry.getLogType(), false);
        embed.addField("🏷️ Categorias", String.join(", ", entry.getCategories()), false);
        embed.addField("📄 Descrição", entry.getDescription(), false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Início", startDate, false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Fim", endDate, false);

        embed.setFooter("Selecione o campo que deseja editar");

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 Squad"));
        editButtons.add(Button.secondary("batch-edit-person", "👤 Pessoa"));
        editButtons.add(Button.secondary("batch-edit-type", "📝 Tipo"));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ Categorias"));
        editButtons.add(Button.secondary("batch-edit-description", "📄 Descrição"));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 Datas"));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ Voltar"));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
             .queue();
    }

    private void showSquadEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput squadInput = TextInput.create("edit-squad", "Squad", TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setPlaceholder("Digite o nome do squad")
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-squad-modal", "🏢 Editar Squad")
                .addActionRow(squadInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showPersonEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput personInput = TextInput.create("edit-person", "Pessoa", TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setPlaceholder("Digite o nome da pessoa")
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-person-modal", "👤 Editar Pessoa")
                .addActionRow(personInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showTypeEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput typeInput = TextInput.create("edit-type", "Tipo", TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setPlaceholder("Digite o tipo do log (ex: Daily, Retrospective)")
                .setRequiredRange(1, 50)
                .build();

        Modal modal = Modal.create("batch-edit-type-modal", "📝 Editar Tipo")
                .addActionRow(typeInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showCategoriesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput categoriesInput = TextInput.create("edit-categories", "Categorias", TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setPlaceholder("Digite as categorias separadas por vírgula")
                .setRequiredRange(1, 200)
                .build();

        Modal modal = Modal.create("batch-edit-categories-modal", "🏷️ Editar Categorias")
                .addActionRow(categoriesInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDescriptionEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput descriptionInput = TextInput.create("edit-description", "Descrição", TextInputStyle.PARAGRAPH)
                .setValue(entry.getDescription())
                .setPlaceholder("Digite a descrição do log")
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-description-modal", "📄 Editar Descrição")
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDatesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        String startDateValue = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "";
        String endDateValue = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "";
        
        TextInput startDateInput = TextInput.create("edit-start-date", "Data de Início", TextInputStyle.SHORT)
                .setValue(startDateValue)
                .setPlaceholder("DD-MM-AAAA (ex: 15-01-2025)")
                .setRequiredRange(10, 10)
                .build();

        TextInput endDateInput = TextInput.create("edit-end-date", "Data de Fim", TextInputStyle.SHORT)
                .setValue(endDateValue)
                .setPlaceholder("DD-MM-AAAA (opcional)")
                .setRequired(false)
                .setRequiredRange(0, 10)
                .build();

        Modal modal = Modal.create("batch-edit-dates-modal", "📅 Editar Datas")
                .addActionRow(startDateInput)
                .addActionRow(endDateInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleFieldEditModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        String modalId = event.getModalId();
        List<BatchLogEntry> entries = getBatchEntries(userId);
        int currentIndex = getCurrentIndex(userId);
        
        if (entries == null || entries.isEmpty()) {
            showSessionExpiredError(event);
            return;
        }
        
        event.deferEdit().queue();
        
        BatchLogEntry entry = entries.get(currentIndex);
        boolean needsValidation = false;
        
        switch (modalId) {
            case "batch-edit-squad-modal":
                String newSquad = event.getValue("edit-squad").getAsString().trim();
                entry.setSquadName(newSquad);
                needsValidation = true;
                break;
                
            case "batch-edit-person-modal":
                String newPerson = event.getValue("edit-person").getAsString().trim();
                entry.setPersonName(newPerson);
                needsValidation = true;
                break;
                
            case "batch-edit-type-modal":
                String newType = event.getValue("edit-type").getAsString().trim();
                entry.setLogType(newType);
                needsValidation = true;
                break;
                
            case "batch-edit-categories-modal":
                String newCategoriesStr = event.getValue("edit-categories").getAsString().trim();
                entry.setCategories(Arrays.asList(newCategoriesStr.split(",\\s*")));
                needsValidation = true;
                break;
                
            case "batch-edit-description-modal":
                String newDescription = event.getValue("edit-description").getAsString().trim();
                entry.setDescription(newDescription);
                break;
                
            case "batch-edit-dates-modal":
                handleDatesEdit(event, entry);
                break;
        }
        
        if (needsValidation) {
            BatchParsingResult validationResult = batchValidator.validateEntries(Arrays.asList(entry));
            
            if (!validationResult.hasValidEntries()) {
                showEditValidationError(event, validationResult);
                return;
            }
            
            BatchLogEntry validatedEntry = validationResult.getValidEntries().get(0);
            entries.set(currentIndex, validatedEntry);
        }
        
        showBatchEditSummary(event, entries.get(currentIndex), currentIndex);
    }
    
    private void handleDatesEdit(ModalInteractionEvent event, BatchLogEntry entry) {
        try {
            String startDateStr = event.getValue("edit-start-date").getAsString().trim();
            if (!startDateStr.isEmpty()) {
                entry.setStartDate(java.time.LocalDate.parse(startDateStr, BRAZILIAN_DATE_FORMAT));
            }
            
            String endDateStr = event.getValue("edit-end-date").getAsString().trim();
            if (!endDateStr.isEmpty()) {
                entry.setEndDate(java.time.LocalDate.parse(endDateStr, BRAZILIAN_DATE_FORMAT));
            } else {
                entry.setEndDate(null);
            }
        } catch (Exception e) {
            log.error("Erro ao processar datas: {}", e.getMessage());
            showDateValidationError(event);
        }
    }
    
    private void showDateValidationError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Erro nas Datas")
                .setDescription("Formato de data inválido. Use o formato DD-MM-AAAA (ex: 15-01-2025)")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showBatchEditSummary(ModalInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ Editar Squad Log")
                .setDescription("**Dados atuais do log:**")
                .setColor(Color.BLUE);

        embed.addField("🏢 Squad", entry.getSquadName(), false);
        embed.addField("👤 Pessoa", entry.getPersonName(), false);
        embed.addField("📝 Tipo", entry.getLogType(), false);
        embed.addField("🏷️ Categorias", String.join(", ", entry.getCategories()), false);
        embed.addField("📄 Descrição", entry.getDescription(), false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Início", startDate, false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Fim", endDate, false);

        embed.setFooter("Selecione o campo que deseja editar");

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 Squad"));
        editButtons.add(Button.secondary("batch-edit-person", "👤 Pessoa"));
        editButtons.add(Button.secondary("batch-edit-type", "📝 Tipo"));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ Categorias"));
        editButtons.add(Button.secondary("batch-edit-description", "📄 Descrição"));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 Datas"));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ Voltar"));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
             .queue();
    }

    private void showPostCreationMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎉 Squad Logs Criados com Sucesso!")
                .setDescription("O que você gostaria de fazer agora?")
                .setColor(Color.GREEN);

        Button createMoreButton = Button.primary("batch-create-more", "📝 Criar Mais Lotes");
        Button editBulkButton = Button.secondary("batch-edit-bulk", "✏️ Editar em Lote");
        Button exitButton = Button.danger("batch-exit", "🚪 Sair");

        event.getHook().editOriginalEmbeds(embed.build())
             .setActionRow(createMoreButton, editBulkButton, exitButton)
             .queue();
    }

    private void showCreateMoreModal(ButtonInteractionEvent event) {
        clearBatchState(event.getUser().getId());
        
        TextInput textInput = TextInput.create("batch-text", "Digite os squad logs", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Squad - Pessoa - Tipo - Categorias - Data início [a Data fim] [- Descrição]")
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 Criar Squad Logs em Lote")
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue(success -> {
            event.getMessage().delete().queue(
                deleteSuccess -> log.info("Mensagem anterior deletada com sucesso"),
                deleteError -> log.warn("Não foi possível deletar mensagem anterior: {}", deleteError.getMessage())
            );
        });
    }

    private void showBulkEditNotReady(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🚧 Funcionalidade em Desenvolvimento")
                .setDescription("A funcionalidade de **Editar em Lote** ainda não está disponível.\n\n" +
                               "Esta feature permitirá editar múltiplos logs existentes de uma vez.\n" +
                               "Aguarde futuras atualizações!")
                .setColor(Color.ORANGE);

        Button backButton = Button.primary("batch-back-to-menu", "⬅️ Voltar ao Menu");

        event.getHook().editOriginalEmbeds(embed.build())
             .setActionRow(backButton)
             .queue();
    }

    private void showExitMessage(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👋 Obrigado por usar o Squad Log Bot!")
                .setDescription("Até a próxima! 🚀")
                .setColor(Color.BLUE);

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents()
             .queue();

        event.getHook().deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
        
        clearBatchState(event.getUser().getId());
    }
}
