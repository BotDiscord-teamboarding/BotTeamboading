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
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
@Order(2)
public class UserSelectionHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    @Autowired
    private SummaryHandler summaryHandler;

    @Autowired
    private MessageSource messageSource;

    private java.util.Locale getUserLocale(long userId) {
        return formStateService.getOrCreateState(userId).getLocale();
    }
    
    public UserSelectionHandler(FormStateService formStateService, SquadLogService squadLogService) {
        super(formStateService);
        this.squadLogService = squadLogService;
    }
    @Override
    public boolean canHandle(String componentId) {
        return "user-select".equals(componentId) || 
               "select-user".equals(componentId) ||
               "edit-pessoa".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        if ("select-user".equals(buttonId)) {
            handleSelectUserButton(event, state);
        } else if ("edit-pessoa".equals(buttonId)) {
            handleEditUserButton(event, state);
        }
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        if ("user-select".equals(event.getComponentId())) {
            handleUserSelect(event, state);
        }
    }
    private void handleSelectUserButton(ButtonInteractionEvent event, FormState state) {
        log.info("Iniciando seleção de usuário");
        state.setStep(FormStep.USER_SELECTION);
        updateFormState(event.getUser().getIdLong(), state);
        showUserSelection(event, state.getSquadId());
    }
    private void handleEditUserButton(ButtonInteractionEvent event, FormState state) {
        log.info("Editando usuário");
        state.setStep(FormStep.USER_MODIFY);
        updateFormState(event.getUser().getIdLong(), state);
        showUserSelection(event, state.getSquadId());
    }
    private void handleUserSelect(StringSelectInteractionEvent event, FormState state) {
        String selectedUserId = event.getValues().get(0);
        log.info("Usuário selecionado: {}", selectedUserId);
        try {
            withUserContext(event.getUser().getId(), () -> {
                if (selectedUserId.equals(state.getSquadId())) {
                    state.setUserId(selectedUserId);
                    state.setUserName("All team");
                } else {
                    try {
                        loadUserFromSquad(state, selectedUserId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            
            event.deferEdit().queue();
            updateFormState(event.getUser().getIdLong(), state);
            if (state.getStep() == FormStep.USER_MODIFY) {
                showSummary(event);
            } else {
                state.setStep(FormStep.TYPE_SELECTION);
                updateFormState(event.getUser().getIdLong(), state);
                showTypeSelectionAfterUser(event);
            }
        } catch (Exception e) {
            log.error("Erro na seleção de usuário: {}", e.getMessage());
            showError(event,  messageSource.getMessage("txt_erro_processar_selecao_do_usuario", null, getUserLocale(event.getUser().getIdLong()))+".");
        }
    }
    private void loadUserFromSquad(FormState state, String selectedUserId) throws Exception {
        String squadsJson = squadLogService.getSquads();
        JSONObject obj = new JSONObject(squadsJson);
        JSONArray squadsArray = obj.optJSONArray("items");
        if (squadsArray != null) {
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                if (String.valueOf(squad.get("id")).equals(state.getSquadId())) {
                    JSONArray userSquads = squad.optJSONArray("user_squads");
                    if (userSquads != null) {
                        for (int j = 0; j < userSquads.length(); j++) {
                            JSONObject userSquad = userSquads.getJSONObject(j);
                            JSONObject user = userSquad.optJSONObject("user");
                            if (user != null && String.valueOf(user.get("id")).equals(selectedUserId)) {
                                state.setUserId(selectedUserId);
                                state.setUserName(user.optString("first_name", "") + " " + user.optString("last_name", ""));
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
    private void showUserSelection(ButtonInteractionEvent event, String squadId) {
        try {
            event.deferEdit().queue();
            log.info("Carregando usuários para squad: {}", squadId);
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                log.error("Nenhuma squad encontrada na resposta da API");
                showUserSelectionError(event,  messageSource.getMessage("txt_nenhuma_squad_encontrada", null, getUserLocale(event.getUser().getIdLong()))+".");
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
                log.error("Squad com ID {} não encontrada", squadId);
                showUserSelectionError(event, messageSource.getMessage("txt_squad_nao_encontrada", null, getUserLocale(event.getUser().getIdLong()))+".");
                return;
            }
            StringSelectMenu.Builder userMenuBuilder = StringSelectMenu.create("user-select")
                    .setPlaceholder(messageSource.getMessage("txt_selecione_uma_pessoa", null, getUserLocale(event.getUser().getIdLong())));
            userMenuBuilder.addOption("All team", squadId);
            JSONArray userSquads = selectedSquad.optJSONArray("user_squads");
            int userCount = 0;
            if (userSquads != null) {
                for (int i = 0; i < userSquads.length(); i++) {
                    JSONObject userSquad = userSquads.getJSONObject(i);
                    JSONObject user = userSquad.optJSONObject("user");
                    if (user != null) {
                        String firstName = user.optString("first_name", "");
                        String lastName = user.optString("last_name", "");
                        String name = (firstName + " " + lastName).trim();
                        String userIdStr = String.valueOf(user.opt("id"));
                        if (!name.isEmpty() && !userIdStr.equals("null")) {
                            userMenuBuilder.addOption(name, userIdStr);
                            userCount++;
                        }
                    }
                }
            }
            log.info("Encontrados {} usuários na squad {}", userCount, squadId);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 "+ messageSource.getMessage("txt_selecione_uma_pessoa", null, getUserLocale(event.getUser().getIdLong())))
                .setColor(0x0099FF);
            if (userCount > 0) {
                embed.setDescription(messageSource.getMessage("txt_escolha_quem_ira_responder_ao_questionario", null, getUserLocale(event.getUser().getIdLong())));
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(userMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription(messageSource.getMessage("txt_apenas_a_opcao_all_team_esta_disponivel", null, getUserLocale(event.getUser().getIdLong()))+":");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(userMenuBuilder.build())
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar usuários: {}", e.getMessage(), e);
            showUserSelectionError(event, messageSource.getMessage("txt_erro_interno_ao_carregar_usuarios", null, getUserLocale(event.getUser().getIdLong()))+": "+ e.getMessage() );
        }
    }
    private void showUserSelectionError(ButtonInteractionEvent event, String message) {
        try {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ "+ messageSource.getMessage("txt_erro_carregar_usuarios", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(message)
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setComponents()
                .queue();
        } catch (Exception e) {
            log.error("Erro ao mostrar erro de seleção de usuário: {}", e.getMessage());
            try {
                event.deferEdit().queue();
                EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ "+ messageSource.getMessage("txt_erro_carregar_usuarios", null, getUserLocale(event.getUser().getIdLong())))
                    .setDescription(message)
                    .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setComponents()
                    .queue();
            } catch (Exception ex) {
                log.error("Fallback também falhou: {}", ex.getMessage());
            }
        }
    }
    private void showTypeSelectionAfterUser(StringSelectInteractionEvent event) {
        try {
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
                .setTitle("📝 " + messageSource.getMessage("txt_selecione_o_tipo", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_escolha_o_tipo_do_log", null, getUserLocale(event.getUser().getIdLong()))+":")
                .setColor(0x0099FF);
            if (hasTypes) {
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(typeMenuBuilder.build())
                    .queue();
            } else {
                embed.setDescription("❌ " + messageSource.getMessage("txt_nenhum_tipo_disponivel_no_momento", null, getUserLocale(event.getUser().getIdLong())) +".");
                event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents()
                    .queue();
            }
        } catch (Exception e) {
            log.error("Erro ao carregar tipos: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ "+ messageSource.getMessage("txt_erro_carregar_tipos", null, getUserLocale(event.getUser().getIdLong())))
                .setDescription(messageSource.getMessage("txt_erro_carregar_tipos", null, getUserLocale(event.getUser().getIdLong()))+ ". "+messageSource.getMessage("txt_tente_novamente", null, getUserLocale(event.getUser().getIdLong()))+".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build()).setComponents().queue();
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
            .setComponents()
            .queue();
    }
    @Override
    public int getPriority() {
        return 2;
    }
}
