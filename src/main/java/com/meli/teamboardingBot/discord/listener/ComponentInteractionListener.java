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
import org.springframework.http.HttpStatus;
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
import java.util.regex.Pattern;

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

        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (buttonId) {
            case "criar" -> {
                logger.info("[CRIAR_SQUAD] Iniciando processo de criação para usuário: {}", discordUserId);
                showSquadSelection(event, state);
            }
            case "atualizar" -> {
                logger.info("[ATUALIZAR_SQUAD] Iniciando processo de atualização para usuário: {}", discordUserId);
                showQuestionarioSelection(event, state);
            }
            case "criar-log" -> {
                logger.info("[CRIAR_LOG] Criando squad log para usuário: {}", discordUserId);
                createSquadLog(event, state);
            }
            case "alterar-log" -> {
                logger.info("[ALTERAR_LOG] Mostrando opções de alteração para usuário: {}", discordUserId);
                showEditFieldsMenu(event, state);
            }
            case "criar-novo-log" -> {
                logger.info("[CRIAR_NOVO_LOG] Iniciando novo processo de criação para usuário: {}", discordUserId);
                
                userFormState.put(discordUserId, new FormState());
                event.deferEdit().queue();
                showSquadSelectionWithHook(event.getHook(), userFormState.get(discordUserId));
            }
            case "alterar-log-existente" -> {
                logger.info("[ALTERAR_LOG_EXISTENTE] Iniciando processo de alteração para usuário: {}", discordUserId);
                event.deferEdit().queue();
                showQuestionarioSelectionWithHook(event.getHook(), state);
            }
            case "sair" -> {
                logger.info("[SAIR] Usuário saindo: {}", discordUserId);
                event.deferEdit().queue();
                exitBotWithHook(event.getHook(), discordUserId);
            }
            case "voltar-questionarios" -> {
                logger.info("[VOLTAR_QUESTIONARIOS] Voltando para seleção de questionários: {}", discordUserId);
                showQuestionarioSelection(event, state);
            }
            case "editar-questionario" -> {
                logger.info("[EDITAR_QUESTIONARIO] Mostrando menu de edição: {}", discordUserId);
                showEditFieldsMenu(event, state);
            }
            case "retornar-resumo" -> {
                logger.info("[RETORNAR_RESUMO] Retornando ao resumo: {}", discordUserId);
                showSummaryWithButtons(event, state);
            }
            
            case "edit-squad" -> editSquad(event, state);
            case "edit-pessoa" -> editPessoa(event, state);
            case "edit-tipo" -> editTipo(event, state);
            case "edit-categorias" -> editCategorias(event, state);
            case "edit-descricao" -> editDescricao(event, state);
            case "edit-datas" -> {
                logger.info("[EDIT_DATAS] Editando datas para usuário: {}", discordUserId);

                TextInput.Builder startDateInputBuilder = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986")
                        .setRequired(true)
                        .setMaxLength(10);

                if (state.startDate != null) {
                    String currentStartDate = formatToBrazilianDate(state.startDate);
                    startDateInputBuilder.setValue(currentStartDate);
                }
                
                TextInput.Builder endDateInputBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - OPCIONAL", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 25-06-1986 (deixe em branco para remover)")
                        .setRequired(false)
                        .setMaxLength(10);

                if (state.endDate != null) {
                    String currentEndDate = formatToBrazilianDate(state.endDate);
                    endDateInputBuilder.setValue(currentEndDate);
                }
                
                TextInput startDateInput = startDateInputBuilder.build();
                TextInput endDateInput = endDateInputBuilder.build();

                Modal datesModal = Modal.create("dates-modal-edit", "📅 Editar Datas")
                        .addActionRow(startDateInput)
                        .addActionRow(endDateInput)
                        .build();

                event.replyModal(datesModal).queue();
            }
            case "open-dates-modal-auto" -> {
                logger.info("[OPEN_DATES_MODAL_AUTO] Abrindo modal de datas para usuário: {}", discordUserId);

                TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986")
                        .setMinLength(10)
                        .setMaxLength(10)
                        .build();

                TextInput endDateInput = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - OPCIONAL", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986 (deixe em branco se não houver)")
                        .setRequired(false)
                        .setMaxLength(10)
                        .build();

                Modal datesModal = Modal.create("dates-modal-create", "📅 Adicionar Datas")
                        .addActionRow(startDateInput)
                        .addActionRow(endDateInput)
                        .build();

                event.replyModal(datesModal).queue();
            }
            case "open-description-modal-create" -> {
                
                TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Digite a descrição detalhada...")
                        .setMinLength(10)
                        .setMaxLength(1000)
                        .build();

                Modal modal = Modal.create("description-modal-create", "📝 Adicionar Descrição")
                        .addActionRow(descriptionInput)
                        .build();

                event.replyModal(modal).queue();
            }
            case "open-start-date-modal" -> {
                
                TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986")
                        .setMinLength(10)
                        .setMaxLength(10)
                        .build();

                TextInput hasEndDateInput = TextInput.create("has_end_date", "Há data de fim? (sim/não)", TextInputStyle.SHORT)
                        .setPlaceholder("Digite: sim, s, não, nao, n")
                        .setMinLength(1)
                        .setMaxLength(10)
                        .build();

                Modal startDateModal = Modal.create("start-date-modal-create", "📅 Adicionar Data de Início")
                        .addActionRow(startDateInput)
                        .addActionRow(hasEndDateInput)
                        .build();

                event.replyModal(startDateModal).queue();
            }
            case "confirmar-atualizacao" -> {
                logger.info("[CONFIRMAR_ATUALIZACAO] Atualizando squad log para usuário: {}", discordUserId);
                updateSquadLog(event, state);
            }
            case "has-end-date-yes" -> {
                
                TextInput endDateInput = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA)", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986")
                        .setMinLength(10)
                        .setMaxLength(10)
                        .build();

                Modal endDateModal = Modal.create("end-date-modal-create", "📅 Adicionar Data de Fim")
                        .addActionRow(endDateInput)
                        .build();

                event.replyModal(endDateModal).queue();
            }
            case "has-end-date-no" -> {
                
                FormState state2 = userFormState.get(discordUserId);
                if (state2 != null) {
                    state2.endDate = null;
                    state2.step = FormStep.REVIEW;
                    showSummaryWithButtons(event, state2);
                }
            }
            case "open-end-date-modal-modify" -> {
                
                TextInput.Builder endDateInputBuilder = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - OPCIONAL", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986 (deixe em branco para remover)")
                        .setRequired(false)
                        .setMaxLength(10);

                if (state.endDate != null) {
                    String formattedDate = formatToBrazilianDate(state.endDate);
                    if (formattedDate != null && !formattedDate.trim().isEmpty()) {
                        endDateInputBuilder.setValue(formattedDate);
                    }
                }
                
                TextInput endDateInput = endDateInputBuilder.build();

                Modal endDateModal = Modal.create("end-date-modal-edit", "📅 Editar Data de Fim (Opcional)")
                        .addActionRow(endDateInput)
                        .build();

                event.replyModal(endDateModal).queue();
            }
            default -> {
                logger.warn("[BUTTON_UNKNOWN] Botão desconhecido: {} | Usuário: {}", buttonId, discordUserId);
                event.reply("❌ Botão não reconhecido.").setEphemeral(true).queue();
            }
        }
    }

    private void showSquadSelection(ButtonInteractionEvent event, FormState state) {
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
                .setDescription("Escolha a squad para criar o log:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
                
            state.step = FormStep.SQUAD;
            
        } catch (Exception e) {
            logger.error("[SHOW_SQUAD_SELECTION] Erro ao carregar squads: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar squads. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showSquadSelectionWithHook(InteractionHook hook, FormState state) {
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
                .setDescription("Escolha a squad para criar o log:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
                
            state.step = FormStep.SQUAD;
            
        } catch (Exception e) {
            logger.error("[SHOW_SQUAD_SELECTION_HOOK] Erro ao carregar squads: {}", e.getMessage());
            hook.editOriginal("❌ Erro ao carregar squads. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showQuestionarioSelection(ButtonInteractionEvent event, FormState state) {
        logger.info("[SHOW_QUESTIONARIO_SELECTION] Mostrando seleção de questionários");
        
        try {
            String squadLogsJson = squadLogService.getSquadLogAll();
            logger.info("[SHOW_QUESTIONARIO_SELECTION] Response da API: {}", squadLogsJson);
            
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            
            logger.info("[SHOW_QUESTIONARIO_SELECTION] Array de logs: {} items encontrados", 
                squadLogsArray != null ? squadLogsArray.length() : 0);

            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.editMessage("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }

            StringSelectMenu.Builder questionarioMenuBuilder = StringSelectMenu.create("squad-logs-select-update")
                    .setPlaceholder("Selecione um questionário");

            buildSelectMenuUpdate(squadLogsArray, questionarioMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);

            event.editMessageEmbeds(embed.build())
                .setActionRow(questionarioMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("[SHOW_QUESTIONARIO_SELECTION] Erro ao carregar questionários: {}", e.getMessage());
            event.editMessage("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void showQuestionarioSelectionWithHook(InteractionHook hook, FormState state) {
        logger.info("[SHOW_QUESTIONARIO_SELECTION_HOOK] Mostrando seleção de questionários");
        
        try {
            String squadLogsJson = squadLogService.getSquadLogAll();
            logger.info("[SHOW_QUESTIONARIO_SELECTION_HOOK] Response da API: {}", squadLogsJson);
            
            JSONObject obj = new JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            
            logger.info("[SHOW_QUESTIONARIO_SELECTION_HOOK] Array de logs: {} items encontrados", 
                squadLogsArray != null ? squadLogsArray.length() : 0);

            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                hook.editOriginal("❌ Nenhum questionário encontrado.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }

            StringSelectMenu.Builder questionarioMenuBuilder = StringSelectMenu.create("squad-logs-select-update")
                    .setPlaceholder("Selecione um questionário");

            buildSelectMenuUpdate(squadLogsArray, questionarioMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Selecione um Questionário")
                .setDescription("Escolha o questionário que deseja atualizar:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(questionarioMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("[SHOW_QUESTIONARIO_SELECTION_HOOK] Erro ao carregar questionários: {}", e.getMessage());
            hook.editOriginal("❌ Erro ao carregar questionários. Tente novamente.")
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }

    private void createSquadLog(ButtonInteractionEvent event, FormState state) {
        
        EmbedBuilder creatingEmbed = new EmbedBuilder()
            .setTitle("⏳ Criando Log...")
            .setColor(0xFFFF00);
        
        event.editMessageEmbeds(creatingEmbed.build())
            .setComponents()
            .queue(hook -> {
                
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    try {
                        
                        String payload = createSquadLogPayload(state);
                        logger.info("[CREATE_SQUAD_LOG] Payload criado: {}", payload);

                        ResponseEntity<String> response = squadLogService.createSquadLog(payload);
                        logger.info("[CREATE_SQUAD_LOG] Response da API: {} - {}", response.getStatusCode(), response.getBody());

                        EmbedBuilder successEmbed = new EmbedBuilder()
                            .setTitle("✅ Log criado com sucesso!")
                            .setColor(0x00FF00);
                        
                        hook.editOriginalEmbeds(successEmbed.build()).queue(message -> {
                            
                            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                showPostCreationMenu(hook);
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

    private void showPostCreationMenu(net.dv8tion.jda.api.interactions.InteractionHook hook) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🎉 O que você gostaria de fazer agora?")
            .setColor(0x0099FF);

        hook.editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.primary("criar-novo-log", "📝 Criar novo Log"),
                Button.secondary("alterar-log-existente", "✏️ Alterar Log existente"),
                Button.danger("sair", "🚪 Sair")
            )
            .queue();
    }

    private void exitBot(ButtonInteractionEvent event, long discordUserId) {
        
        EmbedBuilder exitingEmbed = new EmbedBuilder()
            .setTitle("⏳ Saindo...")
            .setColor(0xFFFF00);
        
        event.editMessageEmbeds(exitingEmbed.build())
            .setComponents()
            .queue(hook -> {
                
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    EmbedBuilder thanksEmbed = new EmbedBuilder()
                        .setTitle("🙏 Obrigado por usar o Bot TeamBoarding!")
                        .setColor(0x0099FF);
                    
                    hook.editOriginalEmbeds(thanksEmbed.build()).queue(message -> {
                        
                        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                            hook.deleteOriginal().queue();
                            
                            userFormState.remove(discordUserId);
                        });
                    });
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

    private void showEditFieldsMenu(ButtonInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);

        event.editMessageEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-pessoa", "👤 Pessoa"),
                    Button.secondary("edit-tipo", "📝 Tipo"),
                    Button.secondary("edit-categorias", "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary("edit-descricao", "📄 Descrição"),
                    Button.secondary("edit-datas", "📅 Editar Datas"),
                    Button.primary("retornar-resumo", "↩️ Retornar ao resumo")
                )
            )
            .queue();
    }

    private void editSquad(ButtonInteractionEvent event, FormState state) {
        showSquadSelection(event, state);
    }

    private void editPessoa(ButtonInteractionEvent event, FormState state) {
        state.isEditing = true; 
        event.deferEdit().queue();
        showUserSelection(event.getHook(), state, state.squadId);
    }

    private void editTipo(ButtonInteractionEvent event, FormState state) {
        state.isEditing = true; 
        event.deferEdit().queue();
        showTypeSelection(event.getHook(), state);
    }

    private void editCategorias(ButtonInteractionEvent event, FormState state) {
        state.isEditing = true; 
        event.deferEdit().queue();
        showCategorySelection(event.getHook(), state);
    }

    private void editDescricao(ButtonInteractionEvent event, FormState state) {
        
        TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Digite a descrição detalhada...")
                .setValue(state.description != null ? state.description : "")
                .setMinLength(10)
                .setMaxLength(1000)
                .build();

        Modal modal = Modal.create("description-modal-edit", "📝 Editar Descrição")
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void editDataInicio(ButtonInteractionEvent event, FormState state) {
        
        TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                .setPlaceholder("Ex: 20-06-1986")
                .setValue(state.startDate != null ? formatToBrazilianDate(state.startDate) : "")
                .setMinLength(10)
                .setMaxLength(10)
                .build();

        TextInput hasEndDateInput = TextInput.create("has_end_date", "Há data de fim? (sim/não)", TextInputStyle.SHORT)
                .setPlaceholder("Digite: sim, s, não, nao, n")
                .setValue(state.endDate != null ? "sim" : "não")
                .setMinLength(1)
                .setMaxLength(10)
                .build();

        Modal modal = Modal.create("start-date-modal-edit", "📅 Editar Data de Início")
                .addActionRow(startDateInput)
                .addActionRow(hasEndDateInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void editDataFim(ButtonInteractionEvent event, FormState state) {
        
        TextInput endDateInput = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA)", TextInputStyle.SHORT)
                .setPlaceholder("Ex: 20-06-1986")
                .setValue(state.endDate != null ? formatToBrazilianDate(state.endDate) : "")
                .setMinLength(10)
                .setMaxLength(10)
                .build();

        Modal modal = Modal.create("end-date-modal-edit", "📅 Editar Data de Fim")
                .addActionRow(endDateInput)
                .build();

        event.replyModal(modal).queue();
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

    private void showTypeSelection(net.dv8tion.jda.api.interactions.InteractionHook hook, FormState state) {
        try {
            String logTypesJson = squadLogService.getSquadLogTypes();
            JSONArray logTypesArray = new JSONArray(logTypesJson);

            StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                    .setPlaceholder("Selecione o tipo");
            buildSelectMenu(logTypesArray, typeMenuBuilder);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Selecione um Tipo")
                .setDescription("Escolha o tipo do log:")
                .setColor(0x0099FF);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(typeMenuBuilder.build())
                .queue();
                
        } catch (Exception e) {
            logger.error("[SHOW_TYPE_SELECTION] Erro: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar tipos")
                .setColor(0xFF0000);
            hook.editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
        }
    }

    private void showCategorySelection(net.dv8tion.jda.api.interactions.InteractionHook hook, FormState state) {
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

    private void showSummaryWithButtons(ButtonInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", formatToBrazilianDate(state.startDate), false);
        embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

        // Usar "Salvar" se estiver editando, "Criar" se estiver criando novo
        String buttonText = state.isEditing ? "💾 Salvar" : "✅ Criar";
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", buttonText),
                Button.secondary("alterar-log", "✏️ Editar")
            )
            .queue();
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
        
        switch (componentId) {
            case "squad-select" -> {
                String squadId = event.getValues().getFirst();
                String squadName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[SQUAD_SELECT] Usuário: {} | Squad selecionada: {} (ID: {})", 
                           discordUserId, squadName, squadId);
                state.squadId = squadId;
                state.squadName = squadName;

                EmbedBuilder successEmbed = new EmbedBuilder()
                    .setTitle("✅ Squad selecionada com sucesso!")
                    .setDescription("Squad: **" + squadName + "**")
                    .setColor(0x00FF00); 
                
                // Mostrar sucesso e depois carregar usuários
                event.editMessageEmbeds(successEmbed.build())
                    .setComponents()
                    .queue();

                // Aguardar 2 segundos e então carregar usuários
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    try {
                        showUserSelection(event.getHook(), state, squadId);
                    } catch (Exception e) {
                        logger.error("[SQUAD_SELECT] Erro ao mostrar usuários: {}", e.getMessage());
                        event.getHook().editOriginal("❌ Erro ao carregar usuários. Tente novamente.").queue();
                    }
                });
                state.step = FormStep.USER;
            }

            case "user-select" -> {
                String selectedUserId = event.getValues().getFirst();
                String selectedUserName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[USER_SELECT] Usuário: {} | Pessoa selecionada: {} (ID: {})", 
                           discordUserId, selectedUserName, selectedUserId);
                state.userId = selectedUserId;
                state.userName = selectedUserName;

                EmbedBuilder successEmbed = new EmbedBuilder()
                    .setTitle("✅ Pessoa selecionada com sucesso!")
                    .setDescription("Pessoa: **" + selectedUserName + "**")
                    .setColor(0x00FF00); 
                
                event.editMessageEmbeds(successEmbed.build())
                    .setComponents()
                    .queue(hook -> {
                        
                        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                            if (state.isEditing) {
                                
                                state.isEditing = false; 
                                showSummaryFromModalWithHook(hook, state);
                            } else {
                                
                                EmbedBuilder processingEmbed = new EmbedBuilder()
                                    .setTitle("⏳ Processando...")
                                    .setColor(0xFFFF00); 
                                
                                hook.editOriginalEmbeds(processingEmbed.build()).queue(message -> {
                                    
                                    CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                        showTypeSelection(hook, state);
                                    });
                                });
                            }
                        });
                    });
                state.step = FormStep.TYPE;
            }

            case "type-select" -> {
                String typeId = event.getValues().getFirst();
                String typeName = event.getSelectedOptions().getFirst().getLabel();
                logger.info("[TYPE_SELECT] Usuário: {} | Tipo selecionado: {} (ID: {})", 
                           discordUserId, typeName, typeId);
                state.typeId = typeId;
                state.typeName = typeName;

                EmbedBuilder successEmbed = new EmbedBuilder()
                    .setTitle("✅ Tipo selecionado com sucesso!")
                    .setDescription("Tipo: **" + typeName + "**")
                    .setColor(0x00FF00); 
                
                if (state.isEditing) {
                    // Se estiver editando, apenas mostrar sucesso e voltar ao resumo
                    state.isEditing = false; 
                    event.editMessageEmbeds(successEmbed.build())
                        .setComponents()
                        .queue(hook -> {
                            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                showSummaryFromModalWithHook(hook, state);
                            });
                        });
                } else {
                    // Se estiver criando, mostrar sucesso e depois abrir modal de descrição
                    event.editMessageEmbeds(successEmbed.build())
                        .setComponents()
                        .queue();

                    // Aguardar 2 segundos e então mostrar seleção de categorias
                    CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                        try {
                            showCategorySelection(event.getHook(), state);
                        } catch (Exception e) {
                            logger.error("[TYPE_SELECT] Erro ao mostrar categorias: {}", e.getMessage());
                            event.getHook().editOriginal("❌ Erro ao carregar categorias. Tente novamente.").queue();
                        }
                    });
                }
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

                EmbedBuilder successEmbed = new EmbedBuilder()
                    .setTitle("✅ Categorias selecionadas com sucesso!")
                    .setDescription("Categorias: **" + String.join(", ", selectedNames) + "**")
                    .setColor(0x00FF00); 
                
                if (state.isEditing) {
                    // Se estiver editando, apenas mostrar sucesso e voltar ao resumo
                    state.isEditing = false; 
                    event.editMessageEmbeds(successEmbed.build())
                        .setComponents()
                        .queue(hook -> {
                            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                                showSummaryFromModalWithHook(hook, state);
                            });
                        });
                } else {
                    // Se estiver criando, abrir modal de descrição
                    TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
                            .setPlaceholder("Descreva o que foi feito...")
                            .setMinLength(10)
                            .setMaxLength(1000)
                            .build();

                    Modal descriptionModal = Modal.create("description-modal-create", "📝 Adicionar Descrição")
                            .addActionRow(descriptionInput)
                            .build();

                    // Abrir modal diretamente sem editar mensagem primeiro
                    event.replyModal(descriptionModal).queue();
                }
                state.step = FormStep.DESCRIPTION;
            }

            case "squad-logs-select-update" -> {
                try {
                    String squadLogId = event.getValues().getFirst();
                    logger.info("[ATUALIZAR_SQUAD] Carregando dados do questionário ID: {} para usuário: {}", squadLogId, discordUserId);
                    
                    JSONObject squadLog = new JSONObject(squadLogService.getSquadLogId(squadLogId));
                    logger.info("[ATUALIZAR_SQUAD] Montando resumo dos dados para ser alterado");
                    
                    showSummaryUpdate(event, squadLog);
                    
                } catch (Exception e) {
                    logger.error("[ATUALIZAR_SQUAD] Erro ao carregar questionário: {}", e.getMessage());
                    event.editMessage("❌ Erro ao carregar questionário. Tente novamente.")
                        .setEmbeds()
                        .setComponents()
                        .queue();
                }
            }

            default -> {
                logger.warn("[SELECT_UNKNOWN] Select desconhecido: {} | Usuário: {}", componentId, discordUserId);
                event.reply("❌ Seleção não reconhecida.").setEphemeral(true).queue();
            }
        }
    }

    private void handleOpenDescriptionModal(ButtonInteractionEvent event, FormState state) {
        TextInput descriptionInput = TextInput.create("description", "Descrição", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Digite a descrição detalhada...")
                .setMinLength(10)
                .setMaxLength(1000)
                .build();

        Modal modal = Modal.create("description-modal-create", "📝 Adicionar Descrição")
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        long discordUserId = event.getUser().getIdLong();
        String modalId = event.getModalId();
        
        logger.info("[MODAL_INTERACTION] Usuário: {} | Modal: {}", discordUserId, modalId);
        
        FormState state = userFormState.get(discordUserId);
        if (state == null) {
            event.reply("❌ Formulário não encontrado ou expirado.").setEphemeral(true).queue();
            return;
        }

        switch (modalId) {
            case "description-modal-create" -> {
                String description = event.getValue("description").getAsString().trim();
                logger.info("[MODAL_DESCRIPTION_CREATE] Usuário: {} | Descrição: {}", discordUserId, description);
                
                state.description = description;
                state.step = FormStep.START_DATE;

                // Abrir modal de datas automaticamente
                TextInput startDateInput = TextInput.create("start_date", "Data de Início (DD-MM-AAAA)", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986")
                        .setMinLength(10)
                        .setMaxLength(10)
                        .build();

                TextInput endDateInput = TextInput.create("end_date", "Data de Fim (DD-MM-AAAA) - OPCIONAL", TextInputStyle.SHORT)
                        .setPlaceholder("Ex: 20-06-1986 (deixe em branco se não houver)")
                        .setRequired(false)
                        .setMaxLength(10)
                        .build();

                Modal datesModal = Modal.create("dates-modal-create", "📅 Adicionar Datas")
                        .addActionRow(startDateInput)
                        .addActionRow(endDateInput)
                        .build();

                // SOLUÇÃO REAL: Mostrar mensagem de sucesso e editar a mensagem original com botão de datas
                // Como não podemos abrir modal diretamente de modal, vamos editar a mensagem original
                event.reply("✅ Descrição salva com sucesso!").setEphemeral(true).queue();
                
                // Editar a mensagem original para mostrar o próximo passo
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📝 Descrição Adicionada")
                    .setDescription("**Descrição:** " + description + "\n\n📅 **Próximo passo:** Clique no botão abaixo para inserir as datas")
                    .setColor(0x00FF00);

                // Usar o hook da interação original (não do modal) para editar a mensagem
                InteractionHook originalHook = event.getInteraction().getHook();
                if (originalHook != null) {
                    originalHook.editOriginalEmbeds(embed.build())
                        .setComponents(ActionRow.of(
                            Button.primary("open-dates-modal-auto", "📅 Inserir Datas")
                        ))
                        .queue(
                            success -> logger.info("[DESCRIPTION_SUCCESS] Mensagem editada com botão de datas para usuário: {}", discordUserId),
                            error -> logger.error("[DESCRIPTION_SUCCESS] Erro ao editar mensagem: {}", error.getMessage())
                        );
                } else {
                    logger.error("[DESCRIPTION_SUCCESS] Hook original não encontrado para usuário: {}", discordUserId);
                }
            }

            case "dates-modal-create" -> {
                String startDate = event.getValue("start_date").getAsString().trim();
                String endDate = event.getValue("end_date") != null ? event.getValue("end_date").getAsString().trim() : "";
                
                logger.info("[MODAL_DATES_CREATE] Usuário: {} | Data início: {} | Data fim: {}", 
                           discordUserId, startDate, endDate.isEmpty() ? "Não informado" : endDate);

                // Validar data de início
                if (!isValidBrazilianDate(startDate)) {
                    event.reply("❌ Data de início inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                            .setEphemeral(true).queue();
                    return;
                }

                // Validar data de fim se fornecida
                if (!endDate.isEmpty() && !isValidBrazilianDate(endDate)) {
                    event.reply("❌ Data de fim inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                            .setEphemeral(true).queue();
                    return;
                }

                // Salvar datas no estado
                state.startDate = convertToIsoDate(startDate);
                state.endDate = endDate.isEmpty() ? null : convertToIsoDate(endDate);
                state.step = FormStep.END_DATE;

                logger.info("[MODAL_DATES_CREATE] Datas convertidas - Início: {} | Fim: {}", 
                           state.startDate, state.endDate != null ? state.endDate : "Não informado");

                // Mostrar resumo final
                showSummaryFromModal(event, state);
            }


            case "end-date-modal-create" -> {
                String endDate = event.getValue("end_date").getAsString().trim();
                logger.info("[MODAL_END_DATE_CREATE] Usuário: {} | Data fim: {}", discordUserId, endDate);

                if (!endDate.isEmpty() && !isValidBrazilianDate(endDate)) {
                    event.reply("❌ Data de fim inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                            .setEphemeral(true).queue();
                    return;
                }
                
                if (endDate.isEmpty()) {
                    state.endDate = null;
                    logger.info("[MODAL_END_DATE_CREATE] Usuário: {} | Sem data fim", discordUserId);
                } else {
                    state.endDate = convertToIsoDate(endDate);
                }
                
                state.step = FormStep.REVIEW;
                showSummaryFromModal(event, state);
            }


            case "dates-modal-edit" -> {
                String newStartDate = event.getValue("start_date").getAsString().trim();
                String newEndDate = event.getValue("end_date").getAsString().trim();
                
                logger.info("[MODAL_DATES_EDIT] Usuário: {} | Nova data início: {} | Nova data fim: {}", 
                           discordUserId, newStartDate, newEndDate);

                if (!isValidBrazilianDate(newStartDate)) {
                    event.reply("❌ Data de início inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                            .setEphemeral(true).queue();
                    return;
                }
                
                state.startDate = convertToIsoDate(newStartDate);

                if (newEndDate.isEmpty()) {
                    state.endDate = null;
                    logger.info("[MODAL_DATES_EDIT] Usuário: {} | Data fim removida", discordUserId);
                } else {
                    if (!isValidBrazilianDate(newEndDate)) {
                        event.reply("❌ Data de fim inválida! Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                                .setEphemeral(true).queue();
                        return;
                    }
                    state.endDate = convertToIsoDate(newEndDate);
                }

                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ Datas atualizadas com sucesso!")
                    .setDescription("**Data de início:** " + newStartDate + 
                                  (newEndDate.isEmpty() ? "\n**Data de fim:** Removida" : "\n**Data de fim:** " + newEndDate))
                    .setColor(0x00FF00);
                
                event.deferEdit().queue();
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();

                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    showSummaryFromModalWithHook(event.getHook(), state);
                });
            }

            case "description-modal-edit" -> {
                String newDescription = event.getValue("description").getAsString().trim();
                logger.info("[MODAL_DESCRIPTION_EDIT] Usuário: {} | Nova descrição: {}", discordUserId, newDescription);
                
                state.description = newDescription;

                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ Descrição atualizada com sucesso!")
                    .setDescription("**Nova descrição:** " + newDescription)
                    .setColor(0x00FF00);
                
                event.deferEdit().queue();
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();

                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    showSummaryFromModalForUpdateWithHook(event.getHook(), state);
                });
            }

            case "start-date-modal-edit" -> {
                String newStartDate = event.getValue("start_date").getAsString().trim();
                String hasEndDate = event.getValue("has_end_date").getAsString().trim();
                
                logger.info("[MODAL_START_DATE_EDIT] Usuário: {} | Nova data início: {} | Tem data fim: {}", 
                           discordUserId, newStartDate, hasEndDate);
                
                if (!isValidBrazilianDate(newStartDate)) {
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Data de início inválida!")
                        .setDescription("Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                        .setColor(0xFF0000);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setComponents()
                        .queue();
                    return;
                }

                if (!isYesResponse(hasEndDate) && !isNoResponse(hasEndDate)) {
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Resposta inválida!")
                        .setDescription("Digite: sim, s, não, nao ou n")
                        .setColor(0xFF0000);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(errorEmbed.build())
                        .setComponents()
                        .queue();
                    return;
                }
                
                state.startDate = convertToIsoDate(newStartDate);
                
                if (isYesResponse(hasEndDate)) {
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("✅ Data de início atualizada com sucesso!")
                        .setDescription("**Data de início:** " + newStartDate)
                        .setColor(0x00FF00);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(embed.build())
                        .setActionRow(Button.primary("open-end-date-modal-modify", "📅 Alterar Data de Fim"))
                        .queue();
                } else {
                    
                    state.endDate = null;
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("✅ Data de início atualizada e data de fim removida!")
                        .setDescription("**Data de início:** " + newStartDate)
                        .setColor(0x00FF00);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(embed.build())
                        .setComponents()
                        .queue();

                    CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                        showSummaryFromModalWithHook(event.getHook(), state);
                    });
                }
            }

            case "end-date-modal-edit" -> {
                String newEndDate = event.getValue("end_date").getAsString().trim();
                logger.info("[MODAL_END_DATE_EDIT] Usuário: {} | Nova data fim: {}", discordUserId, newEndDate);

                if (newEndDate.isEmpty()) {
                    state.endDate = null;
                    logger.info("[MODAL_END_DATE_EDIT] Usuário: {} | Data fim removida", discordUserId);
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("✅ Data de fim removida com sucesso!")
                        .setDescription("A data de fim foi removida do log.")
                        .setColor(0x00FF00);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(embed.build())
                        .setComponents()
                        .queue();

                    CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                        showSummaryFromModalWithHook(event.getHook(), state);
                    });
                } else {
                    
                    if (!isValidBrazilianDate(newEndDate)) {
                        EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Data de fim inválida!")
                            .setDescription("Use o formato DD-MM-AAAA (ex: 20-06-1986)")
                            .setColor(0xFF0000);
                        
                        event.deferEdit().queue();
                        event.getHook().editOriginalEmbeds(errorEmbed.build())
                            .setComponents()
                            .queue();
                        return;
                    }
                    
                    state.endDate = convertToIsoDate(newEndDate);
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("✅ Data de fim atualizada com sucesso!")
                        .setDescription("**Data de fim:** " + newEndDate)
                        .setColor(0x00FF00);
                    
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(embed.build())
                        .setComponents()
                        .queue();

                    CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                        showSummaryFromModalWithHook(event.getHook(), state);
                    });
                }
            }

            default -> {
                logger.warn("[MODAL_UNKNOWN] Modal desconhecido: {} | Usuário: {}", modalId, discordUserId);
                event.reply("❌ Modal não reconhecido.").setEphemeral(true).queue();
            }
        }
    }

    private void showSummaryFromModal(ModalInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", formatToBrazilianDate(state.startDate), false);
        embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

        // Usar "Salvar" se estiver editando, "Criar" se estiver criando novo
        String buttonText = state.isEditing ? "💾 Salvar" : "✅ Criar";
        
        event.editMessage("✅ Dados inseridos com sucesso!")
                .setEmbeds(embed.build())
                .setActionRow(
                        Button.success("criar-log", buttonText),
                        Button.secondary("alterar-log", "✏️ Editar")
                )
                .queue();
    }

    private void showSummaryFromModalWithHook(InteractionHook hook, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo dos dados inseridos");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName, false);
        embed.addField("👤 Pessoa", state.userName, false);
        embed.addField("📝 Tipo", state.typeName, false);
        embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
        embed.addField("📄 Descrição", state.description, false);
        embed.addField("📅 Data de início", formatToBrazilianDate(state.startDate), false);
        embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

        // Usar "Salvar" se estiver editando, "Criar" se estiver criando novo
        String buttonText = state.isEditing ? "💾 Salvar" : "✅ Criar";
        
        hook.editOriginal("✅ Dados inseridos com sucesso!")
                .setEmbeds(embed.build())
                .setActionRow(
                        Button.success("criar-log", buttonText),
                        Button.secondary("alterar-log", "✏️ Editar")
                )
                .queue();
    }

    private void showEditFieldsMenuFromModal(ModalInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);

        event.editMessage("")
            .setEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-pessoa", "👤 Pessoa"),
                    Button.secondary("edit-tipo", "📝 Tipo"),
                    Button.secondary("edit-categorias", "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary("edit-descricao", "📄 Descrição"),
                    Button.secondary("edit-datas", "📅 Editar Datas"),
                    Button.primary("retornar-resumo", "↩️ Retornar ao resumo")
                )
            )
            .queue();
    }

    private void showEditFieldsMenuFromModalWithHook(InteractionHook hook, FormState state) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚙️ Selecione o campo para editar")
            .setDescription("Escolha qual campo você deseja modificar:")
            .setColor(0x0099FF);

        hook.editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("edit-squad", "🏢 Squad"),
                    Button.secondary("edit-pessoa", "👤 Pessoa"),
                    Button.secondary("edit-tipo", "📝 Tipo"),
                    Button.secondary("edit-categorias", "🏷️ Categorias")
                ),
                ActionRow.of(
                    Button.secondary("edit-descricao", "📄 Descrição"),
                    Button.secondary("edit-datas", "📅 Editar Datas"),
                    Button.primary("retornar-resumo", "↩️ Retornar ao resumo")
                )
            )
            .queue();
    }

    private void showSummaryFromModalForUpdate(ModalInteractionEvent event, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo do Questionário");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName != null ? state.squadName : "Não informado", false);
        embed.addField("👤 Pessoa", state.userName != null ? state.userName : "Não informado", false);
        embed.addField("📝 Tipo", state.typeName != null ? state.typeName : "Não informado", false);
        embed.addField("🏷️ Categorias", state.categoryNames != null && !state.categoryNames.isEmpty() ? 
                      String.join(", ", state.categoryNames) : "Não informado", false);
        embed.addField("📄 Descrição", state.description != null ? state.description : "Não informado", false);
        embed.addField("📅 Data de início", state.startDate != null ? formatToBrazilianDate(state.startDate) : "Não informado", false);
        embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

        event.editMessage("")
            .setEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.success("confirmar-atualizacao", "✅ Confirmar"),
                    Button.secondary("editar-questionario", "✏️ Editar")
                )
            )
            .queue();
    }

    private void showSummaryFromModalForUpdateWithHook(InteractionHook hook, FormState state) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Resumo do Questionário");
        embed.setColor(0x0099FF);
        embed.addField("🏢 Squad", state.squadName != null ? state.squadName : "Não informado", false);
        embed.addField("👤 Pessoa", state.userName != null ? state.userName : "Não informado", false);
        embed.addField("📝 Tipo", state.typeName != null ? state.typeName : "Não informado", false);
        embed.addField("🏷️ Categorias", state.categoryNames != null && !state.categoryNames.isEmpty() ? 
                      String.join(", ", state.categoryNames) : "Não informado", false);
        embed.addField("📄 Descrição", state.description != null ? state.description : "Não informado", false);
        embed.addField("📅 Data de início", state.startDate != null ? formatToBrazilianDate(state.startDate) : "Não informado", false);
        embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

        // Usar "Salvar" se estiver editando, "Criar" se estiver criando novo
        String buttonText = state.isEditing ? "💾 Salvar" : "✅ Criar";
        
        hook.editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.success("criar-log", buttonText),
                    Button.secondary("alterar-log", "✏️ Alterar")
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

            // Definir que estamos no modo de edição
            state.isEditing = true;
            logger.info("[SHOW_SUMMARY_UPDATE] Estado isEditing definido como true para usuário: {}", discordUserId);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("📋 Resumo do Questionário Selecionado");
            embed.setColor(0x0099FF);
            embed.addField("🏢 Squad", state.squadName, false);
            embed.addField("👤 Pessoa", state.userName, false);
            embed.addField("📝 Tipo", state.typeName, false);
            embed.addField("🏷️ Categorias", String.join(", ", state.categoryNames), false);
            embed.addField("📄 Descrição", state.description, false);
            embed.addField("📅 Data de início", formatToBrazilianDate(state.startDate), false);
            embed.addField("📅 Data de fim", state.endDate != null ? formatToBrazilianDate(state.endDate) : "Não informado", false);

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

    private static void buildSelectMenuUpdate(JSONArray squadLogsArray, StringSelectMenu.Builder menuBuilder) {
        for (int i = 0; i < squadLogsArray.length(); i++) {
            JSONObject squadLog = squadLogsArray.getJSONObject(i);
            String id = String.valueOf(squadLog.getInt("id"));
            String description = squadLog.optString("description", "");
            String person = squadLog.getJSONObject("user").getString("first_name") + " " + 
                          squadLog.getJSONObject("user").getString("last_name");
            String addedBy = squadLog.getJSONObject("register_user").getString("first_name") + " " + 
                           squadLog.getJSONObject("register_user").getString("last_name");
            String type = squadLog.getJSONObject("squad_log_type").getString("name");
            String project = squadLog.getJSONObject("squad").getJSONObject("project").getString("name");
            String startDate = squadLog.getString("start_date");
            
            if (!description.isEmpty()) {
                String optionDescription = id + " | " + project + " | " + person + " | " + addedBy + " | " + type + " | " + startDate;
                menuBuilder.addOption(description.length() > 100 ? description.substring(0, 97) + "..." : description, 
                                    id, optionDescription);
            }
        }
    }

    private static final DateTimeFormatter BRAZILIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Pattern BRAZILIAN_DATE_PATTERN = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");

    private boolean isValidBrazilianDate(String date) {
        if (date == null || !BRAZILIAN_DATE_PATTERN.matcher(date).matches()) {
            return false;
        }
        try {
            LocalDate.parse(date, BRAZILIAN_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String convertToIsoDate(String brazilianDate) {
        try {
            LocalDate date = LocalDate.parse(brazilianDate, BRAZILIAN_DATE_FORMATTER);
            return date.toString(); 
        } catch (DateTimeParseException e) {
            logger.error("[CONVERT_TO_ISO] Erro ao converter data brasileira: {}", brazilianDate);
            return brazilianDate; 
        }
    }

    private String formatToBrazilianDate(String isoDate) {
        if (isoDate == null) return null;
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return date.format(BRAZILIAN_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("[FORMAT_TO_BRAZILIAN] Erro ao formatar data ISO: {}", isoDate);
            return isoDate; 
        }
    }

    public static class FormState {
        public String squadId;
        public String squadName;
        public String userId;
        public String userName;
        public String typeId;
        public String typeName;
        public List<String> categoryIds = new ArrayList<>();
        public List<String> categoryNames = new ArrayList<>();
        public String description;
        public String startDate;
        public String endDate;
        public FormStep step = FormStep.SQUAD;
        public boolean isEditing = false; 
        public Long squadLogId; 
    }

    public enum FormStep {
        SQUAD, USER, TYPE, CATEGORY, DESCRIPTION, START_DATE, HAS_END, END_DATE, REVIEW,
        DESCRIPTION_MODIFY, START_DATE_MODIFY, END_DATE_MODIFY
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

    private String createSquadLogPayload(FormState state) {
        JSONObject payload = new JSONObject();

        payload.put("squad_id", Long.parseLong(state.squadId));
        payload.put("user_id", Long.parseLong(state.userId));
        payload.put("squad_log_type_id", Long.parseLong(state.typeId));
        payload.put("description", state.description);
        payload.put("start_date", state.startDate);

        if (state.endDate != null) {
            payload.put("end_date", state.endDate);
        }

        JSONArray categoriesArray = new JSONArray();
        if (state.categoryIds != null) {
            for (String categoryId : state.categoryIds) {
                categoriesArray.put(Long.parseLong(categoryId));
            }
        }
        payload.put("squad_category_ids", categoriesArray);
        
        return payload.toString();
    }

    private String updateSquadLogPayload(FormState state) {
        JSONObject payload = new JSONObject();

        if (state.squadId != null) {
            payload.put("squad_id", Long.parseLong(state.squadId));
        }
        if (state.userId != null) {
            payload.put("user_id", Long.parseLong(state.userId));
        }
        if (state.typeId != null) {
            payload.put("squad_log_type_id", Long.parseLong(state.typeId));
        }
        if (state.description != null) {
            payload.put("description", state.description);
        }
        if (state.startDate != null) {
            payload.put("start_date", state.startDate);
        }

        if (state.endDate != null) {
            payload.put("end_date", state.endDate);
        } else {
            payload.put("end_date", JSONObject.NULL);
        }

        if (state.categoryIds != null) {
            JSONArray categoriesArray = new JSONArray();
            for (String categoryId : state.categoryIds) {
                categoriesArray.put(Long.parseLong(categoryId));
            }
            payload.put("squad_category_ids", categoriesArray);
        }
        
        return payload.toString();
    }

    private void updateSquadLog(ButtonInteractionEvent event, FormState state) {
        
        EmbedBuilder updatingEmbed = new EmbedBuilder()
            .setTitle("⏳ Atualizando Log...")
            .setColor(0xFFFF00);
        
        event.editMessageEmbeds(updatingEmbed.build())
            .setComponents()
            .queue(hook -> {
                
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    try {
                        
                        String payload = updateSquadLogPayload(state);
                        logger.info("[UPDATE_SQUAD_LOG] Payload criado: {}", payload);

                        if (state.squadLogId == null) {
                            throw new IllegalStateException("ID do squad log não encontrado para atualização");
                        }

                        ResponseEntity<String> response = squadLogService.updateSquadLog(state.squadLogId, payload);
                        logger.info("[UPDATE_SQUAD_LOG] Response da API: {} - {}", response.getStatusCode(), response.getBody());

                        EmbedBuilder successEmbed = new EmbedBuilder()
                            .setTitle("✅ Log atualizado com sucesso!")
                            .setDescription("O que você gostaria de fazer agora?")
                            .setColor(0x00FF00);
                        
                        hook.editOriginalEmbeds(successEmbed.build())
                            .setActionRow(
                                Button.primary("criar", "📝 Criar novo Log"),
                                Button.secondary("atualizar", "✏️ Atualizar Log existente"),
                                Button.danger("sair", "🚪 Sair")
                            )
                            .queue();

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

}