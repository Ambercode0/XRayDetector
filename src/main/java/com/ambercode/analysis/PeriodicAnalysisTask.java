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

package com.ambercode.analysis;

import com.ambercode.XRayDetector;
import com.ambercode.config.StandardConfig;
import com.ambercode.data.Miner;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

final class PeriodicAnalysisTask implements Runnable {

    private final XRayDetector xRayDetector;
    private final StandardConfig conf;

    PeriodicAnalysisTask(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.conf = xRayDetector.getStandardConfig();
    }

    @Override
    public void run() {

    }

    private void warnAdminsOfSuspect(@NotNull Miner miner) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("xraydetector.admin"))
                .forEach(p -> {});
    }

    private void applyPunishment() {

    }
}
