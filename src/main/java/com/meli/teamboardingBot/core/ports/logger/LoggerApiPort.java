package com.meli.teamboardingBot.core.ports.logger;

public interface LoggerApiPort  {

    void info (String txt, Object... arguments);

    void debug (String txt, Object... arguments);

    void error (String txt, Object... arguments);

    void warn (String txt, Object... arguments);

}
