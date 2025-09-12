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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class PeriodicAnalyzer {

    private final XRayDetector xRayDetector;
    private final StandardConfig config;
    private BukkitTask task = null;
    private boolean running = false;

    public PeriodicAnalyzer(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.config = xRayDetector.getStandardConfig();
    }

    /**
     * Starts the periodic analysis process if it is not already running.
     * <p>
     * This method schedules a periodic task to perform analysis operations asynchronously.
     * The task is executed at intervals determined by the analysis interval value defined
     * in the configuration. If a task is already running or has been initiated previously,
     * the method will exit early without scheduling a new task.
     */
    public void start() {
        if (task != null) {
            return;
        }

        if (running) {
            return;
        }

        final long delay = config.getAnalysisInterval() * 60L * 20L;
        Runnable runnable = new PeriodicAnalysisTask(xRayDetector);
        task = xRayDetector.getServer().getScheduler().runTaskTimerAsynchronously(xRayDetector, runnable, delay, delay);
        running = true;
    }

    /**
     * Sets the running state of the periodic analyzer.
     *
     * @param running the new state of the running flag
     */
    private void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Checks if the periodic analysis process is currently running.
     *
     * @return true if the periodic analysis process is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the periodic analysis task if it is running.
     * <p>
     * This method checks if a scheduled task exists and is currently running. If so,
     * it cancels the task and sets the running state to false. If no task exists
     * or the task is not running, the method exits without making changes.
     */
    public void stop() {
        if (task == null) {
            return;
        }

        if (!running) {
            return;
        }

        if (!task.isCancelled()) {
            task.cancel();
            setRunning(false);
        }
    }
}
