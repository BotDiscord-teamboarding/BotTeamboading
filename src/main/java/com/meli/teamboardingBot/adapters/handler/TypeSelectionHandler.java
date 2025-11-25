package com.meli.teamboardingBot.adapters.handler;
import com.meli.teamboardingBot.core.domain.enums.FormStep;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(3)
public class TypeSelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    @Autowired
    private SummaryHandler summaryHandler;

    @Autowired
    private MessageSource messageSource;

    private java.util.Locale getUserLocale(long userId) {
        return formStateService.getOrCreateState(userId).getLocale();
    }
    
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
            String typesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadLogTypes());
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
            showError(event, messageSource.getMessage("txt_erro_processar_selecao_do_tipo", null, getUserLocale(event.getUser().getIdLong())) + ".");
        }
    }
    private void showTypeSelection(ButtonInteractionEvent event) {
        try {
            event.deferEdit().queue();
            String logTypesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadLogTypes());
            JSONArray logTypesArray = new JSONArray(logTypesJson);
            StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_o_tipo", null, getUserLocale(event.getUser().getIdLong())));
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
                .setTitle("📝 " + messageSource.getMessage("txt_selecione_um_tipo", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription( messageSource.getMessage("txt_escolha_o_tipo_do_log", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(0x0099FF);
            if (hasTypes) {
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(typeMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription("❌ "+ messageSource.getMessage("txt_nenhum_tipo_disponivel_no_momento", null, getUserLocale(event.getUser().getIdLong())) + ".");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar tipos: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ "+ messageSource.getMessage("txt_erro_carregar_tipos", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_erro_carregar_tipos", null, getUserLocale(event.getUser().getIdLong())) + ". " +messageSource.getMessage("txt_tente_novamente", null, getUserLocale(event.getUser().getIdLong()))+".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))))
                .queue();
        }
    }
    private void showCategorySelectionAfterType(StringSelectInteractionEvent event) {
        try {
            String categoriesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadCategories());
            JSONArray categoriesArray = new JSONArray(categoriesJson);
            StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_as_categorias", null, getUserLocale(event.getUser().getIdLong())))
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
                .setTitle("🏷️ " + messageSource.getMessage("txt_selecione_as_categorias", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_escolha_uma_ou_mais_categorias", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(0x0099FF);
            if (hasCategories) {
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(categoryMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription("❌ "+ messageSource.getMessage("txt_nenhuma_categoria_disponivel", null, getUserLocale(event.getUser().getIdLong())) + ".");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar categorias: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ "+ messageSource.getMessage("txt_erro_carregar_categorias", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_erro_carregar_categorias", null, getUserLocale(event.getUser().getIdLong())) + ". " + messageSource.getMessage("txt_tente_novamente", null, getUserLocale(event.getUser().getIdLong())) + "." )
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))))
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
            .setTitle("❌ " + messageSource.getMessage("txt_erro", null, getUserLocale(event.getUser().getIdLong())))
            .setDescription(message)
            .setColor(0xFF0000);
        event.getHook().editOriginalEmbeds(errorEmbed.build())
            .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))))
            .queue();
    }
    @Override
    public int getPriority() {
        return 3;
    }
}
