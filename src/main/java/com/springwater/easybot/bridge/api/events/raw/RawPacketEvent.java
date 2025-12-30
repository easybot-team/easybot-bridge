package com.springwater.easybot.bridge.api.events.raw;


import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import com.springwater.easybot.bridge.packet.Packet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RawPacketEvent extends BridgeEvent {
    private Packet packet;
    private JsonObject body;
    /**
     * 跳过内部处理
     */
    private boolean skipInternalProcessing = false;
    public RawPacketEvent(BridgeClient client) {
        super(client);
    }
}
