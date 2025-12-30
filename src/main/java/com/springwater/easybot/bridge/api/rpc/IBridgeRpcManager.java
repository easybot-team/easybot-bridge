package com.springwater.easybot.bridge.api.rpc;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.IBridgeExtension;

import java.util.HashMap;
import java.util.List;

public interface IBridgeRpcManager {
    void registerRpc(IBridgeExtension instance, IRpcListener listener);

    void unregisterRpc(IBridgeExtension instance);

    HashMap<IBridgeExtension, List<IRpcListener>> getRpcListeners();

    static IBridgeRpcManager getInstance() {
        return BridgeClient.getRpcManager();
    }
}
