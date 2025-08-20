package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ComponentInteractionListener extends ListenerAdapter {

    @Autowired
    private SquadLogService squadLogService;

    private final Map<Long, FormState> userFormState = new HashMap<>();

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.equals("criar")) {
            event.deferReply().setEphemeral(true).queue(interaction -> {
                String squadsJson = squadLogService.getSquads();
                JSONArray squadsArray = new JSONArray(squadsJson);

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("squad-select")
                        .setPlaceholder("Selecione uma Squad");
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    String name = squad.optString("name", "");
                    if (!name.isEmpty()) {
                        menuBuilder.addOption(name, String.valueOf(squad.get("id")));
                    }
                }

                interaction.editOriginal("Selecione uma Squad:")
                        .setComponents(ActionRow.of(menuBuilder.build()))
                        .queue();
            });
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        long discordUserId = event.getUser().getIdLong();
        FormState state = userFormState.computeIfAbsent(discordUserId, k -> new FormState());

        switch (event.getComponentId()) {
            case "squad-select" -> {
                String squadId = event.getValues().getFirst();
                String squadName = event.getSelectedOptions().getFirst().getLabel();
                state.squadId = squadId;
                state.squadName = squadName;

                JSONArray squadsArray = new JSONArray(squadLogService.getSquads());
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

                event.reply("Squad selecionada: " + squadName + " (ID: " + squadId + ")")
                        .setEphemeral(true)
                        .queue();

                event.getInteraction().getHook().sendMessage("Selecione quem irá responder:")
                        .addActionRow(userMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.USER;
            }

            case "user-select" -> {
                String selectedUserId = event.getValues().getFirst();
                String selectedUserName = event.getSelectedOptions().getFirst().getLabel();
                state.userId = selectedUserId;
                state.userName = selectedUserName;

                event.reply("Pessoa selecionada: " + selectedUserName)
                        .setEphemeral(true)
                        .queue();

                String logTypesJson = squadLogService.getSquadLogTypes();
                JSONArray logTypesArray = new JSONArray(logTypesJson);

                StringSelectMenu.Builder typeMenuBuilder = StringSelectMenu.create("type-select")
                        .setPlaceholder("Selecione o tipo");
                for (int i = 0; i < logTypesArray.length(); i++) {
                    JSONObject type = logTypesArray.getJSONObject(i);
                    String name = type.optString("name", "");
                    if (!name.isEmpty()) {
                        typeMenuBuilder.addOption(name, String.valueOf(type.get("id")));
                    }
                }

                event.getInteraction().getHook().sendMessage("Selecione o tipo:")
                        .addActionRow(typeMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.TYPE;
            }

            case "type-select" -> {
                String typeId = event.getValues().getFirst();
                String typeName = event.getSelectedOptions().getFirst().getLabel();
                state.typeId = typeId;
                state.typeName = typeName;

                event.reply("Tipo selecionado: " + typeName + " (ID: " + typeId + ")")
                        .setEphemeral(true)
                        .queue();

                String categoriesJson = squadLogService.getSquadCategories();
                JSONArray categoriesArray = new JSONArray(categoriesJson);

                StringSelectMenu.Builder categoryMenuBuilder = StringSelectMenu.create("category-select")
                        .setPlaceholder("Selecione as categorias")
                        .setMinValues(1)
                        .setMaxValues(categoriesArray.length());
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject category = categoriesArray.getJSONObject(i);
                    String name = category.optString("name", "");
                    if (!name.isEmpty()) {
                        categoryMenuBuilder.addOption(name, String.valueOf(category.get("id")));
                    }
                }

                event.getInteraction().getHook().sendMessage("Selecione as categorias:")
                        .addActionRow(categoryMenuBuilder.build())
                        .setEphemeral(true)
                        .queue();
                state.step = FormStep.CATEGORY;
            }
            case "category-select" -> {
                List<String> selectedIds = event.getSelectedOptions().stream()
                    .map(opt -> opt.getValue())
                    .toList();
                state.categoryNames = selectedIds;

                event.reply("Categorias selecionadas: " +
                    event.getSelectedOptions().stream().map(opt -> opt.getLabel()).toList())
                    .setEphemeral(true)
                    .queue();

                event.getInteraction().getHook().sendMessage("Digite uma descrição:")
                    .setEphemeral(true)
                    .queue();
                state.step = FormStep.DESCRIPTION;
            }
        }
    }

    @Override
    public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        long discordUserId = event.getAuthor().getIdLong();
        if (!userFormState.containsKey(discordUserId)) return;
        FormState state = userFormState.get(discordUserId);

        if (event.isFromGuild() && event.getAuthor().isBot()) return;

        switch (state.step) {
            case DESCRIPTION -> {
                state.description = event.getMessage().getContentRaw();
                event.getChannel().sendMessage("Descrição registrada. Informe a data de início (YYYY-MM-DD):").queue();
                state.step = FormStep.START_DATE;
            }
            case START_DATE -> {
                state.startDate = event.getMessage().getContentRaw();
                event.getChannel().sendMessage("O formulario tem data de fim? (s/n)").queue();
                state.step = FormStep.HAS_END;
            }
            case HAS_END -> {
                String content = event.getMessage().getContentRaw().trim().toLowerCase();
                if (content.equals("s")) {
                    event.getChannel().sendMessage("Informe a data de fim (YYYY-MM-DD):").queue();
                    state.step = FormStep.END_DATE;
                } else {
                    state.endDate = null;
                    showSummary(event, state);
                    userFormState.remove(discordUserId);
                }
            }
            case END_DATE -> {
                state.endDate = event.getMessage().getContentRaw();
                showSummary(event, state);
                userFormState.remove(discordUserId);
            }
            default -> {
                event.getChannel().sendMessage("O formulario foi cancelado.").queue();
                userFormState.remove(discordUserId);
            }
        }
    }

    private void showSummary(net.dv8tion.jda.api.events.message.MessageReceivedEvent event, FormState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("**Resumo dos dados inseridos:**\n");
        builder.append("Squad: ").append(state.squadName).append("\n");
        builder.append("Pessoa: ").append(state.userName).append("\n");
        builder.append("Tipo: ").append(state.typeName).append("\n");
        builder.append("Categorias: ").append(String.join(", ", state.categoryNames)).append("\n");
        builder.append("Descrição: ").append(state.description).append("\n");
        builder.append("Data de início: ").append(state.startDate).append("\n");
        builder.append("Data de fim: ").append(state.endDate != null ? state.endDate : "Não informado").append("\n");

        event.getChannel().sendMessage(builder.toString()).queue();

        String payload = buildSquadLogPayload(
                state.squadId,
                state.userId,
                state.typeId,
                state.categoryNames,
                state.description,
                state.startDate,
                state.endDate
        );

        event.getChannel().sendMessage("---------------------------------------------").queue();
        ResponseEntity<String> response = squadLogService.createSquadLog(payload);
        event.getChannel().sendMessage("Status Code da API: " + response.getStatusCode()).queue();
        event.getChannel().sendMessage("Body de Resposta da API: " + response.getBody()).queue();
    }

    enum FormStep {
        USER, TYPE, CATEGORY, DESCRIPTION, START_DATE, HAS_END, END_DATE
    }

    static class FormState {
        String squadId;
        String squadName;
        String userId;
        String userName;
        String typeId;
        String typeName;
        List<String> categoryNames;
        String description;
        String startDate;
        String endDate;
        FormStep step;
    }

    public String buildSquadLogPayload(
            String squadId,
            String userId,
            String squadLogTypeId,
            List<String> skillCategoryIds,
            String description,
            String startDate,
            String endDate
    ) {
        JSONObject payload = new JSONObject();
        payload.put("squad_id", Integer.parseInt(squadId));
        payload.put("user_id", Integer.parseInt(userId));
        payload.put("squad_log_type_id", Integer.parseInt(squadLogTypeId));
        payload.put("skill_categories", skillCategoryIds.stream().map(Integer::parseInt).toList());
        payload.put("description", description);
        payload.put("start_date", startDate);
        if (endDate != null && !endDate.isBlank()) {
            payload.put("end_date", endDate);
        }
        return payload.toString();
    }
}