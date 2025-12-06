package org.easybot.bridge.packet;

import com.google.gson.annotations.SerializedName;
import org.easybot.bridge.OpCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Packet {
    @SerializedName("op")
    private OpCode opCode;
}
