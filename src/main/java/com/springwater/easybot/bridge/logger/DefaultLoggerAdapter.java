package com.springwater.easybot.bridge.logger;

import java.util.logging.Logger;

public class DefaultLoggerAdapter implements ILogger{
    private static final Logger logger = Logger.getLogger("EasyBot");
    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.severe(message);
    }

    @Override
    public void debug(String message) {
        logger.fine(message);
    }
}
