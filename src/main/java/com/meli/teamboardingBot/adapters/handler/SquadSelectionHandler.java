package com.meli.teamboardingBot.adapters.handler;
import com.meli.teamboardingBot.core.context.DiscordUserContext;
import com.meli.teamboardingBot.core.domain.enums.FormStep;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.PendingAuthMessageService;
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
@Order(1)
public class SquadSelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    private final GetIsUserAuthenticatedPort isUserAuthenticated;
    private final PendingAuthMessageService pendingAuthMessageService;
    @Autowired
    private SummaryHandler summaryHandler;
    
    public SquadSelectionHandler(FormStateService formStateService, 
                                SquadLogService squadLogService, 
                                 GetIsUserAuthenticatedPort isUserAuthenticated,
                                PendingAuthMessageService pendingAuthMessageService) {
        super(formStateService);
        this.squadLogService = squadLogService;
        this.isUserAuthenticated = isUserAuthenticated;
        this.pendingAuthMessageService = pendingAuthMessageService;
    }

    @Autowired
    private MessageSource messageSource;

    private java.util.Locale getUserLocale(long userId) {
        return formStateService.getOrCreateState(userId).getLocale();
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
        String userId = event.getUser().getId();
        
        if (!isUserAuthenticated.isUserAuthenticated(userId)) {
            log.warn("Usuário {} não autenticado tentando criar squad-log", userId);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 " + messageSource.getMessage("txt_autenticacao_necessaria", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_faca_a_autenticacao_atraves_do_comando", null, getUserLocale(event.getUser().getIdLong())) + 
                    "\n\n💡 " + messageSource.getMessage("txt_use_comando_start_ou_clique_botao", null, getUserLocale(event.getUser().getIdLong())))
                .setColor(0xFFA500);
            event.editMessageEmbeds(embed.build())
                .setActionRow(
                    Button.primary("btn-autenticar", "🔐 " + messageSource.getMessage("txt_fazer_login", null, getUserLocale(event.getUser().getIdLong()))),
                    Button.secondary("status-close", "🚪 " + messageSource.getMessage("txt_fechar", null, getUserLocale(event.getUser().getIdLong())))
                )
                .queue(message -> pendingAuthMessageService.storePendingAuthMessage(userId, event.getMessage()));
            return;
        }
        
        log.info("Iniciando fluxo de criação para usuário autenticado: {}", userId);
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
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
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
            showError(event, messageSource.getMessage("txt_erro_processar_selecao_das_squads", null, getUserLocale(event.getUser().getIdLong())) + ".");
        }
    }
    private void showSquadSelection(ButtonInteractionEvent event) {
        try {
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            event.deferEdit().queue();
            if (squadsArray == null || squadsArray.length() == 0) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ " + messageSource.getMessage("txt_nenhuma_squad_encontrada", null, getUserLocale(event.getUser().getIdLong())))
                    .setDescription(messageSource.getMessage("txt_nao_ha_squads_disponiveis_no_momento", null, getUserLocale(event.getUser().getIdLong())) + ".")
                    .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))))
                    .queue();
                return;
            }
            StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_uma_squad", null, getUserLocale(event.getUser().getIdLong())));
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                String squadName = squad.optString("name", "");
                String squadId = String.valueOf(squad.get("id"));
                if (!squadName.isEmpty()) {
                    squadMenuBuilder.addOption(squadName, squadId);
                }
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏢 " + messageSource.getMessage("txt_selecione_uma_squad", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_escolha_a_squad_para_o_seu_log", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(0x0099FF);
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(squadMenuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao carregar squads: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " +messageSource.getMessage("txt_erro_carregar_squads", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_ocorreu_um_erro_ao_carregar_as_squads", null, getUserLocale(event.getUser().getIdLong())) + ". " +
                        messageSource.getMessage("txt_tente_novamente", null, getUserLocale(event.getUser().getIdLong())) + ".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))))
                .queue();
        }
    }
    private void showUserSelectionAfterSquad(StringSelectInteractionEvent event, FormState state) {
        try {
            String squadId = state.getSquadId();
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                showError(event, messageSource.getMessage("txt_nenhuma_squad_encontrada_na_resposta_da_api", null, getUserLocale(event.getUser().getIdLong())) + ".");
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
                showError(event, messageSource.getMessage("txt_squad_selecionada_nao_encontrada", null, getUserLocale(event.getUser().getIdLong())) + ".");
                return;
            }
            JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
            if (userSquads == null || userSquads.length() == 0) {
                showError(event, messageSource.getMessage("txt_nenhum_usuario_encontrado_na_squad_selecionada", null, getUserLocale(event.getUser().getIdLong())) + ".");
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 " + messageSource.getMessage("txt_selecao_de_usuario", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_selecione_o_usuario_que_ira_responder_ao_questionario", null, getUserLocale(event.getUser().getIdLong())) + ":")
                .setColor(0x0099FF);
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("user-select")
                .setPlaceholder(messageSource.getMessage("txt_escolha_um_usuario", null, getUserLocale(event.getUser().getIdLong())) + "...");
            
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
            showError(event, messageSource.getMessage("txt_erro_carregar_selecao_de_usuario", null, getUserLocale(event.getUser().getIdLong())) + ".");
        } finally {
            DiscordUserContext.clear();
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
            .setActionRow(Button.secondary("voltar-inicio", "🏠 "+ messageSource.getMessage("txt_voltar_inicio", null, getUserLocale(event.getUser().getIdLong()))) )
            .queue();
    }
    @Override
    public int getPriority() {
        return 1;
    }
}
