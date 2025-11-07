package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import static net.dv8tion.jda.api.interactions.components.buttons.Button.*;

@Slf4j
@Component
@Order(6)
public class CrudOperationHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;
    private final DiscordUserAuthenticationService discordAuthService;
    private static final int LIMIT_PAGE = 15;

    public CrudOperationHandler(FormStateService formStateService, 
                               SquadLogService squadLogService,
                               DiscordUserAuthenticationService discordAuthService) {
        super(formStateService);
        this.squadLogService = squadLogService;
        this.discordAuthService = discordAuthService;
    }
    @Override
    public boolean canHandle(String componentId) {
        return "criar-log".equals(componentId) ||
               "confirmar-criacao".equals(componentId) ||
               "confirmar-atualizacao".equals(componentId) ||
               "criar-novo-log".equals(componentId) ||
               "atualizar-log-existente".equals(componentId) ||
               "atualizar".equals(componentId) ||
               "voltar-inicio".equals(componentId) ||
               "voltar".equals(componentId) ||
               "avancar".equals(componentId);
    }
    @Override
    protected void handleButtonInternal(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
        log.info("Button clicked: {}", buttonId);
        
        try {
            if ("criar-log".equals(buttonId) || "confirmar-criacao".equals(buttonId)) {
                if (state.isCreating()) {
                    handleCreateSquadLog(event, state);
                } else {
                    handleUpdateSquadLog(event, state);
                }
            } else if ("confirmar-atualizacao".equals(buttonId)) {
                handleUpdateSquadLog(event, state);
            } else if ("criar-novo-log".equals(buttonId)) {
                handleCreateNewLog(event);
            } else if ("atualizar-log-existente".equals(buttonId) || "atualizar".equals(buttonId)) {
                handleUpdateExistingLog(event);
            } else if ("sair-bot".equals(buttonId)) {
                handleExitBot(event);
            } else if ("voltar-inicio".equals(buttonId)) {
                handleVoltarInicio(event);
            } else if ("voltar".equals(buttonId)) {
                handleVoltarPage(event);
            } else if ("avancar".equals(buttonId)) {
                handleAvancarPage(event);
            }
        } catch (Exception e) {
            log.error("Error handling button click: {}", e.getMessage(), e);
            showErrorMessage(event, "❌ Ocorreu um erro ao processar sua solicitação. Por favor, tente novamente.");
        }
    }
    
    @Override
    protected void handleStringSelectInternal(StringSelectInteractionEvent event, FormState state) {
        log.warn("String select handling not implemented for: {}", event.getComponentId());
    }
    private void handleCreateSquadLog(ButtonInteractionEvent event, FormState state) {
        log.info("Criando squad log");
        event.deferEdit().queue();
        if (!isStateValid(state)) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Dados Incompletos")
                .setDescription("Verifique se todos os campos foram preenchidos.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
            return;
        }
        try {
            String payload = buildCreatePayload(state);
            log.info("Payload de criação: {}", payload);
            ResponseEntity<String> response = withUserContext(event.getUser().getId(), 
                () -> squadLogService.createSquadLog(payload));
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ Squad Log criado com sucesso!", true);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ Erro ao criar Squad Log. Código: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao criar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ Erro interno ao criar Squad Log. Tente novamente.");
        }
    }
    private void handleUpdateSquadLog(ButtonInteractionEvent event, FormState state) {
        log.info("Atualizando squad log ID: {}", state.getSquadLogId());
        event.deferEdit().queue();
        if (!isStateValid(state) || state.getSquadLogId() == null) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Dados Incompletos")
                .setDescription("Dados incompletos ou ID do log não encontrado.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
            return;
        }
        try {
            String payload = buildUpdatePayload(state);
            log.info("Payload de atualização para squad-log ID {}: {}", state.getSquadLogId(), payload);
            log.info("Estado atual: squadId={}, userId={}, typeId={}, categoryIds={}, startDate={}, endDate={}", 
                       state.getSquadId(), state.getUserId(), state.getTypeId(), 
                       state.getCategoryIds(), state.getStartDate(), state.getEndDate());
            ResponseEntity<String> response = withUserContext(event.getUser().getId(), 
                () -> squadLogService.updateSquadLog(state.getSquadLogId(), payload));
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ Squad Log atualizado com sucesso!", false);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ Erro ao atualizar Squad Log. Código: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao atualizar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ Erro interno ao atualizar Squad Log. Tente novamente.");
        }
    }
    private boolean isStateValid(FormState state) {
        return state.getSquadId() != null &&
               state.getUserId() != null &&
               state.getTypeId() != null &&
               !state.getCategoryIds().isEmpty() &&
               state.getDescription() != null &&
               state.getStartDate() != null;
    }
    private String buildCreatePayload(FormState state) {
        JSONObject payload = new JSONObject();
        
        payload.put("squad_id", Integer.parseInt(state.getSquadId()));
        
        if (!state.getUserId().equals(state.getSquadId())) {
            payload.put("user_id", Integer.parseInt(state.getUserId()));
        }
        
        payload.put("squad_log_type_id", Integer.parseInt(state.getTypeId()));
        payload.put("description", state.getDescription());
        payload.put("start_date", convertToApiDateFormat(state.getStartDate()));
        if (state.getEndDate() != null && !state.getEndDate().isEmpty()) {
            payload.put("end_date", convertToApiDateFormat(state.getEndDate()));
        }
        payload.put("skill_categories", state.getCategoryIds().stream()
            .mapToInt(Integer::parseInt)
            .toArray());
        
        log.info("Create payload: {}", payload.toString());
        return payload.toString();
    }
    private String buildUpdatePayload(FormState state) {
        log.info("DEBUG buildUpdatePayload: squadId={}, userId={}, typeId={}, categoryIds={}",
                   state.getSquadId(), state.getUserId(), state.getTypeId(), state.getCategoryIds());
        JSONObject payload = new JSONObject();
        
        payload.put("squad_id", Integer.parseInt(state.getSquadId()));
        
        if (!state.getUserId().equals(state.getSquadId())) {
            payload.put("user_id", Integer.parseInt(state.getUserId()));
            log.info("Incluindo user_id no payload: {}", state.getUserId());
        } else {
            log.info("All team detectado - omitindo user_id do payload");
        }
        
        payload.put("squad_log_type_id", Integer.parseInt(state.getTypeId()));
        payload.put("description", state.getDescription());
        payload.put("start_date", convertToApiDateFormat(state.getStartDate()));
        if (state.getEndDate() != null && !state.getEndDate().isEmpty()) {
            payload.put("end_date", convertToApiDateFormat(state.getEndDate()));
        }
        payload.put("skill_categories", state.getCategoryIds().stream()
            .mapToInt(Integer::parseInt)
            .toArray());
        
        log.info("Update payload: {}", payload.toString());
        return payload.toString();
    }
    private String convertToApiDateFormat(String inputDate) {
        if (inputDate == null || inputDate.isEmpty()) {
            log.warn("Data de entrada é null ou vazia");
            return null;
        }
        log.info("Convertendo data '{}' para formato API", inputDate);
        if (inputDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            log.info("Data já está no formato API: '{}'", inputDate);
            return inputDate;
        }
        if (inputDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = inputDate.split("-");
            String apiDate = parts[2] + "-" + parts[1] + "-" + parts[0];
            log.info("Data convertida de DD-MM-YYYY para API: '{}' -> '{}'", inputDate, apiDate);
            return apiDate;
        }
        log.warn("Formato de data não reconhecido: '{}' - retornando como está", inputDate);
        return inputDate;
    }

    private void showSuccessMessageWithHook(ButtonInteractionEvent event, String message, boolean isCreation) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription(isCreation ? 
                "O Squad Log foi criado com sucesso! O que deseja fazer agora?" :
                "O Squad Log foi atualizado com sucesso! O que deseja fazer agora?")
            .setColor(0x00FF00);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                primary("criar-novo-log", "🆕 Criar Novo Squad-Log"),
                secondary("atualizar-log-existente", "📝 Atualizar Squad-Log Existente"),
                danger("sair-bot", "🚪 Sair")
            )
            .queue();
    }
    private void handleCreateNewLog(ButtonInteractionEvent event) {
        log.info("Iniciando criação de novo squad log");
        FormState newState = new FormState();
        newState.setCreating(true);
        newState.setEditing(false);
        newState.setStep(FormStep.SQUAD_SELECTION);
        formStateService.updateState(event.getUser().getIdLong(), newState);
        event.deferEdit().queue();
        showSquadSelectionDirectly(event, newState);
    }
    private void showSquadSelectionDirectly(ButtonInteractionEvent event, FormState state) {
        try {
            String squadsJson = withUserContext(event.getUser().getId(), () -> squadLogService.getSquads());
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Erro ao carregar squads")
                    .setDescription("Nenhuma squad encontrada.")
                    .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
                    .queue();
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏢 Seleção de Squad")
                .setDescription("Selecione a squad para criar o log:")
                .setColor(0x0099FF);
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("squad-select")
                .setPlaceholder("Escolha uma squad...");
            for (int i = 0; i < squadsArray.length(); i++) {
                JSONObject squad = squadsArray.getJSONObject(i);
                String squadId = String.valueOf(squad.get("id"));
                String squadName = squad.optString("name", "Squad " + squadId);
                menuBuilder.addOption(squadName, squadId);
            }
            event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(menuBuilder.build())
                .queue();
        } catch (Exception e) {
            log.error("Erro ao exibir seleção de squad: {}", e.getMessage());
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ Erro ao carregar squads")
                .setDescription("Ocorreu um erro ao carregar as squads. Tente novamente.")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
        }
    }
    private void handleUpdateExistingLog(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        
        if (!discordAuthService.isUserAuthenticated(userId)) {
            log.warn("Usuário {} não autenticado tentando atualizar squad-log", userId);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 Autenticação Necessária")
                .setDescription("faça a autenticação atraves do comando `/start`")
                .setColor(0xFFA500);
            event.editMessageEmbeds(embed.build())
                .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue();
            return;
        }
        
        log.info("Iniciando atualização de squad log existente para usuário autenticado: {}", userId);
        FormState newState = new FormState();
        newState.setCreating(false);
        newState.setEditing(true);
        newState.setStep(FormStep.LOG_SELECTION);
        formStateService.updateState(event.getUser().getIdLong(), newState);
        event.deferEdit().queue();
        try {
            log.info("Carregando lista de squad logs...");
            FormState state = formStateService.getOrCreateState(event.getUser().getIdLong());
            String squadLogsJson = withUserContext(event.getUser().getId(), 
                () -> squadLogService.getSquadLogAll(state.getCurrentPage(), LIMIT_PAGE));
            log.info("Resposta da API getSquadLogAll (página {}): {}", state.getCurrentPage(), squadLogsJson);
            JSONObject obj = new org.json.JSONObject(squadLogsJson);
            JSONArray squadLogsArray = obj.optJSONArray("items");
            int totalItems = obj.optInt("total", squadLogsArray != null ? squadLogsArray.length() : 0);
            state.setTotalPages((int) Math.ceil((double) totalItems / (double) LIMIT_PAGE));
            formStateService.updateState(event.getUser().getIdLong(), state);
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhum Squad Log encontrado para atualização.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            StringSelectMenu.Builder logMenuBuilder =
                StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um Squad Log para atualizar ");
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Atualizar Squad Log Existente")
                .setDescription("Escolha o Squad Log que deseja atualizar:\n📄 Página " + state.getCurrentPage() + " de " + state.getTotalPages())
                .setColor(0xFFAA00);


            Button voltarBtn = secondary("voltar", "⬅️ Anterior");
            Button avancarBtn = secondary("avancar", "➡️ Próxima");
            

            if (state.getCurrentPage() <= 1) {
                voltarBtn = voltarBtn.asDisabled();
            }
            if (state.getCurrentPage() >= state.getTotalPages()) {
                avancarBtn = avancarBtn.asDisabled();
            }
            
            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(
                            ActionRow.of(logMenuBuilder.build()),
                            ActionRow.of(
                                    voltarBtn,
                                    avancarBtn,
                                    primary("voltar-inicio", "🏠 Voltar ao Início")
                            )
                    )
                    .queue();

        } catch (Exception e) {
            log.error("Erro ao carregar Squad Logs: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ Erro ao carregar Squad Logs: " + e.getMessage())
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    private void buildLogSelectMenu(JSONArray squadLogsArray, StringSelectMenu.Builder logMenuBuilder) {
        for (int i = 0; i < squadLogsArray.length(); i++) {
            org.json.JSONObject log = squadLogsArray.getJSONObject(i);
            String logId = String.valueOf(log.get("id"));
            String squadName = "";
            org.json.JSONObject squad = log.optJSONObject("squad");
            if (squad != null) {
                squadName = squad.optString("name", "");
            }
            String userName = "";
            org.json.JSONObject user = log.optJSONObject("user");
            if (user != null) {
                userName = user.optString("name", "");
            }
            String description = log.optString("description", "");
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            String optionLabel = String.format("%s - %s: %s", squadName, userName, description);
            if (optionLabel.length() > 100) {
                optionLabel = optionLabel.substring(0, 97) + "...";
            }
            logMenuBuilder.addOption(optionLabel, logId);
        }
    }
    private void showErrorMessage(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription("Tente novamente ou entre em contato com o suporte.")
            .setColor(0xFF0000);
        event.editMessageEmbeds(embed.build())
            .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
            .queue();
    }
    private void showErrorMessageWithHook(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription("Tente novamente ou entre em contato com o suporte.")
            .setColor(0xFF0000);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(primary("voltar-inicio", "🏠 Voltar ao Início"))
            .queue();
    }
    private void handleExitBot(ButtonInteractionEvent event) {
        log.info("Usuário saindo do bot");
        formStateService.removeState(event.getUser().getIdLong());
        
        event.deferEdit().queue();
        
        EmbedBuilder thankYouEmbed = new EmbedBuilder()
            .setTitle("🙏 Obrigado por usar o Bot TeamBoarding!")
            .setDescription("Esperamos que tenha tido uma ótima experiência. Até a próxima!")
            .setColor(0x0099FF);
        
        event.getHook().editOriginalEmbeds(thankYouEmbed.build())
            .setComponents()
            .queue(success -> {
                try {
                    Thread.sleep(2000);
                    
                    EmbedBuilder exitingEmbed = new EmbedBuilder()
                        .setTitle("👋 Saindo...")
                        .setDescription("Finalizando sessão...")
                        .setColor(0xFFAA00);
                    
                    event.getHook().editOriginalEmbeds(exitingEmbed.build())
                        .setComponents()
                        .queue(success2 -> {
                            try {
                                Thread.sleep(2000);
                                event.getHook().deleteOriginal().queue(
                                    deleteSuccess -> log.info("Mensagem apagada com sucesso após saída do bot"),
                                    deleteError -> log.error("Erro ao apagar mensagem: {}", deleteError.getMessage())
                                );
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("Thread interrompida durante sleep final: {}", e.getMessage());
                            }
                        }, error2 -> log.error("Erro ao mostrar mensagem 'Saindo...': {}", error2.getMessage()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrompida durante sleep inicial: {}", e.getMessage());
                }
            }, error -> log.error("Erro ao mostrar mensagem de agradecimento: {}", error.getMessage()));
    }
    private void handleVoltarInicio(ButtonInteractionEvent event) {
        log.info("Usuário voltando ao início");
        formStateService.removeState(event.getUser().getIdLong());
        event.deferEdit().queue();
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🏠 Squad Log")
            .setDescription("Escolha uma opção")
            .setColor(0x0099FF);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar", "🆕 Criar"),
                secondary("atualizar", "📝 Atualizar")
            )
            .queue();
    }
    
    private void handleVoltarPage(ButtonInteractionEvent event) {
        FormState state = formStateService.getOrCreateState(event.getUser().getIdLong());
        log.info("Navegando para página anterior (atual: {})", state.getCurrentPage());
        if (state.getCurrentPage() > 1) {
            state.setCurrentPage(state.getCurrentPage() - 1);
            formStateService.updateState(event.getUser().getIdLong(), state);
            refreshLogSelection(event, state);
        } else {
            log.warn("Tentativa de voltar da primeira página");
            event.reply("❌ Você já está na primeira página!").setEphemeral(true).queue();
        }
    }
    
    private void handleAvancarPage(ButtonInteractionEvent event) {
        FormState state = formStateService.getOrCreateState(event.getUser().getIdLong());
        log.info("Navegando para próxima página (atual: {})", state.getCurrentPage());
        if (state.getCurrentPage() < state.getTotalPages()) {
            state.setCurrentPage(state.getCurrentPage() + 1);
            formStateService.updateState(event.getUser().getIdLong(), state);
            refreshLogSelection(event, state);
        } else {
            log.warn("Tentativa de avançar da última página");
            event.reply("❌ Você já está na última página!").setEphemeral(true).queue();
        }
    }
    
    private void refreshLogSelection(ButtonInteractionEvent event, FormState state) {
        event.deferEdit().queue();
        try {
            log.info("Atualizando lista de squad logs para página {}", state.getCurrentPage());
            String squadLogsJson = withUserContext(event.getUser().getId(), 
                () -> squadLogService.getSquadLogAll(state.getCurrentPage(), LIMIT_PAGE));
            log.info("Resposta da API getSquadLogAll (página {}): {}", state.getCurrentPage(), squadLogsJson);
            org.json.JSONObject obj = new org.json.JSONObject(squadLogsJson);
            org.json.JSONArray squadLogsArray = obj.optJSONArray("items");
            int totalItems = obj.optInt("total", squadLogsArray != null ? squadLogsArray.length() : 0);
            state.setTotalPages((int) Math.ceil((double) totalItems / (double) LIMIT_PAGE));
            formStateService.updateState(event.getUser().getIdLong(), state);
            
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhum Squad Log encontrado nesta página.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            
            StringSelectMenu.Builder logMenuBuilder =
                StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um Squad Log para atualizar");
            
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Atualizar Squad Log Existente")
                .setDescription("Escolha o Squad Log que deseja atualizar:\n📄 Página " + state.getCurrentPage() + " de " + state.getTotalPages())
                .setColor(0xFFAA00);

            Button voltarBtn = secondary("voltar", "⬅️ Anterior");
            Button avancarBtn = secondary("avancar", "➡️ Próxima");
            
            if (state.getCurrentPage() <= 1) {
                voltarBtn = voltarBtn.asDisabled();
            }
            if (state.getCurrentPage() >= state.getTotalPages()) {
                avancarBtn = avancarBtn.asDisabled();
            }
            
            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(
                            ActionRow.of(logMenuBuilder.build()),
                            ActionRow.of(
                                    voltarBtn,
                                    avancarBtn,
                                    primary("voltar-inicio", "🏠 Voltar ao Início")
                            )
                    )
                    .queue();
            
        } catch (Exception e) {
            log.error("Erro ao atualizar lista de Squad Logs: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ Erro ao carregar Squad Logs: " + e.getMessage())
                .setEmbeds()
                .setComponents()
                .queue();
        }
    }
    @Override
    public int getPriority() {
        return 6;
    }
}
