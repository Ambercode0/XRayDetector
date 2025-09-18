package com.ambercode.data;

/**
 * PackedBlockPos packs x,y,z into a single long for compact map keys.
 * Layout (64-bit): [unused:8][x:20][y:20][z:20] -- supports coordinate ranges
 * of roughly +/- 524k, sufficient for most servers. Adjust if you need more.
 */
public final class PackedBlockPos {
    private static final int BITS = 20;
    private static final int SHIFT_Z = 0;
    private static final int SHIFT_Y = BITS;
    private static final int SHIFT_X = BITS * 2;
    private static final long MASK = (1L << BITS) - 1L;
    // Derived constants for clarity and safety
    private static final int MASK_INT = (1 << BITS) - 1;
    private static final int SIGN_BIT_MASK_INT = 1 << (BITS - 1);
    public static final int MIN_COORD = -(1 << (BITS - 1));
    public static final int MAX_COORD = (1 << (BITS - 1)) - 1;

    public final long packed;

    private PackedBlockPos(long packed) {
        this.packed = packed;
    }

    public static PackedBlockPos of(int x, int y, int z) {
        validateRange(x, y, z);
        long p = pack(x, y, z);
        return new PackedBlockPos(p);
    }

    // Centralize packing logic
    private static long pack(int x, int y, int z) {
        long xMasked = ((long) (x & MASK_INT)) << SHIFT_X;
        long yMasked = ((long) (y & MASK_INT)) << SHIFT_Y;
        long zMasked = ((long) (z & MASK_INT)) << SHIFT_Z;
        return xMasked | yMasked | zMasked;
    }

    // Validate coordinates are representable in BITS bits (two's complement)
    private static void validateRange(int x, int y, int z) {
        if (x < MIN_COORD || x > MAX_COORD ||
                y < MIN_COORD || y > MAX_COORD ||
                z < MIN_COORD || z > MAX_COORD) {
            throw new IllegalArgumentException(
                    "Coordinates out of range [" + MIN_COORD + "," + MAX_COORD + "]: x=" + x + ", y=" + y + ", z=" + z
            );
        }
    }

    // Bitwise sign-extend for arbitrary bit-width
    private static int signExtend(int v, int bits) {
        int shift = Integer.SIZE - bits;
        return (v << shift) >> shift;
    }

    // Helper to extract and sign-extend a coordinate
    private static int unpack(long value, int shift) {
        int v = (int) ((value >> shift) & MASK);
        return signExtend(v, BITS);
    }

    public int getX() {
        return unpack(packed, SHIFT_X);
    }

    public int getY() {
        return unpack(packed, SHIFT_Y);
    }

    public int getZ() {
        return unpack(packed, SHIFT_Z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackedBlockPos other)) return false;
        return packed == other.packed;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(packed);
    }

    @Override
    public String toString() {
        return "PackedBlockPos[x=" + getX() + ", y=" + getY() + ", z=" + getZ() + "]";
    }
}
