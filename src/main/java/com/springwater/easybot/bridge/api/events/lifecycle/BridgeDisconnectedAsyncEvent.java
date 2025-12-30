package com.springwater.easybot.bridge.api.events.lifecycle;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BridgeDisconnectedAsyncEvent extends BridgeEvent {
    /**
     * 主机信息
     */
    private String host;
    /**
     * 断开原因
     */
    private String reason;

    public BridgeDisconnectedAsyncEvent(BridgeClient client) {
        super(client);
    }
}
