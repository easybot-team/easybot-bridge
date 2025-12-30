package com.springwater.easybot.bridge.api.events.lifecycle;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import com.springwater.easybot.bridge.packet.HelloPacket;
import com.springwater.easybot.bridge.packet.IdentifySuccessPacket;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BridgeOnlineAsyncEvent extends BridgeEvent {
    /**
     * 主机信息
     */
    private HelloPacket hostInfo;

    /**
     * 登录信息
     */
    private IdentifySuccessPacket identify;

    public BridgeOnlineAsyncEvent(BridgeClient client) {
        super(client);
    }
}
