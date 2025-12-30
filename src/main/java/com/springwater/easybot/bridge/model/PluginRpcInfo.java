package com.springwater.easybot.bridge.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
@NoArgsConstructor
public class PluginRpcInfo {

    @SerializedName("methods")
    private List<String> methods = new ArrayList<>();

    @SerializedName("descriptions")
    private Map<String, PluginRpcDescriptions> descriptions = new HashMap<>();
}

