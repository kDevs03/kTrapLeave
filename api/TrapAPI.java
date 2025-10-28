package ru.wishmine.ktrapleave.api;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.data.TrapData;
import ru.wishmine.ktrapleave.utils.MessageManager;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrapAPI {
   private KTrapLeave plugin;
   private MessageManager messageManager;
   private TrapData trapData;
   private Map<Location, List<BlockState>> resetMap = new HashMap<>();

   private Map<String, PlayerTrapState> playersInTraps = new ConcurrentHashMap<>();

   private static class PlayerTrapState {
      public Player player;
      public ProtectedRegion region;
      public TrapData.TrapConfig trapConfig;
      public Boolean originalFly;
      public Boolean originalGod;
      public int taskId;
      public List<BlockState> originalBlocks;
      public Location pasteLocation;
      public Clipboard clipboard;
      public BlockVector3 minPoint;
      public BlockVector3 maxPoint;

      public PlayerTrapState(Player player, ProtectedRegion region, TrapData.TrapConfig trapConfig,
                             Location pasteLocation, Clipboard clipboard) {
         this.player = player;
         this.region = region;
         this.trapConfig = trapConfig;
         this.pasteLocation = pasteLocation;
         this.clipboard = clipboard;
         this.originalBlocks = new ArrayList<>();
         this.minPoint = clipboard.getMinimumPoint();
         this.maxPoint = clipboard.getMaximumPoint();
      }
   }

   public TrapAPI(KTrapLeave plugin, MessageManager messageManager, TrapData trapData) {
      this.plugin = plugin;
      this.messageManager = messageManager;
      this.trapData = trapData;
   }

   public boolean createTrap(Player player, String trapId) {
      TrapData.TrapConfig trapConfig = trapData.getTrapConfig(trapId);
      if (trapConfig == null) {
         messageManager.sendMessage(player, "trap-not-found");
         return false;
      }

      final RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
      if (regionManager == null) {
         this.plugin.getLogger().warning("RegionManager is null!");
         return false;
      }

      String schematicPath = trapConfig.getSchematic();
      File schematicFile;

      if (schematicPath.contains("/")) {
         schematicFile = new File(plugin.getDataFolder(), schematicPath);
      } else {
         schematicFile = new File(plugin.getTrapsFolder(), schematicPath);
      }

      if (!schematicFile.exists()) {
         schematicFile = new File(plugin.getTrapsFolder(), schematicPath);
         if (!schematicFile.exists()) {
            plugin.getLogger().warning("Схема не найдена: " + schematicPath);
            messageManager.sendMessage(player, "schematic-not-found");
            return false;
         }
      }

      if (hasNearbyTrapRegions(player, trapConfig, player.getLocation().clone())) {
         messageManager.sendMessage(player, "trap-too-close");
         return false;
      }

      try (FileInputStream fis = new FileInputStream(schematicFile);
           ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(fis)) {

         final Clipboard clipboard = reader.read();
         if (clipboard == null) {
            plugin.getLogger().warning("Не удалось загрузить схему: " + schematicPath);
            messageManager.sendMessage(player, "schematic-not-found");
            return false;
         }

         final Location pasteLocation = player.getLocation().clone();

         if (hasNearbyTrapRegions(player, trapConfig, pasteLocation, clipboard)) {
            messageManager.sendMessage(player, "trap-too-close");
            return false;
         }

         this.saveBlockStates(pasteLocation, plugin.getConfig().getInt("settings.save-radius", 5));

         final PlayerTrapState trapState = new PlayerTrapState(player, null, trapConfig, pasteLocation, clipboard);

         saveOriginalBlocks(trapState);

         clearSchematicAreaSafely(trapState);

         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            try {
               pasteSchematic(trapState);

               ProtectedRegion region = createTrapRegion(player, trapConfig, pasteLocation, clipboard);
               regionManager.addRegion(region);
               trapState.region = region;

               if (plugin.getConfig().getBoolean("settings.enable-particles", true)) {
                  spawnSchematicParticles(pasteLocation, trapConfig, true);
               }

               savePlayerStates(trapState);
               playersInTraps.put(player.getName(), trapState);

               playTrapEffects(player, trapConfig, true);
               applyTrapEffects(player, trapConfig);

               startTrapMonitoring(trapState);

               messageManager.sendMessageWithReplacements(player, "trap-create-success");
               messageManager.sendMessageWithReplacements(player, "trap-active",
                       "%time%", String.valueOf(trapConfig.getTime())
               );

               final int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                  removeTrap(trapState);
               }, trapConfig.getTime() * 20L).getTaskId();

               trapState.taskId = taskId;

            } catch (Exception e) {
               plugin.getLogger().warning("Ошибка при установке схемы: " + e.getMessage());
               e.printStackTrace();
               messageManager.sendMessage(player, "save-error");
               restoreOriginalBlocksSafely(trapState);
            }
         }, 2L);

         return true;

      } catch (Exception e) {
         plugin.getLogger().warning("Ошибка при создании трапки: " + e.getMessage());
         e.printStackTrace();
         messageManager.sendMessage(player, "save-error");
         return false;
      }
   }

   private boolean hasNearbyTrapRegions(Player player, TrapData.TrapConfig trapConfig, Location pasteLocation) {
      RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
      if (regionManager == null) return false;

      int padding = trapConfig.getRegionPadding();

      int radius = 5;

      int minX = pasteLocation.getBlockX() - radius - padding;
      int minY = pasteLocation.getBlockY() - 1 - padding;
      int minZ = pasteLocation.getBlockZ() - radius - padding;

      int maxX = pasteLocation.getBlockX() + radius + padding;
      int maxY = pasteLocation.getBlockY() + 5 + padding;
      int maxZ = pasteLocation.getBlockZ() + radius + padding;

      BlockVector3 testMin = BlockVector3.at(minX, minY, minZ);
      BlockVector3 testMax = BlockVector3.at(maxX, maxY, maxZ);

      for (ProtectedRegion existingRegion : regionManager.getRegions().values()) {
         if (existingRegion.getId().startsWith("trap_")) {
            if (regionsIntersect(existingRegion, testMin, testMax)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean hasNearbyTrapRegions(Player player, TrapData.TrapConfig trapConfig, Location pasteLocation, Clipboard clipboard) {
      RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
      if (regionManager == null) return false;

      int padding = trapConfig.getRegionPadding();
      BlockVector3 minPoint = clipboard.getMinimumPoint();
      BlockVector3 maxPoint = clipboard.getMaximumPoint();

      int minX = pasteLocation.getBlockX() + minPoint.getBlockX() - padding;
      int minY = pasteLocation.getBlockY() + minPoint.getBlockY() - 1;
      int minZ = pasteLocation.getBlockZ() + minPoint.getBlockZ() - padding;

      int maxX = pasteLocation.getBlockX() + maxPoint.getBlockX() + padding;
      int maxY = pasteLocation.getBlockY() + maxPoint.getBlockY() + 1;
      int maxZ = pasteLocation.getBlockZ() + maxPoint.getBlockZ() + padding;

      BlockVector3 testMin = BlockVector3.at(minX, minY, minZ);
      BlockVector3 testMax = BlockVector3.at(maxX, maxY, maxZ);

      for (ProtectedRegion existingRegion : regionManager.getRegions().values()) {
         if (existingRegion.getId().startsWith("trap_")) {
            if (regionsIntersect(existingRegion, testMin, testMax)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean regionsIntersect(ProtectedRegion region, BlockVector3 min, BlockVector3 max) {
      BlockVector3 regionMin = region.getMinimumPoint();
      BlockVector3 regionMax = region.getMaximumPoint();

      return (min.getX() <= regionMax.getX() && max.getX() >= regionMin.getX()) &&
              (min.getY() <= regionMax.getY() && max.getY() >= regionMin.getY()) &&
              (min.getZ() <= regionMax.getZ() && max.getZ() >= regionMin.getZ());
   }

   private void saveOriginalBlocks(PlayerTrapState trapState) {
      World world = trapState.pasteLocation.getWorld();

      int minX = trapState.pasteLocation.getBlockX() + trapState.minPoint.getBlockX();
      int minY = trapState.pasteLocation.getBlockY() + trapState.minPoint.getBlockY();
      int minZ = trapState.pasteLocation.getBlockZ() + trapState.minPoint.getBlockZ();

      int maxX = trapState.pasteLocation.getBlockX() + trapState.maxPoint.getBlockX();
      int maxY = trapState.pasteLocation.getBlockY() + trapState.maxPoint.getBlockY();
      int maxZ = trapState.pasteLocation.getBlockZ() + trapState.maxPoint.getBlockZ();

      plugin.getLogger().info("Сохраняем блоки в области: " + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ);

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               Location blockLoc = new Location(world, x, y, z);
               Block block = blockLoc.getBlock();

               BlockState blockState = block.getState();
               trapState.originalBlocks.add(blockState);

               if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR && block.getType() != Material.VOID_AIR) {
                  world.playEffect(blockLoc, Effect.SMOKE, 10);
                  world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 0.3f, 1.0f);
               }
            }
         }
      }

      plugin.getLogger().info("Сохранено " + trapState.originalBlocks.size() + " блоков в области трапки");
   }

   private void clearSchematicAreaSafely(PlayerTrapState trapState) {
      World world = trapState.pasteLocation.getWorld();

      int minX = trapState.pasteLocation.getBlockX() + trapState.minPoint.getBlockX();
      int minY = trapState.pasteLocation.getBlockY() + trapState.minPoint.getBlockY();
      int minZ = trapState.pasteLocation.getBlockZ() + trapState.minPoint.getBlockZ();

      int maxX = trapState.pasteLocation.getBlockX() + trapState.maxPoint.getBlockX();
      int maxY = trapState.pasteLocation.getBlockY() + trapState.maxPoint.getBlockY();
      int maxZ = trapState.pasteLocation.getBlockZ() + trapState.maxPoint.getBlockZ();

      plugin.getLogger().info("Безопасная очистка области схемы");

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               Location blockLoc = new Location(world, x, y, z);
               Block block = blockLoc.getBlock();

               if (block.getState() instanceof org.bukkit.block.Container) {
                  org.bukkit.block.Container container = (org.bukkit.block.Container) block.getState();
                  container.getInventory().clear();
               }

               block.setType(Material.AIR, false);
            }
         }
      }
   }

   private void pasteSchematic(PlayerTrapState trapState) throws Exception {
      plugin.getLogger().info("Устанавливаем схему...");

      try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(trapState.player.getWorld()))) {
         editSession.setFastMode(false);

         Operations.complete(new ClipboardHolder(trapState.clipboard)
                 .createPaste(editSession)
                 .to(BlockVector3.at(
                         trapState.pasteLocation.getBlockX(),
                         trapState.pasteLocation.getBlockY(),
                         trapState.pasteLocation.getBlockZ()
                 ))
                 .ignoreAirBlocks(false)
                 .build());

         editSession.flushSession();
      }

      plugin.getLogger().info("Схема установлена успешно");
   }

   private void restoreOriginalBlocksSafely(PlayerTrapState trapState) {
      if (trapState.originalBlocks.isEmpty()) {
         plugin.getLogger().warning("Нет сохраненных блоков для восстановления");
         return;
      }

      final World world = trapState.pasteLocation.getWorld();

      plugin.getLogger().info("Безопасное восстановление " + trapState.originalBlocks.size() + " блоков...");

      clearSchematicAreaSafely(trapState);

      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         int restoredCount = 0;

         for (BlockState blockState : trapState.originalBlocks) {
            try {
               Location blockLoc = blockState.getLocation();

               Block block = blockLoc.getBlock();
               block.setType(Material.AIR, false);

               this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                  try {
                     blockState.update(true, false);

                     if (blockState.getType() != Material.AIR) {
                        world.playEffect(blockLoc, Effect.ENDEREYE_LAUNCH, 5);
                        world.playSound(blockLoc, Sound.BLOCK_STONE_PLACE, 0.3f, 1.0f);
                     }

                  } catch (Exception e) {
                     plugin.getLogger().warning("Ошибка при восстановлении блока: " + e.getMessage());
                  }
               }, 1L);

               restoredCount++;

            } catch (Exception e) {
               plugin.getLogger().warning("Ошибка при подготовке восстановления блока: " + e.getMessage());
            }
         }

         int finalRestoredCount = restoredCount;
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            trapState.originalBlocks.clear();
            plugin.getLogger().info("Восстановлено " + finalRestoredCount + " оригинальных блоков после трапки");
         }, 10L);

      }, 1L);
   }

   private void restoreOriginalBlocksSimple(PlayerTrapState trapState) {
      if (trapState.originalBlocks.isEmpty()) {
         return;
      }

      final World world = trapState.pasteLocation.getWorld();
      int restoredCount = 0;

      plugin.getLogger().info("Простое восстановление " + trapState.originalBlocks.size() + " блоков...");

      for (BlockState blockState : trapState.originalBlocks) {
         try {
            Location blockLoc = blockState.getLocation();
            Block block = blockLoc.getBlock();

            if (blockState instanceof org.bukkit.block.Container) {
               block.setType(Material.AIR, false);

               this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                  try {
                     blockState.update(true, false);
                  } catch (Exception e) {
                     plugin.getLogger().warning("Ошибка при восстановлении контейнера: " + e.getMessage());
                  }
               }, 1L);
            } else {
               blockState.update(true, false);
            }

            restoredCount++;

         } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при восстановлении блока: " + e.getMessage());
            try {
               Block block = blockState.getLocation().getBlock();
               block.setType(blockState.getType());
               if (blockState.getBlockData() != null) {
                  block.setBlockData(blockState.getBlockData());
               }
            } catch (Exception e2) {
               plugin.getLogger().warning("Критическая ошибка при восстановлении блока: " + e2.getMessage());
            }
         }
      }

      trapState.originalBlocks.clear();
      plugin.getLogger().info("Восстановлено " + restoredCount + " блоков простым методом");
   }

   private ProtectedRegion createTrapRegion(Player player, TrapData.TrapConfig trapConfig, Location pasteLocation, Clipboard clipboard) {
      int padding = trapConfig.getRegionPadding();

      BlockVector3 minPoint = clipboard.getMinimumPoint();
      BlockVector3 maxPoint = clipboard.getMaximumPoint();

      int minX = pasteLocation.getBlockX() + minPoint.getBlockX() - padding;
      int minY = pasteLocation.getBlockY() + minPoint.getBlockY() - 1;
      int minZ = pasteLocation.getBlockZ() + minPoint.getBlockZ() - padding;

      int maxX = pasteLocation.getBlockX() + maxPoint.getBlockX() + padding;
      int maxY = pasteLocation.getBlockY() + maxPoint.getBlockY() + 1;
      int maxZ = pasteLocation.getBlockZ() + maxPoint.getBlockZ() + padding;

      ProtectedRegion region = new ProtectedCuboidRegion(
              "trap_" + System.currentTimeMillis() + "_" + player.getName(),
              BlockVector3.at(minX, minY, minZ),
              BlockVector3.at(maxX, maxY, maxZ)
      );

      applyRegionFlags(region, trapConfig.getRegionFlags());
      return region;
   }

   private void spawnSchematicParticles(Location center, TrapData.TrapConfig trapConfig, boolean isStart) {
      try {
         String particleName = isStart ? trapConfig.getParticleStart() : trapConfig.getParticleStop();
         if (particleName == null || particleName.isEmpty()) {
            return;
         }

         Particle particle = Particle.valueOf(particleName);
         World world = center.getWorld();

         int count = trapData.getParticleSpawnCount();
         double radius = trapData.getParticleSpawnRadius();
         double height = trapData.getParticleSpawnHeight();

         if (particle == Particle.REDSTONE) {
            Color color = trapConfig.getParticleColor(isStart);
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1);

            for (int i = 0; i < count; i++) {
               double theta = 2 * Math.PI * Math.random();
               double phi = Math.acos(2 * Math.random() - 1);
               double x = radius * Math.sin(phi) * Math.cos(theta);
               double y = height * Math.random();
               double z = radius * Math.sin(phi) * Math.sin(theta);

               Location particleLoc = center.clone().add(x, y, z);
               world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0, dustOptions);
            }
         } else {
            for (int i = 0; i < count; i++) {
               double angle = 2 * Math.PI * i / count;
               double x = Math.cos(angle) * radius;
               double z = Math.sin(angle) * radius;
               double y = height * Math.random();

               Location particleLoc = center.clone().add(x, y, z);
               world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
            }
         }
      } catch (Exception e) {
         plugin.getLogger().warning("Ошибка при создании партиклов схемы: " + e.getMessage());
      }
   }

   private void savePlayerStates(PlayerTrapState trapState) {
      Player player = trapState.player;

      if (plugin.getConfig().getBoolean("settings.disable-flight", true)) {
         trapState.originalFly = player.getAllowFlight();
         player.setAllowFlight(false);
         if (player.isFlying()) {
            player.setFlying(false);
         }
      }

      if (plugin.getConfig().getBoolean("settings.disable-god", true)) {
         trapState.originalGod = player.isInvulnerable();
         player.setInvulnerable(false);
      }
   }

   private void restorePlayerStates(PlayerTrapState trapState) {
      Player player = trapState.player;

      if (plugin.getConfig().getBoolean("settings.disable-flight", true) && trapState.originalFly != null) {
         player.setAllowFlight(trapState.originalFly);
      }

      if (plugin.getConfig().getBoolean("settings.disable-god", true) && trapState.originalGod != null) {
         player.setInvulnerable(trapState.originalGod);
      }
   }

   private void removeTrap(PlayerTrapState trapState) {
      final Player player = trapState.player;

      try {
         RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
         if (regionManager != null && trapState.region != null) {
            regionManager.removeRegion(trapState.region.getId());
         }
      } catch (Exception e) {
         plugin.getLogger().warning("Ошибка при удалении региона: " + e.getMessage());
      }

      restoreOriginalBlocksSimple(trapState);

      this.restoreBlockStates(trapState.pasteLocation);

      if (plugin.getConfig().getBoolean("settings.enable-particles", true)) {
         spawnSchematicParticles(trapState.pasteLocation, trapState.trapConfig, false);
      }

      restorePlayerStates(trapState);
      playersInTraps.remove(player.getName());

      playTrapEffects(player, trapState.trapConfig, false);
      messageManager.sendMessage(player, "trap-expired");
   }

   private void startTrapMonitoring(final PlayerTrapState trapState) {
      final Player player = trapState.player;
      final ProtectedRegion region = trapState.region;

      new java.util.Timer().schedule(new java.util.TimerTask() {
         int secondsPassed = 0;

         @Override
         public void run() {
            if (!playersInTraps.containsKey(player.getName()) || secondsPassed >= trapState.trapConfig.getTime()) {
               this.cancel();
               return;
            }

            if (region.contains(BukkitAdapter.asBlockVector(player.getLocation()))) {
               if (plugin.getConfig().getBoolean("settings.disable-flight", true) && player.getAllowFlight()) {
                  player.setAllowFlight(false);
                  player.setFlying(false);
               }
               if (plugin.getConfig().getBoolean("settings.disable-god", true) && player.isInvulnerable()) {
                  player.setInvulnerable(false);
               }

               for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                  if (!nearbyPlayer.getName().equals(player.getName()) &&
                          region.contains(BukkitAdapter.asBlockVector(nearbyPlayer.getLocation()))) {

                     if (plugin.getConfig().getBoolean("settings.disable-flight", true) && nearbyPlayer.getAllowFlight()) {
                        nearbyPlayer.setAllowFlight(false);
                        nearbyPlayer.setFlying(false);
                     }
                     if (plugin.getConfig().getBoolean("settings.disable-god", true) && nearbyPlayer.isInvulnerable()) {
                        nearbyPlayer.setInvulnerable(false);
                     }
                  }
               }
            }

            secondsPassed++;
         }
      }, 0, 1000);
   }

   private void applyRegionFlags(ProtectedRegion region, List<String> flags) {
      Map<String, Flag<?>> allFlags = getAllWorldGuardFlags();

      region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
      region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
      region.setFlag(Flags.BUILD, StateFlag.State.DENY);

      for (String flagConfig : flags) {
         try {
            String[] parts = flagConfig.split(":");
            String flagName = parts[0].trim();
            String flagValue = parts.length > 1 ? parts[1].trim() : "deny";

            Flag<?> flag = allFlags.get(flagName.toLowerCase());
            if (flag != null) {
               setFlagValue(region, flag, flagValue);
            }
         } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при установке флага " + flagConfig + ": " + e.getMessage());
         }
      }
   }

   private Map<String, Flag<?>> getAllWorldGuardFlags() {
      Map<String, Flag<?>> flags = new HashMap<>();
      try {
         Field[] fields = Flags.class.getDeclaredFields();
         for (Field field : fields) {
            if (Flag.class.isAssignableFrom(field.getType())) {
               Flag<?> flag = (Flag<?>) field.get(null);
               flags.put(field.getName().toLowerCase(), flag);
               flags.put(flag.getName().toLowerCase(), flag);
            }
         }
      } catch (Exception e) {
         plugin.getLogger().warning("Ошибка при получении флагов WorldGuard: " + e.getMessage());
      }
      return flags;
   }

   @SuppressWarnings("unchecked")
   private <T> void setFlagValue(ProtectedRegion region, Flag<T> flag, String value) {
      try {
         if (flag instanceof StateFlag) {
            StateFlag stateFlag = (StateFlag) flag;
            if (value.equalsIgnoreCase("allow") || value.equalsIgnoreCase("true")) {
               region.setFlag(stateFlag, StateFlag.State.ALLOW);
            } else {
               region.setFlag(stateFlag, StateFlag.State.DENY);
            }
         }
      } catch (Exception e) {
         plugin.getLogger().warning("Не удалось установить значение " + value + " для флага " + flag.getName());
      }
   }

   private void playTrapEffects(Player player, TrapData.TrapConfig trapConfig, boolean isStart) {
      if (!plugin.getConfig().getBoolean("settings.enable-sounds", true) &&
              !plugin.getConfig().getBoolean("settings.enable-particles", true)) {
         return;
      }

      String soundName = isStart ? trapConfig.getSoundStart() : trapConfig.getSoundStop();
      String particleName = isStart ? trapConfig.getParticleStart() : trapConfig.getParticleStop();

      try {
         if (plugin.getConfig().getBoolean("settings.enable-sounds", true) && soundName != null && !soundName.isEmpty()) {
            try {
               Sound sound = Sound.valueOf(soundName);
               player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
               plugin.getLogger().warning("Неверный звук: " + soundName);
            }
         }

         if (plugin.getConfig().getBoolean("settings.enable-particles", true) && particleName != null && !particleName.isEmpty()) {
            try {
               Particle particle = Particle.valueOf(particleName);
               Location loc = player.getLocation();

               int count = trapData.getParticleCount();
               double offsetX = trapData.getParticleOffsetX();
               double offsetY = trapData.getParticleOffsetY();
               double offsetZ = trapData.getParticleOffsetZ();
               double speed = trapData.getParticleSpeed();
               double radius = trapData.getParticleCircleRadius();

               if (particle == Particle.REDSTONE) {
                  Color color = trapConfig.getParticleColor(isStart);
                  Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1);

                  for (int i = 0; i < count; i++) {
                     double angle = 2 * Math.PI * i / count;
                     double x = Math.cos(angle) * radius;
                     double z = Math.sin(angle) * radius;

                     player.getWorld().spawnParticle(
                             particle,
                             loc.getX() + x, loc.getY() + 1, loc.getZ() + z,
                             1, 0, 0, 0, 0, dustOptions
                     );
                  }
               } else {
                  player.getWorld().spawnParticle(
                          particle,
                          loc.getX(), loc.getY() + 1, loc.getZ(),
                          count, offsetX, offsetY, offsetZ, speed
                  );
               }
            } catch (IllegalArgumentException e) {
               plugin.getLogger().warning("Неверная частица: " + particleName);
            }
         }
      } catch (Exception e) {
         plugin.getLogger().warning("Ошибка при создании эффектов: " + e.getMessage());
      }
   }

   private void applyTrapEffects(Player player, TrapData.TrapConfig trapConfig) {
      if (!plugin.getConfig().getBoolean("settings.enable-effects", true)) {
         return;
      }

      for (String effectString : trapConfig.getEffects()) {
         try {
            String[] parts = effectString.split(":");
            if (parts.length >= 3) {
               String effectName = parts[0].toUpperCase();
               int duration = Integer.parseInt(parts[1]) * 20;
               int amplifier = Integer.parseInt(parts[2]) - 1;

               PotionEffectType effectType = PotionEffectType.getByName(effectName);
               if (effectType != null) {
                  player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, true));
               }
            }
         } catch (Exception e) {
            plugin.getLogger().warning("Неверный формат эффекта: " + effectString);
         }
      }
   }

   private void saveBlockStates(Location location, int radius) {
      List<BlockState> trapResetList = new ArrayList<>();
      for(int x = -radius; x <= radius; ++x) {
         for(int y = -radius; y <= radius; ++y) {
            for(int z = -radius; z <= radius; ++z) {
               Location loc = location.clone().add(x, y, z);
               BlockState state = loc.getBlock().getState();
               trapResetList.add(state);
            }
         }
      }
      this.resetMap.put(location, trapResetList);
   }

   private void restoreBlockStates(Location location) {
      List<BlockState> trapResetList = this.resetMap.get(location);
      if (trapResetList != null) {
         for (BlockState block : trapResetList) {
            try {
               block.update(true, false);
            } catch (Exception e) {
               plugin.getLogger().warning("Ошибка при восстановлении блока вокруг: " + e.getMessage());
            }
         }
         this.resetMap.remove(location);
      }
   }

   public void onDisable() {
      for (PlayerTrapState trapState : playersInTraps.values()) {
         try {
            if (trapState.taskId != 0) {
               plugin.getServer().getScheduler().cancelTask(trapState.taskId);
            }

            restoreOriginalBlocksSimple(trapState);

            restorePlayerStates(trapState);

            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(trapState.player.getWorld()));
            if (regionManager != null && trapState.region != null) {
               regionManager.removeRegion(trapState.region.getId());
            }
         } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при восстановлении состояния игрока " + trapState.player.getName() + ": " + e.getMessage());
         }
      }
      playersInTraps.clear();
   }
}