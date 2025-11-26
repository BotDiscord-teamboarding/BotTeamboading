package com.meli.teamboardingBot.adapters.config.logger;

import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggerApi implements LoggerApiPort {

    private final Logger logger = LoggerFactory.getLogger(LoggerApi.class);

    public void info(String txt, Object... arguments) {
        logger.info(txt, arguments);
    }

    public void debug(String txt, Object... arguments) {
        logger.debug(txt, arguments);
    }

    public void error(String txt, Object... arguments) {
        logger.debug(txt, arguments);
    }

    public void warn(String txt, Object... arguments) {
        logger.warn(txt, arguments);
    }
}
