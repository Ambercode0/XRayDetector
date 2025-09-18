package com.ambercode.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TunnelStructure {

    public final int id;
    public final UUID ownerId; // nullable
    private final List<Integer> unitIds = new ArrayList<>();
    public volatile int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    public volatile int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    public TunnelStructure(int id, UUID ownerId) { this.id = id; this.ownerId = ownerId; }

    public synchronized void addUnit(TunnelUnit u) {
        unitIds.add(u.id);
        int x = u.pos.getX(), y = u.pos.getY(), z = u.pos.getZ();
        minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
    }

    public synchronized int size() { return unitIds.size(); }

    public synchronized List<Integer> getUnitIds() { return new ArrayList<>(unitIds); }
}