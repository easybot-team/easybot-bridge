package org.easybot.bridge;

import org.easybot.bridge.message.Segment;
import org.easybot.bridge.model.PlayerInfo;
import org.easybot.bridge.model.ServerInfo;
import java.util.List;

public interface BridgeBehavior {
    String runCommand(String playerName, String command, boolean enablePapi);
    String papiQuery(String playerName, String query);
    ServerInfo getInfo();
    void SyncToChat(String message);
    void BindSuccessBroadcast(String playerName,String accountId, String accountName);
    void KickPlayer(String plauer, String kickMessage);
    void SyncToChatExtra(List<Segment> segments, String text);
    List<PlayerInfo> getPlayerList();
}
