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
import net.dv8tion.jda.api.interactions.components.ActionRow;
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
@Order(1)
public class SquadSelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    @Autowired
    private SummaryHandler summaryHandler;
    
    public SquadSelectionHandler(FormStateService formStateService, SquadLogService squadLogService) {
        super(formStateService);
        this.squadLogService = squadLogService;
    }
    @Override
    public boolean canHandle(String componentId) {
        return "criar".equals(componentId) || 
               "squad-select".equals(componentId) ||
               "edit-squad".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        if ("criar".equals(buttonId)) {
            handleCreateButton(event, state);
        } else if ("edit-squad".equals(buttonId)) {
            handleEditSquadButton(event, state);
        }
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("squad-select".equals(event.getComponentId())) {
            handleSquadSelect(event, state);
        }
    }
    private void handleCreateButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando fluxo de criação");
        state.setCreating(true);
        state.setEditing(false);
        state.setStep(FormStep.SQUAD_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showSquadSelection(event);
    }
    private void handleEditSquadButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando squad");
        state.setStep(FormStep.SQUAD_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showSquadSelection(event);
    }
    private void handleSquadSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedSquadId = event.getValues().get(0);
        log.info("Squad selecionada: {}", selectedSquadId);
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    if (String.valueOf(squad.get("id")).equals(selectedSquadId)) {
                        state.setSquadId(selectedSquadId);
                        state.setSquadName(squad.optString("name", ""));
                        break;
                    }
                }
            }
            event.deferEdit().queue();
            updateFormState(event.getUser().getIdLong(), state);
            if (state.getStep() == FormStep.SQUAD_MODIFY) {
                showSummary(event);
            } else {
                state.setStep(FormStep.USER_SELECTION);
                updateFormState(event.getUser().getIdLong(), state);
                showUserSelectionAfterSquad(event, state);
            }
        } catch (Exception e) {
            log.error("Erro na seleção de squad: {}", e.getMessage());
            showError(event, "Erro ao processar seleção da squad.");
        }
    }
    private void showSquadSelection(ButtonInteractionEvent event) {
        try {
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            event.deferEdit().queue();
            if (squadsArray == null || squadsArray.length() == 0) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Nenhuma squad encontrada")
                    .setDescription("Não há squads disponíveis no momento.")
                    .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
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
                .setDescription("Escolha a squad para o seu log:")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar squads: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar squads")
                .setDescription("Ocorreu um erro ao carregar as squads. Tente novamente.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
        }
    }
    private void showUserSelectionAfterSquad(StringSelectInteractionEvent event, FormState state) {
        try {
            String squadId = state.getSquadId();
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                showError(event, "Nenhuma squad encontrada na resposta da API.");
                return;
            }
            JSONObject selectedSquad = null;
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                if (String.valueOf(squad.get("id")).equals(squadId)) {
                    selectedSquad = squad;
                    break;
                }
            }
            if (selectedSquad == null) {
                showError(event, "Squad selecionada não encontrada.");
                return;
            }
            JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
            if (userSquads == null || userSquads.length() == 0) {
                showError(event, "Nenhum usuário encontrado na squad selecionada.");
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Seleção de Usuário")
                .setDescription("Selecione o usuário que irá responder ao questionário:")
                .setColor(0x0099FF);
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("user-select")
                .setPlaceholder("Escolha um usuário...");
            
            menuBuilder.addOption("All team", squadId);
            
            for (int i = 0; i < userSquads.length(); i++) {
                JSONObject userSquad = userSquads.getJSONObject(i);
                JSONObject user = userSquad.optJSONObject("user");
                if (user != null) {
                    String userId = String.valueOf(user.get("id"));
                    String firstName = user.optString("first_name", "");
                    String lastName = user.optString("last_name", "");
                    String fullName = user.optString("name", ""); 
                    String userName;
                    if (!fullName.isEmpty()) {
                        userName = fullName;
                    } else if (!firstName.isEmpty() || !lastName.isEmpty()) {
                        userName = (firstName + " " + lastName).trim();
                    } else {
                        userName = "Usuário " + userId;
                    }
                    menuBuilder.addOption(userName, userId);
                    log.debug("Adicionado usuário: {} (ID: {})", userName, userId);
                }
            }
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(menuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao exibir seleção de usuário: {}", e.getMessage());
            showError(event, "Erro ao carregar seleção de usuário.");
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
        return 1;
    }
}
