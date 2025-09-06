package com.ambercode.data;

import com.ambercode.utils.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class Path {

    public Path() {}

    public Path(Unit unit) {
        units.add(unit);
    }

    public Path(Block block) {
        units.add(new Unit(BlockFace.SELF, block.getLocation(), Utils.isOre(block), System.currentTimeMillis()));
    }

    private final List<Unit> units = new ArrayList<>();

    public List<Unit> getUnits() {
        return units;
    }
}