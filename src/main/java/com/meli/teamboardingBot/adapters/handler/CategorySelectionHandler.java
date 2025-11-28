package com.meli.teamboardingBot.adapters.handler;

import com.meli.teamboardingBot.core.domain.enums.FormStep;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.*;
import com.meli.teamboardingBot.adapters.out.language.SquadLogService;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
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
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Order(4)
public class CategorySelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    private SummaryHandler summaryHandler;
    private MessageSource messageSource;

    private Locale getUserLocale(long userId) {
        try {
            FormState state = getOrCreateFormStatePort.getOrCreateState(userId);
            if (state != null && state.getLocale() != null) {
                return state.getLocale();
            }
        } catch (Exception e) {
            loggerApiPort.warn("Erro ao obter locale do usuário {}: {}", userId, e.getMessage());
        }
        return Locale.getDefault();
    }

    @Autowired
    public CategorySelectionHandler(GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, GetFormStatePort getFormStatePort, SetBatchEntriesPort setBatchEntriesPort, SetBatchCurrentIndexPort setBatchCurrentIndexPort, GetBatchEntriesPort getBatchEntriesPort, GetBatchCurrentIndexPort getBatchCurrentIndexPort, ClearBatchStatePort clearBatchStatePort, DeleteFormStatePort deleteFormStatePort, ResetFormStatePort resetFormStatePort, LoggerApiPort loggerApiPort, SquadLogService squadLogService, SummaryHandler summaryHandler, MessageSource messageSource) {
        super(getOrCreateFormStatePort, putFormStatePort, getFormStatePort, setBatchEntriesPort, setBatchCurrentIndexPort, getBatchEntriesPort, getBatchCurrentIndexPort, clearBatchStatePort, deleteFormStatePort, resetFormStatePort, loggerApiPort);
        this.squadLogService = squadLogService;
        this.summaryHandler = summaryHandler;
        this.messageSource = messageSource;
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
           loggerApiPort.info("Iniciando seleção de categoria");
        state.setStep(FormStep.CATEGORY_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showCategorySelection(event);
    }

    private void handleEditCategoriesButton(ButtonInteractionEvent event, FormState state) {
           loggerApiPort.info("Editando categorias");
        state.setStep(FormStep.CATEGORY_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showCategorySelection(event);
    }


    private void handleCategorySelect(StringSelectInteractionEvent event, FormState state) {
        List<String> selectedCategoryIds = event.getValues();
           loggerApiPort.info("Categorias selecionadas: {}", selectedCategoryIds);
        try {
            loggerApiPort.info("Obtendo categorias da API...");
            String categoriesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadCategories());
            loggerApiPort.info("Resposta da API de categorias: {}", categoriesJson);
            JSONArray categoriesArray = new JSONArray(categoriesJson);
            loggerApiPort.info("JSON parseado com sucesso, {} categorias encontradas", categoriesArray.length());
            state.getCategoryIds().clear();
            state.getCategoryNames().clear();
            loggerApiPort.info("Processando {} categorias selecionadas", selectedCategoryIds.size());
            for (String categoryId : selectedCategoryIds) {
                loggerApiPort.info("Procurando categoria com ID: {}", categoryId);
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject category = categoriesArray.getJSONObject(i);
                    if (String.valueOf(category.get("id")).equals(categoryId)) {
                        state.getCategoryIds().add(categoryId);
                        state.getCategoryNames().add(category.optString("name", ""));
                        loggerApiPort.info("Categoria encontrada: {} - {}", categoryId, category.optString("name", ""));
                        break;
                    }
                }
            }
            loggerApiPort.info("Atualizando FormState...");
            updateFormState(event.getUser().getIdLong(), state);
            loggerApiPort.info("FormState atualizado. Step atual: {}", state.getStep());
            if (state.getStep() == FormStep.CATEGORY_MODIFY) {
                loggerApiPort.info("Mostrando resumo (modo edição)");
                // Para modo edição, precisamos defer antes de chamar showSummary
                event.deferEdit().queue();
                showSummary(event);
            } else {
                loggerApiPort.info("Abrindo modal de descrição (modo criação)");
                openDescriptionModal(event, state);
            }
        } catch (Exception e) {
               loggerApiPort.error("Erro na seleção de categorias: {}", e.getMessage(), e);
            try {
                String errorMessage = "Erro ao processar seleção das categorias.";
                if (state != null && state.getLocale() != null) {
                    errorMessage = messageSource.getMessage("txt_erro_processar_selecao_das_categorias", null, state.getLocale());
                }
                showError(event, errorMessage);
            } catch (Exception errorHandlingException) {
                loggerApiPort.error("Erro ao tratar erro de seleção de categorias: {}", errorHandlingException.getMessage());
                showError(event, "Erro ao processar seleção das categorias.");
            }
        }
    }

    private void showCategorySelection(ButtonInteractionEvent event) {
        try {
            event.deferEdit().queue();
            FormState state = getFormState(event.getUser().getIdLong());
            if (state == null) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Sessão Expirada")
                        .setDescription("Sua sessão expirou. Use /squad-log para começar novamente.")
                        .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setComponents()
                        .queue();
                return;
            }
            String categoriesJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquadCategories());
            JSONArray categoriesArray = new JSONArray(categoriesJson);
            StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_as_categorias", null, state.getLocale()))
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
                    .setTitle("🏷️ " + messageSource.getMessage("txt_selecione_as_categorias", null, state.getLocale()))
                    .setDescription(messageSource.getMessage("txt_escolha_uma_ou_mais_categorias", null, state.getLocale()) + ":")
                    .setColor(0x0099FF);
            if (hasCategories) {
                event.getHook().editOriginalEmbeds(embed.build())
                        .setActionRow(categoryMenuBuilder.build())
                        .queue();
            } else {
                embed.setDescription("❌ " + messageSource.getMessage("txt_nenhuma_categoria_disponivel", null, state.getLocale()) + ".");
                event.getHook().editOriginalEmbeds(embed.build())
                        .setComponents()
                        .queue();
            }
        } catch (Exception e) {
               loggerApiPort.error("Erro ao carregar categorias: {}", e.getMessage());
            FormState state = getFormState(event.getUser().getIdLong());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Erro ao Carregar Categorias")
                    .setDescription("Ocorreu um erro ao carregar as categorias. Tente novamente.")
                    .setColor(0xFF0000);
            
            if (state != null) {
                errorEmbed.setTitle("❌ " + messageSource.getMessage("txt_erro_carregar_categorias", null, state.getLocale()));
                errorEmbed.setDescription(messageSource.getMessage("txt_erro_carregar_categorias_mensagem", null, state.getLocale()) + ".");
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, state.getLocale())))
                        .queue();
            } else {
                errorEmbed.setTitle("❌ Sessão Expirada");
                errorEmbed.setDescription("Sua sessão expirou. Use /squad-log para começar novamente.");
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setComponents()
                        .queue();
            }
        }
    }

    private void openDescriptionModal(StringSelectInteractionEvent event, FormState state) {
           loggerApiPort.info("Abrindo modal de descrição e datas diretamente");
        try {
            TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Digite a descrição do log...")
                    .setMaxLength(1000)
                    .setRequired(true)
                    .build();
            TextInput startDateInput = TextInput.create("start_date", "Data Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                    .setPlaceholder("Ex: 28-11-2025")
                    .setMaxLength(10)
                    .setRequired(true)
                    .build();
            TextInput endDateInput = TextInput.create("end_date", "Data Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
                    .setPlaceholder("Ex: 30-11-2025 (opcional)")
                    .setMaxLength(10)
                    .setRequired(false)
                    .build();
            
            // Tentar usar mensagens localizadas se possível
            try {
                if (state != null && state.getLocale() != null) {
                    String descLabel = messageSource.getMessage("txt_descricao", null, state.getLocale());
                    String descPlaceholder = messageSource.getMessage("txt_digite_a_descricao_do_log", null, state.getLocale());
                    String startLabel = messageSource.getMessage("txt_data_inicio", null, state.getLocale());
                    String endLabel = messageSource.getMessage("txt_data_fim", null, state.getLocale());
                    String opcional = messageSource.getMessage("txt_opcional", null, state.getLocale());
                    String modalTitle = messageSource.getMessage("txt_finalizar_criacao_do_log", null, state.getLocale());
                    
                    descriptionInput = TextInput.create("description", descLabel, TextInputStyle.PARAGRAPH)
                            .setPlaceholder(descPlaceholder + "...")
                            .setMaxLength(1000)
                            .setRequired(true)
                            .build();
                    startDateInput = TextInput.create("start_date", startLabel + " (DD-MM-AAAA)", TextInputStyle.SHORT)
                            .setPlaceholder("Ex: 28-11-2025")
                            .setMaxLength(10)
                            .setRequired(true)
                            .build();
                    endDateInput = TextInput.create("end_date", endLabel + " (DD-MM-AAAA) - " + opcional, TextInputStyle.SHORT)
                            .setPlaceholder("Ex: 30-11-2025 (opcional)")
                            .setMaxLength(10)
                            .setRequired(false)
                            .build();
                    
                    Modal modal = Modal.create("create-complete-modal", "📝 " + modalTitle)
                            .addActionRow(descriptionInput)
                            .addActionRow(startDateInput)
                            .addActionRow(endDateInput)
                            .build();
                    
                    event.replyModal(modal).queue();
                    loggerApiPort.info("Modal aberto com sucesso (localizado)!");
                    return;
                }
            } catch (Exception localeError) {
                loggerApiPort.warn("Erro ao usar mensagens localizadas no modal, usando padrão: {}", localeError.getMessage());
            }
            
            Modal modal = Modal.create("create-complete-modal", "📝 Finalizar Criação do Log")
                    .addActionRow(descriptionInput)
                    .addActionRow(startDateInput)
                    .addActionRow(endDateInput)
                    .build();
            
            event.replyModal(modal).queue();
            loggerApiPort.info("Modal aberto com sucesso!");
        } catch (Exception modalError) {
               loggerApiPort.error("Erro ao abrir modal diretamente: {}", modalError.getMessage(), modalError);
            // Como fallback, mostrar uma mensagem de erro
            event.reply("❌ Erro ao abrir modal. Tente novamente.").setEphemeral(true).queue();
        }
    }

    private void showSummary(StringSelectInteractionEvent event) {
        FormState state = getFormState(event.getUser().getIdLong());
        if (state != null) {
            summaryHandler.showUpdateSummary(event, state);
        }
    }

    private void showError(StringSelectInteractionEvent event, String message) {
        try {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Erro")
                    .setDescription(message)
                    .setColor(0xFF0000);
            
            // Tentar usar locale se possível
            try {
                Locale userLocale = getUserLocale(event.getUser().getIdLong());
                if (userLocale != null) {
                    errorEmbed.setTitle("❌ " + messageSource.getMessage("txt_erro", null, userLocale));
                }
            } catch (Exception localeError) {
                loggerApiPort.warn("Erro ao obter locale do usuário: {}", localeError.getMessage());
            }
            
            if (event.getHook() != null) {
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setComponents()
                        .queue();
            }
        } catch (Exception e) {
            loggerApiPort.error("Erro ao mostrar mensagem de erro: {}", e.getMessage());
        }
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
