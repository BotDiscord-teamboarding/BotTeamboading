package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(9)
public class FieldEditHandler extends AbstractInteractionHandler {
    
    @Autowired
    private SquadLogService squadLogService;
    
    @Override
    public boolean canHandle(String componentId) {
        return "edit-squad".equals(componentId) ||
               "edit-user".equals(componentId) ||
               "edit-type".equals(componentId) ||
               "edit-categories".equals(componentId) ||
               "edit-description".equals(componentId) ||
               "edit-dates".equals(componentId) ||
               "cancelar-edicao".equals(componentId) ||
               "edit-squad-select".equals(componentId) ||
               "edit-user-select".equals(componentId) ||
               "edit-type-select".equals(componentId) ||
               "edit-categories-select".equals(componentId);
    }
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "edit-squad":
                handleEditSquad(event, state);
                break;
            case "edit-user":
                handleEditUser(event, state);
                break;
            case "edit-type":
                handleEditType(event, state);
                break;
            case "edit-categories":
                handleEditCategories(event, state);
                break;
            case "edit-description":
                handleEditDescription(event, state);
                break;
            case "edit-dates":
                handleEditDates(event, state);
                break;
            case "cancelar-edicao":
                handleCancelEdit(event);
                break;
        }
    }
    
    @Override
    public void handleStringSelect(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectId = event.getComponentId();
        
        switch (selectId) {
            case "edit-squad-select":
                handleSquadSelection(event, state);
                break;
            case "edit-user-select":
                handleUserSelection(event, state);
                break;
            case "edit-type-select":
                handleTypeSelection(event, state);
                break;
            case "edit-categories-select":
                handleCategoriesSelection(event, state);
                break;
        }
    }
    
    private void handleSquadSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectedSquadId = event.getValues().get(0);
        String selectedSquadName = event.getSelectedOptions().get(0).getLabel();
        
        logger.info("Squad selecionada para edição: {} - {}", selectedSquadId, selectedSquadName);
        logger.info("Estado atual: isEditing={}, isCreating={}, step={}", state.isEditing(), state.isCreating(), state.getStep());
        
        state.setSquadId(selectedSquadId);
        state.setSquadName(selectedSquadName);
        updateFormState(event.getUser().getIdLong(), state);
        
        event.deferEdit().queue();
        returnToEditSummary(event, state);
    }
    
    private void handleUserSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectedUserId = event.getValues().get(0);
        String selectedUserName = event.getSelectedOptions().get(0).getLabel();
        
        logger.info("Usuário selecionado para edição: {} - {}", selectedUserId, selectedUserName);
        
        state.setUserId(selectedUserId);
        state.setUserName(selectedUserName);
        updateFormState(event.getUser().getIdLong(), state);
        
        event.deferEdit().queue();
        returnToEditSummary(event, state);
    }
    
    private void handleTypeSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectedTypeId = event.getValues().get(0);
        String selectedTypeName = event.getSelectedOptions().get(0).getLabel();
        
        logger.info("Tipo selecionado para edição: {} - {}", selectedTypeId, selectedTypeName);
        
        state.setTypeId(selectedTypeId);
        state.setTypeName(selectedTypeName);
        updateFormState(event.getUser().getIdLong(), state);
        
        event.deferEdit().queue();
        returnToEditSummary(event, state);
    }
    
    private void handleCategoriesSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        logger.info("Categorias selecionadas para edição: {}", event.getValues());
        
        state.getCategoryIds().clear();
        state.getCategoryNames().clear();
        
        for (int i = 0; i < event.getValues().size(); i++) {
            String categoryId = event.getValues().get(i);
            String categoryName = event.getSelectedOptions().get(i).getLabel();
            state.getCategoryIds().add(categoryId);
            state.getCategoryNames().add(categoryName);
        }
        
        updateFormState(event.getUser().getIdLong(), state);
        
        event.deferEdit().queue();
        returnToEditSummary(event, state);
    }
    
    private void returnToEditSummary(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        logger.info("Retornando ao resumo de edição após alteração");
        
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
        
        String startDate = state.getStartDate() != null ? state.getStartDate() : "Não informado";
        String endDate = state.getEndDate() != null ? state.getEndDate() : "Não informado";
        embed.addField("📅 Data Início", startDate, true);
        embed.addField("📅 Data Fim", endDate, true);
        
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
    
    private void handleEditSquad(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando squad do log");
        
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray == null || squadsArray.length() == 0) {
                event.reply("❌ Nenhuma squad encontrada.").setEphemeral(true).queue();
                return;
            }
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder squadMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-squad-select")
                    .setPlaceholder("Selecione uma nova squad");
            
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                String squadId = String.valueOf(squad.get("id"));
                String squadName = squad.optString("name", "");
                squadMenuBuilder.addOption(squadName, squadId);
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏢 Editar Squad")
                .setDescription("Selecione a nova squad:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar squads: {}", e.getMessage());
            event.reply("❌ Erro ao carregar squads.").setEphemeral(true).queue();
        }
    }
    
    private void handleEditUser(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando usuário do log");
        
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder userMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-user-select")
                    .setPlaceholder("Selecione um novo usuário");
            
            boolean hasUsers = false;
            
            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    JSONArray userSquads = squad.optJSONArray("user_squads");
                    
                    if (userSquads != null) {
                        for (int j = 0; j < userSquads.length(); j++) {
                            JSONObject userSquad = userSquads.getJSONObject(j);
                            JSONObject user = userSquad.optJSONObject("user");
                            
                            if (user != null) {
                                String userId = String.valueOf(user.get("id"));
                                String userName = user.optString("name", "");
                                userMenuBuilder.addOption(userName, userId);
                                hasUsers = true;
                            }
                        }
                    }
                }
            }
            
            if (!hasUsers) {
                event.reply("❌ Nenhum usuário encontrado.").setEphemeral(true).queue();
                return;
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Editar Pessoa")
                .setDescription("Selecione a nova pessoa:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(userMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar usuários: {}", e.getMessage());
            event.reply("❌ Erro ao carregar usuários.").setEphemeral(true).queue();
        }
    }
    
    private void handleEditType(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando tipo do log");
        
        try {
            String typesJson = squadLogService.getSquadLogTypes();
            logger.info("Resposta da API de tipos: {}", typesJson);
            
            if (typesJson == null || typesJson.trim().isEmpty()) {
                logger.error("API retornou resposta vazia para tipos");
                event.reply("❌ Erro: API retornou resposta vazia para tipos.").setEphemeral(true).queue();
                return;
            }
            
            JSONArray typesArray;
            try {
                typesArray = new JSONArray(typesJson);
            } catch (JSONException e) {
                logger.error("API retornou JSON inválido para tipos. Resposta: {}", typesJson);
                event.reply("❌ Erro: API retornou formato JSON inválido para tipos.").setEphemeral(true).queue();
                return;
            }
            
            if (typesArray == null || typesArray.length() == 0) {
                event.reply("❌ Nenhum tipo encontrado.").setEphemeral(true).queue();
                return;
            }
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder typeMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-type-select")
                    .setPlaceholder("Selecione um novo tipo");
            
            for (int i = 0; i < typesArray.length(); i++) {
                JSONObject type = typesArray.getJSONObject(i);
                String typeId = String.valueOf(type.get("id"));
                String typeName = type.optString("name", "");
                typeMenuBuilder.addOption(typeName, typeId);
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Editar Tipo")
                .setDescription("Selecione o novo tipo:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(typeMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar tipos: {}", e.getMessage(), e);
            event.reply("❌ Erro ao carregar tipos: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
    
    private void handleEditCategories(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando categorias do log");
        
        event.deferEdit().queue();
        
        try {
            String categoriesJson = squadLogService.getSquadCategories();
            logger.info("Resposta da API de categorias: {}", categoriesJson);
            
            if (categoriesJson == null || categoriesJson.trim().isEmpty()) {
                logger.error("API retornou resposta vazia para categorias");
                event.getHook().editOriginal("❌ Erro: API retornou resposta vazia para categorias.").queue();
                return;
            }
            
            JSONArray categoriesArray;
            try {
                categoriesArray = new JSONArray(categoriesJson);
            } catch (JSONException e) {
                logger.error("API retornou JSON inválido para categorias. Resposta: {}", categoriesJson);
                event.getHook().editOriginal("❌ Erro: API retornou formato JSON inválido para categorias.").queue();
                return;
            }
            
            if (categoriesArray == null || categoriesArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhuma categoria encontrada.").queue();
                return;
            }
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder categoryMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-categories-select")
                    .setPlaceholder("Selecione as novas categorias");
            
            int validCategoryCount = 0;
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String categoryId = String.valueOf(category.get("id"));
                String categoryName = category.optString("name", "");
                
                if (categoryName == null || categoryName.trim().isEmpty()) {
                    logger.warn("Categoria com ID {} tem nome vazio, pulando...", categoryId);
                    continue;
                }
                
                categoryMenuBuilder.addOption(categoryName, categoryId);
                validCategoryCount++;
            }
            
            if (validCategoryCount == 0) {
                event.getHook().editOriginal("❌ Nenhuma categoria válida encontrada.").queue();
                return;
            }
            
            categoryMenuBuilder.setRequiredRange(1, Math.min(25, validCategoryCount));
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏷️ Editar Categorias")
                .setDescription("Selecione as novas categorias (pode selecionar múltiplas):")
                .setColor(0x0099FF);
            
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(categoryMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias: {}", e.getMessage());
            event.getHook().editOriginal("❌ Erro ao carregar categorias.").queue();
        }
    }
    
    private void handleEditDescription(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando descrição do log");
        
        TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a nova descrição do log...")
            .setValue(state.getDescription() != null ? state.getDescription() : "")
            .setMaxLength(1000)
            .setRequired(true)
            .build();
        
        Modal modal = Modal.create("edit-description-modal", "📄 Editar Descrição")
            .addActionRow(descriptionInput)
            .build();
        
        event.replyModal(modal).queue();
    }
    
    private void handleEditDates(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando datas do log");
        
        TextInput.Builder startDateBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);
        
        if (state.getStartDate() != null) {
            startDateBuilder.setValue(convertApiDateToBrazilian(state.getStartDate()));
        }
        
        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (opcional)")
            .setMaxLength(10)
            .setRequired(false);
        
        if (state.getEndDate() != null) {
            endDateBuilder.setValue(convertApiDateToBrazilian(state.getEndDate()));
        }
        
        Modal modal = Modal.create("edit-dates-modal", "📅 Editar Datas")
            .addActionRow(startDateBuilder.build())
            .addActionRow(endDateBuilder.build())
            .build();
        
        event.replyModal(modal).queue();
    }
    
    /**
     * Converts API date format (YYYY-MM-DD) to Brazilian format (DD-MM-YYYY)
     */
    private String convertApiDateToBrazilian(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return "";
        }
        
        if (apiDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return apiDate;
        }
        
        if (apiDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = apiDate.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0]; // DD-MM-YYYY
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
    
    private void handleCancelEdit(ButtonInteractionEvent event) {
        logger.info("Cancelando edição do log");
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("❌ Edição Cancelada")
            .setDescription("A edição do Squad Log foi cancelada.")
            .setColor(0xFF0000);
        
        event.editMessageEmbeds(embed.build())
            .setComponents()
            .queue();
        
        formStateService.removeState(event.getUser().getIdLong());
    }
    
    @Override
    public int getPriority() {
        return 9;
    }
}
