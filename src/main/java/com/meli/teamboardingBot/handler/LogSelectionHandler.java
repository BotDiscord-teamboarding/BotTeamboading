package com.meli.teamboardingBot.handler;

import lombok.extern.slf4j.Slf4j;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(8)
public class LogSelectionHandler extends AbstractInteractionHandler {
    @Autowired
    private SquadLogService squadLogService;
    
    public LogSelectionHandler(FormStateService formStateService) {
        super(formStateService);
    }
    @Override
    public boolean canHandle(String componentId) {
        return "log-select".equals(componentId);
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("log-select".equals(event.getComponentId())) {
            handleLogSelect(event, state);
        }
    }
    private void handleLogSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedLogId = event.getValues().get(0);
        log.info("Log selecionado: {}", selectedLogId);
        try {
            String squadLogsJson = withUserContext(event.getUser().getId(), 
                () -> squadLogService.getSquadLogAll());
            log.info("Resposta completa da API getSquadLogAll: {}", squadLogsJson);
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            if (squadLogsArray != null) {
                for (int i = 0; i < squadLogsArray.length(); i++) {
                    JSONObject logJson = squadLogsArray.getJSONObject(i);
                    if (String.valueOf(logJson.get("id")).equals(selectedLogId)) {
                        log.info("JSON do log selecionado: {}", logJson.toString());
                        loadLogDataIntoState(logJson, state);
                        break;
                    }
                }
            }
            state.setSquadLogId(Long.valueOf(selectedLogId));
            state.setEditing(true);
            state.setCreating(false);
            state.setStep(FormStep.SUMMARY);
            updateFormState(event.getUser().getIdLong(), state);
            log.info("Estado após carregar log para edição: isEditing={}, isCreating={}, squadLogId={}", 
                       state.isEditing(), state.isCreating(), state.getSquadLogId());
            event.deferEdit().queue();
            showUpdateSummaryWithHook(event.getHook(), state);
        } catch (Exception e) {
            log.error("Erro na seleção de log: {}", e.getMessage());
            event.reply("❌ Erro ao carregar dados do questionário.").setEphemeral(true).queue();
        }
    }
    private void loadLogDataIntoState(JSONObject logJson, FormState state) {
        log.info("Carregando dados do log no estado");
        log.info("DEBUG: JSON do log completo: {}", logJson.toString());
        state.setCreating(false);
        state.setEditing(true);
        state.setDescription(logJson.optString("description", ""));
        state.setStartDate(logJson.optString("start_date", ""));
        state.setEndDate(logJson.optString("end_date", null));
        JSONObject squad = logJson.optJSONObject("squad");
        if (squad != null) {
            state.setSquadId(String.valueOf(squad.get("id")));
            state.setSquadName(squad.optString("name", ""));
        }
        JSONObject user = logJson.optJSONObject("user");
        if (user != null) {
            String userId = String.valueOf(user.get("id"));
            String firstName = user.optString("first_name", "");
            String lastName = user.optString("last_name", "");
            String userName = "";
            if (!firstName.isEmpty() && !lastName.isEmpty()) {
                userName = firstName + " " + lastName;
            } else if (!firstName.isEmpty()) {
                userName = firstName;
            } else if (!lastName.isEmpty()) {
                userName = lastName;
            } else {
                userName = user.optString("name", "");
            }
            log.info("Carregando dados do usuário: id={}, firstName={}, lastName={}, fullName={}", 
                       userId, firstName, lastName, userName);
            state.setUserId(userId);
            state.setUserName(userName);
            log.info("DEBUG: Definindo userId={} no estado (squadId={})", userId, state.getSquadId());
        } else {
            log.warn("Objeto 'user' não encontrado no log JSON: {}", logJson.toString());
        }
        JSONObject type = findTypeObject(logJson);
        if (type != null) {
            state.setTypeId(String.valueOf(type.get("id")));
            state.setTypeName(type.optString("name", ""));
        }
        JSONArray categories = findCategoriesArray(logJson);
        state.getCategoryIds().clear();
        state.getCategoryNames().clear();
        if (categories != null) {
            for (int j = 0; j < categories.length(); j++) {
                JSONObject category = categories.getJSONObject(j);
                state.getCategoryIds().add(String.valueOf(category.get("id")));
                state.getCategoryNames().add(category.optString("name", ""));
            }
        }
        log.info("Estado carregado: squadId={}, squadName={}, userId={}, userName={}, typeId={}, typeName={}",
                   state.getSquadId(), state.getSquadName(), state.getUserId(), state.getUserName(), 
                   state.getTypeId(), state.getTypeName());
    }
    private JSONObject findTypeObject(JSONObject logJson) {
        JSONObject type = logJson.optJSONObject("type");
        if (type == null) {
            type = logJson.optJSONObject("squad_log_type");
        }
        if (type == null) {
            type = logJson.optJSONObject("log_type");
        }
        return type;
    }
    private JSONArray findCategoriesArray(JSONObject logJson) {
        JSONArray categories = logJson.optJSONArray("categories");
        if (categories == null) {
            categories = logJson.optJSONArray("skill_categories");
        }
        if (categories == null) {
            categories = logJson.optJSONArray("squad_categories");
        }
        return categories;
    }
    public void showLogSelection(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        try {
            String squadLogsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadLogAll());
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.editMessage("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            StringSelectMenu.Builder logMenuBuilder = StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um questionário");
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);
            event.editMessageEmbeds(embed.build())
                .setActionRow(logMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar questionários: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    public void showLogSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, String userId) {
        try {
            String squadLogsJson = withUserContext(userId, () -> squadLogService.getSquadLogAll());
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                hook.editOriginal("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            StringSelectMenu.Builder logMenuBuilder = StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um questionário");
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);
            hook.editOriginalEmbeds(embed.build())
                .setActionRow(logMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar questionários: {}", e.getMessage());
            hook.editOriginal("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    private void buildLogSelectMenu(JSONArray squadLogsArray, StringSelectMenu.Builder logMenuBuilder) {
        for (int i = 0; i < squadLogsArray.length(); i++) {
            JSONObject log = squadLogsArray.getJSONObject(i);
            String logId = String.valueOf(log.get("id"));
            String squadName = "";
            JSONObject squad = log.optJSONObject("squad");
            if (squad != null) {
                squadName = squad.optString("name", "");
            }
            String userName = "";
            JSONObject user = log.optJSONObject("user");
            if (user != null) {
                userName = user.optString("name", "");
            }
            String description = log.optString("description", "");
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            String optionLabel = String.format("%s - %s: %s", squadName, userName, description);
            if (optionLabel.length() > 100) {
                optionLabel = optionLabel.substring(0, 97) + "...";
            }
            logMenuBuilder.addOption(optionLabel, logId);
        }
    }
    private void showUpdateSummary(StringSelectInteractionEvent event, FormState state) {
        log.info("Mostrando resumo para edição do squad log ID: {}", state.getSquadLogId());
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
    private void showUpdateSummaryWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, FormState state) {
        log.info("Mostrando resumo para edição do squad log ID: {}", state.getSquadLogId());
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
        hook.editOriginal("")
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
    @Override
    public int getPriority() {
        return 8;
    }
}
