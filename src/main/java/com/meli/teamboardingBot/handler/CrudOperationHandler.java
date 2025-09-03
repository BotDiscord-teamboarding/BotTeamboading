package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class CrudOperationHandler extends AbstractInteractionHandler {
    
    @Autowired
    private SquadLogService squadLogService;
    
    @Override
    public boolean canHandle(String componentId) {
        return "criar-log".equals(componentId) ||
               "confirmar-criacao".equals(componentId) ||
               "confirmar-atualizacao".equals(componentId) ||
               "criar-novo-log".equals(componentId) ||
               "atualizar-log-existente".equals(componentId) ||
               "atualizar".equals(componentId);
    }
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        
        if ("criar-log".equals(buttonId) || "confirmar-criacao".equals(buttonId)) {
            if (state.isCreating()) {
                handleCreateSquadLog(event, state);
            } else {
                handleUpdateSquadLog(event, state);
            }
        } else if ("confirmar-atualizacao".equals(buttonId)) {
            handleUpdateSquadLog(event, state);
        } else if ("criar-novo-log".equals(buttonId)) {
            handleCreateNewLog(event);
        } else if ("atualizar-log-existente".equals(buttonId) || "atualizar".equals(buttonId)) {
            handleUpdateExistingLog(event);
        }
    }
    
    private void handleCreateSquadLog(ButtonInteractionEvent event, FormState state) {
        logger.info("Criando squad log");
        
        // Defer imediatamente para evitar timeout
        event.deferEdit().queue();
        
        if (!isStateValid(state)) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Dados Incompletos")
                .setDescription("Verifique se todos os campos foram preenchidos.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
            return;
        }
        
        try {
            String payload = buildCreatePayload(state);
            logger.info("Payload de criação: {}", payload);
            
            ResponseEntity<String> response = squadLogService.createSquadLog(payload);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ Squad Log criado com sucesso!", true);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ Erro ao criar Squad Log. Código: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao criar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ Erro interno ao criar Squad Log. Tente novamente.");
        }
    }
    
    private void handleUpdateSquadLog(ButtonInteractionEvent event, FormState state) {
        logger.info("Atualizando squad log ID: {}", state.getSquadLogId());
        
        // Defer imediatamente para evitar timeout
        event.deferEdit().queue();
        
        if (!isStateValid(state) || state.getSquadLogId() == null) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Dados Incompletos")
                .setDescription("Dados incompletos ou ID do log não encontrado.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
            return;
        }
        
        try {
            String payload = buildUpdatePayload(state);
            logger.info("Payload de atualização para squad-log ID {}: {}", state.getSquadLogId(), payload);
            logger.info("Estado atual: squadId={}, userId={}, typeId={}, categoryIds={}, startDate={}, endDate={}", 
                       state.getSquadId(), state.getUserId(), state.getTypeId(), 
                       state.getCategoryIds(), state.getStartDate(), state.getEndDate());
            
            ResponseEntity<String> response = squadLogService.updateSquadLog(state.getSquadLogId(), payload);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ Squad Log atualizado com sucesso!", false);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ Erro ao atualizar Squad Log. Código: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao atualizar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ Erro interno ao atualizar Squad Log. Tente novamente.");
        }
    }
    
    private boolean isStateValid(FormState state) {
        return state.getSquadId() != null &&
               state.getUserId() != null &&
               state.getTypeId() != null &&
               !state.getCategoryIds().isEmpty() &&
               state.getDescription() != null &&
               state.getStartDate() != null;
    }
    
    private String buildCreatePayload(FormState state) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(state.getSquadId()));
        payload.put("user_id", Integer.parseInt(state.getUserId()));
        payload.put("squad_log_type_id", Integer.parseInt(state.getTypeId()));
        payload.put("description", state.getDescription());
        payload.put("start_date", convertToApiDateFormat(state.getStartDate()));
        
        if (state.getEndDate() != null && !state.getEndDate().isEmpty()) {
            payload.put("end_date", convertToApiDateFormat(state.getEndDate()));
        }
        
        payload.put("skill_categories", state.getCategoryIds().stream()
            .mapToInt(Integer::parseInt)
            .toArray());
        
        return payload.toString();
    }
    
    private String buildUpdatePayload(FormState state) {

        logger.info("DEBUG buildUpdatePayload: squadId={}, userId={}, typeId={}, categoryIds={}",
                   state.getSquadId(), state.getUserId(), state.getTypeId(), state.getCategoryIds());
        
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(state.getSquadId()));
        payload.put("user_id", Integer.parseInt(state.getUserId()));
        payload.put("squad_log_type_id", Integer.parseInt(state.getTypeId()));
        payload.put("description", state.getDescription());
        payload.put("start_date", convertToApiDateFormat(state.getStartDate()));
        
        if (state.getEndDate() != null && !state.getEndDate().isEmpty()) {
            payload.put("end_date", convertToApiDateFormat(state.getEndDate()));
        }
        
        payload.put("skill_categories", state.getCategoryIds().stream()
            .mapToInt(Integer::parseInt)
            .toArray());
        
        logger.info("Update payload (same as create): {}", payload.toString());
        return payload.toString();
    }
    
    private String convertToApiDateFormat(String inputDate) {
        if (inputDate == null || inputDate.isEmpty()) {
            logger.warn("Data de entrada é null ou vazia");
            return null;
        }
        
        logger.info("Convertendo data '{}' para formato API", inputDate);
        
        if (inputDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            logger.info("Data já está no formato API: '{}'", inputDate);
            return inputDate;
        }
        
        if (inputDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = inputDate.split("-");
            String apiDate = parts[2] + "-" + parts[1] + "-" + parts[0];
            logger.info("Data convertida de DD-MM-YYYY para API: '{}' -> '{}'", inputDate, apiDate);
            return apiDate;
        }
        
        logger.warn("Formato de data não reconhecido: '{}' - retornando como está", inputDate);
        return inputDate;
    }
    
    private void showSuccessMessage(ButtonInteractionEvent event, String message, boolean isCreation) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription(isCreation ? 
                "O Squad Log foi criado com sucesso! O que deseja fazer agora?" :
                "O Squad Log foi atualizado com sucesso! O que deseja fazer agora?")
            .setColor(0x00FF00);
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("criar-novo-log", "🆕 Criar Novo Squad-Log"),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("atualizar-log-existente", "📝 Atualizar Squad-Log Existente")
            )
            .queue();
    }
    
    private void showSuccessMessageWithHook(ButtonInteractionEvent event, String message, boolean isCreation) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription(isCreation ? 
                "O Squad Log foi criado com sucesso! O que deseja fazer agora?" :
                "O Squad Log foi atualizado com sucesso! O que deseja fazer agora?")
            .setColor(0x00FF00);
        
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("criar-novo-log", "🆕 Criar Novo Squad-Log"),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("atualizar-log-existente", "📝 Atualizar Squad-Log Existente")
            )
            .queue();
    }
    
    private void handleCreateNewLog(ButtonInteractionEvent event) {
        logger.info("Iniciando criação de novo squad log");
        
        FormState newState = new FormState();
        newState.setCreating(true);
        newState.setEditing(false);
        newState.setStep(FormStep.SQUAD_SELECTION);
        
        formStateService.updateState(event.getUser().getIdLong(), newState);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🆕 Criar Novo Squad Log")
            .setDescription("Vamos começar um novo Squad Log. Primeiro, selecione uma squad:")
            .setColor(0x0099FF);
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("criar", "Selecionar Squad")
            )
            .queue();
    }
    
    private void handleUpdateExistingLog(ButtonInteractionEvent event) {
        logger.info("Iniciando atualização de squad log existente");
        
        FormState newState = new FormState();
        newState.setCreating(false);
        newState.setEditing(true);
        newState.setStep(FormStep.LOG_SELECTION);
        
        formStateService.updateState(event.getUser().getIdLong(), newState);
        
        event.deferEdit().queue();
        
        try {
            logger.info("Carregando lista de squad logs...");
            String squadLogsJson = squadLogService.getSquadLogAll();
            logger.info("Resposta da API getSquadLogAll: {}", squadLogsJson);
            org.json.JSONObject obj = new org.json.JSONObject(squadLogsJson);
            org.json.JSONArray squadLogsArray = obj.optJSONArray("items");
            
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhum Squad Log encontrado para atualização.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder logMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um Squad Log para atualizar");
            
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Atualizar Squad Log Existente")
                .setDescription("Escolha o Squad Log que deseja atualizar:")
                .setColor(0xFFAA00);
            
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(logMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar Squad Logs: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ Erro ao carregar Squad Logs: " + e.getMessage())
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    
    private void buildLogSelectMenu(org.json.JSONArray squadLogsArray, net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder logMenuBuilder) {
        for (int i = 0; i < squadLogsArray.length(); i++) {
            org.json.JSONObject log = squadLogsArray.getJSONObject(i);
            String logId = String.valueOf(log.get("id"));
            
            String squadName = "";
            org.json.JSONObject squad = log.optJSONObject("squad");
            if (squad != null) {
                squadName = squad.optString("name", "");
            }
            
            String userName = "";
            org.json.JSONObject user = log.optJSONObject("user");
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
    
    private void showErrorMessage(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription("Tente novamente ou entre em contato com o suporte.")
            .setColor(0xFF0000);
        
        event.editMessageEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    
    private void showErrorMessageWithHook(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription("Tente novamente ou entre em contato com o suporte.")
            .setColor(0xFF0000);
        
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    
    @Override
    public int getPriority() {
        return 6;
    }
}
