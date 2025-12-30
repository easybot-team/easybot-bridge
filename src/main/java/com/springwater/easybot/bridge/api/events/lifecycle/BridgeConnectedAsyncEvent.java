package com.springwater.easybot.bridge.api.events.lifecycle;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BridgeConnectedAsyncEvent extends BridgeEvent {
    /**
     * 连接的url
     */
    private String url;

    public BridgeConnectedAsyncEvent(BridgeClient client) {
        super(client);
    }
}
