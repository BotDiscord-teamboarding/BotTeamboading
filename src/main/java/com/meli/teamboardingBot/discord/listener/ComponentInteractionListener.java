package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.regex.Pattern;

// @Component - DESATIVADO: Usando RefactoredComponentInteractionListener
public class ComponentInteractionListener extends ListenerAdapter {

    @Autowired
    private SquadLogService squadLogService;

    private final Map<Long, FormState> userFormState = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(ComponentInteractionListener.class);

    private static final String BTN_CRIAR = "criar";
    private static final String BTN_ATUALIZAR = "atualizar";
    private static final String BTN_CRIAR_LOG = "criar-log";
    private static final String BTN_EDITAR_LOG = "editar-log";
    private static final String BTN_VOLTAR_LOGS = "voltar-logs";
    private static final String BTN_CRIAR_NOVO = "criar-novo";
    private static final String BTN_ATUALIZAR_EXISTENTE = "atualizar-existente";
    private static final String BTN_SAIR = "sair";

    private static final String BTN_EDIT_SQUAD = "edit-squad";
    private static final String BTN_EDIT_PESSOA = "edit-pessoa";
    private static final String BTN_EDIT_TIPO = "edit-tipo";
    private static final String BTN_EDIT_CATEGORIAS = "edit-categorias";
    private static final String BTN_EDIT_DESCRICAO = "edit-descricao";
    private static final String BTN_EDIT_DATAS = "edit-datas";
    private static final String BTN_VOLTAR_RESUMO = "voltar-resumo";

    private static final String SELECT_SQUAD = "squad-select";
    private static final String SELECT_USER = "user-select";
    private static final String SELECT_TYPE = "type-select";
    private static final String SELECT_CATEGORY = "category-select";
    private static final String SELECT_LOG = "log-select";

    private static final String MODAL_CREATE_FINAL = "modal-create-final";
    private static final String MODAL_EDIT_DESCRICAO = "modal-edit-descricao";
    private static final String MODAL_EDIT_DATAS = "modal-edit-datas";
    private static final String MODAL_DESCRIPTION = "modal-description";
    private static final String MODAL_START_DATE = "modal-start-date";
    private static final String MODAL_END_DATE = "modal-end-date";
    private static final String MODAL_EDIT_DESCRIPTION = "modal-edit-description";
    private static final String MODAL_EDIT_DATES = "modal-edit-dates";

    private static final String BTN_OPEN_START_DATE_MODAL = "open-start-date-modal";
    private static final String BTN_HAS_END_DATE_YES = "has-end-date-yes";
    private static final String BTN_HAS_END_DATE_NO = "has-end-date-no";

    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        long discordUserId = event.getUser().getIdLong();
        logger.info("[BUTTON] Usuário: {} | Botão: {}", discordUserId, buttonId);

        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (buttonId) {
            case BTN_CRIAR -> handleCriarButton(event, state);
            case BTN_ATUALIZAR -> handleAtualizarButton(event, state);
            case BTN_CRIAR_LOG -> handleCriarLogButton(event, state);
            case BTN_EDITAR_LOG -> handleEditarLogButton(event, state);
            case BTN_VOLTAR_LOGS -> handleVoltarLogsButton(event, state);
            case BTN_CRIAR_NOVO -> handleCriarNovoButton(event, state);
            case BTN_ATUALIZAR_EXISTENTE -> handleAtualizarExistenteButton(event, state);
            case BTN_SAIR -> handleSairButton(event, state);
            case BTN_EDIT_SQUAD -> handleEditSquadButton(event, state);
            case BTN_EDIT_PESSOA -> handleEditPessoaButton(event, state);
            case BTN_EDIT_TIPO -> handleEditTipoButton(event, state);
            case BTN_EDIT_CATEGORIAS -> handleEditCategoriasButton(event, state);
            case BTN_EDIT_DESCRICAO -> handleEditDescricaoButton(event, state);
            case BTN_EDIT_DATAS -> handleEditDatasButton(event, state);
            case BTN_VOLTAR_RESUMO -> handleVoltarResumoButton(event, state);
            case "open-create-complete-modal-btn" -> handleOpenCreateCompleteModalButton(event, state);
            case "editar-questionario" -> handleEditarLogButton(event, state);
            case "confirmar-criacao" -> handleCriarLogButton(event, state);
            case "confirmar-atualizacao" -> handleCriarLogButton(event, state);
            case "voltar-questionarios" -> handleVoltarLogsButton(event, state);

            default -> {
                logger.warn("[BUTTON_UNKNOWN] Botão desconhecido: {} | Usuário: {}", buttonId, discordUserId);
                event.reply("❌ Botão não reconhecido.").setEphemeral(true).queue();
            }
        }
    }


    private void handleCriarButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[CRIAR] Iniciando fluxo de criação");
        state.isCreating = true;
        state.isEditing = false;
        showSquadSelection(event);
    }

    private void handleAtualizarButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[ATUALIZAR] Iniciando fluxo de atualização");
        state.isCreating = false;
        state.isEditing = true;
        showLogSelection(event);
    }

    private void handleCriarLogButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[CRIAR_LOG] Executando criação/salvamento do log");
        if (state.isCreating) {
            createSquadLog(event, state);
        } else {
            updateSquadLog(event, state);
        }
    }

    private void handleEditarLogButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDITAR_LOG] Mostrando menu de edição");




        state.isEditing = true;


        logger.info("[EDITAR_LOG] Estado mantido: isEditing={}, isCreating={}", state.isEditing, state.isCreating);
        showEditFieldsMenu(event);
    }

    private void handleVoltarLogsButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[VOLTAR_LOGS] Voltando para seleção de logs");
        showLogSelection(event);
    }

    private void handleCriarNovoButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[CRIAR_NOVO] Iniciando novo fluxo de criação");

        userFormState.put(event.getUser().getIdLong(), new FormState());
        FormState newState = userFormState.get(event.getUser().getIdLong());
        newState.isCreating = true;
        newState.isEditing = false;

        event.deferEdit().queue();
        showSquadSelectionWithHook(event.getHook());
    }

    private void handleAtualizarExistenteButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[ATUALIZAR_EXISTENTE] Iniciando fluxo de atualização");
        state.isCreating = false;
        state.isEditing = true;

        event.deferEdit().queue();
        showLogSelectionWithHook(event.getHook());
    }

    private void handleSairButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[SAIR] Usuário saindo do bot");
        event.deferEdit().queue();
        exitBotWithHook(event.getHook(), event.getUser().getIdLong());
    }

    private void handleEditSquadButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_SQUAD] Editando squad");
        showSquadSelection(event);
    }

    private void handleEditPessoaButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_PESSOA] Editando pessoa");
        showUserSelection(event, state.squadId);
    }

    private void handleEditTipoButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_TIPO] Editando tipo");
        showTypeSelection(event);
    }

    private void handleEditCategoriasButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_CATEGORIAS] Editando categorias");
        showCategorySelection(event);
    }

    private void handleEditDescricaoButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_DESCRICAO] Editando descrição");

        TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a descrição do log...")
            .setValue(state.description != null ? state.description : "")
            .setMaxLength(1000)
            .setRequired(true)
            .build();

        Modal modal = Modal.create(MODAL_EDIT_DESCRIPTION, "📝 Editar Descrição")
            .addActionRow(descriptionInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleEditDatasButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[EDIT_DATAS] Editando datas");


        TextInput.Builder startDateBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 20-06-1986")
            .setMaxLength(10)
            .setRequired(true);


        if (state.startDate != null) {
            startDateBuilder.setValue(formatToBrazilianDate(state.startDate));
        }
        TextInput startDateInput = startDateBuilder.build();

        TextInput.Builder endDateBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - Opcional", TextInputStyle.SHORT)
            .setPlaceholder("Ex: 25-06-1986 (deixe vazio se não houver)")
            .setMaxLength(10)
            .setRequired(false);


        if (state.endDate != null) {
            endDateBuilder.setValue(formatToBrazilianDate(state.endDate));
        }
        TextInput endDateInput = endDateBuilder.build();

        Modal modal = Modal.create(MODAL_EDIT_DATES, "📅 Editar Datas")
            .addActionRow(startDateInput)
            .addActionRow(endDateInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleVoltarResumoButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[VOLTAR_RESUMO] Voltando ao resumo - isEditing={}, isCreating={}", state.isEditing, state.isCreating);


        if (state.isEditing && !state.isCreating) {
            logger.info("[VOLTAR_RESUMO] Voltando ao resumo de criação (2 botões)");
            showCreateSummary(event, state);
        } else if (state.isCreating) {
            logger.info("[VOLTAR_RESUMO] Voltando ao resumo de criação (modo criando)");
            showCreateSummary(event, state);
        } else {
            logger.info("[VOLTAR_RESUMO] Voltando ao resumo de atualização (3 botões)");
            showUpdateSummary(event, state);
        }
    }

    private void handleOpenCreateCompleteModalButton(ButtonInteractionEvent event, FormState state) {
        logger.info("[OPEN_CREATE_COMPLETE_MODAL_BUTTON] Abrindo modal único de criação");

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

        event.replyModal(modal).queue();
    }



    private void showCreateSummaryWithHook(InteractionHook hook, FormState state) {
        logger.info("[SHOW_CREATE_SUMMARY_WITH_HOOK] Exibindo resumo final de criação");


        String squadName = state.squadName != null ? state.squadName : "Não informado";
        String userName = state.userName != null ? state.userName : "Não informado";
        String typeName = state.typeName != null ? state.typeName : "Não informado";
        String categoryNames = (state.categoryNames != null && !state.categoryNames.isEmpty()) ? String.join(", ", state.categoryNames) : "Não informado";
        String description = state.description != null ? state.description : "Não informado";
        String startDateText = state.startDate != null ? formatToBrazilianDate(state.startDate) : "Não informado";
        String endDateText = state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informada";

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 Resumo do Squad Log")
            .setDescription("Confirme os dados antes de criar:")
            .addField("🏢 Squad", squadName, false)
            .addField("👤 Pessoa", userName, false)
            .addField("📝 Tipo", typeName, false)
            .addField("🏷️ Categorias", categoryNames, false)
            .addField("📄 Descrição", description, false)
            .addField("📅 Data de Início", startDateText, false)
            .addField("📅 Data de Fim", endDateText, false)
            .setColor(0x0099FF);


        Button createButton = Button.success("confirmar-criacao", "✅ Criar");
        Button editButton = Button.secondary(BTN_EDITAR_LOG, "✏️ Editar");

        hook.editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(createButton, editButton))
            .queue();
    }

    private void showSquadSelection(ButtonInteractionEvent event) {
        logger.info("[SHOW_SQUAD_SELECTION] Mostrando seleção de squads");

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

            StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create(SELECT_SQUAD)
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
            logger.error("[SHOW_SQUAD_SELECTION] Erro ao carregar squads: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar squads. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showSquadSelectionWithHook(InteractionHook hook) {
        logger.info("[SHOW_SQUAD_SELECTION_HOOK] Mostrando seleção de squads");

        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");

            if (squadsArray == null || squadsArray.length() == 0) {
                hook.editOriginal("❌ Nenhuma squad encontrada.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }

            StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create(SELECT_SQUAD)
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

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_SQUAD_SELECTION_HOOK] Erro ao carregar squads: {}", e.getMessage());
            hook.editOriginal("❌ Erro ao carregar squads. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showUserSelection(ButtonInteractionEvent event, String squadId) {
        logger.info("[SHOW_USER_SELECTION] Mostrando seleção de usuários para squad: {}", squadId);

        try {
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

            StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create(SELECT_USER)
                    .setPlaceholder("Selecione uma pessoa");

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

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Selecione uma Pessoa")
                .setDescription("Escolha quem irá responder:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(userMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_USER_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar usuários")
                .setColor(0xFF0000);
            event.editMessageEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }

    private void showTypeSelection(ButtonInteractionEvent event) {
        logger.info("[SHOW_TYPE_SELECTION] Mostrando seleção de tipos");

        try {
            String logTypesJson = squadLogService.getSquadLogTypes();
            JSONArray logTypesArray = new JSONArray(logTypesJson);

            StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create(SELECT_TYPE)
                    .setPlaceholder("Selecione o tipo");
            buildSelectMenu(logTypesArray, typeMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Selecione um Tipo")
                .setDescription("Escolha o tipo do log:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(typeMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_TYPE_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar tipos")
                .setColor(0xFF0000);
            event.editMessageEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }

    private void showCategorySelection(ButtonInteractionEvent event) {
        logger.info("[SHOW_CATEGORY_SELECTION] Mostrando seleção de categorias");

        try {
            String categoriesJson = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesJson);

            StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create(SELECT_CATEGORY)
                    .setPlaceholder("Selecione as categorias")
                    .setMinValues(1)
                    .setMaxValues(categoriesArray.length());
            buildSelectMenu(categoriesArray, categoryMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏷️ Selecione as Categorias")
                .setDescription("Escolha uma ou mais categorias:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(categoryMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_CATEGORY_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar categorias")
                .setColor(0xFF0000);
            event.editMessageEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }



    private void showLogSelection(ButtonInteractionEvent event) {
        logger.info("[SHOW_LOG_SELECTION] Mostrando seleção de logs");

        try {
            String squadLogsJson = squadLogService.getSquadLogAll();
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");

            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.editMessage("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }

            StringSelectMenu.Builder logMenuBuilder = StringSelectMenu.create(SELECT_LOG)
                    .setPlaceholder("Selecione um questionário");

            buildSelectMenuUpdate(squadLogsArray, logMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(logMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_LOG_SELECTION] Erro ao carregar questionários: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showLogSelectionWithHook(InteractionHook hook) {
        logger.info("[SHOW_LOG_SELECTION_HOOK] Mostrando seleção de logs");

        try {
            String squadLogsJson = squadLogService.getSquadLogAll();
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");

            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                hook.editOriginal("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }

            StringSelectMenu.Builder logMenuBuilder = StringSelectMenu.create(SELECT_LOG)
                    .setPlaceholder("Selecione um questionário");

            buildSelectMenuUpdate(squadLogsArray, logMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(logMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_LOG_SELECTION_HOOK] Erro ao carregar questionários: {}", e.getMessage());
            hook.editOriginal("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }



    private void showCreateSummary(ButtonInteractionEvent event, FormState state) {
        logger.info("[SHOW_CREATE_SUMMARY] Mostrando resumo de criação");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 Resumo do que foi preenchido")
            .setDescription("Verifique todos os dados antes de criar o log:")
            .setColor(0x0099FF);

        buidSumary(state, embed);

        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.success(BTN_CRIAR_LOG, "✅ Criar"),
                Button.secondary(BTN_EDITAR_LOG, "✏️ Editar")
            )
            .queue();
    }

    private void showUpdateSummary(ButtonInteractionEvent event, FormState state) {
        logger.info("[SHOW_UPDATE_SUMMARY] Mostrando resumo de atualização");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 Resumo do Questionário Selecionado")
            .setDescription("Dados atuais do questionário:")
            .setColor(0x0099FF);

        buidSumary(state, embed);


        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.success(BTN_CRIAR_LOG, "💾 Salvar"),
                Button.secondary(BTN_EDITAR_LOG, "✏️ Alterar"),
                Button.primary(BTN_VOLTAR_LOGS, "↩️ Voltar")
            )
            .queue();
    }

    private void buidSumary(FormState state, EmbedBuilder embed) {
        embed.addField("🏢 Squad", state.squadName != null ? state.squadName : "Não informado", false);
        embed.addField("👤 Pessoa", state.userName != null ? state.userName : "Não informado", false);
        embed.addField("📂 Tipo", state.typeName != null ? state.typeName : "Não informado", false);
        embed.addField("🏷️ Categorias", !state.categoryNames.isEmpty() ? String.join(", ", state.categoryNames) : "Não informado", false);
        embed.addField("📝 Descrição", state.description != null ? state.description : "Não informado", false);
        embed.addField("📅 Data de Início", state.startDate != null ? formatToBrazilianDate(state.startDate) : "Não informado", false);
        embed.addField("📅 Data de Fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);
    }


    private void showEditFieldsMenu(ButtonInteractionEvent event) {
        logger.info("[SHOW_EDIT_FIELDS_MENU] Mostrando menu de edição de campos");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);

        event.editMessageEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary(BTN_EDIT_SQUAD, "🏢 Squad"),
                    Button.secondary(BTN_EDIT_PESSOA, "👤 Pessoa"),
                    Button.secondary(BTN_EDIT_TIPO, "📝 Tipo"),
                    Button.secondary(BTN_EDIT_CATEGORIAS, "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary(BTN_EDIT_DESCRICAO, "📄 Descrição"),
                    Button.secondary(BTN_EDIT_DATAS, "📅 Datas"),
                    Button.primary(BTN_VOLTAR_RESUMO, "↩️ Voltar ao resumo")
                )
            )
            .queue();
    }



    private void showWhatToDoMenu(InteractionHook hook) {
        logger.info("[SHOW_WHAT_TO_DO] Mostrando menu 'O que deseja fazer?'");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🎉 O que deseja fazer?")
            .setDescription("Escolha uma das opções abaixo:")
            .setColor(0x0099FF);

        hook.editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.primary(BTN_CRIAR_NOVO, "📝 Criar novo Log"),
                Button.secondary(BTN_ATUALIZAR_EXISTENTE, "✏️ Atualizar Log existente"),
                Button.danger(BTN_SAIR, "🚪 Sair")
            )
            .queue();
    }



    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String selectId = event.getComponentId();
        long discordUserId = event.getUser().getIdLong();
        logger.info("[STRING_SELECT] Usuário: {} | Select: {} | Valor: {}",
                   discordUserId, selectId, event.getValues().get(0));

        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (selectId) {
            case SELECT_SQUAD -> handleSquadSelection(event, state);
            case SELECT_USER -> handleUserSelection(event, state);
            // TYPE selection removed - handled by TypeSelectionHandler
            case SELECT_CATEGORY -> handleCategorySelection(event, state);
            case SELECT_LOG -> handleLogSelection(event, state);
            default -> {
                logger.warn("[STRING_SELECT] ID não reconhecido: {}", selectId);
                event.reply("❌ Seleção não reconhecida.").setEphemeral(true).queue();
            }
        }
    }

    private void handleSquadSelection(StringSelectInteractionEvent event, FormState state) {
        String selectedSquadId = event.getValues().get(0);
        logger.info("[HANDLE_SQUAD_SELECTION] Squad selecionada: {}", selectedSquadId);

        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");

            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(selectedSquadId)) {
                        state.squadId = selectedSquadId;
                        state.squadName = squad.optString("name", "");
                        break;
                    }
                }
            }

            event.deferEdit().queue();

            EmbedBuilder confirmEmbed = new EmbedBuilder()
                .setTitle("✅ Squad selecionada com sucesso!")
                .setDescription("Squad: **" + state.squadName + "**")
                .setColor(0x00FF00);

            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setComponents()
                .queue();

            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {

                if (state.isEditing && !state.isCreating) {
                    showSummaryFromModalForUpdateWithHook(event.getHook(), state);
                } else {

                    showUserSelection(event.getHook(), state, state.squadId);
                }
            });

        } catch (Exception e) {
            logger.error("[HANDLE_SQUAD_SELECTION] Erro: {}", e.getMessage());


            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro na seleção")
                .setDescription("Erro ao processar seleção da squad.")
                .setColor(0xFF0000);

            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setComponents()
                .queue();
        }
    }

    private void handleUserSelection(StringSelectInteractionEvent event, FormState state) {
        String selectedUserId = event.getValues().get(0);
        logger.info("[HANDLE_USER_SELECTION] Usuário selecionado: {}", selectedUserId);

        try {

            if (selectedUserId.equals(state.squadId)) {
                state.userId = selectedUserId;
                state.userName = "All team";
                logger.info("[HANDLE_USER_SELECTION] All team selecionado para squad: {}", state.squadId);
            } else {

                String squadsJson = squadLogService.getSquads();
                JSONObject obj = new JSONObject(squadsJson);
                JSONArray squadsArray = obj.optJSONArray("items");

                if (squadsArray != null) {
                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        if (String.valueOf(squad.get("id")).equals(state.squadId)) {
                            JSONArray userSquads = squad.optJSONArray("user_squads");
                            if (userSquads != null) {
                                for (int j = 0; j < userSquads.length(); j++) {
                                    JSONObject userSquad = userSquads.getJSONObject(j);
                                    JSONObject user = userSquad.optJSONObject("user");
                                    if (user != null && String.valueOf(user.get("id")).equals(selectedUserId)) {
                                        state.userId = selectedUserId;
                                        state.userName = user.optString("first_name", "") + " " + user.optString("last_name", "");
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }

            event.deferEdit().queue();

            EmbedBuilder confirmEmbed = new EmbedBuilder()
                .setTitle("✅ Pessoa selecionada com sucesso!")
                .setDescription("Pessoa: **" + state.userName + "**")
                .setColor(0x00FF00);

            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setComponents()
                .queue();

            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {

                if (state.isEditing && !state.isCreating) {
                    showSummaryFromModalForUpdateWithHook(event.getHook(), state);
                } else {
                    // FIXME: showTypeSelection espera ButtonInteractionEvent, mas temos StringSelectInteractionEvent
                    // Esta classe está desativada - usar RefactoredComponentInteractionListener
                    logger.warn("Tentativa de chamar showTypeSelection com tipos incompatíveis - classe desativada");
                }
            });

        } catch (Exception e) {
            logger.error("[HANDLE_USER_SELECTION] Erro: {}", e.getMessage());


            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro na seleção")
                .setDescription("Erro ao processar seleção do usuário.")
                .setColor(0xFF0000);

            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setComponents()
                .queue();
        }
    }


    private void handleCategorySelection(StringSelectInteractionEvent event, FormState state) {
        List<String> selectedCategoryIds = event.getValues();
        logger.info("[HANDLE_CATEGORY_SELECTION] Categorias selecionadas: {}", selectedCategoryIds);

        try {
            String categoriesJson = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesJson);

            state.categoryIds.clear();
            state.categoryNames.clear();

            for (String categoryId : selectedCategoryIds) {
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject category = categoriesArray.getJSONObject(i);
                    if (String.valueOf(category.get("id")).equals(categoryId)) {
                        state.categoryIds.add(categoryId);
                        state.categoryNames.add(category.optString("name", ""));
                        break;
                    }
                }
            }


            logger.info("[HANDLE_CATEGORY_SELECTION] Estado: isEditing={}, isCreating={}", state.isEditing, state.isCreating);


            if (state.isEditing && !state.isCreating) {
                logger.info("[HANDLE_CATEGORY_SELECTION] Modo edição - voltando ao resumo");
                event.deferEdit().queue();

                EmbedBuilder confirmEmbed = new EmbedBuilder()
                    .setTitle("✅ Categorias selecionadas com sucesso!")
                    .setDescription("Categorias: **" + String.join(", ", state.categoryNames) + "**")
                    .setColor(0x00FF00);

                event.getHook().editOriginalEmbeds(confirmEmbed.build())
                    .setComponents()
                    .queue();

                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    showSummaryFromModalForUpdateWithHook(event.getHook(), state);
                });

            } else {

                logger.info("[HANDLE_CATEGORY_SELECTION] Modo criação - abrindo modal");

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
                    logger.info("[HANDLE_CATEGORY_SELECTION] Modal aberto com sucesso!");
                } catch (Exception modalError) {
                    logger.error("[HANDLE_CATEGORY_SELECTION] Erro ao abrir modal: {}", modalError.getMessage());

                    event.deferEdit().queue();
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Erro ao abrir modal")
                        .setDescription("Erro ao processar seleção das categorias.")
                        .setColor(0xFF0000);
                    event.getHook().editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
                }
            }

        } catch (Exception e) {
            logger.error("[HANDLE_CATEGORY_SELECTION] Erro: {}", e.getMessage());


            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro na seleção")
                .setDescription("Erro ao processar seleção das categorias.")
                .setColor(0xFF0000);


            if (event.getHook() != null) {
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setComponents()
                    .queue();
            }
        }
    }

    private void handleLogSelection(StringSelectInteractionEvent event, FormState state) {
        String selectedLogId = event.getValues().get(0);
        logger.info("[HANDLE_LOG_SELECTION] Log selecionado: {}", selectedLogId);

        try {
            String squadLogsJson = squadLogService.getSquadLogAll();
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");

            if (squadLogsArray != null) {
                for (int i = 0; i < squadLogsArray.length(); i++) {
                    JSONObject log = squadLogsArray.getJSONObject(i);
                    if (String.valueOf(log.get("id")).equals(selectedLogId)) {
                        logger.info("[HANDLE_LOG_SELECTION] Estrutura completa do log JSON: {}", log.toString(2));
                        logger.info("[HANDLE_LOG_SELECTION] Campos disponíveis no JSON: {}", log.keySet());
                        logger.info("[HANDLE_LOG_SELECTION] Campo 'type' existe: {}", log.has("type"));
                        logger.info("[HANDLE_LOG_SELECTION] Campo 'categories' existe: {}", log.has("categories"));
                        logger.info("[HANDLE_LOG_SELECTION] Campo 'squad_log_type' existe: {}", log.has("squad_log_type"));
                        logger.info("[HANDLE_LOG_SELECTION] Campo 'skill_categories' existe: {}", log.has("skill_categories"));

                        state.squadLogId = Long.valueOf(selectedLogId);
                        state.isEditing = true;

                        state.description = log.optString("description", "");
                        state.startDate = log.optString("start_date", "");
                        state.endDate = log.optString("end_date", null);

                        JSONObject squad = log.optJSONObject("squad");
                        if (squad != null) {
                            state.squadId = String.valueOf(squad.get("id"));
                            state.squadName = squad.optString("name", "");
                        }

                        JSONObject user = log.optJSONObject("user");
                        if (user != null) {
                            state.userId = String.valueOf(user.get("id"));
                            state.userName = user.optString("name", "");
                        }

                        JSONObject type = log.optJSONObject("type");
                        if (type == null) {
                            type = log.optJSONObject("squad_log_type");
                        }
                        if (type == null) {
                            type = log.optJSONObject("log_type");
                        }

                        if (type != null) {
                            state.typeId = String.valueOf(type.get("id"));
                            state.typeName = type.optString("name", "");
                            logger.info("[HANDLE_LOG_SELECTION] Type carregado - ID: {}, Nome: {}", state.typeId, state.typeName);
                        } else {
                            logger.error("[HANDLE_LOG_SELECTION] Type não encontrado em nenhum campo (type, squad_log_type, log_type)");
                        }

                        JSONArray categories = log.optJSONArray("categories");
                        if (categories == null) {
                            categories = log.optJSONArray("skill_categories");
                        }
                        if (categories == null) {
                            categories = log.optJSONArray("squad_categories");
                        }

                        state.categoryIds.clear();
                        state.categoryNames.clear();
                        if (categories != null) {
                            for (int j = 0; j < categories.length(); j++) {
                                JSONObject category = categories.getJSONObject(j);
                                state.categoryIds.add(String.valueOf(category.get("id")));
                                state.categoryNames.add(category.optString("name", ""));
                            }
                            logger.info("[HANDLE_LOG_SELECTION] Categories carregadas - IDs: {}, Nomes: {}", state.categoryIds, state.categoryNames);
                        } else {
                            logger.error("[HANDLE_LOG_SELECTION] Categories não encontradas em nenhum campo (categories, skill_categories, squad_categories)");
                        }

                        logger.info("[HANDLE_LOG_SELECTION] Estado completo carregado:");
                        logger.info("[HANDLE_LOG_SELECTION] squadId: {}, userId: {}, typeId: {}", state.squadId, state.userId, state.typeId);
                        logger.info("[HANDLE_LOG_SELECTION] description: {}", state.description);
                        logger.info("[HANDLE_LOG_SELECTION] startDate: {}, endDate: {}", state.startDate, state.endDate);
                        logger.info("[HANDLE_LOG_SELECTION] categoryIds: {}", state.categoryIds);

                        break;
                    }
                }
            }

            event.deferEdit().queue();
            showUpdateSummaryWithHook(event.getHook(), state);

        } catch (Exception e) {
            logger.error("[HANDLE_LOG_SELECTION] Erro: {}", e.getMessage());
            event.reply("❌ Erro ao carregar dados do questionário.").setEphemeral(true).queue();
        }
    }

    private void showUpdateSummaryWithHook(InteractionHook hook, FormState state) {
        logger.info("[SHOW_UPDATE_SUMMARY_HOOK] Mostrando resumo de atualização");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📋 Resumo do Questionário Selecionado")
            .setDescription("Dados atuais do questionário:")
            .setColor(0x0099FF);

        buidSumary(state, embed);


        hook.editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success(BTN_CRIAR_LOG, "💾 Salvar"),
                Button.secondary(BTN_EDITAR_LOG, "✏️ Alterar"),
                Button.primary(BTN_VOLTAR_LOGS, "↩️ Voltar")
            )
            .queue();
    }




    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        long discordUserId = event.getUser().getIdLong();
        logger.info("[MODAL_INTERACTION] Usuário: {} | Modal: {}", discordUserId, modalId);

        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (modalId) {
            case "create-complete-modal" -> handleCreateCompleteModal(event, state);
            case MODAL_DESCRIPTION -> handleDescriptionModal(event, state);
            case MODAL_START_DATE -> handleStartDateModal(event, state);
            case MODAL_END_DATE -> handleEndDateModal(event, state);
            case MODAL_EDIT_DESCRIPTION -> handleEditDescriptionModal(event, state);
            case MODAL_EDIT_DATES -> handleEditDatesModal(event, state);
            default -> {
                logger.warn("[MODAL_INTERACTION] ID não reconhecido: {}", modalId);
                event.reply("❌ Modal não reconhecido.").setEphemeral(true).queue();
            }
        }
    }

    private void handleCreateCompleteModal(ModalInteractionEvent event, FormState state) {
        String description = event.getValue("description").getAsString();
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date").getAsString().trim();

        logger.info("[HANDLE_CREATE_COMPLETE_MODAL] Dados recebidos - Descrição: {} | Data início: {} | Data fim: {}",
                   description, startDate, endDate);


        if (!isValidBrazilianDate(startDate)) {
            event.reply("❌ Data de início inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                .setEphemeral(true).queue();
            return;
        }


        if (!endDate.isEmpty() && !isValidBrazilianDate(endDate)) {
            event.reply("❌ Data de fim inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                .setEphemeral(true).queue();
            return;
        }


        state.description = description;
        state.startDate = convertToIsoDate(startDate);
        state.endDate = endDate.isEmpty() ? null : convertToIsoDate(endDate);

        event.deferEdit().queue();
        showCreateSummaryWithHook(event.getHook(), state);
    }

    private void handleDescriptionModal(ModalInteractionEvent event, FormState state) {
        String description = event.getValue("description").getAsString();
        logger.info("[HANDLE_DESCRIPTION_MODAL] Descrição recebida: {}", description);

        state.description = description;

        event.deferEdit().queue();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Descrição adicionada!")
            .setDescription("**Descrição:** " + description)
            .setColor(0x00FF00);

        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.primary(BTN_OPEN_START_DATE_MODAL, "📅 Inserir Data de Início")
            ))
            .queue();
    }

    private void handleStartDateModal(ModalInteractionEvent event, FormState state) {
        String startDate = event.getValue("start_date").getAsString();
        logger.info("[HANDLE_START_DATE_MODAL] Data de início recebida: {}", startDate);

        if (!isValidBrazilianDate(startDate)) {
            event.reply("❌ Data inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                .setEphemeral(true).queue();
            return;
        }

        state.startDate = convertToIsoDate(startDate);

        event.deferEdit().queue();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ Data de início adicionada!")
            .setDescription("**Data de início:** " + startDate)
            .setColor(0x00FF00);

        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.success(BTN_HAS_END_DATE_YES, "✅ Sim, há data de fim"),
                Button.secondary(BTN_HAS_END_DATE_NO, "❌ Não há data de fim")
            ))
            .queue();
    }

    private void handleEndDateModal(ModalInteractionEvent event, FormState state) {
        String endDate = event.getValue("end_date").getAsString().trim();
        logger.info("[HANDLE_END_DATE_MODAL] Data de fim recebida: {}", endDate);

        if (!endDate.isEmpty()) {
            if (!isValidBrazilianDate(endDate)) {
                event.reply("❌ Data inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                    .setEphemeral(true).queue();
                return;
            }
            state.endDate = convertToIsoDate(endDate);
        } else {
            state.endDate = null;
        }

        event.deferEdit().queue();
        showCreateSummaryWithHook(event.getHook(), state);
    }

    private void handleEditDescriptionModal(ModalInteractionEvent event, FormState state) {
        String description = event.getValue("description").getAsString();
        logger.info("[HANDLE_EDIT_DESCRIPTION_MODAL] Nova descrição: {}", description);

        state.description = description;

        event.deferEdit().queue();


        if (state.isCreating) {

            logger.info("[HANDLE_EDIT_DESCRIPTION_MODAL] Voltando ao resumo de criação (isCreating=true)");
            showCreateSummaryWithHook(event.getHook(), state);
        } else {

            logger.info("[HANDLE_EDIT_DESCRIPTION_MODAL] Voltando ao resumo de edição (isCreating=false)");
            showSummaryFromModalForUpdateWithHook(event.getHook(), state);
        }
    }

    private void handleEditDatesModal(ModalInteractionEvent event, FormState state) {
        String startDate = event.getValue("start_date").getAsString();
        String endDate = event.getValue("end_date").getAsString().trim();

        logger.info("[HANDLE_EDIT_DATES_MODAL] Datas recebidas - Início: {} | Fim: {}", startDate, endDate);

        if (!isValidBrazilianDate(startDate)) {
            event.reply("❌ Data de início inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                .setEphemeral(true).queue();
            return;
        }

        if (!endDate.isEmpty() && !isValidBrazilianDate(endDate)) {
            event.reply("❌ Data de fim inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                .setEphemeral(true).queue();
            return;
        }

        state.startDate = convertToIsoDate(startDate);
        state.endDate = endDate.isEmpty() ? null : convertToIsoDate(endDate);

        event.deferEdit().queue();


        if (state.isCreating) {

            logger.info("[HANDLE_EDIT_DATES_MODAL] Voltando ao resumo de criação (isCreating=true)");
            showCreateSummaryWithHook(event.getHook(), state);
        } else {

            logger.info("[HANDLE_EDIT_DATES_MODAL] Voltando ao resumo de edição (isCreating=false)");
            showSummaryFromModalForUpdateWithHook(event.getHook(), state);
        }
    }



    private void showInstantModalButton(InteractionHook hook) {
        logger.info("[SHOW_INSTANT_MODAL_BUTTON] Exibindo botão para modal instantâneo");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Dados do Log")
            .setDescription("**Próximo:** Preencher descrição e datas")
            .setColor(0x0099FF);

        Button instantButton = Button.success("open-create-complete-modal-btn", "📝 Continuar ➤");

        hook.editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(instantButton))
            .queue();
    }

    private void showAutoModalButton(InteractionHook hook) {
        logger.info("[SHOW_AUTO_MODAL_BUTTON] Exibindo botão automático para modal");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Formulário de Dados")
            .setDescription("**Clique para preencher:**\n• Descrição do log\n• Data de início (DD-MM-AAAA)\n• Data de fim (opcional)")
            .setColor(0x0099FF);

        Button autoModalButton = Button.success("open-create-complete-modal-btn", "📝 Abrir Formulário");

        hook.editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(autoModalButton))
            .queue();
    }

    private void showModalPrompt(InteractionHook hook) {
        logger.info("[SHOW_MODAL_PROMPT] Exibindo prompt para modal único de criação");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Próximo Passo: Dados Finais")
            .setDescription("Agora vamos preencher:\n" +
                          "• **Descrição** do log\n" +
                          "• **Data de início** (DD-MM-AAAA)\n" +
                          "• **Data de fim** (opcional)\n\n" +
                          "👇 **Clique para abrir o formulário:**")
            .setColor(0x0099FF);

        Button openModalButton = Button.success("open-create-complete-modal-btn", "📝 Abrir Formulário");

        hook.editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(openModalButton))
            .queue();
    }

    private void openDescriptionModal(InteractionHook hook) {
        logger.info("[OPEN_DESCRIPTION_MODAL] Abrindo modal de descrição");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📝 Modal de Descrição")
            .setDescription("Por favor, digite a descrição do log:")
            .setColor(0x0099FF);

        hook.editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.primary("open-description-modal-temp", "📝 Abrir Modal de Descrição")
            ))
            .queue();
    }


    private void showEditFieldsMenuWithHook(InteractionHook hook) {
        logger.info("[SHOW_EDIT_FIELDS_MENU_HOOK] Mostrando menu de edição de campos");

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);

        hook.editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary(BTN_EDIT_SQUAD, "🏢 Squad"),
                    Button.secondary(BTN_EDIT_PESSOA, "👤 Pessoa"),
                    Button.secondary(BTN_EDIT_TIPO, "📝 Tipo"),
                    Button.secondary(BTN_EDIT_CATEGORIAS, "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary(BTN_EDIT_DESCRICAO, "📄 Descrição"),
                    Button.secondary(BTN_EDIT_DATAS, "📅 Datas"),
                    Button.primary(BTN_VOLTAR_RESUMO, "↩️ Voltar ao resumo")
                )
            )
            .queue();
    }





    private static class FormState {
        String squadId;
        String squadName;
        String userId;
        String userName;
        String typeId;
        String typeName;
        List<String> categoryIds = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        String description;
        String startDate;
        String endDate;
        Long squadLogId;
        boolean isEditing = false;
        boolean isCreating = false;
    }


    private static final DateTimeFormatter BRAZILIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private boolean isValidBrazilianDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }

        try {
            LocalDate.parse(date.trim(), BRAZILIAN_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            logger.warn("[VALIDATE_DATE] Data inválida: {}", date);
            return false;
        }
    }

    private String convertToIsoDate(String brazilianDate) {
        try {
            LocalDate date = LocalDate.parse(brazilianDate.trim(), BRAZILIAN_DATE_FORMATTER);
            return date.toString();
        } catch (DateTimeParseException e) {
            logger.error("[CONVERT_TO_ISO] Erro ao converter data brasileira: {}", brazilianDate);
            return null;
        }
    }

    private String formatToBrazilianDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(isoDate.trim());
            return date.format(BRAZILIAN_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("[FORMAT_TO_BRAZILIAN] Erro ao formatar data ISO: {}", isoDate);
            return isoDate;
        }
    }


    private void buildSelectMenuUpdate(JSONArray squadLogsArray, StringSelectMenu.Builder menuBuilder) {
        for (int i = 0; i < squadLogsArray.length() && i < 25; i++) {
            JSONObject log = squadLogsArray.getJSONObject(i);

            String logId = String.valueOf(log.get("id"));
            String description = log.optString("description", "Sem descrição");


            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }


            JSONObject squad = log.optJSONObject("squad");
            String squadName = squad != null ? squad.optString("name", "Squad desconhecida") : "Squad desconhecida";

            String optionLabel = squadName + " - " + description;
            if (optionLabel.length() > 100) {
                optionLabel = optionLabel.substring(0, 97) + "...";
            }

            menuBuilder.addOption(optionLabel, logId);
        }
    }


    private JSONObject createSquadLogPayload(FormState state) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("squad_id", Long.valueOf(state.squadId));
            payload.put("user_id", Long.valueOf(state.userId));
            payload.put("squad_log_type_id", Long.valueOf(state.typeId));
            payload.put("description", state.description);
            payload.put("start_date", state.startDate);

            if (state.endDate != null && !state.endDate.trim().isEmpty()) {
                payload.put("end_date", state.endDate);
            }

            JSONArray categoriesArray = new JSONArray();
            for (String categoryId : state.categoryIds) {
                categoriesArray.put(Long.valueOf(categoryId));
            }
            payload.put("squad_category_ids", categoriesArray);

            logger.info("[CREATE_PAYLOAD] Payload criado: {}", payload.toString());

        } catch (Exception e) {
            logger.error("[CREATE_PAYLOAD] Erro ao criar payload: {}", e.getMessage());
        }

        return payload;
    }

    private JSONObject updateSquadLogPayload(FormState state) {
        JSONObject payload = new JSONObject();

        try {
            logger.info("[UPDATE_PAYLOAD] Criando payload com dados:");
            logger.info("[UPDATE_PAYLOAD] squadId: {}", state.squadId);
            logger.info("[UPDATE_PAYLOAD] userId: {}", state.userId);
            logger.info("[UPDATE_PAYLOAD] typeId: {}", state.typeId);
            logger.info("[UPDATE_PAYLOAD] description: {}", state.description);
            logger.info("[UPDATE_PAYLOAD] startDate: {}", state.startDate);
            logger.info("[UPDATE_PAYLOAD] endDate: {}", state.endDate);
            logger.info("[UPDATE_PAYLOAD] categoryIds: {}", state.categoryIds);

            if ((state.typeId == null || state.typeId.trim().isEmpty()) ||
                (state.categoryIds == null || state.categoryIds.isEmpty())) {
                logger.warn("[UPDATE_PAYLOAD] Campos obrigatórios faltando, tentando recarregar do log original");
                reloadMissingFieldsFromOriginalLog(state);
            }

            if (state.squadId != null && !state.squadId.trim().isEmpty()) {
                payload.put("squad_id", Long.valueOf(state.squadId));
            } else {
                logger.error("[UPDATE_PAYLOAD] squadId está nulo ou vazio");
            }

            if (state.userId != null && !state.userId.trim().isEmpty()) {
                payload.put("user_id", Long.valueOf(state.userId));
            } else {
                logger.error("[UPDATE_PAYLOAD] userId está nulo ou vazio");
            }

            if (state.typeId != null && !state.typeId.trim().isEmpty()) {
                payload.put("squad_log_type_id", Long.valueOf(state.typeId));
            } else {
                logger.error("[UPDATE_PAYLOAD] typeId está nulo ou vazio");
            }

            if (state.description != null && !state.description.trim().isEmpty()) {
                payload.put("description", state.description);
            } else {
                logger.error("[UPDATE_PAYLOAD] description está nulo ou vazio");
            }

            if (state.startDate != null && !state.startDate.trim().isEmpty()) {
                payload.put("start_date", state.startDate);
            } else {
                logger.error("[UPDATE_PAYLOAD] startDate está nulo ou vazio");
            }

            if (state.endDate != null && !state.endDate.trim().isEmpty()) {
                payload.put("end_date", state.endDate);
            }

            if (state.categoryIds != null && !state.categoryIds.isEmpty()) {
                JSONArray categoriesArray = new JSONArray();
                for (String categoryId : state.categoryIds) {
                    if (categoryId != null && !categoryId.trim().isEmpty()) {
                        categoriesArray.put(Long.valueOf(categoryId));
                    }
                }
                payload.put("squad_category_ids", categoriesArray);
            } else {
                logger.error("[UPDATE_PAYLOAD] categoryIds está nulo ou vazio");
            }

            logger.info("[UPDATE_PAYLOAD] Payload final criado: {}", payload.toString());

        } catch (Exception e) {
            logger.error("[UPDATE_PAYLOAD] Erro ao criar payload: {}", e.getMessage());
            e.printStackTrace();
        }

        return payload;
    }

    private void reloadMissingFieldsFromOriginalLog(FormState state) {
        try {
            logger.info("[RELOAD_FIELDS] Recarregando campos do log ID: {}", state.squadLogId);
            String squadLogsJson = squadLogService.getSquadLogAll();
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");

            if (squadLogsArray != null) {
                for (int i = 0; i < squadLogsArray.length(); i++) {
                    JSONObject log = squadLogsArray.getJSONObject(i);
                    if (String.valueOf(log.get("id")).equals(String.valueOf(state.squadLogId))) {

                        if (state.typeId == null || state.typeId.trim().isEmpty()) {
                            JSONObject type = log.optJSONObject("type");
                            if (type == null) {
                                type = log.optJSONObject("squad_log_type");
                            }
                            if (type == null) {
                                type = log.optJSONObject("log_type");
                            }

                            if (type != null) {
                                state.typeId = String.valueOf(type.get("id"));
                                state.typeName = type.optString("name", "");
                                logger.info("[RELOAD_FIELDS] Type recarregado - ID: {}, Nome: {}", state.typeId, state.typeName);
                            } else {
                                logger.error("[RELOAD_FIELDS] Type não encontrado em nenhum campo durante reload");
                            }
                        }

                        if (state.categoryIds == null || state.categoryIds.isEmpty()) {
                            JSONArray categories = log.optJSONArray("categories");
                            if (categories == null) {
                                categories = log.optJSONArray("skill_categories");
                            }
                            if (categories == null) {
                                categories = log.optJSONArray("squad_categories");
                            }

                            state.categoryIds.clear();
                            state.categoryNames.clear();
                            if (categories != null) {
                                for (int j = 0; j < categories.length(); j++) {
                                    JSONObject category = categories.getJSONObject(j);
                                    state.categoryIds.add(String.valueOf(category.get("id")));
                                    state.categoryNames.add(category.optString("name", ""));
                                }
                                logger.info("[RELOAD_FIELDS] Categories recarregadas - IDs: {}", state.categoryIds);
                            } else {
                                logger.error("[RELOAD_FIELDS] Categories não encontradas em nenhum campo durante reload");
                            }
                        }

                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[RELOAD_FIELDS] Erro ao recarregar campos: {}", e.getMessage());
        }
    }

    private void createSquadLog(ButtonInteractionEvent event, FormState state) {
        logger.info("[CREATE_SQUAD_LOG] Iniciando criação de log para usuário: {}", event.getUser().getIdLong());

        EmbedBuilder creatingEmbed = new EmbedBuilder()
            .setTitle("⏳ Criando Log...")
            .setColor(0xFFFF00);

        event.editMessageEmbeds(creatingEmbed.build())
            .setComponents()
            .queue(hook -> {
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    try {
                        JSONObject payload = createSquadLogPayload(state);
                        logger.info("[CREATE_SQUAD_LOG] Payload criado: {}", payload.toString());

                        ResponseEntity<String> response = squadLogService.createSquadLog(payload.toString());
                        logger.info("[CREATE_SQUAD_LOG] Response da API: {}", response.getBody());

                        EmbedBuilder successEmbed = new EmbedBuilder()
                            .setTitle("✅ Log criado com sucesso!")
                            .setColor(0x00FF00);

                        hook.editOriginalEmbeds(successEmbed.build()).queue(message -> {
                            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                showWhatToDoMenu(hook);
                            });
                        });

                        userFormState.remove(event.getUser().getIdLong());

                    } catch (Exception e) {
                        logger.error("[CREATE_SQUAD_LOG] Erro ao criar log: {}", e.getMessage());
                        EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Erro ao criar log")
                            .setDescription("Tente novamente mais tarde.")
                            .setColor(0xFF0000);
                        hook.editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
                    }
                });
            });
    }

    private void updateSquadLog(ButtonInteractionEvent event, FormState state) {
        logger.info("[UPDATE_SQUAD_LOG] Iniciando atualização de log para usuário: {}", event.getUser().getIdLong());

        EmbedBuilder updatingEmbed = new EmbedBuilder()
            .setTitle("⏳ Salvando alterações...")
            .setColor(0xFFFF00);

        event.editMessageEmbeds(updatingEmbed.build())
            .setComponents()
            .queue(hook -> {
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    try {
                        JSONObject payload = updateSquadLogPayload(state);
                        logger.info("[UPDATE_SQUAD_LOG] Payload criado: {}", payload.toString());

                        ResponseEntity<String> response = squadLogService.updateSquadLog(state.squadLogId, payload.toString());
                        logger.info("[UPDATE_SQUAD_LOG] Response da API: {}", response.getBody());

                        EmbedBuilder successEmbed = new EmbedBuilder()
                            .setTitle("✅ Log atualizado com sucesso!")
                            .setColor(0x00FF00);

                        hook.editOriginalEmbeds(successEmbed.build()).queue(message -> {
                            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                showWhatToDoMenu(hook);
                            });
                        });

                        userFormState.remove(event.getUser().getIdLong());

                    } catch (Exception e) {
                        logger.error("[UPDATE_SQUAD_LOG] Erro ao atualizar log: {}", e.getMessage());
                        EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Erro ao atualizar log")
                            .setDescription("Tente novamente mais tarde.")
                            .setColor(0xFF0000);
                        hook.editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
                    }
                });
            });
    }



    private void exitBotWithHook(InteractionHook hook, long discordUserId) {

        EmbedBuilder exitingEmbed = new EmbedBuilder()
            .setTitle("⏳ Saindo...")
            .setColor(0xFFFF00);

        hook.editOriginalEmbeds(exitingEmbed.build())
            .setComponents()
            .queue(message -> {

                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    EmbedBuilder thanksEmbed = new EmbedBuilder()
                        .setTitle("🙏 Obrigado por usar o Bot TeamBoarding!")
                        .setColor(0x0099FF);

                    hook.editOriginalEmbeds(thanksEmbed.build()).queue(finalMessage -> {

                        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                            hook.deleteOriginal().queue();

                            userFormState.remove(discordUserId);
                        });
                    });
                });
            });
    }


    private void showUserSelection(net.dv8tion.jda.api.interactions.InteractionHook hook, FormState state, String squadId) {
        try {
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

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Selecione uma Pessoa")
                .setDescription("Escolha quem irá responder:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(userMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_USER_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar usuários")
                .setColor(0xFF0000);
            hook.editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }

    // showTypeSelection method removed - type selection now handled exclusively by TypeSelectionHandler

    private void showCategorySelection(InteractionHook hook, FormState state) {
        try {
            String categoriesJson = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesJson);

            StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                    .setPlaceholder("Selecione as categorias")
                    .setMinValues(1)
                    .setMaxValues(categoriesArray.length());
            buildSelectMenu(categoriesArray, categoryMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏷️ Selecione as Categorias")
                .setDescription("Escolha uma ou mais categorias:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(categoryMenuBuilder.build())
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_CATEGORY_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar categorias")
                .setColor(0xFF0000);
            hook.editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }


    private void showSummaryFromModalForUpdateWithHook(InteractionHook hook, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo do Questionário");
        embed.setColor(0x0099FF);
        buidSumary(state, embed);


        String buttonText = state.isEditing ? "💾 Salvar" : "✅ Criar";

        hook.editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.success("criar-log", buttonText),
                    Button.secondary(BTN_EDITAR_LOG, "✏️ Alterar")
                )
            )
            .queue();
    }

    private boolean isYesResponse(String response) {
        if (response == null) return false;
        String normalized = response.trim().toLowerCase();
        return normalized.equals("sim") || normalized.equals("s") ||
               normalized.equals("yes") || normalized.equals("y");
    }

    private boolean isNoResponse(String response) {
        if (response == null) return false;
        String normalized = response.trim().toLowerCase();
        return normalized.equals("nao") || normalized.equals("não") ||
               normalized.equals("n") || normalized.equals("no");
    }

    private void showSummaryUpdate(StringSelectInteractionEvent event, JSONObject squadLog) {
        try {
            long discordUserId = event.getUser().getIdLong();
            FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

            state.squadLogId = squadLog.getLong("id");
            logger.info("[SHOW_SUMMARY_UPDATE] Squad Log ID definido: {}", state.squadLogId);

            logger.info("[SHOW_SUMMARY_UPDATE] Estrutura do JSON recebido: {}", squadLog.keySet());
            logger.debug("[SHOW_SUMMARY_UPDATE] JSON completo: {}", squadLog.toString());

            try {
                state.squadId = String.valueOf(squadLog.getJSONObject("squad").get("id"));
                state.squadName = squadLog.getJSONObject("squad").getString("name");
            } catch (Exception e) {
                logger.error("[SHOW_SUMMARY_UPDATE] Erro ao extrair dados da squad: {}", e.getMessage());
                state.squadName = "Não informado";
            }

            try {
                state.userId = String.valueOf(squadLog.getJSONObject("user").get("id"));
                state.userName = squadLog.getJSONObject("user").getString("first_name") + " " +
                               squadLog.getJSONObject("user").getString("last_name");
            } catch (Exception e) {
                logger.error("[SHOW_SUMMARY_UPDATE] Erro ao extrair dados do usuário: {}", e.getMessage());
                state.userName = "Não informado";
            }

            try {
                state.typeId = String.valueOf(squadLog.getJSONObject("squad_log_type").get("id"));
                state.typeName = squadLog.getJSONObject("squad_log_type").getString("name");
            } catch (Exception e) {
                logger.error("[SHOW_SUMMARY_UPDATE] Erro ao extrair dados do tipo: {}", e.getMessage());
                state.typeName = "Não informado";
            }

            state.description = squadLog.optString("description", "Não informado");
            state.startDate = squadLog.optString("start_date", null);
            state.endDate = squadLog.optString("end_date", null);

            state.categoryIds = new ArrayList<>();
            state.categoryNames = new ArrayList<>();

            if (squadLog.has("squad_log_categories")) {
                JSONArray categories = squadLog.getJSONArray("squad_log_categories");
                for (int i = 0; i < categories.length(); i++) {
                    JSONObject category = categories.getJSONObject(i).getJSONObject("squad_category");
                    state.categoryIds.add(String.valueOf(category.get("id")));
                    state.categoryNames.add(category.getString("name"));
                }
            } else if (squadLog.has("categories")) {

                JSONArray categories = squadLog.getJSONArray("categories");
                for (int i = 0; i < categories.length(); i++) {
                    JSONObject category = categories.getJSONObject(i);
                    state.categoryIds.add(String.valueOf(category.get("id")));
                    state.categoryNames.add(category.getString("name"));
                }
            } else {

                logger.warn("[SHOW_SUMMARY_UPDATE] Campo de categorias não encontrado no JSON. Campos disponíveis: {}", squadLog.keySet());
                state.categoryNames.add("Não informado");
            }


            state.isEditing = true;
            logger.info("[SHOW_SUMMARY_UPDATE] Estado isEditing definido como true para usuário: {}", discordUserId);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("📋 Resumo do Questionário Selecionado");
            embed.setColor(0x0099FF);
            buidSumary(state, embed);

            event.editMessageEmbeds(embed.build())
                .setActionRow(
                    Button.primary("editar-questionario", "✏️ Editar"),
                    Button.secondary("voltar-questionarios", "↩️ Voltar")
                )
                .queue();

        } catch (Exception e) {
            logger.error("[SHOW_SUMMARY_UPDATE] Erro: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar dados do questionário.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private static void buildSelectMenu(JSONArray itemsArray, StringSelectMenu.Builder menuBuilder) {
        for (int i = 0; i < itemsArray.length(); i++) {
            JSONObject item = itemsArray.getJSONObject(i);
            String name = item.optString("name", "");
            if (!name.isEmpty()) {
                menuBuilder.addOption(name, String.valueOf(item.get("id")));
            }
        }
    }




    private String buildSquadLogPayload(String squadId, String userId, String typeId,
                                      List<String> categoryIds, String description,
                                      String startDate, String endDate) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(squadId));
        payload.put("user_id", Integer.parseInt(userId));
        payload.put("squad_log_type_id", Integer.parseInt(typeId));
        payload.put("description", description);
        payload.put("start_date", startDate);
        if (endDate != null) {
            payload.put("end_date", endDate);
        }

        JSONArray categoriesArray = new JSONArray();
        for (String categoryId : categoryIds) {
            categoriesArray.put(Integer.parseInt(categoryId));
        }
        payload.put("squad_category_ids", categoriesArray);

        return payload.toString();
    }

    private String formatFormState(FormState state) {
        return String.format("Squad: %s (%s), User: %s (%s), Type: %s (%s), Categories: %s, Description: %s, StartDate: %s, EndDate: %s",
                state.squadName, state.squadId, state.userName, state.userId,
                state.typeName, state.typeId, state.categoryNames,
                state.description, state.startDate, state.endDate);
    }



}