package ru.wishmine.ktrapleave.data;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrapData {
    private final Map<String, TrapConfig> traps = new HashMap<>();
    private final FileConfiguration config;

    public TrapData(FileConfiguration config) {
        this.config = config;

        if (config.contains("traps")) {
            for (String trapKey : config.getConfigurationSection("traps").getKeys(false)) {
                String path = "traps." + trapKey + ".";

                TrapConfig trapConfig = new TrapConfig(
                        config.getString(path + "schematic"),
                        config.getString(path + "name"),
                        config.getStringList(path + "lore"),
                        config.getString(path + "material", "CHORUS_PLANT"),
                        config.getInt(path + "cooldown", 30),
                        config.getInt(path + "time", 15),
                        config.getString(path + "sound-start"),
                        config.getString(path + "sound-stop"),
                        config.getString(path + "particle-start"),
                        config.getString(path + "particle-stop"),
                        config.getString(path + "particle-color-start", "#FF0000"),
                        config.getString(path + "particle-color-stop", "#00FF00"),
                        config.getStringList(path + "effects"),
                        config.getStringList(path + "region-flags"),
                        config.getInt(path + "region-padding", 3)
                );

                traps.put(trapKey, trapConfig);
            }
        }
        if (traps.isEmpty()) {
            traps.put("default", new TrapConfig(
                    "traps/default_trap.schem",
                    "&6Трапка",
                    Arrays.asList("&7Используйте для создания ловушки"),
                    "CHORUS_PLANT",
                    30,
                    15,
                    "BLOCK_ANVIL_PLACE",
                    "BLOCK_ANVIL_LAND",
                    "REDSTONE",
                    "REDSTONE",
                    "#FF0000",
                    "#00FF00",
                    Arrays.asList("SLOW:15:1"),
                    Arrays.asList("enderpearl:deny"),
                    3
            ));
        }
    }

    public TrapConfig getTrapConfig(String trapId) {
        return traps.get(trapId);
    }

    public Map<String, TrapConfig> getAllTraps() {
        return traps;
    }

    public int getParticleCount() {
        return config.getInt("particles.count", 20);
    }

    public double getParticleOffsetX() {
        return config.getDouble("particles.offset-x", 0.5);
    }

    public double getParticleOffsetY() {
        return config.getDouble("particles.offset-y", 0.5);
    }

    public double getParticleOffsetZ() {
        return config.getDouble("particles.offset-z", 0.5);
    }

    public double getParticleSpeed() {
        return config.getDouble("particles.speed", 0.1);
    }

    public double getParticleCircleRadius() {
        return config.getDouble("particles.circle-radius", 2.0);
    }

    public double getParticleSpawnRadius() {
        return config.getDouble("particles.spawn-radius", 3.0);
    }

    public int getParticleSpawnCount() {
        return config.getInt("particles.spawn-count", 50);
    }

    public double getParticleSpawnHeight() {
        return config.getDouble("particles.spawn-height", 2.0);
    }

    public static class TrapConfig {
        private final String schematic;
        private final String name;
        private final List<String> lore;
        private final String material;
        private final int cooldown;
        private final int time;
        private final String soundStart;
        private final String soundStop;
        private final String particleStart;
        private final String particleStop;
        private final String particleColorStart;
        private final String particleColorStop;
        private final List<String> effects;
        private final List<String> regionFlags;
        private final int regionPadding;

        public TrapConfig(String schematic, String name, List<String> lore, String material,
                          int cooldown, int time, String soundStart, String soundStop,
                          String particleStart, String particleStop, String particleColorStart,
                          String particleColorStop, List<String> effects, List<String> regionFlags,
                          int regionPadding) {
            this.schematic = schematic;
            this.name = name;
            this.lore = lore;
            this.material = material;
            this.cooldown = cooldown;
            this.time = time;
            this.soundStart = soundStart;
            this.soundStop = soundStop;
            this.particleStart = particleStart;
            this.particleStop = particleStop;
            this.particleColorStart = particleColorStart;
            this.particleColorStop = particleColorStop;
            this.effects = effects;
            this.regionFlags = regionFlags;
            this.regionPadding = regionPadding;
        }

        public String getSchematic() { return schematic; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public String getMaterial() { return material; }
        public int getCooldown() { return cooldown; }
        public int getTime() { return time; }
        public String getSoundStart() { return soundStart; }
        public String getSoundStop() { return soundStop; }
        public String getParticleStart() { return particleStart; }
        public String getParticleStop() { return particleStop; }
        public String getParticleColorStart() { return particleColorStart; }
        public String getParticleColorStop() { return particleColorStop; }
        public List<String> getEffects() { return effects; }
        public List<String> getRegionFlags() { return regionFlags; }
        public int getRegionPadding() { return regionPadding; }

        public Color getParticleColor(boolean isStart) {
            String hex = isStart ? particleColorStart : particleColorStop;
            try {
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                int rgb = Integer.parseInt(hex, 16);
                return Color.fromRGB(rgb);
            } catch (Exception e) {
                return isStart ? Color.RED : Color.GREEN;
            }
        }
    }
}