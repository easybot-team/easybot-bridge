package com.springwater.easybot.bridge.packet;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcCallPacket extends PacketWithCallBackId {
    @SerializedName("identifier")
    private String identifier;
    @SerializedName("method")
    private String method;
    @SerializedName("body")
    private JsonObject body;
}
