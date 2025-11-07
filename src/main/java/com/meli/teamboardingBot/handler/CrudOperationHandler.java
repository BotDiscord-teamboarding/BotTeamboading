package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.SquadLogService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@Order(6)
public class CrudOperationHandler extends AbstractInteractionHandler {
    private final SquadLogService squadLogService;

    private int currentPage = 1;
    private final int limitPage = 15;
    private int totalPages;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;


    public CrudOperationHandler(FormStateService formStateService, SquadLogService squadLogService) {
        super(formStateService);
        this.squadLogService = squadLogService;
    }
    @Override
    public boolean canHandle(String componentId) {
        return "criar-log".equals(componentId) ||
               "confirmar-criacao".equals(componentId) ||
               "confirmar-atualizacao".equals(componentId) ||
               "criar-novo-log".equals(componentId) ||
               "atualizar-log-existente".equals(componentId) ||
               "atualizar".equals(componentId) ||
               "sair-bot".equals(componentId) ||
               "voltar-inicio".equals(componentId) ||
               "voltar".equals(componentId) ||
               "avancar".equals(componentId);
    }
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String buttonId = event.getComponentId();
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
    }
    private void handleCreateSquadLog(ButtonInteractionEvent event, FormState state) {
        log.info("Criando squad log");
        event.deferEdit().queue();
        if (!isStateValid(state)) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_dados_incompletos", null, state.getLocale()))
                .setDescription(".")
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, state.getLocale())))
                .queue();
            return;
        }
        try {
            String payload = buildCreatePayload(state);
            log.info("Payload de criação: {}", payload);
            ResponseEntity<String> response = squadLogService.createSquadLog(payload);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ "+ messageSource.getMessage("txt_log_criado_com_sucesso", null, state.getLocale()) +"!", true);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ "+ messageSource.getMessage("txt_erro_criar_log", null, state.getLocale()) + ": " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao criar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ " + messageSource.getMessage("txt_erro_criar_log_mensagem", null, state.getLocale()) +".");
        }
    }
    private void handleUpdateSquadLog(ButtonInteractionEvent event, FormState state) {
        log.info("Atualizando squad log ID: {}", state.getSquadLogId());
        event.deferEdit().queue();
        if (!isStateValid(state) || state.getSquadLogId() == null) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + messageSource.getMessage("txt_dados_incompletos", null, state.getLocale()))
                .setDescription(messageSource.getMessage("txt_dados_incompletos_ou_id_log_nao_encontrado", null, state.getLocale()))
                .setColor(0xFF0000);
            event.getHook().editOriginalEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, state.getLocale())))
                .queue();
            return;
        }
        try {
            String payload = buildUpdatePayload(state);
            log.info("Payload de atualização para squad-log ID {}: {}", state.getSquadLogId(), payload);
            log.info("Estado atual: squadId={}, userId={}, typeId={}, categoryIds={}, startDate={}, endDate={}", 
                       state.getSquadId(), state.getUserId(), state.getTypeId(), 
                       state.getCategoryIds(), state.getStartDate(), state.getEndDate());
            ResponseEntity<String> response = squadLogService.updateSquadLog(state.getSquadLogId(), payload);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessMessageWithHook(event, "✅ " + messageSource.getMessage("txt_log_atualizado_com_sucesso", null, state.getLocale()) +"!", false);
                formStateService.removeState(event.getUser().getIdLong());
            } else {
                showErrorMessageWithHook(event, "❌ " + messageSource.getMessage("txt_erro_atualizar_log", null, state.getLocale()) + ": " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao atualizar squad log: {}", e.getMessage());
            showErrorMessageWithHook(event, "❌ " + messageSource.getMessage("txt_erro_atualizar_log_mensagem", null, state.getLocale()) + ".");
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
    private void showSuccessMessage(ButtonInteractionEvent event, String message, boolean isCreation) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription(isCreation ? 
                messageSource.getMessage("txt_squad_log_criado_com_sucesso", null, formState.getLocale()) + "?" :
                messageSource.getMessage("txt_squad_log_atualizado_com_sucesso", null, formState.getLocale()) + "?")
            .setColor(0x00FF00);
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("criar-novo-log", "🆕 " + messageSource.getMessage("txt_criar_novo_squad_log", null, formState.getLocale())),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("atualizar-log-existente", "📝 " + messageSource.getMessage("txt_atualizar_squad_log_existente", null, formState.getLocale())),
                net.dv8tion.jda.api.interactions.components.buttons.Button.danger("sair-bot", "🚪 " + messageSource.getMessage("txt_sair", null, formState.getLocale()))
            )
            .queue();
    }
    private void showSuccessMessageWithHook(ButtonInteractionEvent event, String message, boolean isCreation) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription(isCreation ?
                    messageSource.getMessage("txt_squad_log_criado_com_sucesso", null, formState.getLocale()) + "?" :
                    messageSource.getMessage("txt_squad_log_atualizado_com_sucesso", null, formState.getLocale()) + "?")
            .setColor(0x00FF00);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("criar-novo-log", "🆕 " + messageSource.getMessage("txt_criar_novo_squad_log", null, formState.getLocale())),
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("atualizar-log-existente", "📝 " + messageSource.getMessage("txt_atualizar_squad_log_existente", null, formState.getLocale())),
                net.dv8tion.jda.api.interactions.components.buttons.Button.danger("sair-bot", "🚪 " + messageSource.getMessage("txt_sair", null, formState.getLocale()))
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
            String squadsJson = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsJson);
            JSONArray squadsArray = obj.optJSONArray("items");
            if (squadsArray == null || squadsArray.length() == 0) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Erro ao carregar squads")
                    .setDescription("Nenhuma squad encontrada.")
                    .setColor(0xFF0000);
                event.getHook().editOriginalEmbeds(errorEmbed.build())
                    .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, state.getLocale())))
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
                .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, state.getLocale())))
                .queue();
        }
    }
    private void handleUpdateExistingLog(ButtonInteractionEvent event) {
        log.info("Iniciando atualização de squad log existente");
        FormState newState = new FormState();
        newState.setCreating(false);
        newState.setEditing(true);
        newState.setStep(FormStep.LOG_SELECTION);
        formStateService.updateState(event.getUser().getIdLong(), newState);
        event.deferEdit().queue();
        try {
            log.info("Carregando lista de squad logs...");
            String squadLogsJson = squadLogService.getSquadLogAll(currentPage, limitPage);
            log.info("Resposta da API getSquadLogAll (página {}): {}", currentPage, squadLogsJson);
            org.json.JSONObject obj = new org.json.JSONObject(squadLogsJson);
            org.json.JSONArray squadLogsArray = obj.optJSONArray("items");
            int totalItems = obj.optInt("total", squadLogsArray != null ? squadLogsArray.length() : 0);
            this.totalPages = (int) Math.ceil((double) totalItems / this.limitPage);
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhum Squad Log encontrado para atualização.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder logMenuBuilder =
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um Squad Log para atualizar ");
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Atualizar Squad Log Existente")
                .setDescription("Escolha o Squad Log que deseja atualizar:\n📄 Página " + currentPage + " de " + totalPages)
                .setColor(0xFFAA00);


            Button voltarBtn = Button.secondary("voltar", "⬅️ " + messageSource.getMessage("txt_anterior", null, formState.getLocale()));
            Button avancarBtn = Button.secondary("avancar", "➡️ "  + messageSource.getMessage("txt_proxima", null, formState.getLocale()));
            

            if (currentPage <= 1) {
                voltarBtn = voltarBtn.asDisabled();
            }
            if (currentPage >= totalPages) {
                avancarBtn = avancarBtn.asDisabled();
            }
            
            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(
                            net.dv8tion.jda.api.interactions.components.ActionRow.of(logMenuBuilder.build()),
                            net.dv8tion.jda.api.interactions.components.ActionRow.of(
                                    voltarBtn,
                                    avancarBtn,
                                    Button.primary("voltar-inicio", "🏠 Voltar ao Início")
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
    private void buildLogSelectMenu(org.json.JSONArray squadLogsArray, net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder logMenuBuilder) {
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
            .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
            .queue();
    }
    private void showErrorMessageWithHook(ButtonInteractionEvent event, String message) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(message)
            .setDescription("Tente novamente ou entre em contato com o suporte.")
            .setColor(0xFF0000);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
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
        currentPage = 1;
        event.deferEdit().queue();
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🏠 Squad Log")
            .setDescription("Escolha uma opção")
            .setColor(0x0099FF);
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar", "🆕 Criar"),
                Button.secondary("atualizar", "📝 Atualizar")
            )
            .queue();
    }
    
    private void handleVoltarPage(ButtonInteractionEvent event) {
        log.info("Navegando para página anterior (atual: {})", currentPage);
        if (currentPage > 1) {
            currentPage--;
            refreshLogSelection(event);
        } else {
            log.warn("Tentativa de voltar da primeira página");
            event.reply("❌ Você já está na primeira página!").setEphemeral(true).queue();
        }
    }
    
    private void handleAvancarPage(ButtonInteractionEvent event) {
        log.info("Navegando para próxima página (atual: {})", currentPage);
        if (currentPage < totalPages) {
            currentPage++;
            refreshLogSelection(event);
        } else {
            log.warn("Tentativa de avançar da última página");
            event.reply("❌ Você já está na última página!").setEphemeral(true).queue();
        }
    }
    
    private void refreshLogSelection(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        try {
            log.info("Atualizando lista de squad logs para página {}", currentPage);
            String squadLogsJson = squadLogService.getSquadLogAll(currentPage, limitPage);
            log.info("Resposta da API getSquadLogAll (página {}): {}", currentPage, squadLogsJson);
            org.json.JSONObject obj = new org.json.JSONObject(squadLogsJson);
            org.json.JSONArray squadLogsArray = obj.optJSONArray("items");
            int totalItems = obj.optInt("total", squadLogsArray != null ? squadLogsArray.length() : 0);
            this.totalPages = (int) Math.ceil((double) totalItems / this.limitPage);
            
            if (squadLogsArray == null || squadLogsArray.length() == 0) {
                event.getHook().editOriginal("❌ Nenhum Squad Log encontrado nesta página.")
                    .setEmbeds()
                    .setComponents()
                    .queue();
                return;
            }
            
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder logMenuBuilder =
                net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create("log-select")
                    .setPlaceholder("Selecione um Squad Log para atualizar");
            
            buildLogSelectMenu(squadLogsArray, logMenuBuilder);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📝 Atualizar Squad Log Existente")
                .setDescription("Escolha o Squad Log que deseja atualizar:\n📄 Página " + currentPage + " de " + totalPages)
                .setColor(0xFFAA00);

            Button voltarBtn = Button.secondary("voltar", "⬅️ Anterior");
            Button avancarBtn = Button.secondary("avancar", "➡️ Próxima");
            
            if (currentPage <= 1) {
                voltarBtn = voltarBtn.asDisabled();
            }
            if (currentPage >= totalPages) {
                avancarBtn = avancarBtn.asDisabled();
            }
            
            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(
                            net.dv8tion.jda.api.interactions.components.ActionRow.of(logMenuBuilder.build()),
                            net.dv8tion.jda.api.interactions.components.ActionRow.of(
                                    voltarBtn,
                                    avancarBtn,
                                    Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()))
                            )
                    )
                    .queue();
            
        } catch (Exception e) {
            log.error("Erro ao atualizar lista de Squad Logs: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ " + messageSource.getMessage("txt_erro_carregar_squad_logs", null, formState.getLocale()) + ": " + e.getMessage())
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
