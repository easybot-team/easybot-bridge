package com.springwater.easybot.bridge.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PluginInfo {
    @SerializedName("enabled")
    boolean enabled;
    @SerializedName("manifest")
    PluginManifest manifest;
    @SerializedName("rpc")
    PluginRpcInfo rpc;
}
