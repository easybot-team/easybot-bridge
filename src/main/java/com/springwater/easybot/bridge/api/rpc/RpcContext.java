package com.springwater.easybot.bridge.api.rpc;

import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import lombok.Getter;

@Getter
public class RpcContext {
    /**
     * 桥接客户端
     */
    private final BridgeClient client;
    /**
     * 传递的参数
     */
    private final JsonObject body;
    /**
     * 返回结果
     */
    private final JsonObject result;
    
    private final JsonObject error;
    
    public RpcContext(BridgeClient client, JsonObject body) {
        this.client = client;
        this.body = body;
        this.result = new JsonObject();
        this.error = new JsonObject();
    }
}
