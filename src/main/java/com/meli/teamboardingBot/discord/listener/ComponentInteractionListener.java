package com.meli.teamboardingBot.discord.listener;

import com.meli.teamboardingBot.service.ApiService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

    @Component
    public class ComponentInteractionListener extends ListenerAdapter {

        @Autowired
        private ApiService apiService;

        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            String buttonId = event.getComponentId();

            if (buttonId.equals("criar")) {
                event.deferReply().setEphemeral(true).queue(interaction -> {
                    String squadsJson = apiService.getSquads();
                    JSONArray squadsArray = new JSONArray(squadsJson);

                    StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("squad-select")
                            .setPlaceholder("Selecione uma Squad");
                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        menuBuilder.addOption(squad.getString("name"), String.valueOf(squad.get("id")));
                    }

                    interaction.editOriginal("Selecione uma Squad:")
                            .setComponents(ActionRow.of(menuBuilder.build()))
                            .queue();
                });
            } else if (buttonId.equals("atualizar")) {
                event.reply("Funcionalidade de atualizar ainda não implementada.")
                        .setEphemeral(true)
                        .queue();
            }
        }

        @Override
        public void onStringSelectInteraction(StringSelectInteractionEvent event) {
            // Tratar seleção da Squad após clicar em "Criar"
            if (event.getComponentId().equals("squad-select")) {
                String squadId = event.getValues().get(0);
                String squadName = event.getSelectedOptions().get(0).getLabel();
                
                event.reply("Squad selecionada: " + squadName + " (ID: " + squadId + ")")
                        .setEphemeral(true)
                        .queue();
                
                // Aqui você pode expandir para continuar o fluxo de criação do Squad Log
            }
        }
    }