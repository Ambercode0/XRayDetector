package com.ambercode.manager;

import com.ambercode.data.Miner;
import com.ambercode.data.Tunnel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, Miner> minerMap = new HashMap<>();
    private final Map<Miner, List<Tunnel>> minerTunnelMap = new HashMap<>();

    public Map<UUID, Miner> getMinerMap() {
        return minerMap;
    }

    public Map<Miner, List<Tunnel>> getMinerTunnelMap() {
        return minerTunnelMap;
    }
}
