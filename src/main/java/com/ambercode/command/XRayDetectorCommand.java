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

package com.ambercode.command;

import com.ambercode.XRayDetector;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class XRayDetectorCommand implements BasicCommand {

    private final XRayDetector xRayDetector;

    public XRayDetectorCommand(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender instanceof Player;
    }

    @Override
    public @Nullable String permission() {
        return "xraydetector.admin";
    }
}
