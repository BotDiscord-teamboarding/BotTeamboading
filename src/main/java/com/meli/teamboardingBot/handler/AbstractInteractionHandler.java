package com.meli.teamboardingBot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractInteractionHandler implements InteractionHandler {
    protected static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    protected final FormStateService formStateService;
    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        log.warn("Button handling not implemented for: {}", event.getComponentId());
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        log.warn("String select handling not implemented for: {}", event.getComponentId());
    }
    @Override
    public void handleModal(ModalInteractionEvent event, FormState state) {
        log.warn("Modal handling not implemented for: {}", event.getModalId());
    }
    protected String formatToBrazilianDate(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        if (date.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return date;
        }
        if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = date.split("-");
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            } catch (Exception e) {
                log.error("Error converting ISO date to Brazilian format: {}", e.getMessage());
                return date;
            }
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
