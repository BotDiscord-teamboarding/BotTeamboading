package com.meli.teamboardingBot.adapters.handler;
import com.meli.teamboardingBot.core.domain.enums.FormStep;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.*;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
@Slf4j
@Component
@Order(5)
public class ModalInputHandler extends AbstractInteractionHandler {

    @Autowired
    public ModalInputHandler(GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, GetFormStatePort getFormStatePort, SetBatchEntriesPort setBatchEntriesPort, SetBatchCurrentIndexPort setBatchCurrentIndexPort, GetBatchEntriesPort getBatchEntriesPort, GetBatchCurrentIndexPort getBatchCurrentIndexPort, ClearBatchStatePort clearBatchStatePort, DeleteFormStatePort deleteFormStatePort, ResetFormStatePort resetFormStatePort, LoggerApiPort loggerApiPort, MessageSource messageSource) {
        super(getOrCreateFormStatePort, putFormStatePort, getFormStatePort, setBatchEntriesPort, setBatchCurrentIndexPort, getBatchEntriesPort, getBatchCurrentIndexPort, clearBatchStatePort, deleteFormStatePort, resetFormStatePort, loggerApiPort);
        this.messageSource = messageSource;
    }

    @Override
    public boolean canHandle(String componentId) {
        return "create-complete-modal".equals(componentId) ||
               "modal-edit-description".equals(componentId) ||
               "modal-edit-dates".equals(componentId) ||
               "edit-descricao".equals(componentId) ||
               "edit-datas".equals(componentId) ||
               "edit-description-modal".equals(componentId) ||
               "edit-dates-modal".equals(componentId) ||
               "retry-create-modal".equals(componentId) ||
               "retry-edit-dates-modal".equals(componentId) ||
               "retry-field-edit-dates-modal".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        if ("edit-descricao".equals(buttonId)) {
            handleEditDescriptionButton(event, state);
        } else if ("edit-datas".equals(buttonId)) {
            handleEditDatesButton(event, state);
        } else if ("retry-create-modal".equals(buttonId)) {
            handleRetryCreateModal(event, state);
        } else if ("retry-edit-dates-modal".equals(buttonId)) {
            handleRetryEditDatesModal(event, state);
        } else if ("retry-field-edit-dates-modal".equals(buttonId)) {
            handleRetryFieldEditDatesModal(event, state);
        }
    }
    @Override
    public void handleModal(ModalInteractionEvent event, FormState state) {
        String modalId = event.getModalId();
        if ("create-complete-modal".equals(modalId)) {
            handleCreateCompleteModal(event, state);
        } else if ("modal-edit-description".equals(modalId)) {
            handleEditDescriptionModal(event, state);
        } else if ("modal-edit-dates".equals(modalId)) {
            handleEditDatesModal(event, state);
        } else if ("edit-description-modal".equals(modalId)) {
            handleFieldEditDescriptionModal(event, state);
        } else if ("edit-dates-modal".equals(modalId)) {
            handleFieldEditDatesModal(event, state);
        }
    }
    @Autowired
    private MessageSource messageSource;

    private java.util.Locale getUserLocale(long userId) {
        return getOrCreateFormStatePort.getOrCreateState(userId).getLocale();
    }
    private void handleEditDescriptionButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando descrição");
        TextInput.Builder descriptionBuilder = TextInput.create("description", messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
            .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, getUserLocale(event.getUser().getIdLong())) + "...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();
        Modal modal = Modal.create("modal-edit-description", "📝 " + messageSource.getMessage("txt_editar_descrição", null, getUserLocale(event.getUser().getIdLong())))
            .addActionRow(descriptionInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleEditDatesButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando datas");
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String formattedStartDate = formatToBrazilianDate(state.getStartDate());
            if (!formattedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(formattedStartDate);
            }
        }
        TextInput startDateInput = startDateBuilder.build();
        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data_opcional", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }
        TextInput endDateInput = endDateBuilder.build();
        Modal modal = Modal.create("modal-edit-dates", "📅 " + messageSource.getMessage("txt_editar_datas", null, getUserLocale(event.getUser().getIdLong())) )
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleCreateCompleteModal(ModalInteractionEvent event, FormState state) {
        log.info("Processando modal de criação completa");
        String description = event.getValue("description").getAsString();
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (!isValidDate(startDate)) {
            state.setDescription(description);
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong())) )
                .setDescription(messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong()))
                        + ": `" + startDate + "`\n\n" +messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong()))  +" **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong())) ),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())) )
                )
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            state.setDescription(description);
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())) + ": `" + endDate + "`\n\n"+
                        messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        if (isEndDateBeforeStartDate(startDate, endDate)) {
            state.setDescription(description);
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n**" +
                        messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + ":** " + startDate + "\n" +
                        messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) + ":** " + endDate + "\n\n**" +
                        messageSource.getMessage("txt_por_favor_corrija_as_datas", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        state.setDescription(description);
        state.setStartDate(startDate);
        state.setEndDate(endDate);
        state.setStep(FormStep.SUMMARY);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        showCreateSummary(event, state);
    }
    private void handleEditDescriptionModal(ModalInteractionEvent event, FormState state) {
        log.info("Processando edição de descrição");
        String description = event.getValue("description").getAsString();
        state.setDescription(description);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        showSummary(event, state);
    }
    private void handleEditDatesModal(ModalInteractionEvent event, FormState state) {
        log.info("Processando edição de datas");
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (!isValidDate(startDate)) {
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong())) + ": `" + startDate + "`\n\n"+
                        messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())) + ": `" + endDate + "`\n\n"
                        + messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) +" **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        if (isEndDateBeforeStartDate(startDate, endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n**" +
                        messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + ":** " + startDate + "\n**" +
                        messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) + ":** " + endDate + "\n\n" +
                        messageSource.getMessage("txt_por_favor_corrija_as_datas", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        state.setStartDate(startDate);
        state.setEndDate(endDate);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        showSummary(event, state);
    }
    private boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            String[] parts = dateStr.split("-");
            if (parts.length != 3) {
                log.warn("Data com formato inválido (não tem 3 partes): {}", dateStr);
                return false;
            }
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            log.info("Validando data: dateStr={}, day={}, month={}, year={}", dateStr, day, month, year);
            if (year < 1900 || year > 2100) {
                log.warn("Ano inválido: {}", year);
                return false;
            }
            if (month < 1 || month > 12) {
                log.warn("Mês inválido: {}", month);
                return false;
            }
            if (day < 1 || day > 31) {
                log.warn("Dia inválido: {}", day);
                return false;
            }
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeParseException e) {
            log.error("Erro ao criar LocalDate: {}", e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            log.error("Erro ao converter string para número na data {}: {}", dateStr, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Erro inesperado ao validar data {}: {}", dateStr, e.getMessage());
            return false;
        }
    }
    private boolean isEndDateBeforeStartDate(String startDateStr, String endDateStr) {
        if (startDateStr == null || endDateStr == null || endDateStr.trim().isEmpty()) {
            return false;
        }
        try {
            String[] startParts = startDateStr.split("-");
            String[] endParts = endDateStr.split("-");
            
            LocalDate startDate = LocalDate.of(
                Integer.parseInt(startParts[2]), 
                Integer.parseInt(startParts[1]), 
                Integer.parseInt(startParts[0])
            );
            
            LocalDate endDate = LocalDate.of(
                Integer.parseInt(endParts[2]), 
                Integer.parseInt(endParts[1]), 
                Integer.parseInt(endParts[0])
            );
            
            return endDate.isBefore(startDate);
        } catch (Exception e) {
            log.error("Erro ao comparar datas: startDate={}, endDate={}, erro={}", startDateStr, endDateStr, e.getMessage());
            return false;
        }
    }
    private void showCreateSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo após preenchimento do modal");
        long userId = event.getUser().getIdLong();
        net.dv8tion.jda.api.EmbedBuilder embed = buildCompleteSummaryEmbed(state, userId);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("criar-log", "✅ " + messageSource.getMessage("txt_criar_log", null, getUserLocale(event.getUser().getIdLong()))),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_editar", null, getUserLocale(event.getUser().getIdLong())))
            )
            .queue();
    }
    private void showSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo após edição via modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            long userId = event.getUser().getIdLong();
            EmbedBuilder embed = buildCompleteSummaryEmbed(state, userId);
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.primary("voltar-logs", "↩️ " + messageSource.getMessage("txt_voltar", null, getUserLocale(event.getUser().getIdLong())))
                ))
                .queue();
        }
    }
    private EmbedBuilder buildCompleteSummaryEmbed(FormState state, long userId) {
        java.util.Locale locale = getUserLocale(userId);
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 " + messageSource.getMessage("txt_resumo_completo_do_squad_log", null, locale))
            .setDescription(messageSource.getMessage("txt_verifique_todos_os_dados_antes_de_criar_o_log", null, locale) + ":")
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, locale), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, locale), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, locale), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, locale), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, locale), description, false);
        String startDateText = state.getStartDate() != null ?
            formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("📅 " + messageSource.getMessage("txt_data_inicio", null, locale), startDateText, false);
        String endDateText = state.getEndDate() != null && !state.getEndDate().isEmpty() ?
            formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, locale);
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, locale) , endDateText, false);
        return embed;
    }
    private void handleFieldEditDescriptionModal(ModalInteractionEvent event, FormState state) {
        log.info("Processando edição de descrição via modal de campo");
        String description = event.getValue("description").getAsString();
        state.setDescription(description);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        returnToFieldEditSummaryWithHook(event, state);
    }
    private void handleFieldEditDatesModal(ModalInteractionEvent event, FormState state) {
        log.info("Processando edição de datas via modal de campo");
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (endDate != null && endDate.trim().isEmpty()) {
            endDate = null;
        }
        if (!isValidDate(startDate)) {
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription( messageSource.getMessage("txt_data_de_inicio_invalida", null, getUserLocale(event.getUser().getIdLong())) +": `" + startDate + "`\n\n"
                        +  messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 Preencher Novamente"),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())) + ": `" + endDate + "`\n\n"
                        + messageSource.getMessage("txt_use_o_formato", null, getUserLocale(event.getUser().getIdLong())) + " **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        if (isEndDateBeforeStartDate(startDate, endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + ".\n\n" +
                              "**" + messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) +":** " + startDate + "\n" +
                              "**"+messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) +":** " + endDate + "\n\n" +
                                messageSource.getMessage("txt_por_favor_corrija_as_datas", null, getUserLocale(event.getUser().getIdLong()))+".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue();
            return;
        }
        log.info("Atualizando datas no estado: startDate={}, endDate={}", startDate, endDate);
        state.setStartDate(startDate);
        state.setEndDate(endDate);
        updateFormState(event.getUser().getIdLong(), state);
        log.info("Estado atualizado. Novas datas: startDate={}, endDate={}", state.getStartDate(), state.getEndDate());
        event.deferEdit().queue();
        returnToFieldEditSummaryWithHook(event, state);
    }
    private void returnToFieldEditSummary(ModalInteractionEvent event, FormState state) {
        log.info("Retornando ao resumo de edição após modal (descrição/datas)");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, getUserLocale(event.getUser().getIdLong())))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, getUserLocale(event.getUser().getIdLong()))
                    + ". " + messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, getUserLocale(event.getUser().getIdLong())) + ":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("🏢 "+ messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("👤 "+ messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("📝 "+ messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("🏷️ "+ messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 "+ messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("📅 "+ messageSource.getMessage("txt_data_inicio", null, getUserLocale(event.getUser().getIdLong())), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, getUserLocale(event.getUser().getIdLong())) , endDate, false);
        event.getHook().editOriginal("")
            .setEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-user", "👤 "+ messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-type", "📝 "+ messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())))
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, getUserLocale(event.getUser().getIdLong())))
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ " + messageSource.getMessage("txt_salvar_alteracoes", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.danger("cancelar-edicao", "❌ " + messageSource.getMessage("txt_cancelar", null, getUserLocale(event.getUser().getIdLong())))
                )
            )
            .queue();
    }
    private void returnToFieldEditSummaryWithHook(ModalInteractionEvent event, FormState state) {
        log.info("Retornando ao resumo de edição após modal (descrição/datas) via hook");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, getUserLocale(event.getUser().getIdLong())))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, getUserLocale(event.getUser().getIdLong()))
                    + ". "+ messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, getUserLocale(event.getUser().getIdLong()))+":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("🏢 "+ messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong())), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("👤 "+ messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong())), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("📝 "+ messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("🏷️ "+ messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong())), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 "+ messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())) , description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, getUserLocale(event.getUser().getIdLong()) );
        embed.addField("📅 "+ messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())), endDate, false);
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-user", "👤 "+ messageSource.getMessage("txt_pessoa", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-type", "📝 "+ messageSource.getMessage("txt_tipo", null, getUserLocale(event.getUser().getIdLong())))
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ "+ messageSource.getMessage("txt_categorias", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-description", "📄 "+ messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, getUserLocale(event.getUser().getIdLong())))
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ " + messageSource.getMessage("txt_salvar_alteracoes", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.danger("cancelar-edicao", "❌ " + messageSource.getMessage("txt_cancelar", null, getUserLocale(event.getUser().getIdLong())))
                )
            )
            .queue();
    }
    private void handleRetryCreateModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de criação após erro de data");
        
        TextInput.Builder descriptionBuilder = TextInput.create("description", messageSource.getMessage("txt_descricao", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.PARAGRAPH)
            .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, getUserLocale(event.getUser().getIdLong())) + "...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();

        TextInput startDateInput = TextInput.create("start_date",  messageSource.getMessage("txt_data_inicio", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(true)
            .build();

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_fim", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data_opcional", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            endDateBuilder.setValue(state.getEndDate());
        }
        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create("create-complete-modal", "📝 " + messageSource.getMessage("txt_finalizar_criacao_do_log", null, getUserLocale(event.getUser().getIdLong())) )
            .addActionRow(descriptionInput)
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryEditDatesModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de edição de datas após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String formattedStartDate = formatToBrazilianDate(state.getStartDate());
            if (!formattedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(formattedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, getUserLocale(event.getUser().getIdLong())), TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data_opcional", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }

        Modal modal = Modal.create("modal-edit-dates", "📅 " + messageSource.getMessage("txt_editar_datas", null, getUserLocale(event.getUser().getIdLong())))
            .addActionRow(startDateBuilder.build())
            .addActionRow(endDateBuilder.build())
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryFieldEditDatesModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de edição de datas de campo após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String convertedStartDate = convertApiDateToBrazilian(state.getStartDate());
            if (!convertedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(convertedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim", null, getUserLocale(event.getUser().getIdLong())) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data_opcional", null, getUserLocale(event.getUser().getIdLong())))
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String convertedEndDate = convertApiDateToBrazilian(state.getEndDate());
            if (!convertedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(convertedEndDate);
            }
        }

        Modal modal = Modal.create("edit-dates-modal", "📅 "+messageSource.getMessage("txt_editar_datas", null, getUserLocale(event.getUser().getIdLong()) ))
            .addActionRow(startDateBuilder.build())
            .addActionRow(endDateBuilder.build())
            .build();

        event.replyModal(modal).queue();
    }

    private String convertApiDateToBrazilian(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return "";
        }
        if (apiDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return apiDate;
        }
        if (apiDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = apiDate.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0]; 
        }
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(apiDate);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
            return localDate.format(formatter);
        } catch (Exception e) {
            log.warn("Não foi possível converter a data da API: {}", apiDate);
            return apiDate;
        }
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
