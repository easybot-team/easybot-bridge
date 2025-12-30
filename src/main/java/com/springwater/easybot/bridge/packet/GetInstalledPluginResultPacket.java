package com.springwater.easybot.bridge.packet;

import com.google.gson.annotations.SerializedName;
import com.springwater.easybot.bridge.model.PluginInfo;
import lombok.Getter;

import java.util.HashMap;
@Getter
public class GetInstalledPluginResultPacket extends PacketWithCallBackId{
    @SerializedName("plugins")
    private HashMap<String, PluginInfo> plugins;
}
