package com.springwater.easybot.bridge.api;
public enum BridgeHandlerPriority {
    /**
     * 最低优先级 - 事件处理器将在最后执行
     */
    LOWEST,
    /**
     * 低优先级 - 事件处理器在较低优先级执行
     */
    LOW,
    /**
     * 中等优先级 - 事件处理器在中间优先级执行
     */
    MEDIUM,
    /**
     * 普通优先级 - 事件处理器在普通优先级执行
     */
    NORMAL,
    /**
     * 高优先级 - 事件处理器在较高优先级执行
     */
    HIGH,
    /**
     * 最高优先级 - 事件处理器将首先执行
     */
    HIGHEST;

    public static BridgeHandlerPriority getPriority(int priority) {
        return values()[priority];
    }

    public static int getPriority(BridgeHandlerPriority priority) {
        return priority.ordinal();
    }

    public static int getPriority(String priority) {
        return getPriority(BridgeHandlerPriority.valueOf(priority));
    }
}
