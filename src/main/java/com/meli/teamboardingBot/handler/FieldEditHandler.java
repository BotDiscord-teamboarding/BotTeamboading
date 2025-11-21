package com.meli.teamboardingBot.handler;

import lombok.extern.slf4j.Slf4j;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
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
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(9)
public class FieldEditHandler extends AbstractInteractionHandler {
    @Autowired
    private SquadLogService squadLogService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;
    
    public FieldEditHandler(FormStateService formStateService) {
        super(formStateService);
    }
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
        log.info("Squad selecionada para edição: {} - {}", selectedSquadId, selectedSquadName);
        log.info("Estado atual: isEditing={}, isCreating={}, step={}", state.isEditing(), state.isCreating(), state.getStep());
        state.setSquadId(selectedSquadId);
        state.setSquadName(selectedSquadName);
        updateFormState(event.getUser().getIdLong(), state);
        log.info("Atualizando mensagem com resumo dos dados após seleção de squad...");
        event.deferEdit().queue();
        showEditSummary(event.getHook(), state);
    }
    private void handleUserSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectedUserId = event.getValues().get(0);
        log.info("Usuário selecionado para edição: {}", selectedUserId);
        try {
            if (selectedUserId.equals(state.getSquadId())) {
                state.setUserId(selectedUserId);
                state.setUserName("All team");
            } else {
                loadUserFromSquad(state, selectedUserId);
            }
            updateFormState(event.getUser().getIdLong(), state);
            event.deferEdit().queue();
            showEditSummary(event.getHook(), state);
        } catch (Exception e) {
            log.error("Erro ao carregar dados do usuário: {}", e.getMessage());
            String selectedUserName = event.getSelectedOptions().get(0).getLabel();
            state.setUserId(selectedUserId);
            state.setUserName(selectedUserName);
            updateFormState(event.getUser().getIdLong(), state);
            event.deferEdit().queue();
            showEditSummary(event.getHook(), state);
        }
    }
    private void loadUserFromSquad(FormState state, String selectedUserId) throws Exception {
        String squadsJson = squadLogService.getSquads();
        JSONObject obj = new JSONObject(squadsJson);
        JSONArray squadsArray = obj.optJSONArray("items");
        if (squadsArray != null) {
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                if (String.valueOf(squad.get("id")).equals(state.getSquadId())) {
                    JSONArray userSquads = squad.optJSONArray("user_squads");
                    if (userSquads != null) {
                        for (int j = 0; j < userSquads.length(); j++) {
                            JSONObject userSquad = userSquads.getJSONObject(j);
                            JSONObject user = userSquad.optJSONObject("user");
                            if (user != null && String.valueOf(user.get("id")).equals(selectedUserId)) {
                                state.setUserId(selectedUserId);
                                state.setUserName(user.optString("first_name", "") + " " + user.optString("last_name", ""));
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
    private void handleTypeSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        String selectedTypeId = event.getValues().get(0);
        String selectedTypeName = event.getSelectedOptions().get(0).getLabel();
        log.info("Tipo selecionado para edição: {} - {}", selectedTypeId, selectedTypeName);
        state.setTypeId(selectedTypeId);
        state.setTypeName(selectedTypeName);
        updateFormState(event.getUser().getIdLong(), state);
        event.deferEdit().queue();
        showEditSummary(event.getHook(), state);
    }
    private void handleCategoriesSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        log.info("Categorias selecionadas para edição: {}", event.getValues());
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
        showEditSummary(event.getHook(), state);
    }
    private void showEditSummary(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        log.info("Atualizando mensagem com resumo dos dados após seleção...");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, formState.getLocale())
                    + ". " + messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale()) +":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale() );
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()) , description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_de_fim ", null, formState.getLocale()), endDate, false);
        event.editMessageEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale())),
                    Button.secondary("edit-user", "👤 " +  messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                    Button.secondary("edit-type", "📝 " +  messageSource.getMessage("txt_tipo", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.secondary("edit-categories", "🏷️ " +  messageSource.getMessage("txt_categorias", null, formState.getLocale())),
                    Button.secondary("edit-description", "📄 " +  messageSource.getMessage("txt_descricao", null, formState.getLocale())),
                    Button.secondary("edit-dates", "📅 " + messageSource.getMessage("txt_datas", null, formState.getLocale()))
                ),
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ " +  messageSource.getMessage("txt_salvar_alteracoes", null, formState.getLocale())),
                    Button.danger("cancelar-edicao", "❌ " +  messageSource.getMessage("txt_cancelar", null, formState.getLocale()))
                )
            )
            .queue(
                success -> log.info("Mensagem atualizada com sucesso - resumo mostrado"),
                error -> log.error("Erro ao atualizar mensagem: {}", error.getMessage())
            );
    }
    private void showEditSummary(net.dv8tion.jda.api.interactions.InteractionHook hook, FormState state) {
        log.info("Atualizando mensagem com resumo dos dados após seleção (via hook)...");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 " + messageSource.getMessage("txt_editar_squad_log", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, formState.getLocale())
                    + ". " + messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale())  +":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " + messageSource.getMessage("txt_data_inicio", null, formState.getLocale()), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, formState.getLocale()), endDate, false);
        hook.editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 "+ messageSource.getMessage("txt_squad", null, formState.getLocale())),
                    Button.secondary("edit-user", "👤 "+ messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                    Button.secondary("edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()))
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
            .queue(
                success -> log.info("Mensagem atualizada com sucesso - resumo com nova squad mostrado"),
                error -> log.error("Erro ao atualizar mensagem: {}", error.getMessage())
            );
    }
    private void returnToEditSummary(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, FormState state) {
        log.info("Retornando ao resumo de edição após alteração");
        log.info("Estado no resumo: squadName={}, userName={}, typeName={}", state.getSquadName(), state.getUserName(), state.getTypeName());
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Editar Squad Log")
            .setDescription(messageSource.getMessage("txt_dados_atuais_do_squad_log", null, formState.getLocale()) + ". "
                    + messageSource.getMessage("txt_selecione_o_campo_que_deseja_editar", null, formState.getLocale()) +":")
            .setColor(0xFFAA00);
        String squadName = state.getSquadName() != null ? state.getSquadName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale()), squadName, false);
        String userName = state.getUserName() != null ? state.getUserName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale()), userName, false);
        String typeName = state.getTypeName() != null ? state.getTypeName() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()), typeName, false);
        String categoryNames = (!state.getCategoryNames().isEmpty()) ?
            String.join(", ", state.getCategoryNames()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("🏷️ " + messageSource.getMessage("txt_categorias", null, formState.getLocale()), categoryNames, false);
        String description = state.getDescription() != null ? state.getDescription() : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        embed.addField("📄 " + messageSource.getMessage("txt_descricao", null, formState.getLocale()), description, false);
        String startDate = state.getStartDate() != null ? formatToBrazilianDate(state.getStartDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        String endDate = state.getEndDate() != null ? formatToBrazilianDate(state.getEndDate()) : messageSource.getMessage("txt_nao_informado", null, formState.getLocale());
        embed.addField("📅 " + messageSource.getMessage("txt_data_inicio", null, formState.getLocale()), startDate, false);
        embed.addField("📅 " + messageSource.getMessage("txt_data_fim", null, formState.getLocale()), endDate, false);
        log.info("Tentando editar mensagem original com resumo atualizado...");
        try {
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.secondary("edit-squad", "🏢 " + messageSource.getMessage("txt_squad", null, formState.getLocale())),
                        Button.secondary("edit-user", "👤 " + messageSource.getMessage("txt_pessoa", null, formState.getLocale())),
                        Button.secondary("edit-type", "📝 " + messageSource.getMessage("txt_tipo", null, formState.getLocale()))
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
                .queue(
                    success -> log.info("Mensagem editada com sucesso - resumo atualizado mostrado"),
                    error -> log.error("Erro ao editar mensagem: {}", error.getMessage())
                );
        } catch (Exception e) {
            log.error("Exceção ao tentar editar mensagem: {}", e.getMessage(), e);
        }
    }
    private void handleEditSquad(ButtonInteractionEvent event, FormState state) {
        log.info("Editando squad do log");
        try {
            event.deferEdit().queue();
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhuma_squad_encontrada", null, formState.getLocale()) + ".").queue();
                return;
            }
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder squadMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-squad-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_uma_nova_squad", null, formState.getLocale()));
            boolean hasSquads = false;
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                String squadId = String.valueOf(squad.get("id"));
                String squadName = squad.optString("name", "");
                if (squadName != null && !squadName.trim().isEmpty()) {
                    squadMenuBuilder.addOption(squadName, squadId);
                    hasSquads = true;
                } else {
                    log.warn("Squad com ID {} tem nome vazio, pulando...", squadId);
                }
            }
            if (!hasSquads) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhuma_squad_encontrada", null, formState.getLocale()) +".").queue();
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏢 " + messageSource.getMessage("txt_editar_squad", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_selecione_anova_squad", null, formState.getLocale()) + ":")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar squads: {}", e.getMessage());
            event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_carregar_squads", null, formState.getLocale())  +".").queue();
        }
    }
    private void handleEditUser(ButtonInteractionEvent event, FormState state) {
        log.info("Editando usuário do log - Squad ID atual: {}", state.getSquadId());
        try {
            event.deferEdit().queue();
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder userMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-user-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_um_novo_usuario", null, formState.getLocale()));
            boolean hasUsers = false;
            
            userMenuBuilder.addOption("All team", state.getSquadId());
            hasUsers = true;
            
            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    String squadId = String.valueOf(squad.get("id"));
                    
                    if (squadId.equals(state.getSquadId())) {
                        log.info("Encontrada squad correspondente: {} (ID: {})", squad.optString("name", ""), squadId);
                        JSONArray userSquads = squad.optJSONArray("user_squads");
                        if (userSquads != null) {
                            for (int j = 0; j < userSquads.length(); j++) {
                                JSONObject userSquad = userSquads.getJSONObject(j);
                                JSONObject user = userSquad.optJSONObject("user");
                                if (user != null) {
                                    String userId = String.valueOf(user.get("id"));
                                    String firstName = user.optString("first_name", "");
                                    String lastName = user.optString("last_name", "");
                                    String fullName = user.optString("name", "");
                                    
                                    String userName;
                                    if (!fullName.isEmpty()) {
                                        userName = fullName;
                                    } else if (!firstName.isEmpty() || !lastName.isEmpty()) {
                                        userName = (firstName + " " + lastName).trim();
                                    } else {
                                        userName = "Usuário " + userId;
                                    }
                                    
                                    if (!userName.trim().isEmpty()) {
                                        userMenuBuilder.addOption(userName, userId);
                                        hasUsers = true;
                                        log.info("Adicionado usuário: {} (ID: {})", userName, userId);
                                    } else {
                                        log.warn("Usuário com ID {} tem nome vazio, pulando...", userId);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            
            if (!hasUsers) {
                event.getHook().editOriginal("❌ "+ messageSource.getMessage("txt_nenhum_usuario_encontrado_na_squad_atual", null, formState.getLocale()) +".").queue();
                return;
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 " + messageSource.getMessage("txt_editar_pessoa", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_selecione_a_nova_pessoa", null, formState.getLocale()) + ":")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(userMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar usuários: {}", e.getMessage());
            event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_carregar_usuarios", null, formState.getLocale()) + ".").queue();
        }
    }
    private void handleEditType(ButtonInteractionEvent event, FormState state) {
        log.info("Editando tipo do log");
        try {
            event.deferEdit().queue();
            String typesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadLogTypes());
            log.info("Resposta da API de tipos: {}", typesJson);
            if (typesJson == null || typesJson.trim().isEmpty()) {
                log.error("API retornou resposta vazia para tipos");
                event.getHook().editOriginal("❌ " +messageSource.getMessage("txt_Erro_API_retornou_resposta_vazia_para_tipos", null, formState.getLocale()) + ".").queue();
                return;
            }
            JSONArray typesArray;
            try {
                typesArray = new JSONArray(typesJson);
            } catch (JSONException e) {
                log.error("API retornou JSON inválido para tipos. Resposta: {}", typesJson);
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_api_retornou_formato_json_invalido_para_categorias", null, formState.getLocale()) +".").queue();
                return;
            }
            if (typesArray == null || typesArray.length() == 0) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhum_tipo_valido_encontrado", null, formState.getLocale()) +".").queue();
                return;
            }
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder typeMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-type-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_um_novo_tipo", null, formState.getLocale()));
            boolean hasTypes = false;
            for (int i = 0; i < typesArray.length(); i++) {
                JSONObject type = typesArray.getJSONObject(i);
                String typeId = String.valueOf(type.get("id"));
                String typeName = type.optString("name", "");
                if (typeName != null && !typeName.trim().isEmpty()) {
                    typeMenuBuilder.addOption(typeName, typeId);
                    hasTypes = true;
                } else {
                    log.warn("Tipo com ID {} tem nome vazio, pulando...", typeId);
                }
            }
            if (!hasTypes) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhum_tipo_valido_encontrado", null, formState.getLocale()) +".").queue();
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 " + messageSource.getMessage("txt_editar_tipo", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_selecione_o_novo_tipo", null, formState.getLocale())+ ":")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(typeMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar tipos: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_carregar_tipos", null, formState.getLocale()) +": " + e.getMessage()).queue();
        }
    }
    private void handleEditCategories(ButtonInteractionEvent event, FormState state) {
        log.info("Editando categorias do log");
        event.deferEdit().queue();
        try {
            String categoriesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadCategories());
            log.info("Resposta da API de categorias: {}", categoriesJson);
            if (categoriesJson == null || categoriesJson.trim().isEmpty()) {
                log.error("API retornou resposta vazia para categorias");
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_api_retornou_resposta_vazia_para_categorias", null, formState.getLocale()) +".").queue();
                return;
            }
            JSONArray categoriesArray;
            try {
                categoriesArray = new JSONArray(categoriesJson);
            } catch (JSONException e) {
                log.error("API retornou JSON inválido para categorias. Resposta: {}", categoriesJson);
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_api_retornou_formato_json_invalido_para_categorias", null, formState.getLocale()) +".").queue();
                return;
            }
            if (categoriesArray == null || categoriesArray.length() == 0) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhuma_categoria_encontrada", null, formState.getLocale()) + ".").queue();
                return;
            }
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder categoryMenuBuilder = 
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("edit-categories-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_as_novas_categorias", null, formState.getLocale()));
            int validCategoryCount = 0;
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String categoryId = String.valueOf(category.get("id"));
                String categoryName = category.optString("name", "");
                if (categoryName == null || categoryName.trim().isEmpty()) {
                    log.warn("Categoria com ID {} tem nome vazio, pulando...", categoryId);
                    continue;
                }
                categoryMenuBuilder.addOption(categoryName, categoryId);
                validCategoryCount++;
            }
            if (validCategoryCount == 0) {
                event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_nenhuma_categoria_valida_encontrada", null, formState.getLocale()) +".").queue();
                return;
            }
            categoryMenuBuilder.setRequiredRange(1, Math.min(25, validCategoryCount));
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏷️ " + messageSource.getMessage("txt_editar_categorias", null, formState.getLocale()))
                .setDescription(messageSource.getMessage("txt_selecione_as_novas_categorias_pode_selecionar_multiplos", null, formState.getLocale()) +":")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(categoryMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar categorias: {}", e.getMessage());
            event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_carregar_categorias", null, formState.getLocale()) +".").queue();
        }
    }
    private void handleEditDescription(ButtonInteractionEvent event, FormState state) {
        log.info("Editando descrição do log");
        TextInput.Builder descriptionBuilder = TextInput.create("description", messageSource.getMessage("txt_descricao", null, formState.getLocale()), TextInputStyle.PARAGRAPH)
            .setPlaceholder(messageSource.getMessage("txt_digite_a_nova_descricao_do_log", null, formState.getLocale()) + "...")
            .setMaxLength(1000)
            .setRequired(true);
        if (state.getDescription() != null && !state.getDescription().trim().isEmpty()) {
            descriptionBuilder.setValue(state.getDescription());
        }
        TextInput descriptionInput = descriptionBuilder.build();
        Modal modal = Modal.create("edit-description-modal", "📄 " + messageSource.getMessage("txt_editar_descricao", null, formState.getLocale()))
            .addActionRow(descriptionInput)
            .build();
        event.replyModal(modal).queue();
    }
    private void handleEditDates(ButtonInteractionEvent event, FormState state) {
        log.info("Editando datas do log");
        TextInput.Builder startDateBuilder = TextInput.create("start_date", messageSource.getMessage("txt_data_de_inicio", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data", null, formState.getLocale()))
            .setMaxLength(10)
            .setRequired(true);
        if (state.getStartDate() != null && !state.getStartDate().trim().isEmpty()) {
            String convertedStartDate = convertApiDateToBrazilian(state.getStartDate());
            if (!convertedStartDate.trim().isEmpty()) {
                startDateBuilder.setValue(convertedStartDate);
            }
        }
        TextInput.Builder endDateBuilder = TextInput.create("end_date",  messageSource.getMessage("txt_data_de_fim", null, formState.getLocale()) + " (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder(messageSource.getMessage("txt_exemplo_data_opcional", null, formState.getLocale()))
            .setMaxLength(10)
            .setRequired(false);
        if (state.getEndDate() != null && !state.getEndDate().trim().isEmpty()) {
            String convertedEndDate = convertApiDateToBrazilian(state.getEndDate());
            if (!convertedEndDate.trim().isEmpty()) {
                endDateBuilder.setValue(convertedEndDate);
            }
        }
        Modal modal = Modal.create("edit-dates-modal", "📅 " + messageSource.getMessage("txt_editar_datas", null, formState.getLocale()))
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
    private void handleCancelEdit(ButtonInteractionEvent event) {
        log.info("Cancelando edição do log");
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("❌ " + messageSource.getMessage("txt_edicao_cancelada", null, formState.getLocale()))
            .setDescription(messageSource.getMessage("txt_edicao_do_squad_log_foi_cancelada", null, formState.getLocale()) + ".\n\n"
                    + messageSource.getMessage("txt_oque_deseja_fazer_agora", null, formState.getLocale()) +"?")
            .setColor(0xFF0000);
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.primary("atualizar", "🔄 "+ messageSource.getMessage("txt_tentar_novamente", null, formState.getLocale())),
                Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
            )
            .queue();
    }
    @Override
    public int getPriority() {
        return 9;
    }
}
