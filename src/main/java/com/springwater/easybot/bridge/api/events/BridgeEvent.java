package com.springwater.easybot.bridge.api.events;

import com.springwater.easybot.bridge.BridgeClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BridgeEvent {
    private BridgeClient client;
    /**
     * 取消后是否继续分发
     */
    private boolean continuePropagation = true;
    /**
     * 是否被取消
     */
    private boolean cancelled = false;
    
    public BridgeEvent(BridgeClient client) {
        this.client = client;
    }
}
