package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
@Order(3)
public class TypeSelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    @Autowired
    private SummaryHandler summaryHandler;
    
    public TypeSelectionHandler(FormStateService formStateService, SquadLogService squadLogService) {
        super(formStateService);
        this.squadLogService = squadLogService;
    }
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
        log.info("Iniciando seleção de tipo");
        state.setStep(FormStep.TYPE_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showTypeSelection(event);
    }
    private void handleEditTypeButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando tipo");
        state.setStep(FormStep.TYPE_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showTypeSelection(event);
    }
    private void handleTypeSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedTypeId = event.getValues().get(0);
        log.info("Tipo selecionado: {}", selectedTypeId);
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
            updateFormState(event.getUser().getIdLong(), state);
            if (state.getStep() == FormStep.TYPE_MODIFY) {
                showSummary(event);
            } else {
                state.setStep(FormStep.CATEGORY_SELECTION);
                updateFormState(event.getUser().getIdLong(), state);
                showCategorySelectionAfterType(event);
            }
        } catch (Exception e) {
            log.error("Erro na seleção de tipo: {}", e.getMessage());
            showError(event, "Erro ao processar seleção do tipo.");
        }
    }
    private void showTypeSelection(ButtonInteractionEvent event) {
        try {
            event.deferEdit().queue();
            String logTypesJson = squadLogService.getSquadLogTypes();
            JSONArray logTypesArray = new JSONArray(logTypesJson);
            StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                    .setPlaceholder("Selecione o tipo");
            boolean hasTypes = false;
            for (int i = 0; i < logTypesArray.length(); i++) {
                JSONObject type = logTypesArray.getJSONObject(i);
                String typeName = type.optString("name", "");
                String typeId = String.valueOf(type.get("id"));
                if (!typeName.isEmpty()) {
                    typeMenuBuilder.addOption(typeName, typeId);
                    hasTypes = true;
                }
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Selecione um Tipo")
                .setDescription("Escolha o tipo do log:")
                .setColor(0x0099FF);
            if (hasTypes) {
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(typeMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription("❌ Nenhum tipo disponível no momento.");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar tipos: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar tipos")
                .setDescription("Ocorreu um erro ao carregar os tipos. Tente novamente.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
        }
    }
    private void showCategorySelectionAfterType(StringSelectInteractionEvent event) {
        try {
            String categoriesJson = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesJson);
            StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                    .setPlaceholder("Selecione as categorias")
                    .setMinValues(1)
                    .setMaxValues(Math.min(categoriesArray.length(), 25)); 
            boolean hasCategories = false;
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String categoryName = category.optString("name", "");
                String categoryId = String.valueOf(category.get("id"));
                if (!categoryName.isEmpty()) {
                    categoryMenuBuilder.addOption(categoryName, categoryId);
                    hasCategories = true;
                }
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏷️ Selecione as Categorias")
                .setDescription("Escolha uma ou mais categorias:")
                .setColor(0x0099FF);
            if (hasCategories) {
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(categoryMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription("❌ Nenhuma categoria disponível no momento.");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar categorias: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar categorias")
                .setDescription("Ocorreu um erro ao carregar as categorias. Tente novamente.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
        }
    }
    private void showSummary(StringSelectInteractionEvent event) {
        FormState state = getFormState(event.getUser().getIdLong());
        if (state != null) {
            summaryHandler.showUpdateSummary(event, state);
        }
    }
    private void showError(StringSelectInteractionEvent event, String message) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
            .setTitle("❌ Erro")
            .setDescription(message)
            .setColor(0xFF0000);
        event.getHook().editOriginalEmbeds(errorEmbed.build())
            .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
            .queue();
    }
    @Override
    public int getPriority() {
        return 3;
    }
}
