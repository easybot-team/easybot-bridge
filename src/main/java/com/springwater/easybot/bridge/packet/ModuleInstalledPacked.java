package com.springwater.easybot.bridge.packet;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleInstalledPacked extends PacketWithCallBackId {
    @SerializedName("module_name")
    private String moduleName;
}
