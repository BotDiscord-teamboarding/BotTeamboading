package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class UserSelectionHandler extends AbstractInteractionHandler {
    
    @Autowired
    private SquadLogService squadLogService;
    
    @Override
    public boolean canHandle(String componentId) {
        return "user-select".equals(componentId) || 
               "select-user".equals(componentId) ||
               "edit-pessoa".equals(componentId);
    }
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        
        if ("select-user".equals(buttonId)) {
            handleSelectUserButton(event, state);
        } else if ("edit-pessoa".equals(buttonId)) {
            handleEditUserButton(event, state);
        }
    }
    
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("user-select".equals(event.getComponentId())) {
            handleUserSelect(event, state);
        }
    }
    
    private void handleSelectUserButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Iniciando seleção de usuário");
        state.setStep(FormStep.USER_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showUserSelection(event, state.getSquadId());
    }
    
    private void handleEditUserButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando usuário");
        state.setStep(FormStep.USER_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showUserSelection(event, state.getSquadId());
    }
    
    private void handleUserSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedUserId = event.getValues().get(0);
        logger.info("Usuário selecionado: {}", selectedUserId);
        
        try {
            if (selectedUserId.equals(state.getSquadId())) {
                state.setUserId(selectedUserId);
                state.setUserName("All team");
            } else {
                loadUserFromSquad(state, selectedUserId);
            }
            
            event.deferEdit().queue();
            
            EmbedBuilder confirmEmbed = new EmbedBuilder()
                .setTitle("✅ Pessoa selecionada com sucesso!")
                .setDescription("Pessoa: **" + state.getUserName() + "**")
                .setColor(0x00FF00);
            
            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setComponents()
                .queue();
            
            updateFormState(event.getUser().getIdLong(), state);
            
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                if (state.getStep() == FormStep.USER_MODIFY) {
                    showSummary(event);
                } else {
                    state.setStep(FormStep.TYPE_SELECTION);
                    updateFormState(event.getUser().getIdLong(), state);
                    
                    EmbedBuilder nextEmbed = new EmbedBuilder()
                        .setTitle("📝 Próximo Passo: Seleção de Tipo")
                        .setDescription("Agora vamos selecionar o tipo do log.")
                        .setColor(0x0099FF);
                    
                    event.getHook().editOriginalEmbeds(nextEmbed.build())
                        .setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.primary("select-type", "Selecionar Tipo"))
                        .queue();
                }
            });
            
        } catch (Exception e) {
            logger.error("Erro na seleção de usuário: {}", e.getMessage());
            showError(event, "Erro ao processar seleção do usuário.");
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
    
    private void showUserSelection(ButtonInteractionEvent event, String squadId) {
        try {
            logger.info("Carregando usuários para squad: {}", squadId);
            
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray == null || squadsArray.length() == 0) {
                logger.error("Nenhuma squad encontrada na resposta da API");
                showUserSelectionError(event, "Nenhuma squad encontrada.");
                return;
            }
            
            JSONObject selectedSquad = null;
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                if (String.valueOf(squad.get("id")).equals(squadId)) {
                    selectedSquad = squad;
                    break;
                }
            }
            
            if (selectedSquad == null) {
                logger.error("Squad com ID {} não encontrada", squadId);
                showUserSelectionError(event, "Squad não encontrada.");
                return;
            }
            
            StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create("user-select")
                    .setPlaceholder("Selecione uma pessoa");
            
            userMenuBuilder.addOption("All team", squadId);
            
            JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
            int userCount = 0;
            
            if (userSquads != null) {
                for (int i = 0; i < userSquads.length(); i++) {
                    JSONObject userSquad = userSquads.getJSONObject(i);
                    JSONObject user = userSquad.optJSONObject("user");
                    if (user != null) {
                        String firstName = user.optString("first_name", "");
                        String lastName = user.optString("last_name", "");
                        String name = (firstName + " " + lastName).trim();
                        String userIdStr = String.valueOf(user.opt("id"));
                        
                        if (!name.isEmpty() && !userIdStr.equals("null")) {
                            userMenuBuilder.addOption(name, userIdStr);
                            userCount++;
                        }
                    }
                }
            }
            
            logger.info("Encontrados {} usuários na squad {}", userCount, squadId);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Selecione uma Pessoa")
                .setDescription("Escolha quem irá responder ao questionário:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(userMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar usuários: {}", e.getMessage(), e);
            showUserSelectionError(event, "Erro interno ao carregar usuários: " + e.getMessage());
        }
    }
    
    private void showUserSelectionError(ButtonInteractionEvent event, String message) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
            .setTitle("❌ Erro ao carregar usuários")
            .setDescription(message)
            .setColor(0xFF0000);
        
        event.editMessageEmbeds(errorEmbed.build())
            .setComponents()
            .queue();
    }
    
    private void showTypeSelection(StringSelectInteractionEvent event) {
    }
    
    private void showSummary(StringSelectInteractionEvent event) {
    }
    
    private void showError(StringSelectInteractionEvent event, String message) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
            .setTitle("❌ Erro")
            .setDescription(message)
            .setColor(0xFF0000);
        
        event.getHook().editOriginalEmbeds(errorEmbed.build())
            .setComponents()
            .queue();
    }
    
    @Override
    public int getPriority() {
        return 2;
    }
}
