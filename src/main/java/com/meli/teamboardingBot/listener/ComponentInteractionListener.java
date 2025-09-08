package com.meli.teamboardingBot.listener;
import com.meli.teamboardingBot.handler.InteractionHandler;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
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
    private final FormStateService formStateService;
    @Autowired
    public ComponentInteractionListener(List<InteractionHandler> handlers, FormStateService formStateService) {
        this.handlers = handlers;
        this.formStateService = formStateService;
        this.handlers.sort((h1, h2) -> Integer.compare(h1.getPriority(), h2.getPriority()));
        logger.info("Initialized with {} handlers", handlers.size());
    }
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        long userId = event.getUser().getIdLong();
        logger.info("Button interaction: {} from user: {}", buttonId, userId);
        FormState state = formStateService.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(buttonId)) {
                try {
                    handler.handleButton(event, state);
                    formStateService.updateState(userId, state);
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
        FormState state = formStateService.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(selectId)) {
                try {
                    handler.handleStringSelect(event, state);
                    formStateService.updateState(userId, state);
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
        FormState state = formStateService.getOrCreateState(userId);
        if (state == null) {
            event.reply("❌ Sessão expirada. Use /squad-log para começar novamente.").setEphemeral(true).queue();
            return;
        }
        for (InteractionHandler handler : handlers) {
            if (handler.canHandle(modalId)) {
                try {
                    handler.handleModal(event, state);
                    formStateService.updateState(userId, state);
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
}
