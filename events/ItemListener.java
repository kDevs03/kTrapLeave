package ru.wishmine.ktrapleave.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.api.TrapAPI;
import ru.wishmine.ktrapleave.data.TrapData;
import ru.wishmine.ktrapleave.utils.MessageManager;

import java.util.List;

public class ItemListener implements Listener {
   private TrapAPI trapAPI;
   private KTrapLeave plugin;
   private MessageManager messageManager;
   private TrapData trapData;

   public ItemListener(KTrapLeave plugin, TrapAPI trapAPI, MessageManager messageManager, TrapData trapData) {
      this.plugin = plugin;
      this.trapAPI = trapAPI;
      this.messageManager = messageManager;
      this.trapData = trapData;
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      Action action = event.getAction();

      if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
         return;
      }

      ItemStack item = event.getItem();
      if (item == null || !hasTrapTag(item)) {
         return;
      }

      event.setCancelled(true);

      String trapId = getTrapType(item);
      TrapData.TrapConfig trapConfig = trapData.getTrapConfig(trapId);
      if (trapConfig == null) {
         messageManager.sendMessage(player, "trap-not-found");
         return;
      }

      if (isWorldDisabled(player)) {
         messageManager.sendMessage(player, "world-disabled");
         return;
      }

      if (plugin.getConfig().getBoolean("settings.check-regions", true) && !canUseInRegion(player)) {
         messageManager.sendMessage(player, "in-region");
         return;
      }

      if (player.getLocation().getY() < plugin.getConfig().getDouble("settings.min-height", 10.0)) {
         messageManager.sendMessage(player, "height-block");
         return;
      }

      Material trapMaterial = Material.getMaterial(trapConfig.getMaterial());
      if (trapMaterial == null) {
         trapMaterial = Material.CHORUS_PLANT;
      }

      if (player.hasCooldown(trapMaterial) && !player.hasPermission("ktrapleave.nocooldown")) {
         int cooldownSeconds = player.getCooldown(trapMaterial) / 20;
         messageManager.sendMessageWithReplacements(player, "cooldown",
                 "%cooldown%", String.valueOf(cooldownSeconds)
         );
         return;
      }

      if (trapAPI.createTrap(player, trapId)) {
         // Удаляем предмет только если трапка успешно создана
         if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
         } else {
            player.getInventory().setItemInMainHand(null);
         }
         if (!player.hasPermission("ktrapleave.nocooldown")) {
            player.setCooldown(trapMaterial, trapConfig.getCooldown() * 20);
         }
      }
   }

   private boolean isWorldDisabled(Player player) {
      List<String> disabledWorlds = plugin.getConfig().getStringList("settings.disabled-worlds");
      return disabledWorlds.contains(player.getWorld().getName());
   }

   private boolean canUseInRegion(Player player) {

      if (plugin.getConfig().getBoolean("settings.allow-in-regions", false)) {
         return true;
      }

      RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
      if (regionManager != null) {
         ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
         return regions.getRegions().isEmpty();
      }

      return true;
   }

   private boolean hasTrapTag(ItemStack item) {
      if (item == null) return false;
      ItemMeta meta = item.getItemMeta();
      if (meta == null) return false;
      NamespacedKey key = new NamespacedKey(plugin, "ktrapleave_trap");
      return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
   }

   private String getTrapType(ItemStack item) {
      if (item == null) return "basic";
      ItemMeta meta = item.getItemMeta();
      if (meta == null) return "basic";
      NamespacedKey key = new NamespacedKey(plugin, "ktrapleave_trap_type");
      return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "basic");
   }
}