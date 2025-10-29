package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
@Order(4)
public class CategorySelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    @Autowired
    private SummaryHandler summaryHandler;
    
    public CategorySelectionHandler(FormStateService formStateService, SquadLogService squadLogService) {
        super(formStateService);
        this.squadLogService = squadLogService;
    }
    @Override
    public boolean canHandle(String componentId) {
        return "category-select".equals(componentId) || 
               "select-category".equals(componentId) ||
               "edit-categorias".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        if ("select-category".equals(buttonId)) {
            handleSelectCategoryButton(event, state);
        } else if ("edit-categorias".equals(buttonId)) {
            handleEditCategoriesButton(event, state);
        }
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("category-select".equals(event.getComponentId())) {
            handleCategorySelect(event, state);
        }
    }
    private void handleSelectCategoryButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando seleção de categoria");
        state.setStep(FormStep.CATEGORY_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showCategorySelection(event);
    }
    private void handleEditCategoriesButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando categorias");
        state.setStep(FormStep.CATEGORY_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showCategorySelection(event);
    }
    private void handleCategorySelect(StringSelectInteractionEvent event, FormState state) {
        List<String> selectedCategoryIds = event.getValues();
        log.info("Categorias selecionadas: {}", selectedCategoryIds);
        try {
            String categoriesJson = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesJson);
            state.getCategoryIds().clear();
            state.getCategoryNames().clear();
            for (String categoryId : selectedCategoryIds) {
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject category = categoriesArray.getJSONObject(i);
                    if (String.valueOf(category.get("id")).equals(categoryId)) {
                        state.getCategoryIds().add(categoryId);
                        state.getCategoryNames().add(category.optString("name", ""));
                        break;
                    }
                }
            }
            updateFormState(event.getUser().getIdLong(), state);
            if (state.getStep() == FormStep.CATEGORY_MODIFY) {
                showSummary(event);
            } else {
                openDescriptionModal(event, state);
            }
        } catch (Exception e) {
            log.error("Erro na seleção de categorias: {}", e.getMessage());
            showError(event, "Erro ao processar seleção das categorias.");
        }
    }
    private void showCategorySelection(ButtonInteractionEvent event) {
        try {
            event.deferEdit().queue();
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
    private void openDescriptionModal(StringSelectInteractionEvent event, FormState state) {
        log.info("Abrindo modal de descrição e datas");
        TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a descrição do log...")
            .setMaxLength(1000)
            .setRequired(true)
            .build();
        TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true)
            .build();
        TextInput endDateInput = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (deixe vazio se não houver)")
            .setMaxLength(10)
            .setRequired(false)
            .build();
        Modal modal = Modal.create("create-complete-modal", "📝 Finalizar Criação do Log")
            .addActionRow(descriptionInput)
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();
        try {
            event.replyModal(modal).queue();
            log.info("Modal aberto com sucesso!");
        } catch (Exception modalError) {
            log.error("Erro ao abrir modal: {}", modalError.getMessage());
            showError(event, "Erro ao processar seleção das categorias.");
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
        if (event.getHook() != null) {
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setComponents()
                .queue();
        }
    }
    @Override
    public int getPriority() {
        return 4;
    }
}
