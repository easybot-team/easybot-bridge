package com.springwater.easybot.bridge.api;

import java.util.List;

public interface IBridgeExtension {
    /**
     * 获取插件标识
     */
    String getIdentifier();

    /**
     * 获取插件名称
     */
    String getName();
    
    /**
     * 获取插件描述
     */
    String getDescription();

    /**
     * 获取插件作者
     */
    String getAuthor();

    /**
     * 获取插件版本
     */
    String getVersion();

    /**
     * 获取插件依赖的插件
     */
    List<String> requiredPlugins();
}
