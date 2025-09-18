package com.ambercode.data;

import java.util.Objects;

public final class ChunkKey {
    public final int cx;
    public final int cz;

    private ChunkKey(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
    }

    public static ChunkKey of(int cx, int cz) {
        return new ChunkKey(cx, cz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkKey k)) return false;
        return cx == k.cx && cz == k.cz;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cx, cz);
    }

    @Override
    public String toString() {
        return "ChunkKey(" + cx + "," + cz + ")";
    }
}