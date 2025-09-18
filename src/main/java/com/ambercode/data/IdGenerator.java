package com.ambercode.data;

import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {

    public static final IdGenerator UNIT_ID_GEN = new IdGenerator();
    public static final IdGenerator VEIN_ID_GEN = new IdGenerator();
    public static final IdGenerator STRUCT_ID_GEN = new IdGenerator();

    private final AtomicInteger counter = new AtomicInteger(1);

    public int next() { return counter.getAndIncrement(); }
}
