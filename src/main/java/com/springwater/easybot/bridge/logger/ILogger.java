package com.springwater.easybot.bridge.logger;

public interface ILogger {
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable t);
    void debug(String message);
}