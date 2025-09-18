/*
 *     XRayDetector - An advanced automatic detector to prevent X-Ray in your server
 *     Copyright (C) 2025 'AmberCode'
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ambercode;

import com.ambercode.command.XRayDetectorCommand;
import com.ambercode.config.StandardConfig;
import com.ambercode.listener.BlockListener;
import com.ambercode.listener.FlagListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class XRayDetector extends JavaPlugin {

    private StandardConfig standardConfig;

    @Override
    public void onEnable() {
        standardConfig = new StandardConfig(this);
        registerCommand("xraydetector", new XRayDetectorCommand(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new FlagListener(this), this);
    }

    @NotNull
    public StandardConfig getStandardConfig() {
        return standardConfig;
    }
}
