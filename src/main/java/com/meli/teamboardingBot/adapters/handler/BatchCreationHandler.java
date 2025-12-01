package com.meli.teamboardingBot.adapters.handler;

import com.meli.teamboardingBot.adapters.out.session.ActiveFlowMessageService;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.domain.batch.BatchParsingResult;
import com.meli.teamboardingBot.core.ports.formstate.*;
import com.meli.teamboardingBot.adapters.out.client.SquadLogService;
import com.meli.teamboardingBot.adapters.out.batch.BatchValidator;
import com.meli.teamboardingBot.adapters.out.batch.PreviewNavigator;
import com.meli.teamboardingBot.adapters.out.batch.TextParser;
import com.meli.teamboardingBot.adapters.out.batch.impl.EmbedPreviewNavigationService;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
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

    private MessageSource messageSource;
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final TextParser intelligentTextParser;
    private final BatchValidator batchValidator;
    private final PreviewNavigator previewNavigator;
    private final SquadLogService squadLogService;
    private final ActiveFlowMessageService activeFlowMessageService;
    

    @Autowired
    public BatchCreationHandler(GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, GetFormStatePort getFormStatePort, SetBatchEntriesPort setBatchEntriesPort, SetBatchCurrentIndexPort setBatchCurrentIndexPort, GetBatchEntriesPort getBatchEntriesPort, GetBatchCurrentIndexPort getBatchCurrentIndexPort, ClearBatchStatePort clearBatchStatePort, DeleteFormStatePort deleteFormStatePort, ResetFormStatePort resetFormStatePort, LoggerApiPort loggerApiPort, TextParser intelligentTextParser, BatchValidator batchValidator, PreviewNavigator previewNavigator, SquadLogService squadLogService, MessageSource messageSource, ActiveFlowMessageService activeFlowMessageService) {
        super(getOrCreateFormStatePort, putFormStatePort, getFormStatePort, setBatchEntriesPort, setBatchCurrentIndexPort, getBatchEntriesPort, getBatchCurrentIndexPort, clearBatchStatePort, deleteFormStatePort, resetFormStatePort, loggerApiPort);
        this.intelligentTextParser = intelligentTextParser;
        this.batchValidator = batchValidator;
        this.previewNavigator = previewNavigator;
        this.squadLogService = squadLogService;
        this.messageSource = messageSource;
        this.activeFlowMessageService = activeFlowMessageService;
    }


    private java.util.Locale getUserLocale(long userId) {
        return getOrCreateFormStatePort.getOrCreateState(userId).getLocale();
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
                componentId.equals("batch-back-to-preview") || componentId.equals("batch-edit-page2") ||
                componentId.equals("batch-edit-squad-modal") || componentId.equals("batch-edit-person-modal") ||
                componentId.equals("batch-edit-type-modal") || componentId.equals("batch-edit-categories-modal") ||
                componentId.equals("batch-edit-description-modal") || componentId.equals("batch-edit-dates-modal") ||
                componentId.equals("batch-edit-modal-page1") || componentId.equals("batch-edit-modal-page2");
    }

    public void handleBatchCreationCommand(SlashCommandInteractionEvent event) {
        loggerApiPort.info("Iniciando comando /squad-log-lote para usuário: {}", event.getUser().getId());

        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_categoria_data_ex", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleOpenBatchModalButton(ButtonInteractionEvent event) {
        loggerApiPort.info("Abrindo modal de criação em lote via botão para usuário: {}", event.getUser().getId());

        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_categoria_data_ex", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue();
    }

    public void handleBatchCreationModal(ModalInteractionEvent event) {
        loggerApiPort.info("Processando modal de criação em lote");

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

        loggerApiPort.info("Parsed entries before validation:");
        for (int i = 0; i < parsedEntries.size(); i++) {
            BatchLogEntry entry = parsedEntries.get(i);
            loggerApiPort.info("Entry {}: Squad='{}', Person='{}', Type='{}'",
                    i, entry.getSquadName(), entry.getPersonName(), entry.getLogType());
        }

        BatchParsingResult validationResult;
        try {
            loggerApiPort.info("Iniciando validação com API...");
            validationResult = batchValidator.validateEntries(parsedEntries);
            loggerApiPort.info("Validação com API concluída");
        } catch (RuntimeException e) {
            loggerApiPort.error("Erro durante validação com API: {}", e.getMessage());
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

        loggerApiPort.info("Valid entries after validation:");
        List<BatchLogEntry> validEntries = validationResult.getValidEntries();
        for (int i = 0; i < validEntries.size(); i++) {
            BatchLogEntry entry = validEntries.get(i);
            loggerApiPort.info("Valid Entry {}: Squad='{}', Person='{}', Type='{}'",
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
                .setTitle("❌ " + messageSource.getMessage("txt_erro_no_formato", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_interpretar_o_texto_fornecido", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n **" +
                        messageSource.getMessage("txt_formato_esperado", null, getUserLocale(event.getUser().getIdLong())) + ":**\n`" +
                        messageSource.getMessage("txt_squad_pessoa_tipo_categorias_data_inicio_data_fim_descricao", null, getUserLocale(event.getUser().getIdLong())) + "`\n\n**"
                        + messageSource.getMessage("txt_exemplo", null, getUserLocale(event.getUser().getIdLong())) +
                        ":**\n`" + messageSource.getMessage("txt_squad_exemplo", null, getUserLocale(event.getUser().getIdLong())) + "`")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showNoEntriesError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_nenhum_log_encontrado", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_extrair_nenhum_squad_log_do_texto_fornecido", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showValidationErrors(ModalInteractionEvent event, BatchParsingResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erros_de_validacao", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_foram_encontrados_os_seguintes_erros", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField(messageSource.getMessage("txt_erros_encontrados", null, getUserLocale(event.getUser().getIdLong())), errorText.toString(), false);
        embed.setFooter(String.format(messageSource.getMessage("txt_total_processado", null, getUserLocale(event.getUser().getIdLong())) + ": %d | "
                        + messageSource.getMessage("txt_validos", null, getUserLocale(event.getUser().getIdLong())) + ": %d | " +
                        messageSource.getMessage("txt_erros", null, getUserLocale(event.getUser().getIdLong())) + ": %d",
                result.getTotalProcessed(),
                result.getValidCount(),
                result.getErrorCount()));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showApiTimeoutError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_timeout_da_api", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_api_demorou_muito_para_responder", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n**" +
                        messageSource.getMessage("txt_possiveis_causas", null, getUserLocale(event.getUser().getIdLong())) + ":**\n" +
                        messageSource.getMessage("txt_conectividade_lenta_com_a_api", null, getUserLocale(event.getUser().getIdLong())) + "• \n" +
                        messageSource.getMessage("txt_api_temporariamente_indisponivel", null, getUserLocale(event.getUser().getIdLong())) + "• \n" +
                        messageSource.getMessage("txt_sobrecarga_no_servidor", null, getUserLocale(event.getUser().getIdLong())) + "• \n\n**" +
                        messageSource.getMessage("txt_tente_novamente_em_alguns_minutos", null, getUserLocale(event.getUser().getIdLong())) + ".**")
                .setColor(Color.ORANGE)
                .setFooter(messageSource.getMessage("txt_timeout_configurado", null, getUserLocale(event.getUser().getIdLong())));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showAuthenticationRequired(ModalInteractionEvent event) {
        String title = messageSource.getMessage("txt_autenticacao_necessaria", null, getUserLocale(event.getUser().getIdLong()));
        String description = messageSource.getMessage("txt_faca_login_para_usar_os_comandos", null, getUserLocale(event.getUser().getIdLong())) +
                "\n\n" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, getUserLocale(event.getUser().getIdLong()));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + title)
                .setDescription(description)
                .setColor(0xFF6B6B);

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(
                        Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, getUserLocale(event.getUser().getIdLong()))),
                        Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, getUserLocale(event.getUser().getIdLong()))),
                        Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
    }

    private void showApiConnectionError(ModalInteractionEvent event, String errorMessage) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erro_de_conexao_com_a_api", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_nao_foi_possivel_conectar_com_a_api_para_validar_os_dados", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n**" +
                        messageSource.getMessage("txt_erro_tecnico", null, getUserLocale(event.getUser().getIdLong())) + ":**\n" +
                        "```" + errorMessage + "```\n\n**" +
                        messageSource.getMessage("txt_tente_novamente_mais_tarde_ou_contate_o_administrador", null, getUserLocale(event.getUser().getIdLong())) + ".**")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showFirstPreview(ModalInteractionEvent event, BatchParsingResult result) {
        List<BatchLogEntry> entries = result.getValidEntries();
        BatchLogEntry firstEntry = entries.get(0);

        loggerApiPort.info("Creating first preview for entry: Squad='{}', Person='{}', Type='{}'",
                firstEntry.getSquadName(), firstEntry.getPersonName(), firstEntry.getLogType());

        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(firstEntry, 0, entries.size());

        long userId = event.getUser().getIdLong();
        List<ActionRow> actionRows = createNavigationActionRows(0, entries.size(), userId);

        EmbedBuilder summaryEmbed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_logs_processados_com_sucesso", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(String.format("**%d logs** " + messageSource.getMessage("txt_logs_foi_possivel_conectar_com_a_api_para_validar_os_dados", null, getUserLocale(event.getUser().getIdLong())) + "!", entries.size()))
                .setColor(Color.GREEN);

        if (result.hasErrors()) {
            summaryEmbed.addField("⚠️ " + messageSource.getMessage("txt_avisos", null, getUserLocale(event.getUser().getIdLong())),
                    String.format("%d " + messageSource.getMessage("txt_linhas_foram_ignoradas_devido_a_erros", null, getUserLocale(event.getUser().getIdLong())), result.getErrorCount()),
                    false);
        }

        event.getHook().editOriginalEmbeds(summaryEmbed.build(), previewEmbed)
                .setComponents(actionRows)
                .queue(success -> {
                    activeFlowMessageService.registerFlowHook(event.getUser().getIdLong(), event.getHook());
                    loggerApiPort.info("📌 Hook registrado para fluxo em lote do usuário: {}", event.getUser().getIdLong());
                });
    }

    private void updatePreview(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        loggerApiPort.info("Updating preview for entry {}: Squad='{}', Person='{}', Type='{}'",
                currentIndex, entry.getSquadName(), entry.getPersonName(), entry.getLogType());

        MessageEmbed previewEmbed = previewNavigator.createPreviewEmbed(entry, currentIndex, entries.size());

        long userId = event.getUser().getIdLong();
        List<ActionRow> actionRows = createNavigationActionRows(currentIndex, entries.size(), userId);

        event.getHook().editOriginalEmbeds(previewEmbed)
                .setComponents(actionRows)
                .queue();
    }

    private List<ActionRow> createNavigationActionRows(int currentIndex, int totalCount, long userId) {
        List<ActionRow> actionRows = new ArrayList<>();
        java.util.Locale locale = getUserLocale(userId);

        Button previousButton = Button.secondary("batch-previous", "⬅️ " + messageSource.getMessage("txt_anterior", null, locale))
                .withDisabled(!previewNavigator.hasPrevious(currentIndex));
        Button nextButton = Button.secondary("batch-next", messageSource.getMessage("txt_proximo", null, locale) + " ➡️")
                .withDisabled(!previewNavigator.hasNext(currentIndex, totalCount));
        actionRows.add(ActionRow.of(previousButton, nextButton));

        Button squadButton = Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, locale));
        Button personButton = Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, locale));
        Button typeButton = Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, locale));
        actionRows.add(ActionRow.of(squadButton, personButton, typeButton));

        Button categoriesButton = Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, locale));
        Button descriptionButton = Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, locale));
        Button datesButton = Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, locale));
        actionRows.add(ActionRow.of(categoriesButton, descriptionButton, datesButton));

        Button createAllButton = Button.success("batch-create-all", "✅ " + messageSource.getMessage("txt_criar_todos", null, locale));
        Button cancelButton = Button.danger("batch-cancel", "❌ " + messageSource.getMessage("txt_cancelar", null, locale));
        actionRows.add(ActionRow.of(createAllButton, cancelButton));

        return actionRows;
    }

    private void createAllLogs(ButtonInteractionEvent event, List<BatchLogEntry> entries) {
        loggerApiPort.info("Iniciando criação em lote de {} logs", entries.size());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏳ " + messageSource.getMessage("txt_criando_squad_logs", null, getUserLocale(event.getUser().getIdLong())) + "...")
                .setDescription(String.format(messageSource.getMessage("txt_processando", null, getUserLocale(event.getUser().getIdLong())) + " %d logs...", entries.size()))
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
                loggerApiPort.info("Log criado com sucesso: Squad {} - {}", entry.getSquadName(), entry.getPersonName());
            } catch (Exception e) {
                failures.add(String.format("❌ %s - %s: %s", entry.getSquadName(), entry.getPersonName(), e.getMessage()));
                loggerApiPort.error("Erro ao criar log: Squad {} - {}", entry.getSquadName(), entry.getPersonName(), e);
            }
        }

        showCreationResults(event, successes, failures);
    }

    private JSONObject createSquadLogPayload(BatchLogEntry entry) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", entry.getSquadId());
        
        if (entry.getUserId() != 0) {
            payload.put("user_id", entry.getUserId());
            loggerApiPort.info("Incluindo user_id no payload: {}", entry.getUserId());
        } else {
            loggerApiPort.info("All team detectado - omitindo user_id do payload");
        }
        
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
                .setTitle("📊 " + messageSource.getMessage("txt_resultado_da_criacao_em_lote", null, getUserLocale(event.getUser().getIdLong())))
                .setColor(failures.isEmpty() ? Color.GREEN : Color.ORANGE);

        if (!successes.isEmpty()) {
            String successText = String.join("\n", successes.subList(0, Math.min(successes.size(), 10)));
            if (successes.size() > 10) {
                successText += String.format("\n... " + messageSource.getMessage("txt_e_mais", null, getUserLocale(event.getUser().getIdLong())) + " %d logs " + messageSource.getMessage("txt_criados", null, getUserLocale(event.getUser().getIdLong())), successes.size() - 10);
            }
            embed.addField(String.format("✅ " + messageSource.getMessage("txt_sucessos", null, getUserLocale(event.getUser().getIdLong())) + " (%d)", successes.size()), successText, false);
        }

        if (!failures.isEmpty()) {
            String failureText = String.join("\n", failures.subList(0, Math.min(failures.size(), 5)));
            if (failures.size() > 5) {
                failureText += String.format("\n... " + messageSource.getMessage("txt_e_mais", null, getUserLocale(event.getUser().getIdLong())) + " %d " + messageSource.getMessage("txt_erros", null, getUserLocale(event.getUser().getIdLong())), failures.size() - 5);
            }
            embed.addField(String.format("❌ " + messageSource.getMessage("txt_falhas", null, getUserLocale(event.getUser().getIdLong())) + " (%d)", failures.size()), failureText, false);
        }

        embed.setFooter(String.format(messageSource.getMessage("txt_total", null, getUserLocale(event.getUser().getIdLong())) + ": %d | "
                        + messageSource.getMessage("txt_sucessos", null, getUserLocale(event.getUser().getIdLong())) + ": %d | "
                        + messageSource.getMessage("txt_falhas", null, getUserLocale(event.getUser().getIdLong())) + ": %d",
                successes.size() + failures.size(),
                successes.size(),
                failures.size()));

        Button concludeButton = Button.primary("batch-conclude", "✅ " + messageSource.getMessage("txt_concluir", null, getUserLocale(event.getUser().getIdLong())));

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(concludeButton)
                .queue();
    }

    private void cancelBatchCreation(ButtonInteractionEvent event, String userId) {
        clearBatchState(userId);

        EmbedBuilder cancelingEmbed = new EmbedBuilder()
                .setTitle("⏳ " + messageSource.getMessage("txt_cancelando", null, getUserLocale(event.getUser().getIdLong())) + "...")
                .setDescription(messageSource.getMessage("txt_cancelamento_criacao_em_lote", null, getUserLocale(event.getUser().getIdLong())) + "...")
                .setColor(Color.YELLOW);

        event.getHook().editOriginalEmbeds(cancelingEmbed.build())
                .setComponents()
                .queue();

        EmbedBuilder exitEmbed = new EmbedBuilder()
                .setTitle("👋 " + messageSource.getMessage("txt_obrigado_por_usar_o_squad_log_bot", null, getUserLocale(event.getUser().getIdLong())) + "!")
                .setDescription(messageSource.getMessage("txt_ate_a_proxima", null, getUserLocale(event.getUser().getIdLong())) + "! 🚀\n\n" +
                        "**" + messageSource.getMessage("txt_comandos_disponiveis", null, getUserLocale(event.getUser().getIdLong())) + ":**\n" +
                        "`/squad-log` - " + messageSource.getMessage("txt_criar_ou_atualizar_squad_log", null, getUserLocale(event.getUser().getIdLong())) + "\n" +
                        "`/squad-log-lote` - " + messageSource.getMessage("txt_criar_multiplos_logs_de_uma_vez", null, getUserLocale(event.getUser().getIdLong())) + "\n" +
                        "`/language` - " + messageSource.getMessage("txt_alterar_idioma", null, getUserLocale(event.getUser().getIdLong())))
                .setColor(Color.BLUE)
                .setFooter(messageSource.getMessage("txt_esta_mensagem_sera_excluida_automaticamente", null, getUserLocale(event.getUser().getIdLong())));

        event.getHook().editOriginalEmbeds(exitEmbed.build())
                .setComponents()
                .queueAfter(2, TimeUnit.SECONDS);

        event.getHook().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
    }

    private void showSessionExpiredError(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_sessao_expirada", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_sessao_de_criacao_em_lote_expirou", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents()
                .queue();
    }

    private void initializeBatchState(String userId, List<BatchLogEntry> entries) {
        setBatchEntriesPort.setBatchEntries(userId, entries);
        setBatchCurrentIndexPort.setBatchCurrentIndex(userId, 0);
    }

    private List<BatchLogEntry> getBatchEntries(String userId) {
        return getBatchEntriesPort.getBatchEntries(userId);
    }

    private int getCurrentIndex(String userId) {
        return getBatchCurrentIndexPort.getBatchCurrentIndex(userId);
    }

    private void setCurrentIndex(String userId, int index) {
        setBatchCurrentIndexPort.setBatchCurrentIndex(userId, index);
    }

    private void clearBatchState(String userId) {
        clearBatchStatePort.clearBatchState(userId);
    }

    private void showEditEntryModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        showEditEntryModalPage1(event, entries, currentIndex);
    }

    private void showEditEntryModalPage1(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput squadInput = TextInput.create("edit-squad", messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setRequiredRange(1, 100)
                .build();

        TextInput personInput = TextInput.create("edit-person", messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setRequiredRange(1, 100)

                .build();
        TextInput typeInput = TextInput.create("edit-type", messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setRequiredRange(1, 50)
                .build();

        Modal modal = Modal.create("batch-edit-modal-page1", "✏️ " + messageSource.getMessage("txt_ediar_squad_log_um_de_dois", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(squadInput)
                .addActionRow(personInput)
                .addActionRow(typeInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showEditEntryModalPage2(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput categoriesInput = TextInput.create("edit-categories", messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setRequiredRange(1, 200)
                .build();

        String startDateStr = entry.getStartDate() != null ? entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "";
        String endDateStr = entry.getEndDate() != null ? entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "";
        String datesValue = startDateStr;
        if (!endDateStr.isEmpty()) {
            datesValue += " - " + endDateStr;
        }

        TextInput datesInput = TextInput.create("edit-dates", messageSource.getMessage("txt_datas", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA - DD-MM-AAAA)", TextInputStyle.SHORT)
                .setValue(datesValue)
                .setPlaceholder("Ex: 15-01-2025 - 20-01-2025 " + messageSource.getMessage("txt_ou_apenas", null, getUserLocale(event.getUser().getIdLong())) + " 15-01-2025")
                .setRequired(true)
                .build();

        TextInput descriptionInput = TextInput.create("edit-description", messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
                .setValue(entry.getDescription())
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-modal-page2", "✏️ " + messageSource.getMessage("txt_ediar_squad_log_dois_de_dois", null, getUserLocale(event.getUser().getIdLong())))
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
                .setTitle("✅ " + messageSource.getMessage("txt_primeira_pagina_salva", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_dados_salvos_com_sucesso", null, getUserLocale(event.getUser().getIdLong())) + "!\n\n" +
                        messageSource.getMessage("txt_clique_em", null, getUserLocale(event.getUser().getIdLong())) + " **" +
                        messageSource.getMessage("txt_proximo", null, getUserLocale(event.getUser().getIdLong())) + "** " +
                        messageSource.getMessage("txt_para_editar_categorias", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(Color.GREEN);

        Button nextButton = Button.primary("batch-edit-page2", "➡️ " + messageSource.getMessage("txt_proximo", null, getUserLocale(event.getUser().getIdLong())) + " (2/2)");
        Button cancelButton = Button.secondary("batch-back-to-preview", "❌ " + messageSource.getMessage("txt_cancelar", null, getUserLocale(event.getUser().getIdLong())));

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
                showDateValidationError(event, messageSource.getMessage("txt_data_de_inicio_obrigatorio", null, getUserLocale(event.getUser().getIdLong())));
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
                    showDateValidationError(event, messageSource.getMessage("txt_formato_invalido", null, getUserLocale(event.getUser().getIdLong())) + ": DD-MM-AAAA - DD-MM-AAAA " + messageSource.getMessage("txt_ou_apenas", null, getUserLocale(event.getUser().getIdLong())) + "ou apenas DD-MM-AAAA");
                    return;
                }
            } else {
                entry.setStartDate(java.time.LocalDate.parse(datesStr, BRAZILIAN_DATE_FORMAT));
                entry.setEndDate(null);
            }

        } catch (Exception e) {
            loggerApiPort.error(messageSource.getMessage("txt_erro_ao_processar_datas", null, getUserLocale(event.getUser().getIdLong())) + ": {}", e.getMessage());
            showDateValidationError(event, messageSource.getMessage("txt_formato_de_data_invalido_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " DD-MM-AAAA (ex: 15-01-2025)");
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
                .setTitle("❌ " + messageSource.getMessage("txt_erro_na_edicao", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_foram_encontrados_erros_nos_dados_editados", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(Color.RED);

        StringBuilder errorText = new StringBuilder();
        for (String error : result.getErrors()) {
            errorText.append("• ").append(error).append("\n");
        }

        embed.addField(messageSource.getMessage("txt_erros_encontrados", null, getUserLocale(event.getUser().getIdLong())), errorText.toString(), false);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showSessionExpiredError(ModalInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ " + messageSource.getMessage("txt_sessao_expirada", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_sessao_de_criacao_em_lote_expirou", null, getUserLocale(event.getUser().getIdLong())) + ".")
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

        long userId = event.getUser().getIdLong();
        List<ActionRow> actionRows = createNavigationActionRows(currentIndex, entries.size(), userId);

        event.getHook().editOriginalEmbeds(previewEmbed)
                .setComponents(actionRows)
                .queue();
    }

    private void showEditInstructions(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        showBatchEditSummary(event, entry, currentIndex);
    }

    private void showBatchEditSummary(ButtonInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ " + messageSource.getMessage("txt_editar_squad_log", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription("**" + messageSource.getMessage("txt_dados_atuais_do_log", null, getUserLocale(event.getUser().getIdLong())) + ":**")
                .setColor(Color.BLUE);

        String squadName = entry.getSquadName() != null ? entry.getSquadName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), squadName, false);

        String personName = entry.getPersonName() != null ? entry.getPersonName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), personName, false);

        String logType = entry.getLogType() != null ? entry.getLogType() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), logType, false);

        String categories = (entry.getCategories() != null && !entry.getCategories().isEmpty()) ?
                String.join(", ", entry.getCategories()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), categories, false);

        String description = entry.getDescription() != null ? entry.getDescription() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), description, false);

        String startDate = entry.getStartDate() != null ?
                entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())), startDate, false);

        String endDate = entry.getEndDate() != null ?
                entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())), endDate, false);

        embed.setFooter(messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, getUserLocale(event.getUser().getIdLong())));

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong()))));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, getUserLocale(event.getUser().getIdLong()))));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ " + messageSource.getMessage("txt_voltar", null, getUserLocale(event.getUser().getIdLong()))));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
                .queue();
    }

    private void showSquadEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput squadInput = TextInput.create("edit-squad", messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getSquadName())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_nome_do_squad", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-squad-modal", "🏢 " + messageSource.getMessage("txt_editar_squad", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(squadInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showPersonEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput personInput = TextInput.create("edit-person", messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getPersonName())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_nome_da_pessoa", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(1, 100)
                .build();

        Modal modal = Modal.create("batch-edit-person-modal", "👤 " + messageSource.getMessage("txt_editar_pessoa", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(personInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showTypeEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput typeInput = TextInput.create("edit-type", messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(entry.getLogType())
                .setPlaceholder(messageSource.getMessage("txt_digite_o_tipo_do_log", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(1, 50)
                .build();

        Modal modal = Modal.create("batch-edit-type-modal", "📝 " + messageSource.getMessage("txt_editar_tipo", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(typeInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showCategoriesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput categoriesInput = TextInput.create("edit-categories", messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setValue(String.join(", ", entry.getCategories()))
                .setPlaceholder(messageSource.getMessage("txt_digite_as_categorias_separadas_por_virgulas", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(1, 200)
                .build();

        Modal modal = Modal.create("batch-edit-categories-modal", "🏷️ " + messageSource.getMessage("txt_editar_categorias", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(categoriesInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDescriptionEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput descriptionInput = TextInput.create("edit-description", messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
                .setValue(entry.getDescription())
                .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(1, 500)
                .build();

        Modal modal = Modal.create("batch-edit-description-modal", "📄 " + messageSource.getMessage("txt_editar_descricao", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void showDatesEditModal(ButtonInteractionEvent event, List<BatchLogEntry> entries, int currentIndex) {
        BatchLogEntry entry = entries.get(currentIndex);

        TextInput.Builder startDateBuilder = TextInput.create("edit-start-date", messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setPlaceholder("DD-MM-AAAA (ex: 15-01-2025)")
                .setRequiredRange(10, 10);

        if (entry.getStartDate() != null) {
            startDateBuilder.setValue(entry.getStartDate().format(BRAZILIAN_DATE_FORMAT));
        }

        TextInput startDateInput = startDateBuilder.build();

        TextInput.Builder endDateBuilder = TextInput.create("edit-end-date", messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
                .setPlaceholder("DD-MM-AAAA (" + messageSource.getMessage("txt_opcional", null, getUserLocale(event.getUser().getIdLong())) + ")")
                .setRequired(false)
                .setRequiredRange(0, 10);

        if (entry.getEndDate() != null) {
            endDateBuilder.setValue(entry.getEndDate().format(BRAZILIAN_DATE_FORMAT));
        }

        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create("batch-edit-dates-modal", "📅 " + messageSource.getMessage("txt_editar_datas", null, getUserLocale(event.getUser().getIdLong())))
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
            loggerApiPort.error(messageSource.getMessage("txt_erro_ao_processar_datas", null, getUserLocale(event.getUser().getIdLong())) + ": {}", e.getMessage());
            showDateValidationError(event);
        }
    }

    private void showDateValidationError(ModalInteractionEvent event) {
        showDateValidationError(event, messageSource.getMessage("txt_formato_de_data_invalido_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " DD-MM-AAAA (ex: 15-01-2025)");
    }

    private void showDateValidationError(ModalInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_erro_nas_datas", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(message)
                .setColor(Color.RED);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void showBatchEditSummary(ModalInteractionEvent event, BatchLogEntry entry, int currentIndex) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✏️ " + messageSource.getMessage("txt_editar_squad_log", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription("**" + messageSource.getMessage("txt_dados_atuais_do_log", null, getUserLocale(event.getUser().getIdLong())) + ":**")
                .setColor(Color.BLUE);

        String squadName = entry.getSquadName() != null ? entry.getSquadName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), squadName, false);

        String personName = entry.getPersonName() != null ? entry.getPersonName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), personName, false);

        String logType = entry.getLogType() != null ? entry.getLogType() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), logType, false);

        String categories = (entry.getCategories() != null && !entry.getCategories().isEmpty()) ?
                String.join(", ", entry.getCategories()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), categories, false);

        String description = entry.getDescription() != null ? entry.getDescription() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), description, false);

        String startDate = entry.getStartDate() != null ?
                entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())), startDate, false);

        String endDate = entry.getEndDate() != null ?
                entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()));
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())), endDate, false);

        embed.setFooter(messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, getUserLocale(event.getUser().getIdLong())));

        List<Button> editButtons = new ArrayList<>();
        editButtons.add(Button.secondary("batch-edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-person", "👤 " + messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong()))));
        editButtons.add(Button.secondary("batch-edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong()))));

        List<Button> dateButtons = new ArrayList<>();
        dateButtons.add(Button.secondary("batch-edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, getUserLocale(event.getUser().getIdLong()))));
        dateButtons.add(Button.primary("batch-back-to-preview", "⬅️ " + messageSource.getMessage("txt_voltar", null, getUserLocale(event.getUser().getIdLong()))));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(editButtons), ActionRow.of(dateButtons))
                .queue();
    }

    private void showPostCreationMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + messageSource.getMessage("txt_seus_logs_foram_criados_com_sucesso", null, getUserLocale(event.getUser().getIdLong())) + "!")
                .setDescription(messageSource.getMessage("txt_caso_queira_criar_ou_editar_novos_logs", null, getUserLocale(event.getUser().getIdLong())) + ":\n\n" +
                        "• `/squad-log-lote` - " + messageSource.getMessage("txt_criar_logs_em_lote", null, getUserLocale(event.getUser().getIdLong())) + "\n" +
                        "• `/squad-log` - " + messageSource.getMessage("txt_criar_ou_editar_log_individual", null, getUserLocale(event.getUser().getIdLong())) + "\n" +
                        "• `/language` - " + messageSource.getMessage("txt_alterar_idioma", null, getUserLocale(event.getUser().getIdLong())))
                .setColor(Color.GREEN)
                .setFooter(messageSource.getMessage("txt_esta_mensagem_sera_apagada_em_10_segundos", null, getUserLocale(event.getUser().getIdLong())));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents()
                .queue();

        event.getHook().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);

        clearBatchState(event.getUser().getId());
    }

    private void showCreateMoreModal(ButtonInteractionEvent event) {
        clearBatchState(event.getUser().getId());

        TextInput textInput = TextInput.create("batch-text", messageSource.getMessage("txt_digite_os_squad_logs", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_squad_pessoa_tipo_categorias_data_inicio_data_fim_descricao", null, getUserLocale(event.getUser().getIdLong())))
                .setRequiredRange(10, 4000)
                .build();

        Modal modal = Modal.create("batch-creation-modal", "📋 " + messageSource.getMessage("txt_criar_squad_logs_em_lote", null, getUserLocale(event.getUser().getIdLong())))
                .addActionRow(textInput)
                .build();

        event.replyModal(modal).queue(success -> {
            event.getMessage().delete().queue(
                    deleteSuccess -> loggerApiPort.info(messageSource.getMessage("txt_mensagem_anterior_deletada_c_sucesso", null, getUserLocale(event.getUser().getIdLong()))),
                    deleteError -> loggerApiPort.warn(messageSource.getMessage("txt_n_foi_possivel_deletar)mensagem_anteriro", null, getUserLocale(event.getUser().getIdLong())) + ": {}", deleteError.getMessage())
            );
        });
    }

    private void showExitMessage(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👋 " + messageSource.getMessage("txt_obrigado_por_usar_o_squad_log_bot", null, getUserLocale(event.getUser().getIdLong())) + "!")
                .setDescription(messageSource.getMessage("txt_ate_a_proxima", null, getUserLocale(event.getUser().getIdLong())) + "! 🚀")
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
