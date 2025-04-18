package com.thepigcat.spoiler.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;

public final class LSOCompat {
    public static float getTemperature(Level level, BlockPos pos) {
        return TemperatureUtil.getWorldTemperature(level, pos);
    }
}
