package com.springwater.easybot.bridge.api.events.raw;

import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BridgeSendPacketEvent extends BridgeEvent {
    /**
     * 发送的包
     */
    JsonObject packet;
    /**
     * 本条消息是否有回调
     */
    boolean hasCallbackId;
    public BridgeSendPacketEvent(BridgeClient client) {
        super(client);
    }
}
