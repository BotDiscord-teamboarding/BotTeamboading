package com.meli.teamboardingBot.adapters.in.listener;
import com.meli.teamboardingBot.adapters.handler.BatchCreationHandler;
import com.meli.teamboardingBot.adapters.handler.InteractionHandler;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.context.UserContext;
import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.PutFormStatePort;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class ComponentInteractionListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ComponentInteractionListener.class);
    private final List<InteractionHandler> handlers;
    private final GetOrCreateFormStatePort getOrCreateFormStatePort;
    private final PutFormStatePort putFormStatePort;
    private final BatchCreationHandler batchCreationHandler;
    @Autowired
    public ComponentInteractionListener(List<InteractionHandler> handlers, GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, BatchCreationHandler batchCreationHandler) {
        this.handlers = handlers;
        this.getOrCreateFormStatePort = getOrCreateFormStatePort;
        this.putFormStatePort = putFormStatePort;
        this.batchCreationHandler = batchCreationHandler;
        this.handlers.sort((h1, h2) -> Integer.compare(h1.getPriority(), h2.getPriority()));
        logger.info("Initialized with {} handlers", handlers.size());
    }
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        long userId = event.getUser().getIdLong();
        logger.info("Button interaction: {} from user: {}", buttonId, userId);

        if (isLanguageButton(buttonId)) {
            logger.debug("Botão de idioma será processado por LanguageSelectionHandler");
            return;
        }

        if (isAuthenticationButton(buttonId)) {
            logger.debug("Botão de autenticação será processado por LoginModalHandler");
            return;
        }
        
        if (buttonId.equals("open-batch-modal")) {
            try {
                UserContext.setCurrentUserId(String.valueOf(userId));
                batchCreationHandler.handleOpenBatchModalButton(event);
                return;
            } catch (Exception e) {
                logger.error("Error opening batch modal: {}", e.getMessage());
                event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                return;
            } finally {
                UserContext.clear();
            }
        }
        
        if (isBatchButton(buttonId)) {
            try {
                UserContext.setCurrentUserId(String.valueOf(userId));
                batchCreationHandler.handleBatchNavigation(event);
                return;
            } catch (Exception e) {
                logger.error("Error handling batch button {}: {}", buttonId, e.getMessage());
                event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                return;
            } finally {
                UserContext.clear();
            }
        }
        
        FormState state = getOrCreateFormStatePort.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(buttonId)) {
                try {
                    handler.handleButton(event, state);
                    putFormStatePort.updateState(userId, state);
                    return;
                } catch (Exception e) {
                    logger.error("Error handling button {} with handler {}: {}", buttonId, handler.getClass().getSimpleName(), e.getMessage());
                    event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                    return;
                }
            }
        }
        logger.warn("No handler found for button: {}", buttonId);
        event.reply("❌ Componente não reconhecido.").setEphemeral(true).queue();
    }
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String selectId = event.getComponentId();
        long userId = event.getUser().getIdLong();
        logger.info("String select interaction: {} from user: {}", selectId, userId);
        FormState state = getOrCreateFormStatePort.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(selectId)) {
                try {
                    handler.handleStringSelect(event, state);
                    putFormStatePort.updateState(userId, state);
                    return;
                } catch (Exception e) {
                    logger.error("Error handling select {} with handler {}: {}", selectId, handler.getClass().getSimpleName(), e.getMessage());
                    event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                    return;
                }
            }
        }
        logger.warn("No handler found for select: {}", selectId);
        event.reply("❌ Componente não reconhecido.").setEphemeral(true).queue();
    }
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        long userId = event.getUser().getIdLong();
        logger.info("Modal interaction: {} from user: {}", modalId, userId);
        
        if ("login-modal".equals(modalId) || "modal-google-code".equals(modalId)) {
            logger.debug("Modal de login/autenticação será processado por LoginModalHandler");
            return;
        }
        
        if (isBatchModal(modalId)) {
            try {
                UserContext.setCurrentUserId(String.valueOf(userId));
                if (modalId.equals("batch-creation-modal")) {
                    batchCreationHandler.handleBatchCreationModal(event);
                } else if (modalId.equals("batch-edit-modal")) {
                    batchCreationHandler.handleEditEntryModal(event);
                } else if (modalId.equals("batch-edit-modal-page1") || modalId.equals("batch-edit-modal-page2")) {
                    batchCreationHandler.handleEditEntryModal(event);
                } else if (modalId.endsWith("-modal") && modalId.startsWith("batch-edit-")) {
                    batchCreationHandler.handleFieldEditModal(event);
                }
                return;
            } catch (Exception e) {
                logger.error("Error handling batch modal {}: {}", modalId, e.getMessage());
                event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                return;
            } finally {
                UserContext.clear();
            }
        }
        
        FormState state = getOrCreateFormStatePort.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(modalId)) {
                try {
                    handler.handleModal(event, state);
                    putFormStatePort.updateState(userId, state);
                    return;
                } catch (Exception e) {
                    logger.error("Error handling modal {} with handler {}: {}", modalId, handler.getClass().getSimpleName(), e.getMessage());
                    event.reply("❌ Erro interno. Tente novamente.").setEphemeral(true).queue();
                    return;
                }
            }
        }
        logger.warn("No handler found for modal: {}", modalId);
        event.reply("❌ Componente não reconhecido.").setEphemeral(true).queue();
    }

    private boolean isBatchButton(String buttonId) {
        return buttonId.startsWith("batch-");
    }

    private boolean isBatchModal(String modalId) {
        return modalId.equals("batch-creation-modal") || modalId.equals("batch-edit-modal") ||
               modalId.equals("batch-edit-modal-page1") || modalId.equals("batch-edit-modal-page2") ||
               (modalId.startsWith("batch-edit-") && modalId.endsWith("-modal"));
    }

    private boolean isLanguageButton(String buttonId) {
        return buttonId.startsWith("confirm-language-") ||
                buttonId.startsWith("change-language-") ||
                "continue-to-auth".equals(buttonId) ||
                "execute-pending-command".equals(buttonId);
    }

    private boolean isAuthenticationButton(String buttonId) {
        return "btn-autenticar".equals(buttonId) ||
                "btn-auth-manual".equals(buttonId) ||
                "btn-auth-google".equals(buttonId) ||
                "btn-confirm-manual-login".equals(buttonId) ||
                "btn-switch-to-google".equals(buttonId) ||
                "btn-submit-google-code".equals(buttonId) ||
                "auth-manual".equals(buttonId) ||
                "auth-google".equals(buttonId) ||
                "start-auth".equals(buttonId) ||
                "cancel-auth".equals(buttonId) ||
                "voltar-para-escolha".equals(buttonId) ||
                "cancelar-escolha".equals(buttonId) ||
                "status-close".equals(buttonId) ||
                "status-logout".equals(buttonId) ||
                "help-close".equals(buttonId);
    }
}
