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

package com.ambercode.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ErrorComputeReturnCode {
    IS_EXPOSED_VEIN_SHORT_TUNNEL(-6, "The tunnel is an exposed vein"),
    NOT_ENOUGH_DATA(-5, "Not enough data to compute"),
    NO_ORES(-2, "No ores found in tunnel"),
    ONLY_ONE_ORE(-4, "Only one ore found in tunnel"),
    NO_DIAMONDS(-3, "No diamonds found in tunnel"),
    TUNNEL_TOO_SMALL(-1, "Tunnel is too small");

    public final int errorNumber;
    public final String displayText;

    ErrorComputeReturnCode(int errorNumber, @NotNull String displayText) {
        this.errorNumber = errorNumber;
        this.displayText = displayText;
    }

    @Nullable
    public static ErrorComputeReturnCode getErrorByNumber(int number) {
        for (ErrorComputeReturnCode errorCode : ErrorComputeReturnCode.values()) {
            if (errorCode.errorNumber == number) {
                return errorCode;
            }
        }
        return null;
    }
}
