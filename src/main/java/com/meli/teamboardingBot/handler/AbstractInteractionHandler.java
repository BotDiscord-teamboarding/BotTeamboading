package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;

public abstract class AbstractInteractionHandler implements InteractionHandler {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    @Autowired
    protected FormStateService formStateService;
    
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        logger.warn("Button handling not implemented for: {}", event.getComponentId());
    }
    
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        logger.warn("String select handling not implemented for: {}", event.getComponentId());
    }
    
    @Override
    public void handleModal(ModalInteractionEvent event, FormState state) {
        logger.warn("Modal handling not implemented for: {}", event.getModalId());
    }
    
    protected String formatToBrazilianDate(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        return date;
    }
    
    protected void updateFormState(Long userId, FormState state) {
        formStateService.updateState(userId, state);
    }
    
    protected FormState getFormState(Long userId) {
        return formStateService.getState(userId);
    }
}
