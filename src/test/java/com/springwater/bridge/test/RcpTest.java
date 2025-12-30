package com.springwater.bridge.test;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.BridgeEventHandler;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.IBridgeExtensionApi;
import com.springwater.easybot.bridge.api.IBridgeListener;
import com.springwater.easybot.bridge.api.events.lifecycle.BridgeOnlineAsyncEvent;
import com.springwater.easybot.bridge.api.rpc.BridgeRpc;
import com.springwater.easybot.bridge.api.rpc.IBridgeRpcManager;
import com.springwater.easybot.bridge.api.rpc.IRpcListener;
import com.springwater.easybot.bridge.api.rpc.RpcContext;
import com.springwater.easybot.bridge.packet.GetInstalledPluginResultPacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RcpTest implements IRpcListener, IBridgeListener, IBridgeExtension {
    private final BridgeClient client = new BridgeClient("ws://127.0.0.1:26990/bridge", new MockBridgeBehavior());

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Gson gson = new Gson();

    @BridgeEventHandler
    public void connected(BridgeOnlineAsyncEvent event) {
        GetInstalledPluginResultPacket result = event.getClient().getInstalledPlugin();
        BridgeClient.getLogger().info("插件列表: " + gson.toJson(result.getPlugins()));
    }

    @Test
    public void doTest() throws InterruptedException {
        IBridgeRpcManager.getInstance().registerRpc(this, this);
        IBridgeExtensionApi.getInstance().register(this, this);
        client.setToken("JuxMzi6aCr1yHwm9aj0QDe1vvj0PtFzE");
        client.stop();
        System.out.println("等待 RPC 调用...");
        boolean received = latch.await(120, TimeUnit.SECONDS);
        if (received) {
            System.out.println("成功收到 RPC 调用信号");
        } else {
            System.err.println("等待超时，未收到 RPC 调用");
        }
        // 等待一段时间
        Thread.sleep(5000);
        client.close();
    }

    @BridgeRpc(method = "get_date", description = "获取服务器时间", displayName = "获取时间")
    public void getDate(RpcContext context) {
        context.getResult().addProperty("date", System.currentTimeMillis());
        JsonObject obj = context.getClient().rpcCall("rpc_test_plugin", "get_servers", new JsonObject());
        BridgeClient.getLogger().info("对象: " + obj.toString());
        latch.countDown();
    }

    @Override
    public String getIdentifier() {
        return "bridge:test";
    }

    @Override
    public String getName() {
        return "单元测试扩展";
    }

    @Override
    public String getDescription() {
        return "这是一个由单元测试创建的扩展 用于测试EasyBot的各种功能";
    }

    @Override
    public String getAuthor() {
        return "MiuxuE";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> requiredPlugins() {
        return List.of("rpc_test_plugin");
    }
}
