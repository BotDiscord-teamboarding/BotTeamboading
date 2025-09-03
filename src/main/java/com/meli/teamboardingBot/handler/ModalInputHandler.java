package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
@Component
@Order(5)
public class ModalInputHandler extends AbstractInteractionHandler {
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
    private void handleEditDescriptionButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando descrição");
        TextInput.Builder descriptionBuilder = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a descrição do log...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();
        Modal modal = Modal.create("modal-edit-description", "📝 Editar Descrição")
            .addActionRow(descriptionInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleEditDatesButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando datas");
        TextInput.Builder startDateBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
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
        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (deixe vazio se não houver)")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }
        TextInput endDateInput = endDateBuilder.build();
        Modal modal = Modal.create("modal-edit-dates", "📅 Editar Datas")
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleCreateCompleteModal(ModalInteractionEvent event, FormState state) {
        logger.info("Processando modal de criação completa");
        String description = event.getValue("description").getAsString();
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (!isValidDate(startDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setDescription(description);
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Início Inválida")
                .setDescription("Data de início inválida: `" + startDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-create-modal", "🔄 Preencher Novamente"))
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setDescription(description);
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Fim Inválida")
                .setDescription("Data de fim inválida: `" + endDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-create-modal", "🔄 Preencher Novamente"))
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
        logger.info("Processando edição de descrição");
        String description = event.getValue("description").getAsString();
        state.setDescription(description);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        showSummary(event, state);
    }
    private void handleEditDatesModal(ModalInteractionEvent event, FormState state) {
        logger.info("Processando edição de datas");
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (!isValidDate(startDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Início Inválida")
                .setDescription("Data de início inválida: `" + startDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-edit-dates-modal", "🔄 Preencher Novamente"))
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Fim Inválida")
                .setDescription("Data de fim inválida: `" + endDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-edit-dates-modal", "🔄 Preencher Novamente"))
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
                logger.warn("Data com formato inválido (não tem 3 partes): {}", dateStr);
                return false;
            }
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            logger.info("Validando data: dateStr={}, day={}, month={}, year={}", dateStr, day, month, year);
            if (year < 1900 || year > 2100) {
                logger.warn("Ano inválido: {}", year);
                return false;
            }
            if (month < 1 || month > 12) {
                logger.warn("Mês inválido: {}", month);
                return false;
            }
            if (day < 1 || day > 31) {
                logger.warn("Dia inválido: {}", day);
                return false;
            }
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeParseException e) {
            logger.error("Erro ao criar LocalDate: {}", e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            logger.error("Erro ao converter string para número na data {}: {}", dateStr, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Erro inesperado ao validar data {}: {}", dateStr, e.getMessage());
            return false;
        }
    }
    private void showCreateSummary(ModalInteractionEvent event, FormState state) {
        logger.info("Mostrando resumo após preenchimento do modal");
        net.dv8tion.jda.api.EmbedBuilder embed = buildCompleteSummaryEmbed(state);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("criar-log", "✅ Criar Log"),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("editar-log", "✏️ Editar")
            )
            .queue();
    }
    private void showSummary(ModalInteractionEvent event, FormState state) {
        logger.info("Mostrando resumo após edição via modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            EmbedBuilder embed = buildCompleteSummaryEmbed(state);
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 Salvar"),
                    Button.secondary("editar-log", "✏️ Alterar"),
                    Button.primary("voltar-logs", "↩️ Voltar")
                ))
                .queue();
        }
    }
    private EmbedBuilder buildCompleteSummaryEmbed(FormState state) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 Resumo Completo do Squad Log")
            .setDescription("Verifique todos os dados antes de criar o log:")
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : "Não informado";
        embed.addField("🏢 Squad", squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : "Não informado";
        embed.addField("👤 Pessoa", userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : "Não informado";
        embed.addField("📝 Tipo", typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : "Não informado";
        embed.addField("🏷️ Categorias", categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : "Não informado";
        embed.addField("📄 Descrição", description, false);
        String startDateText = state.getStartDate() != null ?
            formatToBrazilianDate(state.getStartDate()) : "Não informado";
        embed.addField("📅 Data de Início", startDateText, false);
        String endDateText = state.getEndDate() != null && !state.getEndDate().isEmpty() ?
            formatToBrazilianDate(state.getEndDate()) : "Não informada";
        embed.addField("📅 Data de Fim", endDateText, false);
        return embed;
    }
    private void handleFieldEditDescriptionModal(ModalInteractionEvent event, FormState state) {
        logger.info("Processando edição de descrição via modal de campo");
        String description = event.getValue("description").getAsString();
        state.setDescription(description);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        returnToFieldEditSummaryWithHook(event, state);
    }
    private void handleFieldEditDatesModal(ModalInteractionEvent event, FormState state) {
        logger.info("Processando edição de datas via modal de campo");
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString() : null;
        if (endDate != null && endDate.trim().isEmpty()) {
            endDate = null;
        }
        if (!isValidDate(startDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setEndDate(endDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Início Inválida")
                .setDescription("Data de início inválida: `" + startDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 20-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-field-edit-dates-modal", "🔄 Preencher Novamente"))
                .queue();
            return;
        }
        if (endDate != null && !endDate.isEmpty() && !isValidDate(endDate)) {
            // Salvar dados temporariamente para preservar no retry
            state.setStartDate(startDate);
            updateFormState(event.getUser().getIdLong(), state);
            
            event.deferEdit().queue();
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Data de Fim Inválida")
                .setDescription("Data de fim inválida: `" + endDate + "`\n\nUse o formato **DD-MM-AAAA** (ex: 25-06-1986)")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("retry-field-edit-dates-modal", "🔄 Preencher Novamente"))
                .queue();
            return;
        }
        logger.info("Atualizando datas no estado: startDate={}, endDate={}", startDate, endDate);
        state.setStartDate(startDate);
        state.setEndDate(endDate);
        updateFormState(event.getUser().getIdLong(), state);
        logger.info("Estado atualizado. Novas datas: startDate={}, endDate={}", state.getStartDate(), state.getEndDate());
        event.deferEdit().queue();
        returnToFieldEditSummaryWithHook(event, state);
    }
    private void returnToFieldEditSummary(ModalInteractionEvent event, FormState state) {
        logger.info("Retornando ao resumo de edição após modal (descrição/datas)");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Editar Squad Log")
            .setDescription("Dados atuais do Squad Log. Selecione o campo que deseja editar:")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : "Não informado";
        embed.addField("🏢 Squad", squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : "Não informado";
        embed.addField("👤 Pessoa", userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : "Não informado";
        embed.addField("📝 Tipo", typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : "Não informado";
        embed.addField("🏷️ Categorias", categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : "Não informado";
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 Descrição", description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : "Não informado";
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : "Não informado";
        embed.addField("📅 Data Início", startDate, false);
        embed.addField("📅 Data Fim", endDate, false);
        event.getHook().editOriginal("")
            .setEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-user", "👤 Pessoa"),
                    Button.secondary("edit-type", "📝 Tipo")
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ Categorias"),
                    Button.secondary("edit-description", "📄 Descrição"),
                    Button.secondary("edit-dates", "📅 Datas")
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ Salvar Alterações"),
                    Button.danger("cancelar-edicao", "❌ Cancelar")
                )
            )
            .queue();
    }
    private void returnToFieldEditSummaryWithHook(ModalInteractionEvent event, FormState state) {
        logger.info("Retornando ao resumo de edição após modal (descrição/datas) via hook");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Editar Squad Log")
            .setDescription("Dados atuais do Squad Log. Selecione o campo que deseja editar:")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : "Não informado";
        embed.addField("🏢 Squad", squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : "Não informado";
        embed.addField("👤 Pessoa", userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : "Não informado";
        embed.addField("📝 Tipo", typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : "Não informado";
        embed.addField("🏷️ Categorias", categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : "Não informado";
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 Descrição", description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : "Não informado";
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : "Não informado";
        embed.addField("📅 Data Início", startDate, false);
        embed.addField("📅 Data Fim", endDate, false);
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-user", "👤 Pessoa"),
                    Button.secondary("edit-type", "📝 Tipo")
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ Categorias"),
                    Button.secondary("edit-description", "📄 Descrição"),
                    Button.secondary("edit-dates", "📅 Datas")
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ Salvar Alterações"),
                    Button.danger("cancelar-edicao", "❌ Cancelar")
                )
            )
            .queue();
    }
    private void handleRetryCreateModal(ButtonInteractionEvent event, FormState state) {
        logger.info("Reabrindo modal de criação após erro de data");
        
        TextInput.Builder descriptionBuilder = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a descrição do log...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();

        TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true)
            .build();

        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (deixe vazio se não houver)")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            endDateBuilder.setValue(state.getEndDate());
        }
        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create("create-complete-modal", "📝 Finalizar Criação do Log")
            .addActionRow(descriptionInput)
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryEditDatesModal(ButtonInteractionEvent event, FormState state) {
        logger.info("Reabrindo modal de edição de datas após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String formattedStartDate = formatToBrazilianDate(state.getStartDate());
            if (!formattedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(formattedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (deixe vazio se não houver)")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String formattedEndDate = formatToBrazilianDate(state.getEndDate());
            if (!formattedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(formattedEndDate);
            }
        }

        Modal modal = Modal.create("modal-edit-dates", "📅 Editar Datas")
            .addActionRow(startDateBuilder.build())
            .addActionRow(endDateBuilder.build())
            .build();

        event.replyModal(modal).queue();
    }

    private void handleRetryFieldEditDatesModal(ButtonInteractionEvent event, FormState state) {
        logger.info("Reabrindo modal de edição de datas de campo após erro");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String convertedStartDate = convertApiDateToBrazilian(state.getStartDate());
            if (!convertedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(convertedStartDate);
            }
        }

        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (opcional)")
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String convertedEndDate = convertApiDateToBrazilian(state.getEndDate());
            if (!convertedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(convertedEndDate);
            }
        }

        Modal modal = Modal.create("edit-dates-modal", "📅 Editar Datas")
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
            logger.warn("Não foi possível converter a data da API: {}", apiDate);
            return apiDate;
        }
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
