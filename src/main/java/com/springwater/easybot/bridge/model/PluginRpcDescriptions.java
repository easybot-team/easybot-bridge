package com.springwater.easybot.bridge.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor; 
@Data
@NoArgsConstructor
public class PluginRpcDescriptions {

    @SerializedName("displayName")
    private String displayName = "";

    @SerializedName("description")
    private String description = "";
}
