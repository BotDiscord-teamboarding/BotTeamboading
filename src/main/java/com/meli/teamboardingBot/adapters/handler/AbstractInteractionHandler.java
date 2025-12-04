package com.meli.teamboardingBot.adapters.handler;

import com.meli.teamboardingBot.core.context.UserContext;
import com.meli.teamboardingBot.core.ports.formstate.*;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;


public abstract class AbstractInteractionHandler implements InteractionHandler {
    protected static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    protected final GetOrCreateFormStatePort getOrCreateFormStatePort;
    protected final PutFormStatePort putFormStatePort;
    protected final GetFormStatePort getFormStatePort;
    protected final SetBatchEntriesPort setBatchEntriesPort;
    protected final SetBatchCurrentIndexPort setBatchCurrentIndexPort;
    protected final GetBatchEntriesPort getBatchEntriesPort;
    protected final GetBatchCurrentIndexPort getBatchCurrentIndexPort;
    protected final ClearBatchStatePort clearBatchStatePort;
    protected final DeleteFormStatePort deleteFormStatePort;
    protected final ResetFormStatePort resetFormStatePort;
    protected  final LoggerApiPort loggerApiPort;

    public AbstractInteractionHandler(GetOrCreateFormStatePort getOrCreateFormStatePort, PutFormStatePort putFormStatePort, GetFormStatePort getFormStatePort, SetBatchEntriesPort setBatchEntriesPort, SetBatchCurrentIndexPort setBatchCurrentIndexPort, GetBatchEntriesPort getBatchEntriesPort, GetBatchCurrentIndexPort getBatchCurrentIndexPort, ClearBatchStatePort clearBatchStatePort, DeleteFormStatePort deleteFormStatePort, ResetFormStatePort resetFormStatePort, LoggerApiPort loggerApiPort) {
        this.getOrCreateFormStatePort = getOrCreateFormStatePort;
        this.putFormStatePort = putFormStatePort;
        this.getFormStatePort = getFormStatePort;
        this.setBatchEntriesPort = setBatchEntriesPort;
        this.setBatchCurrentIndexPort = setBatchCurrentIndexPort;
        this.getBatchEntriesPort = getBatchEntriesPort;
        this.getBatchCurrentIndexPort = getBatchCurrentIndexPort;
        this.clearBatchStatePort = clearBatchStatePort;
        this.deleteFormStatePort = deleteFormStatePort;
        this.resetFormStatePort = resetFormStatePort;
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void handleButton(ButtonInteractionEvent event, FormState state) {
        String userId = event.getUser().getId();
        try {
            UserContext.setCurrentUserId(userId);
            loggerApiPort.debug("Button interaction received: {} from user {}", event.getComponentId(), userId);
            handleButtonInternal(event, state);
        } finally {
            UserContext.clear();
        }
    }
    
    protected void handleButtonInternal(ButtonInteractionEvent event, FormState state) {
        loggerApiPort.warn("Button handling not implemented for: {}", event.getComponentId());
    }
    @Override
    public void handleStringSelect(StringSelectInteractionEvent event, FormState state) {
        String userId = event.getUser().getId();
        try {
            UserContext.setCurrentUserId(userId);
            loggerApiPort.debug("String select interaction received: {} from user {}", event.getComponentId(), userId);
            handleStringSelectInternal(event, state);
        } finally {
            UserContext.clear();
        }
    }
    
    protected void handleStringSelectInternal(StringSelectInteractionEvent event, FormState state) {
        loggerApiPort.warn("String select handling not implemented for: {}", event.getComponentId());
    }
    @Override
    public void handleModal(ModalInteractionEvent event, FormState state) {
        String userId = event.getUser().getId();
        try {
            UserContext.setCurrentUserId(userId);
            loggerApiPort.debug("Modal interaction received: {} from user {}", event.getModalId(), userId);
            handleModalInternal(event, state);
        } finally {
            UserContext.clear();
        }
    }
    
    protected void handleModalInternal(ModalInteractionEvent event, FormState state) {
        loggerApiPort.warn("Modal handling not implemented for: {}", event.getModalId());
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
                loggerApiPort.error("Error converting ISO date to Brazilian format: {}", e.getMessage());
                return date;
            }
        }
        return date;
    }
    protected void updateFormState(Long userId, FormState state) {
        putFormStatePort.updateState(userId, state);
    }
    protected FormState getFormState(Long userId) {
        return getFormStatePort.getState(userId);
    }
    

    protected <T> T withUserContext(String userId, Supplier<T> operation) {
        try {
            UserContext.setCurrentUserId(userId);
            return operation.get();
        } finally {
            UserContext.clear();
        }
    }

    protected void withUserContext(String userId, Runnable operation) {
        try {
            UserContext.setCurrentUserId(userId);
            operation.run();
        } finally {
            UserContext.clear();
        }
    }
}
