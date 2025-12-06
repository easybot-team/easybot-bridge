package org.easybot.bridge.packet;

import com.google.gson.annotations.SerializedName;
import org.easybot.bridge.OpCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerStatePacket extends PacketWithCallBackId {
    @SerializedName("token")
    private String token;
    @SerializedName("players")
    private String players;
    public ServerStatePacket() {
        setOpCode(OpCode.Packet);
        setOperation("SERVER_STATE_CHANGED");
    }
}

