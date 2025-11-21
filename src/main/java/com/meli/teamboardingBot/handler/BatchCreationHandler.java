package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.model.batch.BatchLogEntry;
import com.meli.teamboardingBot.model.batch.BatchParsingResult;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import com.meli.teamboardingBot.service.batch.BatchValidator;
import com.meli.teamboardingBot.service.batch.PreviewNavigator;
import com.meli.teamboardingBot.service.batch.TextParser;
import com.meli.teamboardingBot.service.batch.impl.EmbedPreviewNavigationService;
import com.meli.teamboardingBot.config.MessageConfig;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

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
               componentId.equals("batch-back-to-preview") || componentId.equals("batch-edit-page2") ||
               componentId.equals("batch-edit-squad-modal") || componentId.equals("batch-edit-person-modal") ||
               componentId.equals("batch-edit-type-modal") || componentId.equals("batch-edit-categories-modal") ||
               componentId.equals("batch-edit-description-modal") || componentId.equals("batch-edit-dates-modal") ||
               componentId.equals("batch-edit-modal-page1") || componentId.equals("batch-edit-modal-page2");
    }

    public void handleBatchCreationCommand(SlashCommandInteractionEvent event) {
        log.info("Iniciando comando /squad-log-lote para usuário: {}", event.getUser().getId());
        
        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_categoria_data_ex", null, formState.getLocale()))
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, formState.getLocale()))
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue();
    }
    
    public void handleOpenBatchModalButton(ButtonInteractionEvent event) {
        log.info("Abrindo modal de criação em lote via botão para usuário: {}", event.getUser().getId());
        
        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_categoria_data_ex", null, formState.getLocale()))
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, formState.getLocale()))
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
            } else if (e.getMessage().contains("Credenciais") || e.getMessage().contains("autenticação") || 
                       e.getMessage().contains("autenticacao") || e.getMessage().contains("Unauthorized")) {
                showAuthenticationRequired(event);
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
        
        if (buttonId.equals("batch-exit") || 
            buttonId.equals("batch-back-to-menu") || buttonId.equals("batch-conclude")) {
            
            event.deferEdit().queue();
            
            switch (buttonId) {
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
            buttonId.equals("batch-edit-entry") || buttonId.equals("batch-edit-page2")) {
            
            switch (buttonId) {
                case "batch-edit-squad":
                    showSquadSelectionMenu(event, entries, currentIndex);
                    return;
                case "batch-edit-person":
                    showPersonSelectionMenu(event, entries, currentIndex);
                    return;
                case "batch-edit-type":
                    showTypeSelectionMenu(event, entries, currentIndex);
                    return;
                case "batch-edit-categories":
                    showCategoriesSelectionMenu(event, entries, currentIndex);
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
                case "batch-edit-page2":
                    showEditEntryModalPage2(event, entries, currentIndex);
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
                .setTitle("❌ " + messageSource.getMessage("txt_erro_no_formato", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_interpretar_o_texto_fornecido", null, formState.getLocale()) + ".\n\n **" +
                               messageSource.getMessage("txt_formato_esperado", null, formState.getLocale()) + ":**\n`" +
                                messageSource.getMessage("txt_squad_pessoa_tipo_categorias_data_inicio_data_fim_descricao", null, formState.getLocale()) +"`\n\n**"
                                + messageSource.getMessage("txt_exemplo", null, formState.getLocale()) +
                               ":**\n`" +  messageSource.getMessage("txt_squad_exemplo", null, formState.getLocale()) +"`")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showNoEntriesError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_nenhum_log_encontrado", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_extrair_nenhum_squad_log_do_texto_fornecido", null, formState.getLocale()) + ".")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showValidationErrors(ModalInteractionEvent event, BatchParsingResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erros_de_validacao", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_foram_encontrados_os_seguintes_erros", null, formState.getLocale()) +":")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField(messageSource.getMessage("txt_erros_encontrados", null, formState.getLocale()), errorText.toString(), false);
        embed.setFooter(String.format(messageSource.getMessage("txt_total_processado", null, formState.getLocale()) + ": %d | "
                        + messageSource.getMessage("txt_validos", null, formState.getLocale()) + ": %d | " +
                        messageSource.getMessage("txt_erros", null, formState.getLocale()) + ": %d",
                                     result.getTotalProcessed(), 
                                     result.getValidCount(), 
                                     result.getErrorCount()));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showApiTimeoutError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_timeout_da_api", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_api_demorou_muito_para_responder", null, formState.getLocale()) + ".\n\n**" +
                        messageSource.getMessage("txt_possiveis_causas", null, formState.getLocale()) + ":**\n" +
                        messageSource.getMessage("txt_conectividade_lenta_com_a_api", null, formState.getLocale()) + "• \n" +
                        messageSource.getMessage("txt_api_temporariamente_indisponivel", null, formState.getLocale()) + "• \n" +
                        messageSource.getMessage("txt_sobrecarga_no_servidor", null, formState.getLocale()) + "• \n\n**" +
                        messageSource.getMessage("txt_tente_novamente_em_alguns_minutos", null, formState.getLocale()) + ".**")
                .setColor(Color.ORANGE)
                .setFooter(messageSource.getMessage("txt_timeout_configurado", null, formState.getLocale()));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showAuthenticationRequired(ModalInteractionEvent event) {
        String title = messageSource.getMessage("txt_autenticacao_necessaria", null, formState.getLocale());
        String description = messageSource.getMessage("txt_faca_login_para_usar_os_comandos", null, formState.getLocale()) + 
                           "\n\n" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, formState.getLocale());
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + title)
                .setDescription(description)
                .setColor(0xFF6B6B);

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(
                    Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, formState.getLocale())),
                    Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, formState.getLocale())),
                    Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, formState.getLocale()))
                )
                .queue();
    }
    
    private void showApiConnectionError(ModalInteractionEvent event, String errorMessage) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erro_de_conexao_com_a_api", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_conectar_com_a_api_para_validar_os_dados", null, formState.getLocale()) +".\n\n**" +
                        messageSource.getMessage("txt_erro_tecnico", null, formState.getLocale()) + ":**\n" +
                               "```" + errorMessage + "```\n\n**" +
                        messageSource.getMessage("txt_tente_novamente_mais_tarde_ou_contate_o_administrador", null, formState.getLocale()) + ".**")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showFirstPreview(ModalInteractionEvent event, BatchParsingResult result) {
        List<BatchLogEntry> entries = result.getValidEntries();
        BatchLogEntry firstEntry = entries.get(0);
        
        log.info("Creating first preview for entry: Squad='{}', Person='{}', Type='{}'", 
            firstEntry.getSquadName(), firstEntry.getPersonName(), firstEntry.getLogType());
        
        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(firstEntry, 0, entries.size());
        
        List<ActionRow> actionRows = createNavigationActionRows(0, entries.size());
        
        EmbedBuilder summaryEmbed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_logs_processados_com_sucesso", null, formState.getLocale()))
                .setDescription(String.format("**%d logs** " + messageSource.getMessage("txt_logs_foi_possivel_conectar_com_a_api_para_validar_os_dados", null, formState.getLocale()) +"!", entries.size()))
                .setColor(Color.GREEN);

        if (result.hasErrors()) {
            summaryEmbed.addField("⚠️ " + messageSource.getMessage("txt_avisos", null, formState.getLocale()),
                                String.format("%d " + messageSource.getMessage("txt_linhas_foram_ignoradas_devido_a_erros", null, formState.getLocale()), result.getErrorCount()),
                                false);
        }

        event.getHook().editOriginalEmbeds(summaryEmbed.build(), previewEmbed)
             .setComponents(actionRows)
             .queue();
    }

    private void updatePreview(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        log.info("Updating preview for entry {}: Squad='{}', Person='{}', Type='{}'", 
            currentIndex, entry.getSquadName(), entry.getPersonName(), entry.getLogType());
        
        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(entry, currentIndex, entries.size());
        
        List<ActionRow> actionRows = createNavigationActionRows(currentIndex, entries.size());
        
        event.getHook().editOriginalEmbeds(previewEmbed)
             .setComponents(actionRows)
             .queue();
    }

    private List<ActionRow> createNavigationActionRows(int currentIndex, int totalCount) {
        List<ActionRow> actionRows = new ArrayList<>();
        
        Button previousButton = Button.secondary("batch-previous", "⬅️ " + messageSource.getMessage("txt_anterior", null, formState.getLocale()))
                .withDisabled(!previewNavigator.hasPrevious(currentIndex));
        Button nextButton = Button.secondary("batch-next", messageSource.getMessage("txt_proximo", null, formState.getLocale()) + " ➡️")
                .withDisabled(!previewNavigator.hasNext(currentIndex, totalCount));
        actionRows.add(ActionRow.of(previousButton, nextButton));
        
        Button squadButton = Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()));
        Button personButton = Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()));
        Button typeButton = Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()));
        actionRows.add(ActionRow.of(squadButton, personButton, typeButton));
        
        Button categoriesButton = Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()));
        Button descriptionButton = Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()));
        Button datesButton = Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale()));
        actionRows.add(ActionRow.of(categoriesButton, descriptionButton, datesButton));
        
        Button createAllButton = Button.success("batch-create-all", "✅ " + messageSource.getMessage("txt_criar_todos", null, formState.getLocale()));
        Button cancelButton = Button.danger("batch-cancel", "❌ " + messageSource.getMessage("txt_cancelar", null, formState.getLocale()));
        actionRows.add(ActionRow.of(createAllButton, cancelButton));
        
        return actionRows;
    }

    private void createAllLogs(ButtonInteractionEvent event, List<BatchLogEntry> entries) {
        log.info("Iniciando criação em lote de {} logs", entries.size());
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏳ " + messageSource.getMessage("txt_criando_squad_logs", null, formState.getLocale()) + "...")
                .setDescription(String.format(messageSource.getMessage("txt_processando", null, formState.getLocale()) + " %d logs...", entries.size()))
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
                .setTitle("📊 " + messageSource.getMessage("txt_resultado_da_criacao_em_lote", null, formState.getLocale()))
                .setColor(failures.isEmpty() ? Color.GREEN : Color.ORANGE);

        if (!successes.isEmpty()) {
            String successText = String.join("\n", successes.subList(0, Math.min(successes.size(), 10)));
            if (successes.size() > 10) {
                successText += String.format("\n... " + messageSource.getMessage("txt_e_mais", null, formState.getLocale()) +" %d logs " + messageSource.getMessage("txt_criados", null, formState.getLocale()), successes.size() - 10);
            }
            embed.addField(String.format("✅ " + messageSource.getMessage("txt_sucessos", null, formState.getLocale()) +" (%d)", successes.size()), successText, false);
        }

        if (!failures.isEmpty()) {
            String failureText = String.join("\n", failures.subList(0, Math.min(failures.size(), 5)));
            if (failures.size() > 5) {
                failureText += String.format("\n... "+ messageSource.getMessage("txt_e_mais", null, formState.getLocale()) +" %d " + messageSource.getMessage("txt_erros", null, formState.getLocale()), failures.size() - 5);
            }
            embed.addField(String.format("❌ " + messageSource.getMessage("txt_falhas", null, formState.getLocale()) + " (%d)", failures.size()), failureText, false);
        }

        embed.setFooter(String.format(messageSource.getMessage("txt_total", null, formState.getLocale()) + ": %d | "
                        + messageSource.getMessage("txt_sucessos", null, formState.getLocale()) + ": %d | "
                        + messageSource.getMessage("txt_falhas", null, formState.getLocale()) + ": %d",
                                     successes.size() + failures.size(), 
                                     successes.size(), 
                                     failures.size()));

        Button concludeButton = Button.primary("batch-conclude", "✅ " + messageSource.getMessage("txt_concluir", null, formState.getLocale()));
        
        event.getHook().editOriginalEmbeds(embed.build())
             .setActionRow(concludeButton)
             .queue();
    }

    private void cancelBatchCreation(ButtonInteractionEvent event, String userId) {
        clearBatchState(userId);
        
        EmbedBuilder cancelingEmbed = new EmbedBuilder()
                .setTitle("⏳ " + messageSource.getMessage("txt_cancelando", null, formState.getLocale()) + "...")
                .setDescription(messageSource.getMessage("txt_cancelamento_criacao_em_lote", null, formState.getLocale()) + "...")
                .setColor(Color.YELLOW);

        event.getHook().editOriginalEmbeds(cancelingEmbed.build())
             .setComponents()
             .queue();
        
        EmbedBuilder exitEmbed = new EmbedBuilder()
                .setTitle("👋 " + messageSource.getMessage("txt_obrigado_por_usar_o_squad_log_bot", null, formState.getLocale()) + "!")
                .setDescription(messageSource.getMessage("txt_ate_a_proxima", null, formState.getLocale()) + "! 🚀\n\n" +
                        "**" + messageSource.getMessage("txt_comandos_disponiveis", null, formState.getLocale()) + ":**\n" +
                        "`/squad-log` - " + messageSource.getMessage("txt_criar_ou_atualizar_squad_log", null, formState.getLocale()) + "\n" +
                        "`/squad-log-lote` - " + messageSource.getMessage("txt_criar_multiplos_logs_de_uma_vez", null, formState.getLocale()) + "\n" +
                        "`/language` - " + messageSource.getMessage("txt_alterar_idioma", null, formState.getLocale()))
                .setColor(Color.BLUE)
                .setFooter(messageSource.getMessage("txt_esta_mensagem_sera_excluida_automaticamente", null, formState.getLocale()));
             
        event.getHook().editOriginalEmbeds(exitEmbed.build())
             .setComponents()
             .queueAfter(2, TimeUnit.SECONDS);
             
        event.getHook().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
    }

    private void showSessionExpiredError(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_sessao_expirada", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_sessao_de_criacao_em_lote_expirou", null, formState.getLocale()) + ".")
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
        showEditEntryModalPage1(event, entries, currentIndex);
    }
    
    private void showEditEntryModalPage1(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput squadInput = TextInput.create("edit-squad", messageSource.getMessage("txt_squad", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setRequiredRange(1, 100)
                .build();
                
        TextInput personInput = TextInput.create("edit-person", messageSource.getMessage("txt_pessoa", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setRequiredRange(1, 100)

                .build();
        TextInput typeInput = TextInput.create("edit-type", messageSource.getMessage("txt_tipo", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setRequiredRange(1, 50)
                .build();

        Modal modal = Modal.create("batch-edit-modal-page1", "✏️ " + messageSource.getMessage("txt_ediar_squad_log_um_de_dois", null, formState.getLocale()))
                .addActionRow(squadInput)
                .addActionRow(personInput)
                .addActionRow(typeInput)
                .build();

        event.replyModal(modal).queue();
    }
    
    private void showEditEntryModalPage2(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput categoriesInput = TextInput.create("edit-categories", messageSource.getMessage("txt_categorias", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setRequiredRange(1, 200)
                .build();
                
        String startDateStr = entry.getStartDate() != null ? entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "";
        String endDateStr = entry.getEndDate() != null ? entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "";
        String datesValue = startDateStr;
        if (!endDateStr.isEmpty()) {
            datesValue += " - " + endDateStr;
        }
        
        TextInput datesInput = TextInput.create("edit-dates", messageSource.getMessage("txt_datas", null, formState.getLocale()) + " (DD-MM-AAAA - DD-MM-AAAA)", TextInputStyle.SHORT)
                .setValue(datesValue)
                .setPlaceholder("Ex: 15-01-2025 - 20-01-2025 " + messageSource.getMessage("txt_ou_apenas", null, formState.getLocale()) + " 15-01-2025")
                .setRequired(true)
                .build();
                
        TextInput descriptionInput = TextInput.create("edit-description", messageSource.getMessage("txt_descricao", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
                .setValue(entry.getDescription())
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-modal-page2", "✏️ " + messageSource.getMessage("txt_ediar_squad_log_dois_de_dois", null, formState.getLocale()))
                .addActionRow(categoriesInput)
                .addActionRow(datesInput)
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleEditEntryModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        
        if ("batch-edit-modal-page1".equals(modalId)) {
            handleEditEntryModalPage1(event);
        } else if ("batch-edit-modal-page2".equals(modalId)) {
            handleEditEntryModalPage2(event);
        }
    }
    
    private void handleEditEntryModalPage1(ModalInteractionEvent event) {
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
        
        entry.setSquadName(newSquad);
        entry.setPersonName(newPerson);
        entry.setLogType(newType);
        
        showEditPageTransition(event, entries, currentIndex);
    }
    
    private void showEditPageTransition(ModalInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_primeira_pagina_salva", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_dados_salvos_com_sucesso", null, formState.getLocale()) + "!\n\n" +
                        messageSource.getMessage("txt_clique_em", null, formState.getLocale()) + " **" +
                        messageSource.getMessage("txt_proximo", null, formState.getLocale()) +"** " +
                        messageSource.getMessage("txt_para_editar_categorias", null, formState.getLocale()) + ".")
                .setColor(Color.GREEN);

        Button nextButton = Button.primary("batch-edit-page2", "➡️ " + messageSource.getMessage("txt_proximo", null, formState.getLocale()) + " (2/2)");
        Button cancelButton = Button.secondary("batch-back-to-preview", "❌ " + messageSource.getMessage("txt_cancelar", null, formState.getLocale()));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents(ActionRow.of(nextButton, cancelButton))
             .queue();
    }
    
    private void handleEditEntryModalPage2(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        List<BatchLogEntry> entries = getBatchEntries(userId);
        int currentIndex = getCurrentIndex(userId);
        
        if (entries == null || entries.isEmpty()) {
            showSessionExpiredError(event);
            return;
        }
        
        event.deferEdit().queue();
        
        BatchLogEntry entry = entries.get(currentIndex);
        
        try {
            String newCategoriesStr = event.getValue("edit-categories").getAsString().trim();
            String datesStr = event.getValue("edit-dates").getAsString().trim();
            String newDescription = event.getValue("edit-description").getAsString().trim();
            
            entry.setCategories(Arrays.asList(newCategoriesStr.split(",\\s*")));
            entry.setDescription(newDescription);
            
            if (datesStr.isEmpty()) {
                showDateValidationError(event, messageSource.getMessage("txt_data_de_inicio_obrigatorio", null, formState.getLocale()));
                return;
            }
            
            if (datesStr.contains(" - ")) {
                String[] dateParts = datesStr.split(" - ");
                if (dateParts.length == 2) {
                    String startDateStr = dateParts[0].trim();
                    String endDateStr = dateParts[1].trim();
                    
                    entry.setStartDate(java.time.LocalDate.parse(startDateStr, BRAZILIAN_DATE_FORMAT));
                    entry.setEndDate(java.time.LocalDate.parse(endDateStr, BRAZILIAN_DATE_FORMAT));
                } else {
                    showDateValidationError(event, messageSource.getMessage("txt_formato_invalido", null, formState.getLocale()) + ": DD-MM-AAAA - DD-MM-AAAA " + messageSource.getMessage("txt_ou_apenas", null, formState.getLocale()) +"ou apenas DD-MM-AAAA");
                    return;
                }
            } else {
                entry.setStartDate(java.time.LocalDate.parse(datesStr, BRAZILIAN_DATE_FORMAT));
                entry.setEndDate(null);
            }
            
        } catch (Exception e) {
            log.error(messageSource.getMessage("txt_erro_ao_processar_datas", null, formState.getLocale()) + ": {}", e.getMessage());
            showDateValidationError(event, messageSource.getMessage("txt_formato_de_data_invalido_use_o_formato", null, formState.getLocale()) + " DD-MM-AAAA (ex: 15-01-2025)");
            return;
        }
        
        BatchParsingResult validationResult = batchValidator.validateEntries(Arrays.asList(entry));
        
        if (!validationResult.hasValidEntries()) {
            showEditValidationError(event, validationResult);
            return;
        }
        
        BatchLogEntry validatedEntry = validationResult.getValidEntries().get(0);
        entries.set(currentIndex, validatedEntry);
        
        updatePreviewFromModal(event, entries, currentIndex, null);
    }
    
    private void showEditValidationError(ModalInteractionEvent event, BatchParsingResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erro_na_edicao", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_foram_encontrados_erros_nos_dados_editados", null, formState.getLocale()) + ":")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField(messageSource.getMessage("txt_erros_encontrados", null, formState.getLocale()), errorText.toString(), false);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showSessionExpiredError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_sessao_expirada", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_sessao_de_criacao_em_lote_expirou", null, formState.getLocale()) + ".")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void updatePreviewFromModal(ModalInteractionEvent event, List<BatchLogEntry> entries, int currentIndex, String modifiedField) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        MessageEmbed previewEmbed;
        if (modifiedField != null && previewNavigator instanceof EmbedPreviewNavigationService) {
            previewEmbed = ((EmbedPreviewNavigationService) previewNavigator).createPreviewEmbed(entry, currentIndex, entries.size(), modifiedField);
        } else {
            previewEmbed = previewNavigator.createPreviewEmbed(entry, currentIndex, entries.size());
        }
        
        List<ActionRow> actionRows = createNavigationActionRows(currentIndex, entries.size());
        
        event.getHook().editOriginalEmbeds(previewEmbed)
             .setComponents(actionRows)
             .queue();
    }

    private void showEditInstructions(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        showBatchEditSummary(event, entry, currentIndex);
    }

    private void showBatchEditSummary(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
                .setDescription("**" + messageSource.getMessage("txt_dados_atuais_do_log", null, formState.getLocale()) + ":**")
                .setColor(Color.BLUE);

        String squadName = entry.getSquadName() != null ? entry.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        
        String personName = entry.getPersonName() != null ? entry.getPersonName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), personName, false);
        
        String logType = entry.getLogType() != null ? entry.getLogType() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), logType, false);
        
        String categories = (entry.getCategories() != null && !entry.getCategories().isEmpty()) ? 
            String.join(", ", entry.getCategories()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categories, false);
        
        String description = entry.getDescription() != null ? entry.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 "+ messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDate, false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()), endDate, false);

        embed.setFooter(messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale()));

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale())));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale())));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ " + messageSource.getMessage("txt_voltar", null, formState.getLocale())));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
             .queue();
    }

    private void showSquadEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput squadInput = TextInput.create("edit-squad", messageSource.getMessage("txt_squad", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_nome_do_squad", null, formState.getLocale()))
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-squad-modal", "🏢 " + messageSource.getMessage("txt_editar_squad", null, formState.getLocale()))
                .addActionRow(squadInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showPersonEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput personInput = TextInput.create("edit-person", messageSource.getMessage("txt_pessoa", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_nome_da_pessoa", null, formState.getLocale()))
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-person-modal", "👤 " + messageSource.getMessage("txt_editar_pessoa", null, formState.getLocale()))
                .addActionRow(personInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showTypeEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput typeInput = TextInput.create("edit-type", messageSource.getMessage("txt_tipo", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_tipo_do_log", null, formState.getLocale()))
                .setRequiredRange(1, 50)
                .build();

        Modal modal = Modal.create("batch-edit-type-modal", "📝 " + messageSource.getMessage("txt_editar_tipo", null, formState.getLocale()))
                .addActionRow(typeInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showCategoriesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput categoriesInput = TextInput.create("edit-categories", messageSource.getMessage("txt_categorias", null, formState.getLocale()), TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setPlaceholder(messageSource.getMessage("txt_digite_as_categorias_separadas_por_virgulas", null, formState.getLocale()))
                .setRequiredRange(1, 200)
                .build();

        Modal modal = Modal.create("batch-edit-categories-modal", "🏷️ " + messageSource.getMessage("txt_editar_categorias", null, formState.getLocale()))
                .addActionRow(categoriesInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDescriptionEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput descriptionInput = TextInput.create("edit-description", messageSource.getMessage("txt_descricao", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
                .setValue(entry.getDescription())
                .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, formState.getLocale()))
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-description-modal", "📄 " + messageSource.getMessage("txt_editar_descricao", null, formState.getLocale()))
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDatesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);
        
        TextInput.Builder startDateBuilder = TextInput.create("edit-start-date", messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), TextInputStyle.SHORT)
                .setPlaceholder("DD-MM-AAAA (ex: 15-01-2025)")
                .setRequiredRange(10, 10);
        
        if (entry.getStartDate() != null) {
            startDateBuilder.setValue(entry.getStartDate().format(BRAZILIAN_DATE_FORMAT));
        }
        
        TextInput startDateInput = startDateBuilder.build();

        TextInput.Builder endDateBuilder = TextInput.create("edit-end-date", messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()), TextInputStyle.SHORT)
                .setPlaceholder("DD-MM-AAAA (" + messageSource.getMessage("txt_opcional", null, formState.getLocale()) +")")
                .setRequired(false)
                .setRequiredRange(0, 10);
        
        if (entry.getEndDate() != null) {
            endDateBuilder.setValue(entry.getEndDate().format(BRAZILIAN_DATE_FORMAT));
        }
        
        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create("batch-edit-dates-modal", "📅 " + messageSource.getMessage("txt_editar_datas", null, formState.getLocale()))
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
        String modifiedField = null;
        
        switch (modalId) {
            case "batch-edit-squad-modal":
                String newSquad = event.getValue("edit-squad").getAsString().trim();
                if (!newSquad.isEmpty()) {
                    entry.setSquadName(newSquad);
                    needsValidation = true;
                    modifiedField = "squad";
                }
                break;
                
            case "batch-edit-person-modal":
                String newPerson = event.getValue("edit-person").getAsString().trim();
                if (!newPerson.isEmpty()) {
                    entry.setPersonName(newPerson);
                    needsValidation = true;
                    modifiedField = "person";
                }
                break;
                
            case "batch-edit-type-modal":
                String newType = event.getValue("edit-type").getAsString().trim();
                if (!newType.isEmpty()) {
                    entry.setLogType(newType);
                    needsValidation = true;
                    modifiedField = "type";
                }
                break;
                
            case "batch-edit-categories-modal":
                String newCategoriesStr = event.getValue("edit-categories").getAsString().trim();
                if (!newCategoriesStr.isEmpty()) {
                    entry.setCategories(Arrays.asList(newCategoriesStr.split(",\\s*")));
                    needsValidation = true;
                    modifiedField = "categories";
                }
                break;
                
            case "batch-edit-description-modal":
                String newDescription = event.getValue("edit-description").getAsString().trim();
                if (!newDescription.isEmpty()) {
                    entry.setDescription(newDescription);
                    modifiedField = "description";
                }
                break;
                
            case "batch-edit-dates-modal":
                handleDatesEdit(event, entry);
                modifiedField = "dates";
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
        
        updatePreviewFromModal(event, entries, currentIndex, modifiedField);
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
            log.error(messageSource.getMessage("txt_erro_ao_processar_datas", null, formState.getLocale()) + ": {}", e.getMessage());
            showDateValidationError(event);
        }
    }
    
    private void showDateValidationError(ModalInteractionEvent event) {
        showDateValidationError(event, messageSource.getMessage("txt_formato_de_data_invalido_use_o_formato", null, formState.getLocale()) + " DD-MM-AAAA (ex: 15-01-2025)");
    }
    
    private void showDateValidationError(ModalInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erro_nas_datas", null, formState.getLocale()))
                .setDescription(message)
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    private void showBatchEditSummary(ModalInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
                .setDescription("**" + messageSource.getMessage("txt_dados_atuais_do_log", null, formState.getLocale()) + ":**")
                .setColor(Color.BLUE);

        String squadName = entry.getSquadName() != null ? entry.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        
        String personName = entry.getPersonName() != null ? entry.getPersonName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), personName, false);
        
        String logType = entry.getLogType() != null ? entry.getLogType() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), logType, false);
        
        String categories = (entry.getCategories() != null && !entry.getCategories().isEmpty()) ? 
            String.join(", ", entry.getCategories()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categories, false);
        
        String description = entry.getDescription() != null ? entry.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDate, false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " +messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()), endDate, false);

        embed.setFooter(messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale()));

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale())));
        editButtons.add(Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale())));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale())));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ " + messageSource.getMessage("txt_voltar", null, formState.getLocale())));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
             .queue();
    }

    private void showPostCreationMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_seus_logs_foram_criados_com_sucesso", null, formState.getLocale()) + "!")
                .setDescription(messageSource.getMessage("txt_caso_queira_criar_ou_editar_novos_logs", null, formState.getLocale()) + ":\n\n" +
                               "• `/squad-log-lote` - " + messageSource.getMessage("txt_criar_logs_em_lote", null, formState.getLocale()) + "\n" +
                               "• `/squad-log` - " + messageSource.getMessage("txt_criar_ou_editar_log_individual", null, formState.getLocale()) + "\n" +
                               "• `/language` - " + messageSource.getMessage("txt_alterar_idioma", null, formState.getLocale()))
                .setColor(Color.GREEN)
                .setFooter(messageSource.getMessage("txt_esta_mensagem_sera_apagada_em_10_segundos", null, formState.getLocale()));

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents()
             .queue();
        
        event.getHook().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
        
        clearBatchState(event.getUser().getId());
    }

    private void showCreateMoreModal(ButtonInteractionEvent event) {
        clearBatchState(event.getUser().getId());
        
        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, formState.getLocale()) , TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_tipo_categorias_data_inicio_data_fim_descricao", null, formState.getLocale()) )
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, formState.getLocale()) )
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue(success -> {
            event.getMessage().delete().queue(
                deleteSuccess -> log.info(messageSource.getMessage("txt_mensagem_anterior_deletada_c_sucesso", null, formState.getLocale()) ),
                deleteError -> log.warn(messageSource.getMessage("txt_n_foi_possivel_deletar)mensagem_anteriro", null, formState.getLocale()) + ": {}", deleteError.getMessage())
            );
        });
    }

    private void showExitMessage(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👋 " + messageSource.getMessage("txt_obrigado_por_usar_o_squad_log_bot", null, formState.getLocale()) + "!" )
                .setDescription(messageSource.getMessage("txt_ate_a_proxima", null, formState.getLocale()) + "! 🚀")
                .setColor(Color.BLUE);

        event.getHook().editOriginalEmbeds(embed.build())
             .setComponents()
             .queue();

        event.getHook().deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
        
        clearBatchState(event.getUser().getId());
    }

    private void showSquadSelectionMenu(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        showSquadEditModal(event, entries, currentIndex);
    }

    private void showPersonSelectionMenu(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        showPersonEditModal(event, entries, currentIndex);
    }

    private void showTypeSelectionMenu(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        showTypeEditModal(event, entries, currentIndex);
    }

    private void showCategoriesSelectionMenu(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        showCategoriesEditModal(event, entries, currentIndex);
    }

}
