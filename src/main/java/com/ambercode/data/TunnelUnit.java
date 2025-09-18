package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable, compact representation of a mined block event.
 */
public final class TunnelUnit {
    public final int id; // compact id
    public final PackedBlockPos pos;
    public final int materialId; // plugin-specific material encoding (small int)
    public final long minedAt; // epoch millis
    public final UUID minerId; // player UUID, who mined it
    public volatile int veinId; // set after association (0 = none)
    public volatile int structureId; // 0 = none
    public final boolean isExposedToAir; // computed at insertion time when cheap

    public TunnelUnit(int id, @NotNull PackedBlockPos pos, int materialId, long minedAt, @NotNull UUID minerId, boolean exposed) {
        this.id = id;
        this.pos = pos;
        this.materialId = materialId;
        this.minedAt = minedAt;
        this.minerId = minerId;
        this.veinId = 0;
        this.structureId = 0;
        this.isExposedToAir = exposed;
    }

    public String toShortString() {
        return "TU#" + id + "@" + pos + " m=" + materialId + " t=" + minedAt + (isExposedToAir?" exposed":"");
    }
}
