package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import lombok.RequiredArgsConstructor;
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
    public ModalInputHandler(FormStateService formStateService) {
        super(formStateService);
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

    @Autowired
    private FormState formState;
    private void handleEditDescriptionButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando descrição");
        TextInput.Builder descriptionBuilder = TextInput.create("description", messageSource.getMessage("txt_descricao", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
            .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, formState.getLocale()) + "...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();
        Modal modal = Modal.create("modal-edit-description", "📝 " + messageSource.getMessage("txt_editar_descrição", null, formState.getLocale()))
            .addActionRow(descriptionInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleEditDatesButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando datas");
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String formattedStartDate = formatToBrazilianDate(state.getStartDate());
            if (!formattedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(formattedStartDate);
            }
        }
        TextInput startDateInput = startDateBuilder.build();
        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, formState.getLocale()), TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (" + messageSource.getMessage("txt_deixe_vazio_se_nao_houver", null, formState.getLocale()) +")")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }
        TextInput endDateInput = endDateBuilder.build();
        Modal modal = Modal.create("modal-edit-dates", "📅 " + messageSource.getMessage("txt_editar_datas", null, formState.getLocale()) )
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
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale()) )
                .setDescription(messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale())
                        + ": `" + startDate + "`\n\n" +messageSource.getMessage("txt_use_o_formato", null, formState.getLocale())  +" **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale()) ),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) )
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
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()) + ": `" + endDate + "`\n\n"+
                        messageSource.getMessage("txt_use_o_formato", null, formState.getLocale()) + " **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
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
                .setTitle("❌ Data de Fim Inválida" + messageSource.getMessage("", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, formState.getLocale()) + ".\n\n**" +
                        messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + ":** " + startDate + "\n" +
                        messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) + ":** " + endDate + "\n\n**" +
                        messageSource.getMessage("txt_por_favor_corrija_as_datas", null, formState.getLocale()) + ".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-create-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
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
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale()) + ": `" + startDate + "`\n\n"+
                        messageSource.getMessage("txt_use_o_formato", null, formState.getLocale()) + " **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
                )
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()) + ": `" + endDate + "`\n\n"
                        + messageSource.getMessage("txt_use_o_formato", null, formState.getLocale()) +" **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
                )
                .queue();
            return;
        }
        if (isEndDateBeforeStartDate(startDate, endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, formState.getLocale()) + ".\n\n**" +
                        messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + ":** " + startDate + "\n**" +
                        messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) + ":** " + endDate + "\n\n" +
                        messageSource.getMessage("txt_por_favor_corrija_as_datas", null, formState.getLocale()) + ".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
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
        net.dv8tion.jda.api.EmbedBuilder embed = buildCompleteSummaryEmbed(state);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("criar-log", "✅ " + messageSource.getMessage("txt_criar_log", null, formState.getLocale())),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_editar", null, formState.getLocale()))
            )
            .queue();
    }
    private void showSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo após edição via modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            EmbedBuilder embed = buildCompleteSummaryEmbed(state);
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 " + messageSource.getMessage("txt_salvar", null, formState.getLocale())),
                    Button.secondary("editar-log", "✏️ " + messageSource.getMessage("txt_alterar", null, formState.getLocale())),
                    Button.primary("voltar-logs", "↩️ " + messageSource.getMessage("txt_voltar", null, formState.getLocale()))
                ))
                .queue();
        }
    }
    private EmbedBuilder buildCompleteSummaryEmbed(FormState state) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 " + messageSource.getMessage("txt_resumo_completo_do_squad_log", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_verifique_todos_os_dados_antes_de_criar_o_log", null, formState.getLocale()) + ":")
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        String startDateText = state.getStartDate() != null ?
            formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📅 " + messageSource.getMessage("txt_data_inicio", null, formState.getLocale()), startDateText, false);
        String endDateText = state.getEndDate() != null && !state.getEndDate().isEmpty() ?
            formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, formState.getLocale()) , endDateText, false);
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
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale()))
                .setDescription( messageSource.getMessage("txt_data_de_inicio_invalida", null, formState.getLocale()) +": `" + startDate + "`\n\n"
                        +  messageSource.getMessage("txt_use_o_formato", null, formState.getLocale()) + " **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 Preencher Novamente"),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
                )
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()) + ": `" + endDate + "`\n\n"
                        + messageSource.getMessage("txt_use_o_formato", null, formState.getLocale()) + " **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
                )
                .queue();
            return;
        }
        if (isEndDateBeforeStartDate(startDate, endDate)) {
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_data_de_fim_invalida", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_data_de_fim_nao_pode_ser_anterior_a_data_de_inicio", null, formState.getLocale()) + ".\n\n" +
                              "**" + messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) +":** " + startDate + "\n" +
                              "**"+messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) +":** " + endDate + "\n\n" +
                                messageSource.getMessage("txt_por_favor_corrija_as_datas", null, formState.getLocale())+".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(
                    Button.primary("retry-field-edit-dates-modal", "🔄 " + messageSource.getMessage("txt_preencher_novamente", null, formState.getLocale())),
                    Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
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
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, formState.getLocale())
                    + ". " + messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale()) + ":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📝 "+ messageSource.getMessage("Customize Toolbar…", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏷️ "+ messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 "+ messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📅 "+ messageSource.getMessage("txt_data_inicio", null, formState.getLocale()), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, formState.getLocale()) , endDate, false);
        event.getHook().editOriginal("")
            .setEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale())),
                    Button.secondary("edit-user", "👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                    Button.secondary("edit-type", "📝 "+ messageSource.getMessage("txt_tipo", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale())),
                    Button.secondary("edit-description", "📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale())),
                    Button.secondary("edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ " + messageSource.getMessage("txt_salvar_alteracoes", null, formState.getLocale())),
                    Button.danger("cancelar-edicao", "❌ " + messageSource.getMessage("txt_cancelar", null, formState.getLocale()))
                )
            )
            .queue();
    }
    private void returnToFieldEditSummaryWithHook(ModalInteractionEvent event, FormState state) {
        log.info("Retornando ao resumo de edição após modal (descrição/datas) via hook");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, formState.getLocale())
                    + ". "+ messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale())+":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📝 "+ messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏷️ "+ messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 "+ messageSource.getMessage("txt_descricao", null, formState.getLocale()) , description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("📅 "+ messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()), endDate, false);
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale())),
                    Button.secondary("edit-user", "👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                    Button.secondary("edit-type", "📝 "+ messageSource.getMessage("txt_tipo", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ "+ messageSource.getMessage("txt_categorias", null, formState.getLocale())),
                    Button.secondary("edit-description", "📄 "+ messageSource.getMessage("txt_descricao", null, formState.getLocale())),
                    Button.secondary("edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ " + messageSource.getMessage("txt_salvar_alteracoes", null, formState.getLocale())),
                    Button.danger("cancelar-edicao", "❌ " + messageSource.getMessage("txt_cancelar", null, formState.getLocale()))
                )
            )
            .queue();
    }
    private void handleRetryCreateModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de criação após erro de data");
        
        TextInput.Builder descriptionBuilder = TextInput.create("description", messageSource.getMessage("txt_descricao", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
            .setPlaceholder(messageSource.getMessage("txt_digite_a_descricao_do_log", null, formState.getLocale()) + "...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();

        TextInput startDateInput = TextInput.create("start_date",  messageSource.getMessage("txt_data_inicio", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true)
            .build();

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_fim", null, formState.getLocale()) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, formState.getLocale()), TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 ("+ messageSource.getMessage("txt_deixe_vazio_se_nao_houver", null, formState.getLocale()) +")")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            endDateBuilder.setValue(state.getEndDate());
        }
        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create("create-complete-modal", "📝 " + messageSource.getMessage("txt_finalizar_criacao_do_log", null, formState.getLocale()) )
            .addActionRow(descriptionInput)
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryEditDatesModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de edição de datas após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String formattedStartDate = formatToBrazilianDate(state.getStartDate());
            if (!formattedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(formattedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim ", null, formState.getLocale()) + " (DD-MM-AAAA) - "
                        + messageSource.getMessage("txt_opcional", null, formState.getLocale()), TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (" + messageSource.getMessage("txt_deixe_vazio_se_nao_houver", null, formState.getLocale()) +")")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }

        Modal modal = Modal.create("modal-edit-dates", "📅 " + messageSource.getMessage("txt_editar_datas", null, formState.getLocale()))
            .addActionRow(startDateBuilder.build())
            .addActionRow(endDateBuilder.build())
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryFieldEditDatesModal(ButtonInteractionEvent event, FormState state) {
        log.info("Reabrindo modal de edição de datas de campo após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String convertedStartDate = convertApiDateToBrazilian(state.getStartDate());
            if (!convertedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(convertedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", messageSource.getMessage("txt_data_de_fim ", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (" + messageSource.getMessage("txt_opcional", null, formState.getLocale()) +")")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String convertedEndDate = convertApiDateToBrazilian(state.getEndDate());
            if (!convertedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(convertedEndDate);
            }
        }

        Modal modal = Modal.create("edit-dates-modal", "📅 "+messageSource.getMessage("txt_editar_datas", null, formState.getLocale() ))
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
