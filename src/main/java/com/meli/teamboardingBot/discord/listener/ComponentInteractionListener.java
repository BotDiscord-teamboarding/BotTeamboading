package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ComponentInteractionListener extends ListenerAdapter {

    @Autowired
    private SquadLogService squadLogService;

    private final Map<Long, FormState> userFormState = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(ComponentInteractionListener.class);

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        long discordUserId = event.getUser().getIdLong();
        logger.info("[BUTTON_INTERACTION] Usuário: {} | Botão: {} | Guild: {}", 
                   discordUserId, buttonId, event.getGuild() != null ? event.getGuild().getId() : "DM");

        if (buttonId.equals("criar")) {
            logger.info("[CRIAR_SQUAD] Iniciando processo de criação para usuário: {}", discordUserId);
            event.deferReply().setEphemeral(true).queue(interaction -> {
                logger.debug("[CRIAR_SQUAD] Buscando lista de squads para usuário: {}", discordUserId);
                String squadsJson = squadLogService.getSquads();
                logger.debug("[CRIAR_SQUAD] Resposta da API de squads: {}", squadsJson);
                JSONArray squadsArray;
                if (!squadsJson.trim().startsWith("[")) {
                    JSONObject obj = new JSONObject(squadsJson);
                    squadsArray = obj.optJSONArray("items");
                } else {
                    squadsArray = new JSONArray(squadsJson);
                }

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("squad-select")
                        .setPlaceholder("Selecione uma Squad");
                buildSelectMenu(squadsArray, menuBuilder);

                logger.info("[CRIAR_SQUAD] Enviando menu de seleção de squads para usuário: {} | Total squads: {}", 
                           discordUserId, squadsArray.length());
                interaction.editOriginal("Selecione uma Squad:")
                        .setComponents(ActionRow.of(menuBuilder.build()))
                        .queue();
            });
        } else if (buttonId.equals("criar-log")) {
            logger.info("[CRIAR_LOG] Iniciando criação do squad log para usuário: {}", discordUserId);

            FormState state = userFormState.get(discordUserId);
            if (state != null) {
                logger.info("[CRIAR_LOG] FormState encontrado para usuário: {} | Squad: {} | Tipo: {}", 
                           discordUserId, state.squadName, state.typeName);
                logger.debug("[CRIAR_LOG] FormState completo: {}", formatFormState(state));

                String payload = buildSquadLogPayload(
                        state.squadId,
                        state.userId,
                        state.typeId,
                        state.categoryIds,
                        state.description,
                        state.startDate,
                        state.endDate
                );
                logger.info("Payload montado para criação do Squad Log: {}", payload);

                logger.info("[CRIAR_LOG] Chamando API createSquadLog para usuário: {} | Payload: {}", discordUserId, payload);
                ResponseEntity<String> response = squadLogService.createSquadLog(payload);
                logger.info("[CRIAR_LOG] Resposta da API para usuário: {} | Status: {} | Body: {}", 
                           discordUserId, response.getStatusCode(), response.getBody());

                EmbedBuilder embedBuilder = new EmbedBuilder();
                if(response.getStatusCode() == HttpStatus.OK) {
                    embedBuilder.setColor(0x00FF00);
                    embedBuilder.setDescription("Squad Log criado com sucesso! \nStatus Code da API: " + response.getStatusCode());
                } else {
                    embedBuilder.setColor(0xFF0000);
                    embedBuilder.setDescription("Falha ao criar Squad Log.\nStatus Code da API: " + response.getStatusCode());
                }
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                userFormState.remove(discordUserId);
            } else {
                logger.warn("[CRIAR_LOG] FormState não encontrado para usuário: {} | Estados ativos: {}", 
                           discordUserId, userFormState.keySet());
                event.reply("Formulário não encontrado ou expirado.").setEphemeral(true).queue();
            }
        } else if (buttonId.equals("alterar-log")) {
            logger.info("[ALTERAR_LOG] Usuário solicitou alteração: {}", discordUserId);
            FormState state = userFormState.get(discordUserId);
            
            if (state != null) {
                logger.info("[ALTERAR_LOG] Mostrando botões de modificação para usuário: {}", discordUserId);
                showFieldModificationButtons(event, state);
            } else {
                logger.warn("[ALTERAR_LOG] FormState não encontrado para usuário: {}", discordUserId);
                event.reply("Formulário não encontrado ou expirado.").setEphemeral(true).queue();
            }
        } else if (buttonId.startsWith("modify-")) {
            logger.info("[MODIFY_FIELD] Usuário: {} | Campo: {}", discordUserId, buttonId);
            handleFieldModification(event, buttonId);
        } else {
            logger.warn("[BUTTON_UNKNOWN] Botão desconhecido: {} | Usuário: {}", buttonId, discordUserId);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        long discordUserId = event.getUser().getIdLong();
        String componentId = event.getComponentId();
        List<String> selectedValues = event.getValues();
        
        logger.info("[SELECT_INTERACTION] Usuário: {} | Componente: {} | Valores: {} | Guild: {}", 
                   discordUserId, componentId, selectedValues, 
                   event.getGuild() != null ? event.getGuild().getId() : "DM");
        
        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (event.getComponentId()) {
            case "squad-select" -> {
                String squadId = event.getValues().getFirst();
                String squadName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[SQUAD_SELECT] Usuário: {} | Squad selecionada: {} (ID: {})", 
                           discordUserId, squadName, squadId);
                state.squadId = squadId;
                state.squadName = squadName;

                String squadsJson = squadLogService.getSquads();
                JSONObject obj = new JSONObject(squadsJson);
                JSONArray squadsArray = obj.optJSONArray("items");
                JSONObject selectedSquad = null;
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(squadId)) {
                        selectedSquad = squad;
                        break;
                    }
                }

                StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create("user-select")
                        .setPlaceholder("Selecione quem irá responder");

                if (selectedSquad != null) {
                    userMenuBuilder.addOption("All team", squadId);

                    JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
                    if (userSquads != null) {
                        for (int i = 0; i < userSquads.length(); i++) {
                            JSONObject userSquad = userSquads.getJSONObject(i);
                            JSONObject user = userSquad.optJSONObject("user");
                            if (user != null) {
                                String name = user.optString("first_name", "") + " " + user.optString("last_name", "");
                                String userIdStr = String.valueOf(user.opt("id"));
                                if (!name.trim().isEmpty()) {
                                    userMenuBuilder.addOption(name, userIdStr);
                                }
                            }
                        }
                    }
                }

                logger.debug("[SQUAD_SELECT] Enviando resposta de confirmação para usuário: {}", discordUserId);
                event.reply("Squad selecionada: " + squadName + " (ID: " + squadId + ")")
                        .setEphemeral(true)
                        .queue();

                event.getInteraction().getHook().sendMessage("Selecione quem irá responder:")
                        .addActionRow(userMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.USER;
            }

            case "user-select" -> {
                String selectedUserId = event.getValues().getFirst();
                String selectedUserName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[USER_SELECT] Usuário: {} | Pessoa selecionada: {} (ID: {})", 
                           discordUserId, selectedUserName, selectedUserId);
                state.userId = selectedUserId;
                state.userName = selectedUserName;

                event.reply("Pessoa selecionada: " + selectedUserName)
                        .setEphemeral(true)
                        .queue();

                String logTypesJson = squadLogService.getSquadLogTypes();
                JSONArray logTypesArray = new JSONArray(logTypesJson);

                StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                        .setPlaceholder("Selecione o tipo");
                buildSelectMenu(logTypesArray, typeMenuBuilder);

                event.getInteraction().getHook().sendMessage("Selecione o tipo:")
                        .addActionRow(typeMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.TYPE;
            }

            case "type-select" -> {
                String typeId = event.getValues().getFirst();
                String typeName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[TYPE_SELECT] Usuário: {} | Tipo selecionado: {} (ID: {})", 
                           discordUserId, typeName, typeId);
                state.typeId = typeId;
                state.typeName = typeName;

                event.reply("Tipo selecionado: " + typeName + " (ID: " + typeId + ")")
                        .setEphemeral(true)
                        .queue();

                String categoriesJson = squadLogService.getSquadCategories();
                JSONArray categoriesArray = new JSONArray(categoriesJson);

                StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                        .setPlaceholder("Selecione as categorias")
                        .setMinValues(1)
                        .setMaxValues(categoriesArray.length());
                buildSelectMenu(categoriesArray, categoryMenuBuilder);

                event.getInteraction().getHook().sendMessage("Selecione as categorias:")
                        .addActionRow(categoryMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.CATEGORY;
            }
            case "category-select" -> {
                List<String> selectedIds = event.getSelectedOptions().stream()
                    .map(opt -> opt.getValue())
                    .toList();
                List<String> selectedNames = event.getSelectedOptions().stream()
                    .map(opt -> opt.getLabel())
                    .toList();
                
                logger.info("[CATEGORY_SELECT] Usuário: {} | Categorias selecionadas: {} | IDs: {}", 
                           discordUserId, selectedNames, selectedIds);
                    
                state.categoryIds = selectedIds;
                state.categoryNames = selectedNames;

                event.reply("Categorias selecionadas: " + String.join(", ", selectedNames))
                    .setEphemeral(true)
                    .queue();

                event.getInteraction().getHook().sendMessage("Digite uma descrição:")
                    .setEphemeral(true)
                    .queue();
                state.step = FormStep.DESCRIPTION;
            }
            
            // Modify handlers
            case "squad-select-modify" -> {
                String squadId = event.getValues().getFirst();
                String squadName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[SQUAD_MODIFY] Usuário: {} | Nova squad: {} (ID: {})", 
                           discordUserId, squadName, squadId);
                state.squadId = squadId;
                state.squadName = squadName;

                event.reply("Squad alterada para: " + squadName)
                    .setEphemeral(true)
                    .queue();
                
                // Auto-update user selection for new squad
                String squadsJson = squadLogService.getSquads();
                JSONObject obj = new JSONObject(squadsJson);
                JSONArray squadsArray = obj.optJSONArray("items");
                JSONObject selectedSquad = null;
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(squadId)) {
                        selectedSquad = squad;
                        break;
                    }
                }

                StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create("user-select-modify")
                        .setPlaceholder("Selecione quem irá responder");

                if (selectedSquad != null) {
                    userMenuBuilder.addOption("All team", squadId);
                    JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
                    if (userSquads != null) {
                        for (int i = 0; i < userSquads.length(); i++) {
                            JSONObject userSquad = userSquads.getJSONObject(i);
                            JSONObject user = userSquad.optJSONObject("user");
                            if (user != null) {
                                String name = user.optString("first_name", "") + " " + user.optString("last_name", "");
                                String userIdStr = String.valueOf(user.opt("id"));
                                if (!name.trim().isEmpty()) {
                                    userMenuBuilder.addOption(name, userIdStr);
                                }
                            }
                        }
                    }
                }

                event.getInteraction().getHook().sendMessage("Agora selecione a pessoa para esta nova squad:")
                        .addActionRow(userMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
            }
            
            case "user-select-modify" -> {
                String selectedUserId = event.getValues().getFirst();
                String selectedUserName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[USER_MODIFY] Usuário: {} | Nova pessoa: {} (ID: {})", 
                           discordUserId, selectedUserName, selectedUserId);
                state.userId = selectedUserId;
                state.userName = selectedUserName;

                event.reply("Pessoa alterada para: " + selectedUserName)
                    .setEphemeral(true)
                    .queue();
                
                showSummaryFromSelectInteraction(event, state);
            }
            
            case "type-select-modify" -> {
                String typeId = event.getValues().getFirst();
                String typeName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[TYPE_MODIFY] Usuário: {} | Novo tipo: {} (ID: {})", 
                           discordUserId, typeName, typeId);
                state.typeId = typeId;
                state.typeName = typeName;

                event.reply("Tipo alterado para: " + typeName)
                    .setEphemeral(true)
                    .queue();
                
                showSummaryFromSelectInteraction(event, state);
            }
            
            case "category-select-modify" -> {
                List<String> selectedIds = event.getSelectedOptions().stream()
                    .map(opt -> opt.getValue())
                    .toList();
                List<String> selectedNames = event.getSelectedOptions().stream()
                    .map(opt -> opt.getLabel())
                    .toList();
                
                logger.info("[CATEGORY_MODIFY] Usuário: {} | Novas categorias: {} | IDs: {}", 
                           discordUserId, selectedNames, selectedIds);
                    
                state.categoryIds = selectedIds;
                state.categoryNames = selectedNames;

                event.reply("Categorias alteradas para: " + String.join(", ", selectedNames))
                    .setEphemeral(true)
                    .queue();
                
                showSummaryFromSelectInteraction(event, state);
            }
            default -> {
                logger.warn("[SELECT_UNKNOWN] Componente desconhecido: {} | Usuário: {}", componentId, discordUserId);
            }
        }
    }

    private static void buildSelectMenu(JSONArray categoriesArray, StringSelectMenu.Builder categoryMenuBuilder) {
        for (int i = 0; i < categoriesArray.length(); i++) {
            JSONObject category = categoriesArray.getJSONObject(i);
            String name = category.optString("name", "");
            if (!name.isEmpty()) {
                categoryMenuBuilder.addOption(name, String.valueOf(category.get("id")));
            }
        }
    }

    @Override
    public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        long discordUserId = event.getAuthor().getIdLong();
        if (!userFormState.containsKey(discordUserId)) return;
        
        if (event.isFromGuild() && event.getAuthor().isBot()) return;
        
        FormState state = userFormState.get(discordUserId);
        String messageContent = event.getMessage().getContentRaw();
        
        logger.info("[MESSAGE_RECEIVED] Usuário: {} | Step: {} | Conteúdo: {} | Guild: {}", 
                   discordUserId, state.step, messageContent, 
                   event.getGuild() != null ? event.getGuild().getId() : "DM");

        switch (state.step) {
            case DESCRIPTION -> {
                state.description = messageContent;
                logger.info("[DESCRIPTION_INPUT] Usuário: {} | Descrição: {}", discordUserId, messageContent);
                event.getChannel().sendMessage("Descrição registrada. Informe a data de início (YYYY-MM-DD):").queue();
                state.step = FormStep.START_DATE;
            }
            case START_DATE -> {
                state.startDate = messageContent;
                logger.info("[START_DATE_INPUT] Usuário: {} | Data início: {}", discordUserId, messageContent);
                event.getChannel().sendMessage("O formulario tem data de fim? (s/n)").queue();
                state.step = FormStep.HAS_END;
            }
            case HAS_END -> {
                String content = messageContent.trim().toLowerCase();
                logger.info("[HAS_END_INPUT] Usuário: {} | Resposta: {}", discordUserId, content);
                if (content.equals("s")) {
                    logger.debug("[HAS_END_INPUT] Usuário escolheu adicionar data de fim: {}", discordUserId);
                    event.getChannel().sendMessage("Informe a data de fim (YYYY-MM-DD):").queue();
                    state.step = FormStep.END_DATE;
                } else {
                    logger.debug("[HAS_END_INPUT] Usuário escolheu não adicionar data de fim: {}", discordUserId);
                    state.endDate = null;
                    showSummary(event, state);
                    // Don't remove userFormState here - keep it for button interaction
                }
            }
            case END_DATE -> {
                state.endDate = messageContent;
                logger.info("[END_DATE_INPUT] Usuário: {} | Data fim: {}", discordUserId, messageContent);
                showSummary(event, state);
                // Don't remove userFormState here - keep it for button interaction
            }
            case DESCRIPTION_MODIFY -> {
                state.description = messageContent;
                logger.info("[DESCRIPTION_MODIFY] Usuário: {} | Nova descrição: {}", discordUserId, messageContent);
                event.getChannel().sendMessage("Descrição atualizada para: " + state.description).queue();
                showSummaryFromMessage(event, state);
            }
            case START_DATE_MODIFY -> {
                state.startDate = messageContent;
                logger.info("[START_DATE_MODIFY] Usuário: {} | Nova data início: {}", discordUserId, messageContent);
                event.getChannel().sendMessage("Data de início atualizada para: " + state.startDate).queue();
                showSummaryFromMessage(event, state);
            }
            case END_DATE_MODIFY -> {
                String content = messageContent.trim();
                if (content.equalsIgnoreCase("null")) {
                    logger.info("[END_DATE_MODIFY] Usuário: {} | Removendo data de fim", discordUserId);
                    state.endDate = null;
                    event.getChannel().sendMessage("Data de fim removida.").queue();
                } else {
                    logger.info("[END_DATE_MODIFY] Usuário: {} | Nova data fim: {}", discordUserId, content);
                    state.endDate = content;
                    event.getChannel().sendMessage("Data de fim atualizada para: " + state.endDate).queue();
                }
                showSummaryFromMessage(event, state);
            }
            default -> {
                logger.warn("[MESSAGE_UNKNOWN] Step desconhecido: {} | Usuário: {} | Conteúdo: {}", 
                           state.step, discordUserId, messageContent);
                event.getChannel().sendMessage("O formulario foi cancelado.").queue();
                userFormState.remove(discordUserId);
            }
        }
    }

    private void showSummary(net.dv8tion.jda.api.events.message.MessageReceivedEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", state.startDate, false);
        embed.addField("📅 Data de fim", state.endDate != null ? state.endDate : "Não informado", false);

        event.getChannel().sendMessageEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("alterar-log", "✏️ Alterar")
            )
            .queue();
    }

    private void showFieldModificationButtons(ButtonInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✏️ Selecione o campo que deseja alterar");
        embed.setColor(0xFFA500);
        embed.setDescription("Clique no botão correspondente ao campo que deseja modificar:");

        event.reply("Selecione o campo para alterar:")
            .setEphemeral(true)
            .addActionRow(
                Button.secondary("modify-squad", "🏢 Squad"),
                Button.secondary("modify-user", "👤 Pessoa"),
                Button.secondary("modify-type", "📝 Tipo")
            )
            .addActionRow(
                Button.secondary("modify-category", "🏷️ Categorias"),
                Button.secondary("modify-description", "📄 Descrição")
            )
            .addActionRow(
                Button.secondary("modify-start-date", "📅 Data Início"),
                Button.secondary("modify-end-date", "📅 Data Fim"),
                Button.primary("back-to-summary", "🔙 Voltar ao Resumo")
            )
            .queue();
    }

    private void handleFieldModification(ButtonInteractionEvent event, String buttonId) {
        long discordUserId = event.getUser().getIdLong();
        FormState state = userFormState.get(discordUserId);
        
        if (state == null) {
            event.reply("Formulário não encontrado ou expirado.").setEphemeral(true).queue();
            return;
        }

        switch (buttonId) {
            case "modify-squad" -> {
                event.deferReply().setEphemeral(true).queue(interaction -> {
                    String squadsJson = squadLogService.getSquads();
                    JSONArray squadsArray;
                    if (!squadsJson.trim().startsWith("[")) {
                        JSONObject obj = new JSONObject(squadsJson);
                        squadsArray = obj.optJSONArray("items");
                    } else {
                        squadsArray = new JSONArray(squadsJson);
                    }

                    StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("squad-select-modify")
                            .setPlaceholder("Selecione uma nova Squad");
                    buildSelectMenu(squadsArray, menuBuilder);

                    interaction.editOriginal("Selecione uma nova Squad:")
                            .setComponents(ActionRow.of(menuBuilder.build()))
                            .queue();
                });
            }
            case "modify-user" -> {
                // Get current squad users
                String squadsJson = squadLogService.getSquads();
                JSONObject obj = new JSONObject(squadsJson);
                JSONArray squadsArray = obj.optJSONArray("items");
                JSONObject selectedSquad = null;
                
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(state.squadId)) {
                        selectedSquad = squad;
                        break;
                    }
                }

                StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create("user-select-modify")
                        .setPlaceholder("Selecione uma nova pessoa");

                if (selectedSquad != null) {
                    userMenuBuilder.addOption("All team", state.squadId);
                    JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
                    if (userSquads != null) {
                        for (int i = 0; i < userSquads.length(); i++) {
                            JSONObject userSquad = userSquads.getJSONObject(i);
                            JSONObject user = userSquad.optJSONObject("user");
                            if (user != null) {
                                String name = user.optString("first_name", "") + " " + user.optString("last_name", "");
                                String userIdStr = String.valueOf(user.opt("id"));
                                if (!name.trim().isEmpty()) {
                                    userMenuBuilder.addOption(name, userIdStr);
                                }
                            }
                        }
                    }
                }

                event.reply("Selecione uma nova pessoa:")
                        .setEphemeral(true)
                        .addActionRow(userMenuBuilder.build())
                        .queue();
            }
            case "modify-type" -> {
                String logTypesJson = squadLogService.getSquadLogTypes();
                JSONArray logTypesArray = new JSONArray(logTypesJson);

                StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select-modify")
                        .setPlaceholder("Selecione um novo tipo");
                buildSelectMenu(logTypesArray, typeMenuBuilder);

                event.reply("Selecione um novo tipo:")
                        .setEphemeral(true)
                        .addActionRow(typeMenuBuilder.build())
                        .queue();
            }
            case "modify-category" -> {
                String categoriesJson = squadLogService.getSquadCategories();
                JSONArray categoriesArray = new JSONArray(categoriesJson);

                StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select-modify")
                        .setPlaceholder("Selecione novas categorias")
                        .setMinValues(1)
                        .setMaxValues(categoriesArray.length());
                buildSelectMenu(categoriesArray, categoryMenuBuilder);

                event.reply("Selecione novas categorias:")
                        .setEphemeral(true)
                        .addActionRow(categoryMenuBuilder.build())
                        .queue();
            }
            case "modify-description" -> {
                event.reply("Digite a nova descrição:")
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.DESCRIPTION_MODIFY;
            }
            case "modify-start-date" -> {
                event.reply("Digite a nova data de início (YYYY-MM-DD):")
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.START_DATE_MODIFY;
            }
            case "modify-end-date" -> {
                event.reply("Digite a nova data de fim (YYYY-MM-DD) ou 'null' para remover:")
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.END_DATE_MODIFY;
            }
            case "back-to-summary" -> {
                showSummaryFromButton(event, state);
            }
        }
    }

    private void showSummaryFromButton(ButtonInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", state.startDate, false);
        embed.addField("📅 Data de fim", state.endDate != null ? state.endDate : "Não informado", false);

        event.reply("Resumo atualizado:")
            .setEphemeral(true)
            .addEmbeds(embed.build())
            .addActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("alterar-log", "✏️ Alterar")
            )
            .queue();
    }

    private void showSummaryFromSelectInteraction(StringSelectInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", state.startDate, false);
        embed.addField("📅 Data de fim", state.endDate != null ? state.endDate : "Não informado", false);

        event.getInteraction().getHook().sendMessage("Resumo atualizado:")
            .addEmbeds(embed.build())
            .addActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("alterar-log", "✏️ Alterar")
            )
            .setEphemeral(true)
            .queue();
    }

    private void showSummaryFromMessage(net.dv8tion.jda.api.events.message.MessageReceivedEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", state.startDate, false);
        embed.addField("📅 Data de fim", state.endDate != null ? state.endDate : "Não informado", false);

        event.getChannel().sendMessageEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("alterar-log", "✏️ Alterar")
            )
            .queue();
    }

    enum FormStep {
        USER, TYPE, CATEGORY, DESCRIPTION, START_DATE, HAS_END, END_DATE,
        DESCRIPTION_MODIFY, START_DATE_MODIFY, END_DATE_MODIFY
    }

    static class FormState {
        String squadId;
        String squadName;
        String userId;
        String userName;
        String typeId;
        String typeName;
        List<String> categoryIds;
        List<String> categoryNames;
        String description;
        String startDate;
        String endDate;
        FormStep step;
    }

    public String buildSquadLogPayload(
            String squadId,
            String userId,
            String squadLogTypeId,
            List<String> skillCategoryIds,
            String description,
            String startDate,
            String endDate
    ) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(squadId));
        payload.put("user_id", Integer.parseInt(userId));
        payload.put("squad_log_type_id", Integer.parseInt(squadLogTypeId));
        payload.put("skill_categories", skillCategoryIds.stream().map(Integer::parseInt).toList());
        payload.put("description", description);
        payload.put("start_date", startDate);
        if (endDate != null && !endDate.isBlank()) {
            payload.put("end_date", endDate);
        }
        return payload.toString();
    }

    /**
     * Helper method to format FormState for debugging purposes
     */
    private String formatFormState(FormState state) {
        return String.format("FormState{squadId='%s', squadName='%s', userId='%s', userName='%s', " +
                "typeId='%s', typeName='%s', categoryIds=%s, categoryNames=%s, " +
                "description='%s', startDate='%s', endDate='%s', step=%s}",
                state.squadId, state.squadName, state.userId, state.userName,
                state.typeId, state.typeName, state.categoryIds, state.categoryNames,
                state.description, state.startDate, state.endDate, state.step);
    }
}