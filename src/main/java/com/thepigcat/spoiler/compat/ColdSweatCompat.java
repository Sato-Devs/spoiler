package com.thepigcat.spoiler.compat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ColdSweatCompat {
    public static double getTemperature(Level level, BlockPos pos) {
        return WorldHelper.getTemperatureAt(level, pos);
    }

    public static double getTempFahrenheit(double temp) {
        return Temperature.convert(temp, Temperature.Units.MC, Temperature.Units.F, true);
    }
}
