package com.springwater.bridge.test;

import com.springwater.easybot.bridge.BridgeBehavior;
import com.springwater.easybot.bridge.ClientProfile;
import com.springwater.easybot.bridge.message.Segment;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.ServerInfo;

import java.util.Collections;
import java.util.List;

public class MockBridgeBehavior implements BridgeBehavior {
    
    MockBridgeBehavior() {
        ClientProfile.setPluginVersion("1.0.0");
        ClientProfile.setServerDescription("junit");
    }
    @Override
    public String runCommand(String playerName, String command, boolean enablePapi) {
        return "MOCK RESULT";
    }

    @Override
    public String papiQuery(String playerName, String query) {
        return query;
    }

    @Override
    public ServerInfo getInfo() {
        ServerInfo info = new ServerInfo();
        info.setPapiSupported(true);
        info.setServerVersion("mock");
        info.setPluginVersion("1.0.0");
        info.setServerName("mock@junit");
        info.setCommandSupported(true);
        return info;
    }

    @Override
    public void SyncToChat(String message) {

    }

    @Override
    public void BindSuccessBroadcast(String playerName, String accountId, String accountName) {

    }

    @Override
    public void KickPlayer(String plauer, String kickMessage) {

    }

    @Override
    public void SyncToChatExtra(List<Segment> segments, String text) {

    }

    @Override
    public List<PlayerInfo> getPlayerList() {
        return Collections.emptyList();
    }
}
