package com.springwater.easybot.bridge.api.events.raw;

import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import com.springwater.easybot.bridge.packet.PacketWithCallBackId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RawExecPacketEvent extends BridgeEvent {
    private PacketWithCallBackId packet;
    private JsonObject body;
    private JsonObject callBack;

    /**
     * 如果此值为true 则Bridge内部处理器将忽略此数据包 (谨慎处理)
     */
    private boolean skipInternalProcessing = false;
    
    public RawExecPacketEvent(BridgeClient client) {
        super(client);
    }
}
