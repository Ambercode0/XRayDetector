package com.ambercode.database;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.TunnelPath;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class MySQLDatabase extends HikariPluginDatabase {

    public MySQLDatabase(@NotNull XRayDetector plugin) {
        super(plugin, DatabaseType.MYSQL);
    }


    @Override
    public void createTables() {

    }

    @Override
    public @NotNull List<Miner> getAllData() {
        return List.of();
    }

    @Override
    public void insertTunnelUnit(@NotNull TunnelUnit tunnelUnit, @NotNull TunnelPath tunnelPath) {

    }

    @Override
    public void insertTunnelPath(@NotNull TunnelPath tunnelPath, @NotNull TunnelStructure tunnelStructure) {

    }

    @Override
    public void insertTunnelStructure(@NotNull TunnelStructure tunnelStructure, @NotNull Miner miner) {

    }

    @Override
    public void insertMiner(@NotNull Miner miner) {

    }

    @Override
    public void mergeStructures(UUID minerUuid, TunnelStructure newMergedStructure, TunnelUnit newTunnelUnit, List<TunnelStructure> oldStructures) {

    }

}
