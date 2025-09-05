package com.ambercode.data;

public class Tunnel {

    private final Structure structure;
    private final Miner ownerMiner;

    public Tunnel(Structure structure, Miner miner) {
        this.structure = structure;
        this.ownerMiner = miner;
    }

    public Structure getStructure() {
        return structure;
    }

    public Miner getOwnerMiner() {
        return ownerMiner;
    }
}
