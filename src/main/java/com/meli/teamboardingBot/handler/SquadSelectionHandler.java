package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class SquadSelectionHandler extends AbstractInteractionHandler {
    
    @Autowired
    private SquadLogService squadLogService;
    
    @Override
    public boolean canHandle(String componentId) {
        return "criar".equals(componentId) || 
               "squad-select".equals(componentId) ||
               "edit-squad".equals(componentId);
    }
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        
        if ("criar".equals(buttonId)) {
            handleCreateButton(event, state);
        } else if ("edit-squad".equals(buttonId)) {
            handleEditSquadButton(event, state);
        }
    }
    
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("squad-select".equals(event.getComponentId())) {
            handleSquadSelect(event, state);
        }
    }
    
    private void handleCreateButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Iniciando fluxo de criação");
        state.setCreating(true);
        state.setEditing(false);
        state.setStep(FormStep.SQUAD_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showSquadSelection(event);
    }
    
    private void handleEditSquadButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando squad");
        state.setStep(FormStep.SQUAD_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showSquadSelection(event);
    }
    
    private void handleSquadSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedSquadId = event.getValues().get(0);
        logger.info("Squad selecionada: {}", selectedSquadId);
        
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(selectedSquadId)) {
                        state.setSquadId(selectedSquadId);
                        state.setSquadName(squad.optString("name", ""));
                        break;
                    }
                }
            }
            
            event.deferEdit().queue();
            
            EmbedBuilder confirmEmbed = new EmbedBuilder()
                .setTitle("✅ Squad selecionada com sucesso!")
                .setDescription("Squad: **" + state.getSquadName() + "**")
                .setColor(0x00FF00);
            
            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setComponents()
                .queue();
            
            updateFormState(event.getUser().getIdLong(), state);
            
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                if (state.getStep() == FormStep.SQUAD_MODIFY) {
                    showSummary(event);
                } else {
                    state.setStep(FormStep.USER_SELECTION);
                    updateFormState(event.getUser().getIdLong(), state);
                    
                    EmbedBuilder nextEmbed = new EmbedBuilder()
                        .setTitle("👥 Próximo Passo: Seleção de Usuário")
                        .setDescription("Agora vamos selecionar o usuário para o log.")
                        .setColor(0x0099FF);
                    
                    event.getHook().editOriginalEmbeds(nextEmbed.build())
                        .setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.primary("select-user", "Selecionar Usuário"))
                        .queue();
                }
            });
            
        } catch (Exception e) {
            logger.error("Erro na seleção de squad: {}", e.getMessage());
            showError(event, "Erro ao processar seleção da squad.");
        }
    }
    
    private void showSquadSelection(ButtonInteractionEvent event) {
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray == null || squadsArray.length() == 0) {
                event.editMessage("❌ Nenhuma squad encontrada.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            
            StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                    .setPlaceholder("Selecione uma squad");
            
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                String squadName = squad.optString("name", "");
                String squadId = String.valueOf(squad.get("id"));
                if (!squadName.isEmpty()) {
                    squadMenuBuilder.addOption(squadName, squadId);
                }
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏢 Selecione uma Squad")
                .setDescription("Escolha a squad para o seu log:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar squads: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar squads. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    
    private void showUserSelection(StringSelectInteractionEvent event, FormState state) {
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
        return 1;
    }
}
