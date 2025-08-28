package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.discord.ui.Ui;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ComponentInteractionListener extends ListenerAdapter {

   
    private static final Logger logger = LoggerFactory.getLogger(ComponentInteractionListener.class);
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private SquadLogService squadLogService;

    private static final String BTN_CREATE = "criar";
    private static final String BTN_CONFIRM_CREATE = "criar-log";
    private static final String BTN_EDIT = "alterar-log";
    private static final String BTN_UPDATE = "atualizar";

    private static final String ID_SQUAD_SELECT = "squad-select";
    private static final String ID_USER_SELECT = "user-select";
    private static final String ID_TYPE_SELECT = "type-select";
    private static final String ID_CATEGORY_SELECT = "category-select";
    
    private static final String ID_SQUAD_SELECT_MODIFY = "squad-select-modify";
    private static final String ID_USER_SELECT_MODIFY = "user-select-modify";
    private static final String ID_TYPE_SELECT_MODIFY = "type-select-modify";
    private static final String ID_CATEGORY_SELECT_MODIFY = "category-select-modify";

    private static final String BTN_MOD_SQUAD = "modify-squad";
    private static final String BTN_MOD_USER = "modify-user";
    private static final String BTN_MOD_TYPE = "modify-type";
    private static final String BTN_MOD_CATEGORY = "modify-category";
    private static final String BTN_MOD_DESC = "modify-description";
    private static final String BTN_MOD_START = "modify-start-date";
    private static final String BTN_MOD_END = "modify-end-date";
    private static final String BTN_BACK_SUMMARY = "back-to-summary";
    
    private static final String BTN_NEW_SQUAD_LOG = "new-squad-log";
    private static final String BTN_EXIT = "exit";

    private static final String MODAL_DETAILS = "wiz:details";
    
    private static final int SUCCESS_MESSAGE_DELAY = 2;

    private enum Step { SELECT_SQUAD, SELECT_USER, SELECT_TYPE, SELECT_CATEGORIES, AWAIT_DETAILS_MODAL, REVIEW }

    static class FormState {
        Step step;
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
        JSONArray squads;
        JSONArray logTypes;
        JSONArray categories;
    }

    private final Map<Long, FormState> userFormState = new ConcurrentHashMap<>();

    private void showMenuOnly(InteractionHook hook, StringSelectMenu menu) {
        MessageEditBuilder b = new MessageEditBuilder();
        b.setContent(" ");
        b.setEmbeds();
        b.setComponents(ActionRow.of(menu));
        hook.editOriginal(b.build()).queue();
    }

    private void showSuccess(InteractionHook hook, String msg) {
        MessageEditBuilder b = new MessageEditBuilder();
        b.setContent(" ");
        b.setEmbeds(Ui.success(msg).build());
        b.setComponents();
        hook.editOriginal(b.build()).queue();
    }
    
    private LocalDate parseBrazilianDate(String dateStr) {
        return LocalDate.parse(dateStr, BRAZILIAN_DATE_FORMAT);
    }
    
    private String formatToBrazilianDate(LocalDate date) {
        return date.format(BRAZILIAN_DATE_FORMAT);
    }
    
    private String convertToIsoDate(String brazilianDate) {
        LocalDate date = parseBrazilianDate(brazilianDate);
        return date.toString();
    }
    


    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        long uid = event.getUser().getIdLong();
        logger.info("[BUTTON] user={} id={}", uid, id);

        try {
            if (BTN_CREATE.equals(id)) {
                FormState s = userFormState.computeIfAbsent(uid, k -> new FormState());
                s.step = Step.SELECT_SQUAD;
                s.squads = fetchSquads(event.getUser());

                event.editMessage(" ")
                        .setEmbeds()
                        .setActionRow(buildSquadSelect(s))
                        .queue();
                return;
            }

            if (BTN_CONFIRM_CREATE.equals(id)) {
                FormState s = userFormState.get(uid);
                if (s == null) {
                    event.replyEmbeds(Ui.error("Sessão expirada. Use /squad-log novamente.").build())
                            .setEphemeral(true).queue();
                    return;
                }

                event.editMessage(" ")
                        .setEmbeds(Ui.info("Criando Squad Log...", null).build())
                        .setComponents()
                        .queue();

                String payload = buildSquadLogPayload(
                        s.squadId, s.userId, s.typeId, s.categoryIds, s.description, s.startDate, s.endDate
                );
                logger.info("[CRIAR_LOG] payload={}", payload);

                ResponseEntity<String> resp = squadLogService.createSquadLog(payload);
                if (resp.getStatusCode() == HttpStatus.OK) {
                    event.getHook().editOriginalEmbeds(Ui.success("Squad Log criado com sucesso! ✅").build())
                            .setActionRow(
                                    Button.primary(BTN_NEW_SQUAD_LOG, "🆕 Criar novo Squad Log"),
                                    Button.secondary(BTN_EXIT, "🚪 Sair")
                            )
                            .queue();
                    userFormState.remove(uid);
                } else {
                    event.getHook().editOriginalEmbeds(
                            Ui.error("Erro ao criar", "Status: " + resp.getStatusCode() + "\n" + resp.getBody()).build()
                    ).setActionRow(Button.secondary(BTN_EDIT, "✏️ Editar")).queue();
                }
                return;
            }

            if (BTN_EDIT.equals(id)) {
                FormState s = userFormState.get(uid);
                if (s == null) {
                    event.replyEmbeds(Ui.error("Sessão expirada. Use /squad-log novamente.").build())
                            .setEphemeral(true).queue();
                    return;
                }
                event.editMessage(" ")
                        .setEmbeds(Ui.warning("Selecione o campo que deseja alterar").build())
                        .setComponents(buildModifyButtons().toArray(new ActionRow[0]))
                        .queue();
                return;
            }

            if (BTN_BACK_SUMMARY.equals(id)) {
                FormState s = userFormState.get(uid);
                if (s == null) {
                    event.replyEmbeds(Ui.error("Sessão expirada. Use /squad-log novamente.").build())
                            .setEphemeral(true).queue();
                    return;
                }
                event.editMessage(" ")
                        .setEmbeds(buildReviewEmbed(s).build())
                        .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                Button.secondary(BTN_EDIT, "✏️ Editar"))
                        .queue();
                return;
            }

            if (BTN_UPDATE.equals(id)) {
                event.editMessage(" ")
                        .setEmbeds(Ui.warning("Atualizar", "Fluxo ainda não implementado. Use 'Criar' por enquanto.").build())
                        .setComponents()
                        .queue();
                return;
            }

            if (BTN_NEW_SQUAD_LOG.equals(id)) {
                event.editMessage("Escolha uma opção:")
                        .setEmbeds(Ui.info("Escolha uma opção").build())
                        .setActionRow(
                                Button.success(BTN_CREATE, "Criar"),
                                Button.secondary(BTN_UPDATE, "Atualizar")
                        )
                        .queue();
                return;
            }

            if (BTN_EXIT.equals(id)) {
                event.editMessage("Obrigado por usar o Bot TeamBoarding! 👋 Desligando...")
                        .setEmbeds()
                        .setComponents()
                        .queue(hook -> {
                            hook.deleteOriginal().queueAfter(2, java.util.concurrent.TimeUnit.SECONDS);
                        });
                return;
            }

            if (id.startsWith("modify-")) {
                handleModify(event, id);
            }

        } catch (Exception e) {
            logger.error("Erro no onButtonInteraction", e);
            event.replyEmbeds(Ui.error("Ocorreu um erro. Tente novamente.").build())
                    .setEphemeral(true).queue();
        }
    }


    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        long uid = event.getUser().getIdLong();
        FormState s = userFormState.computeIfAbsent(uid, k -> new FormState());
        InteractionHook hook = event.getHook();

        try {
            switch (event.getComponentId()) {
                case ID_SQUAD_SELECT -> {
                    s.squadName = event.getSelectedOptions().get(0).getLabel();
                    s.squadId   = event.getValues().get(0);

                    event.editMessage(" ")
                            .setEmbeds(Ui.success("Squad selecionada com sucesso: " + s.squadName).build())
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Carregando usuários 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            showMenuOnly(hook, buildUserSelect(s));
                                        });
                            });
                    s.step = Step.SELECT_USER;
                    return;
                }

                case ID_USER_SELECT -> {
                    s.userName = event.getSelectedOptions().get(0).getLabel();
                    s.userId   = event.getValues().get(0);

                    event.editMessage(" ")
                            .setEmbeds(Ui.success("Pessoa selecionada com sucesso: " + s.userName).build())
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Carregando tipos 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            s.logTypes = fetchLogTypes();
                                            showMenuOnly(hook, buildTypeSelect(s));
                                        });
                            });
                    s.step = Step.SELECT_TYPE;
                    return;
                }

                case ID_TYPE_SELECT -> {
                    s.typeName = event.getSelectedOptions().get(0).getLabel();
                    s.typeId   = event.getValues().get(0);

                    event.editMessage(" ")
                            .setEmbeds(Ui.success("Tipo selecionado com sucesso: " + s.typeName).build())
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Carregando categorias 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            s.categories = fetchCategories();
                                            if (s.categories == null || s.categories.length() == 0) {
                                                hook.editOriginalEmbeds(
                                                        Ui.warning("Não há categorias disponíveis para esta seleção.", "Volte e escolha outro tipo.").build()
                                                ).setActionRow(Button.primary(BTN_BACK_SUMMARY, "🔙 Voltar")).queue();
                                            } else {
                                                showMenuOnly(hook, buildCategorySelect(s));
                                            }
                                        });
                            });
                    s.step = Step.SELECT_CATEGORIES;
                    return;
                }

                case ID_CATEGORY_SELECT -> {
                    s.categoryNames = event.getSelectedOptions().stream().map(SelectOption::getLabel).toList();
                    s.categoryIds   = new ArrayList<>(event.getValues());

                    event.replyModal(buildDetailsModal(s)).queue(success -> {
                        event.getHook().deleteOriginal().queue();
                    });
                    s.step = Step.AWAIT_DETAILS_MODAL;
                    return;
                }

                case ID_SQUAD_SELECT_MODIFY -> {
                    s.squadName = event.getSelectedOptions().get(0).getLabel();
                    s.squadId   = event.getValues().get(0);

                    event.editMessage("✅ Squad alterada para: " + s.squadName)
                            .setEmbeds()
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Atualizando resumo 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            hook.editOriginal("📋 Resumo atualizado")
                                                    .setEmbeds(buildReviewEmbed(s).build())
                                                    .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                                            Button.secondary(BTN_EDIT, "✏️ Editar"))
                                                    .queue();
                                        });
                            });
                    return;
                }

                case ID_USER_SELECT_MODIFY -> {
                    s.userName = event.getSelectedOptions().get(0).getLabel();
                    s.userId   = event.getValues().get(0);

                    event.editMessage("✅ Pessoa alterada para: " + s.userName)
                            .setEmbeds()
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Atualizando resumo 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            hook.editOriginal("📋 Resumo atualizado")
                                                    .setEmbeds(buildReviewEmbed(s).build())
                                                    .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                                            Button.secondary(BTN_EDIT, "✏️ Editar"))
                                                    .queue();
                                        });
                            });
                    return;
                }

                case ID_TYPE_SELECT_MODIFY -> {
                    s.typeName = event.getSelectedOptions().get(0).getLabel();
                    s.typeId   = event.getValues().get(0);

                    event.editMessage("✅ Tipo alterado para: " + s.typeName)
                            .setEmbeds()
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Atualizando resumo 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            hook.editOriginal("📋 Resumo atualizado")
                                                    .setEmbeds(buildReviewEmbed(s).build())
                                                    .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                                            Button.secondary(BTN_EDIT, "✏️ Editar"))
                                                    .queue();
                                        });
                            });
                    return;
                }

                case ID_CATEGORY_SELECT_MODIFY -> {
                    s.categoryNames = event.getSelectedOptions().stream().map(SelectOption::getLabel).toList();
                    s.categoryIds   = new ArrayList<>(event.getValues());

                    event.editMessage("✅ Categorias alteradas para: " + String.join(", ", s.categoryNames))
                            .setEmbeds()
                            .setComponents()
                            .queue(v -> {
                                hook.editOriginal(" ")
                                        .setEmbeds(Ui.info("Atualizando resumo 🤔", "Processando...").build())
                                        .setComponents()
                                        .queueAfter(SUCCESS_MESSAGE_DELAY, java.util.concurrent.TimeUnit.SECONDS, thinking -> {
                                            hook.editOriginal("📋 Resumo atualizado")
                                                    .setEmbeds(buildReviewEmbed(s).build())
                                                    .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                                            Button.secondary(BTN_EDIT, "✏️ Editar"))
                                                    .queue();
                                        });
                            });
                    return;
                }

                default -> {
                }
            }
        } catch (Exception e) {
            logger.error("Erro no onStringSelectInteraction", e);
            event.replyEmbeds(Ui.error("Ocorreu um erro. Tente novamente.").build())
                    .setEphemeral(true).queue();
        }
    }


    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!MODAL_DETAILS.equals(event.getModalId())) return;

        long uid = event.getUser().getIdLong();
        FormState s = userFormState.get(uid);
        if (s == null) {
            event.replyEmbeds(Ui.error("Sessão expirada. Use /squad-log novamente.").build())
                    .setEphemeral(true).queue();
            return;
        }

        try {
            String desc = event.getValue("f-desc").getAsString().trim();
            String start = event.getValue("f-start").getAsString().trim();
            String hasEnd = event.getValue("f-has-end").getAsString().trim().toLowerCase();
            String end = event.getValue("f-end") != null ? event.getValue("f-end").getAsString().trim() : "";

            if (!start.matches("\\d{2}-\\d{2}-\\d{4}")) {
                event.replyEmbeds(Ui.error("Data inválida", "Data de início deve estar no formato DD-MM-AAAA (ex: 20-06-1986).").build())
                        .setEphemeral(true).queue();
                return;
            }
            try { parseBrazilianDate(start); } catch (Exception ex) {
                event.replyEmbeds(Ui.error("Data inválida", "Data de início não é uma data válida.").build())
                        .setEphemeral(true).queue();
                return;
            }

            if ("s".equals(hasEnd)) {
                if (end.isEmpty() || !end.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    event.replyEmbeds(Ui.error("Data inválida", "Informe data de fim (DD-MM-AAAA) ou marque 'n'.").build())
                            .setEphemeral(true).queue();
                    return;
                }
                try { parseBrazilianDate(end); } catch (Exception ex) {
                    event.replyEmbeds(Ui.error("Data inválida", "Data de fim não é uma data válida.").build())
                            .setEphemeral(true).queue();
                    return;
                }
                s.endDate = end;
            } else {
                s.endDate = null;
            }

            s.description = desc;
            s.startDate = start;
            s.step = Step.REVIEW;

            event.replyEmbeds(buildReviewEmbed(s).build())
                    .addActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                            Button.secondary(BTN_EDIT, "✏️ Editar"))
                    .setEphemeral(true).queue();

        } catch (Exception e) {
            logger.error("Erro no onModalInteraction", e);
            event.replyEmbeds(Ui.error("Ocorreu um erro. Tente novamente.").build())
                    .setEphemeral(true).queue();
        }
    }


    private void handleModify(ButtonInteractionEvent event, String id) {
        long uid = event.getUser().getIdLong();
        FormState s = userFormState.get(uid);
        if (s == null) {
            event.replyEmbeds(Ui.error("Sessão expirada. Use /squad-log novamente.").build())
                    .setEphemeral(true).queue();
            return;
        }

        switch (id) {
            case BTN_MOD_SQUAD -> {
                s.squads = fetchSquads(event.getUser());
                event.editMessage("Selecione uma nova Squad:")
                        .setEmbeds()
                        .setActionRow(buildSquadSelectModify(s))
                        .queue();
            }
            case BTN_MOD_USER -> {
                s.squads = fetchSquads(event.getUser());
                event.editMessage("Selecione uma nova pessoa:")
                        .setEmbeds()
                        .setActionRow(buildUserSelectModify(s))
                        .queue();
            }
            case BTN_MOD_TYPE -> {
                s.logTypes = fetchLogTypes();
                event.editMessage("Selecione um novo tipo:")
                        .setEmbeds()
                        .setActionRow(buildTypeSelectModify(s))
                        .queue();
            }
            case BTN_MOD_CATEGORY -> {
                s.categories = fetchCategories();
                if (s.categories == null || s.categories.length() == 0) {
                    event.editMessage("❌ Erro")
                            .setEmbeds(Ui.warning("Não há categorias disponíveis.", "Volte e escolha outro tipo.").build())
                            .setActionRow(Button.primary(BTN_BACK_SUMMARY, "🔙 Voltar"))
                            .queue();
                } else {
                    event.editMessage("Selecione novas categorias:")
                            .setEmbeds()
                            .setActionRow(buildCategorySelectModify(s))
                            .queue();
                }
            }
            case BTN_MOD_DESC, BTN_MOD_START, BTN_MOD_END -> {
                event.replyModal(buildDetailsModal(s)).queue(success -> {
                    event.getHook().deleteOriginal().queue();
                });
            }
            case BTN_BACK_SUMMARY -> {
                event.editMessage("📋 Resumo atualizado")
                        .setEmbeds(buildReviewEmbed(s).build())
                        .setActionRow(Button.success(BTN_CONFIRM_CREATE, "✅ Confirmar"),
                                Button.secondary(BTN_EDIT, "✏️ Editar"))
                        .queue();
            }
        }
    }


    private StringSelectMenu buildSquadSelect(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_SQUAD_SELECT)
                .setPlaceholder("Selecione uma Squad");
        if (state.squads != null) {
            for (int i = 0; i < state.squads.length(); i++) {
                JSONObject s = state.squads.getJSONObject(i);
                String id = String.valueOf(s.opt("id"));
                String name = s.optString("name", "");
                if (name == null || name.isBlank()) name = "Squad #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
            }
        }
        return b.build();
    }

    private StringSelectMenu buildUserSelect(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_USER_SELECT)
                .setPlaceholder("Selecione uma pessoa");
        if (state.squads != null && state.squadId != null) {
            JSONObject selected = null;
            for (int i = 0; i < state.squads.length(); i++) {
                JSONObject sq = state.squads.getJSONObject(i);
                if (String.valueOf(sq.opt("id")).equals(state.squadId)) { selected = sq; break; }
            }
            if (selected != null) {
                b.addOption("All team", state.squadId);
                JSONArray userSquads = selected.optJSONArray("user_squads");
                if (userSquads != null) {
                    for (int i = 0; i < userSquads.length(); i++) {
                        JSONObject us = userSquads.getJSONObject(i);
                        JSONObject u = us.optJSONObject("user");
                        if (u != null) {
                            String name = (u.optString("first_name","") + " " + u.optString("last_name","")).trim();
                            String id = String.valueOf(u.opt("id"));
                            if (name == null || name.isBlank()) name = "Usuário #" + id;
                            if (name.length() > 100) name = name.substring(0, 100);
                            b.addOption(name, id);
                        }
                    }
                }
            }
        }
        return b.build();
    }

    private StringSelectMenu buildTypeSelect(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_TYPE_SELECT)
                .setPlaceholder("Selecione um tipo");
        if (state.logTypes != null) {
            for (int i = 0; i < state.logTypes.length(); i++) {
                JSONObject t = state.logTypes.getJSONObject(i);
                String id = String.valueOf(t.opt("id"));
                String name = t.optString("name", "");
                if (name == null || name.isBlank()) name = "Tipo #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
            }
        }
        return b.build();
    }

    private StringSelectMenu buildCategorySelect(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_CATEGORY_SELECT)
                .setPlaceholder("Selecione uma ou mais categorias")
                .setMinValues(1);
        int total = 0;
        if (state.categories != null) {
            for (int i = 0; i < state.categories.length(); i++) {
                JSONObject c = state.categories.getJSONObject(i);
                String id = String.valueOf(c.opt("id"));
                String name = c.optString("name", "");
                if (name == null || name.isBlank()) name = "Categoria #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
                total++;
            }
        }
        if (total == 0) {
            return StringSelectMenu.create(ID_CATEGORY_SELECT)
                    .setPlaceholder("Sem categorias disponíveis")
                    .setDisabled(true)
                    .build();
        }
        b.setMaxValues(Math.max(1, Math.min(total, 25)));
        return b.build();
    }

    private StringSelectMenu buildSquadSelectModify(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_SQUAD_SELECT_MODIFY)
                .setPlaceholder("Selecione uma Squad");
        if (state.squads != null) {
            for (int i = 0; i < state.squads.length(); i++) {
                JSONObject s = state.squads.getJSONObject(i);
                String id = String.valueOf(s.opt("id"));
                String name = s.optString("name", "");
                if (name == null || name.isBlank()) name = "Squad #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
            }
        }
        return b.build();
    }

    private StringSelectMenu buildUserSelectModify(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_USER_SELECT_MODIFY)
                .setPlaceholder("Selecione uma pessoa");
        if (state.squads != null && state.squadId != null) {
            JSONObject selected = null;
            for (int i = 0; i < state.squads.length(); i++) {
                JSONObject sq = state.squads.getJSONObject(i);
                if (String.valueOf(sq.opt("id")).equals(state.squadId)) { selected = sq; break; }
            }
            if (selected != null) {
                b.addOption("All team", state.squadId);
                JSONArray userSquads = selected.optJSONArray("user_squads");
                if (userSquads != null) {
                    for (int i = 0; i < userSquads.length(); i++) {
                        JSONObject us = userSquads.getJSONObject(i);
                        JSONObject u = us.optJSONObject("user");
                        if (u != null) {
                            String name = (u.optString("first_name","") + " " + u.optString("last_name","")).trim();
                            String id = String.valueOf(u.opt("id"));
                            if (name == null || name.isBlank()) name = "Usuário #" + id;
                            if (name.length() > 100) name = name.substring(0, 100);
                            b.addOption(name, id);
                        }
                    }
                }
            }
        }
        return b.build();
    }

    private StringSelectMenu buildTypeSelectModify(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_TYPE_SELECT_MODIFY)
                .setPlaceholder("Selecione um tipo");
        if (state.logTypes != null) {
            for (int i = 0; i < state.logTypes.length(); i++) {
                JSONObject t = state.logTypes.getJSONObject(i);
                String id = String.valueOf(t.opt("id"));
                String name = t.optString("name", "");
                if (name == null || name.isBlank()) name = "Tipo #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
            }
        }
        return b.build();
    }

    private StringSelectMenu buildCategorySelectModify(FormState state) {
        StringSelectMenu.Builder b = StringSelectMenu.create(ID_CATEGORY_SELECT_MODIFY)
                .setPlaceholder("Selecione uma ou mais categorias")
                .setMinValues(1);
        int total = 0;
        if (state.categories != null) {
            for (int i = 0; i < state.categories.length(); i++) {
                JSONObject c = state.categories.getJSONObject(i);
                String id = String.valueOf(c.opt("id"));
                String name = c.optString("name", "");
                if (name == null || name.isBlank()) name = "Categoria #" + id;
                if (name.length() > 100) name = name.substring(0, 100);
                b.addOption(name, id);
                total++;
            }
        }
        if (total == 0) {
            return StringSelectMenu.create(ID_CATEGORY_SELECT_MODIFY)
                    .setPlaceholder("Sem categorias disponíveis")
                    .setDisabled(true)
                    .build();
        }
        b.setMaxValues(Math.max(1, Math.min(total, 25)));
        return b.build();
    }

    private List<ActionRow> buildModifyButtons() {
        return List.of(
                ActionRow.of(Button.secondary(BTN_MOD_SQUAD, "🏢 Squad"),
                        Button.secondary(BTN_MOD_USER, "👤 Pessoa"),
                        Button.secondary(BTN_MOD_TYPE, "📝 Tipo")),
                ActionRow.of(Button.secondary(BTN_MOD_CATEGORY, "🏷️ Categorias"),
                        Button.secondary(BTN_MOD_DESC, "📄 Descrição")),
                ActionRow.of(Button.secondary(BTN_MOD_START, "📅 Data Início"),
                        Button.secondary(BTN_MOD_END, "📅 Data Fim"),
                        Button.primary(BTN_BACK_SUMMARY, "🔙 Voltar ao Resumo"))
        );
    }

    private Modal buildDetailsModal(FormState s) {
        TextInput.Builder descB = TextInput.create("f-desc", "Descrição", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .setMaxLength(1000)
                .setPlaceholder("Explique o que aconteceu, resultados, etc.");
        if (s.description != null && !s.description.isBlank()) {
            descB.setValue(s.description);
        }

        TextInput.Builder startB = TextInput.create("f-start", "Data de início (DD-MM-AAAA)", TextInputStyle.SHORT)
                .setRequired(true)
                .setPlaceholder("20-06-1986");
        if (s.startDate != null && !s.startDate.isBlank()) {
            startB.setValue(s.startDate);
        }

        String hasEndDefault = (s.endDate != null && !s.endDate.isBlank()) ? "s" : "n";
        TextInput.Builder hasEndB = TextInput.create("f-has-end", "Tem data de fim? (s/n)", TextInputStyle.SHORT)
                .setRequired(true)
                .setPlaceholder("s ou n")
                .setValue(hasEndDefault);

        TextInput.Builder endB = TextInput.create("f-end", "Data de fim (opcional, DD-MM-AAAA)", TextInputStyle.SHORT)
                .setRequired(false)
                .setPlaceholder("02-10-1986");
        if (s.endDate != null && !s.endDate.isBlank()) {
            endB.setValue(s.endDate);
        }

        return Modal.create(MODAL_DETAILS, "Descrição e Datas")
                .addActionRow(descB.build())
                .addActionRow(startB.build())
                .addActionRow(hasEndB.build())
                .addActionRow(endB.build())
                .build();
    }

    private EmbedBuilder buildReviewEmbed(FormState s) {
        return new EmbedBuilder()
                .setColor(Ui.INFO)
                .setTitle("📋 Resumo dos dados inseridos")
                .addField("🏢 Squad", valueOr(s.squadName), false)
                .addField("👤 Pessoa", valueOr(s.userName), false)
                .addField("📝 Tipo", valueOr(s.typeName), false)
                .addField("🏷️ Categorias", s.categoryNames == null || s.categoryNames.isEmpty() ? "—" : String.join(", ", s.categoryNames), false)
                .addField("📄 Descrição", valueOr(s.description), false)
                .addField("📅 Data de início", valueOr(s.startDate), false)
                .addField("📅 Data de fim", s.endDate != null ? s.endDate : "Não informado", false);
    }

    private String valueOr(String v) { return (v == null || v.isBlank()) ? "—" : v; }


    private JSONArray fetchSquads(User user) {
        try {
            String squadsJson = squadLogService.getSquads();
            if (!squadsJson.trim().startsWith("[")) {
                JSONObject obj = new JSONObject(squadsJson);
                return obj.optJSONArray("items");
            }
            return new JSONArray(squadsJson);
        } catch (Exception e) {
            logger.error("Erro ao carregar squads", e);
            return new JSONArray();
        }
    }

    private JSONArray fetchLogTypes() {
        try {
            return new JSONArray(squadLogService.getSquadLogTypes());
        } catch (Exception e) {
            logger.error("Erro ao carregar tipos", e);
            return new JSONArray();
        }
    }

    private JSONArray fetchCategories() {
        try {
            return new JSONArray(squadLogService.getSquadCategories());
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias", e);
            return new JSONArray();
        }
    }

    public String buildSquadLogPayload(
            String squadId, String userId, String squadLogTypeId,
            List<String> skillCategoryIds, String description, String startDate, String endDate
    ) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(squadId));
        payload.put("user_id", Integer.parseInt(userId));
        payload.put("squad_log_type_id", Integer.parseInt(squadLogTypeId));
        payload.put("skill_categories", skillCategoryIds.stream().map(Integer::parseInt).toList());
        payload.put("description", description);
        payload.put("start_date", convertToIsoDate(startDate));
        if (endDate != null && !endDate.isBlank()) payload.put("end_date", convertToIsoDate(endDate));
        return payload.toString();
    }
}

