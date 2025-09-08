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
@Order(3)
public class TypeSelectionHandler extends AbstractInteractionHandler {
    
    @Autowired
    private SquadLogService squadLogService;
    
    @Override
    public boolean canHandle(String componentId) {
        return "type-select".equals(componentId) || 
               "select-type".equals(componentId) ||
               "edit-tipo".equals(componentId);
    }
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        
        if ("select-type".equals(buttonId)) {
            handleSelectTypeButton(event, state);
        } else if ("edit-tipo".equals(buttonId)) {
            handleEditTypeButton(event, state);
        }
    }
    
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("type-select".equals(event.getComponentId())) {
            handleTypeSelect(event, state);
        }
    }
    
    private void handleSelectTypeButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Iniciando seleção de tipo");
        state.setStep(FormStep.TYPE_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showTypeSelection(event);
    }
    
    private void handleEditTypeButton(ButtonInteractionEvent event, FormState state) {
        logger.info("Editando tipo");
        state.setStep(FormStep.TYPE_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showTypeSelection(event);
    }
    
    private void handleTypeSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedTypeId = event.getValues().get(0);
        logger.info("Tipo selecionado: {}", selectedTypeId);
        
        try {
            String typesJson = squadLogService.getSquadLogTypes();
            JSONArray typesArray = new JSONArray(typesJson);
            
            for (int i = 0; i < typesArray.length(); i++) {
                JSONObject type = typesArray.getJSONObject(i);
                if (String.valueOf(type.get("id")).equals(selectedTypeId)) {
                    state.setTypeId(selectedTypeId);
                    state.setTypeName(type.optString("name", ""));
                    break;
                }
            }
            
            event.deferEdit().queue();
            
            EmbedBuilder confirmEmbed = new EmbedBuilder()
                .setTitle("✅ Tipo selecionado com sucesso!")
                .setDescription("Tipo: **" + state.getTypeName() + "**")
                .setColor(0x00FF00);
            
            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setComponents()
                .queue();
            
            updateFormState(event.getUser().getIdLong(), state);
            
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                if (state.getStep() == FormStep.TYPE_MODIFY) {
                    showSummary(event);
                } else {
                    state.setStep(FormStep.CATEGORY_SELECTION);
                    updateFormState(event.getUser().getIdLong(), state);
                    
                    EmbedBuilder nextEmbed = new EmbedBuilder()
                        .setTitle("🏷️ Próximo Passo: Seleção de Categoria")
                        .setDescription("Agora vamos selecionar as categorias do log.")
                        .setColor(0x0099FF);
                    
                    event.getHook().editOriginalEmbeds(nextEmbed.build())
                        .setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.primary("select-category", "Selecionar Categoria"))
                        .queue();
                }
            });
            
        } catch (Exception e) {
            logger.error("Erro na seleção de tipo: {}", e.getMessage());
            showError(event, "Erro ao processar seleção do tipo.");
        }
    }
    
    private void showTypeSelection(ButtonInteractionEvent event) {
        try {
            String logTypesJson = squadLogService.getSquadLogTypes();
            JSONArray logTypesArray = new JSONArray(logTypesJson);
            
            StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                    .setPlaceholder("Selecione o tipo");
            
            for (int i = 0; i < logTypesArray.length(); i++) {
                JSONObject type = logTypesArray.getJSONObject(i);
                String typeName = type.optString("name", "");
                String typeId = String.valueOf(type.get("id"));
                if (!typeName.isEmpty()) {
                    typeMenuBuilder.addOption(typeName, typeId);
                }
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Selecione um Tipo")
                .setDescription("Escolha o tipo do log:")
                .setColor(0x0099FF);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(typeMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar tipos: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar tipos")
                .setColor(0xFF0000);
            event.editMessageEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }
    
    private void showCategorySelection(StringSelectInteractionEvent event) {
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
        return 3;
    }
}
