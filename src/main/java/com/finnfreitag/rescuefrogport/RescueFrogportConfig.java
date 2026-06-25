package com.finnfreitag.rescuefrogport;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = RescueFrogport.MODID, bus = EventBusSubscriber.Bus.MOD)
public class RescueFrogportConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue CONGESTION_THRESHOLD = BUILDER
            .comment("Maximum number of packages allowed in a single chain conveyor network",
                     "before excess packages are rescued.",
                     "Default: 20")
            .defineInRange("congestionThreshold", 20, 1, 1000);

    private static final ModConfigSpec.BooleanValue CONGESTION_THRESHOLD_APPLIES_TO_ALL_PACKAGES = BUILDER
            .comment("Whether the congestionThreshold applies to all packages on a conveyor section (true),",
                     "or only to identical packages on that section (false).",
                     "Default: true")
            .define("congestionThresholdAppliesToAllPackages", true);

    private static final ModConfigSpec.IntValue SCAN_INTERVAL_TICKS = BUILDER
            .comment("How often (in game ticks) each rescue frogport scans its chain conveyor network.",
                     "20 ticks = 1 second. Lower values are more responsive but use more CPU.",
                     "Default: 20")
            .defineInRange("scanIntervalTicks", 20, 1, 200);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int congestionThreshold = 20;
    public static boolean congestionThresholdAppliesToAllPackages = true;
    public static int scanIntervalTicks = 20;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        congestionThreshold = CONGESTION_THRESHOLD.get();
        congestionThresholdAppliesToAllPackages = CONGESTION_THRESHOLD_APPLIES_TO_ALL_PACKAGES.get();
        scanIntervalTicks = SCAN_INTERVAL_TICKS.get();
    }
}
