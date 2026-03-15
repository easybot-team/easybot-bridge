package com.springwater.easybot.bridge;

import com.springwater.easybot.bridge.message.Segment;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.ServerInfo;
import java.util.List;

public interface BridgeBehavior {
    String runCommand(String playerName, String command, boolean enablePapi);
    String papiQuery(String playerName, String query);
    ServerInfo getInfo();
    void SyncToChat(String message);
    void BindSuccessBroadcast(String playerName,String accountId, String accountName);
    void KickPlayer(String player, String kickMessage);
    void SyncToChatExtra(List<Segment> segments, String text);
    boolean moduleIsInstalled(String moduleName);
    boolean moduleIsEnabled(String moduleName);
    boolean isAuthenticated(String playerName);
    List<PlayerInfo> getPlayerList();
}
