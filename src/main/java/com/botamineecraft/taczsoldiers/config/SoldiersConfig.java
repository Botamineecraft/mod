package com.botamineecraft.taczsoldiers.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public final class SoldiersConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_POOL;
    public static final ForgeConfigSpec.BooleanValue INFINITE_AMMO;
    public static final ForgeConfigSpec.IntValue AMMO_RESERVE;
    public static final ForgeConfigSpec.DoubleValue ATTACK_RANGE;
    public static final ForgeConfigSpec.DoubleValue SOLDIER_MAX_HEALTH;
    public static final ForgeConfigSpec.BooleanValue TARGET_CREEPERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("TACZ Soldiers configuration").push("soldiers");

        GUN_POOL = builder
                .comment(
                        "Gun ids (TACZ gun pack ids) that spawned soldiers can receive.",
                        "Invalid ids are ignored at runtime. Example: tacz:ak47")
                .defineListAllowEmpty("gun_pool",
                        () -> Arrays.asList(
                                "tacz:ak47",
                                "tacz:m4a1",
                                "tacz:m16a4",
                                "tacz:hk_mp5a5",
                                "tacz:ump45",
                                "tacz:uzi",
                                "tacz:scar_h",
                                "tacz:glock_17"),
                        () -> "tacz:ak47");

        INFINITE_AMMO = builder
                .comment("When true, soldiers never run out of ammo (their TACZ dummy-ammo reserve is refilled automatically).")
                .define("infinite_ammo", true);

        AMMO_RESERVE = builder
                .comment("Size of the dummy-ammo reserve given to each soldier's gun (used for reloading).")
                .defineInRange("ammo_reserve", 2000, 1, 1000000);

        ATTACK_RANGE = builder
                .comment("Maximum distance (in blocks) at which soldiers engage targets with their guns.")
                .defineInRange("attack_range", 24.0D, 4.0D, 128.0D);

        SOLDIER_MAX_HEALTH = builder
                .comment("Max health of a soldier.")
                .defineInRange("max_health", 30.0D, 1.0D, 1000.0D);

        TARGET_CREEPERS = builder
                .comment("Whether soldiers should actively target creepers (bullets may make them explode).")
                .define("target_creepers", false);

        builder.pop();
        SPEC = builder.build();
    }

    private SoldiersConfig() {
    }
}
