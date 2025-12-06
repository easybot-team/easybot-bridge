package org.easybot.bridge.packet;

import org.easybot.bridge.OpCode;

public class HeartbeatPacket extends Packet {
    public HeartbeatPacket(){
        setOpCode(OpCode.HeartBeat);
    }
}
