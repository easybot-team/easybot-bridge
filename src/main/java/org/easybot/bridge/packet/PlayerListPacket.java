package org.easybot.bridge.packet;

import com.google.gson.annotations.SerializedName;
import org.easybot.bridge.model.PlayerInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlayerListPacket {
    @SerializedName("list")
    private List<PlayerInfo> list;
}
