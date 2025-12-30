package com.springwater.easybot.bridge.api;

import com.springwater.easybot.bridge.BridgeClient;

import java.util.stream.Stream;

public interface IBridgeExtensionApi {
    void register(IBridgeExtension instance, IBridgeListener listener);

    void unregister(IBridgeListener listener);
    Stream<IBridgeExtension> getExtensions();
    
    static IBridgeExtensionApi getInstance() {
        return BridgeClient.getEventManager();
    }
}
