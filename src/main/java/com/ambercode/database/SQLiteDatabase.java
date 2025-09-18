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

package com.ambercode.database;

import com.ambercode.XRayDetector;
import com.ambercode.data.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public final class SQLiteDatabase extends CredentialPluginDatabase {

    public SQLiteDatabase(@NotNull XRayDetector plugin) {
        super(plugin, DatabaseType.SQLITE);
    }

    @Override
    public void createTables() {

    }
}

