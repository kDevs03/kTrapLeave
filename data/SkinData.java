package ru.wishmine.ktrapleave.data;

import java.util.List;

public class SkinData {
   private final String id;
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
   private final List<String> effects;
   private final List<String> regionFlags;
   private final int regionPadding;

   public SkinData(String id, String schematic, String name, List<String> lore, String material,
                   int cooldown, int time, String soundStart, String soundStop,
                   String particleStart, String particleStop, List<String> effects,
                   List<String> regionFlags, int regionPadding) {
      this.id = id;
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
      this.effects = effects;
      this.regionFlags = regionFlags;
      this.regionPadding = regionPadding;
   }

   public String getId() {
      return this.id;
   }

   public String getSchematic() {
      return this.schematic;
   }

   public String getName() {
      return this.name;
   }

   public List<String> getLore() {
      return this.lore;
   }

   public String getMaterial() {
      return this.material;
   }

   public int getCooldown() {
      return this.cooldown;
   }

   public int getTime() {
      return this.time;
   }

   public String getSoundStart() {
      return this.soundStart;
   }

   public String getSoundStop() {
      return this.soundStop;
   }

   public String getParticleStart() {
      return this.particleStart;
   }

   public String getParticleStop() {
      return this.particleStop;
   }

   public List<String> getEffects() {
      return this.effects;
   }

   public List<String> getRegionFlags() {
      return this.regionFlags;
   }

   public int getRegionPadding() {
      return this.regionPadding;
   }
}