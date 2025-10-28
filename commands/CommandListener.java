package ru.wishmine.ktrapleave.commands;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.api.SchematicManager;
import ru.wishmine.ktrapleave.data.TrapData;
import ru.wishmine.ktrapleave.utils.HexColorUtil;
import ru.wishmine.ktrapleave.utils.MessageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandListener implements CommandExecutor {
   private final KTrapLeave plugin;
   private final MessageManager messageManager;
   private final SchematicManager schematicManager;
   private final TrapData trapData;

   public CommandListener(KTrapLeave plugin, MessageManager messageManager, SchematicManager schematicManager, TrapData trapData) {
      this.plugin = plugin;
      this.messageManager = messageManager;
      this.schematicManager = schematicManager;
      this.trapData = trapData;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         showUsage(sender);
         return true;
      }

      switch (args[0].toLowerCase()) {
         case "give":
            if (args.length >= 3) {
               giveTrap(sender, args);
            } else {
               messageManager.sendMessage(sender, "give-usage");
            }
            break;
         case "wand":
            if (!sender.hasPermission("ktrapleave.admin")) {
               messageManager.sendMessage(sender, "no-permission");
               return true;
            }
            if (sender instanceof Player) {
               schematicManager.giveWand((Player) sender);
            } else {
               sender.sendMessage("Эта команда только для игроков!");
            }
            break;
         case "save":
            if (!sender.hasPermission("ktrapleave.admin")) {
               messageManager.sendMessage(sender, "no-permission");
               return true;
            }
            if (args.length == 2) {
               if (sender instanceof Player) {
                  schematicManager.saveSchematic((Player) sender, args[1]);
               } else {
                  sender.sendMessage("Эта команда только для игроков!");
               }
            } else {
               messageManager.sendMessage(sender, "save-usage");
            }
            break;
         case "pos1":
            if (!sender.hasPermission("ktrapleave.admin")) {
               messageManager.sendMessage(sender, "no-permission");
               return true;
            }
            if (sender instanceof Player) {
               schematicManager.setPos1((Player) sender);
            } else {
               sender.sendMessage("Эта команда только для игроков!");
            }
            break;
         case "pos2":
            if (!sender.hasPermission("ktrapleave.admin")) {
               messageManager.sendMessage(sender, "no-permission");
               return true;
            }
            if (sender instanceof Player) {
               schematicManager.setPos2((Player) sender);
            } else {
               sender.sendMessage("Эта команда только для игроков!");
            }
            break;
         case "reload":
            if (!sender.hasPermission("ktrapleave.admin")) {
               messageManager.sendMessage(sender, "no-permission");
               return true;
            }
            plugin.reloadConfig();
            messageManager.reloadMessages();
            messageManager.sendMessage(sender, "reload-success");
            break;
         case "list":
            listTraps(sender);
            break;
         case "help":
         default:
            showUsage(sender);
            break;
      }

      return true;
   }

   private void showUsage(CommandSender sender) {
      if (sender.hasPermission("ktrapleave.admin")) {
         messageManager.sendMessageList(sender, "command-usage-admin");
      } else {
         messageManager.sendMessageList(sender, "command-usage");
      }
   }

   private void giveTrap(CommandSender sender, String[] args) {
      if (!sender.hasPermission("ktrapleave.admin")) {
         messageManager.sendMessage(sender, "no-permission");
         return;
      }

      Player target = this.plugin.getServer().getPlayer(args[1]);
      if (target == null) {
         messageManager.sendMessage(sender, "player-not-found");
         return;
      }

      int amount;
      try {
         amount = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
         messageManager.sendMessage(sender, "invalid-amount");
         return;
      }

      if (amount <= 0) {
         messageManager.sendMessage(sender, "invalid-amount");
         return;
      }

      String trapType = "basic";
      if (args.length >= 4) {
         trapType = args[3];
      }

      if (trapData.getTrapConfig(trapType) == null) {
         messageManager.sendMessage(sender, "trap-not-found");
         listTraps(sender);
         return;
      }

      ItemStack trapItem = createTrapItem(trapType);
      if (trapItem == null) {
         messageManager.sendMessage(sender, "item-creation-failed");
         return;
      }

      trapItem.setAmount(amount);
      target.getInventory().addItem(trapItem);

      messageManager.sendMessageWithReplacements(sender, "give-success",
              "%player%", target.getName(),
              "%amount%", String.valueOf(amount),
              "%trap%", trapType
      );
   }

   private void listTraps(CommandSender sender) {
      Map<String, TrapData.TrapConfig> traps = trapData.getAllTraps();
      messageManager.sendMessage(sender, "trap-list-header");

      for (Map.Entry<String, TrapData.TrapConfig> entry : traps.entrySet()) {
         String message = messageManager.getMessageWithReplacements("trap-list-entry",
                 "%id%", entry.getKey(),
                 "%name%", HexColorUtil.translateHexColors(entry.getValue().getName())
         );
         sender.sendMessage(message);
      }
   }

   private ItemStack createTrapItem(String trapType) {
      TrapData.TrapConfig trapConfig = trapData.getTrapConfig(trapType);
      if (trapConfig == null) {
         return null;
      }

      Material material;
      try {
         material = Material.valueOf(trapConfig.getMaterial());
      } catch (IllegalArgumentException e) {
         material = Material.CHORUS_PLANT;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();

      if (meta != null) {
         meta.setDisplayName(HexColorUtil.translateHexColors(trapConfig.getName()));

         List<String> lore = trapConfig.getLore().stream()
                 .map(HexColorUtil::translateHexColors)
                 .collect(Collectors.toList());
         meta.setLore(lore);

         NamespacedKey key = new NamespacedKey(this.plugin, "ktrapleave_trap");
         meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "trap");

         NamespacedKey typeKey = new NamespacedKey(this.plugin, "ktrapleave_trap_type");
         meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, trapType);

         item.setItemMeta(meta);
      }

      return item;
   }
}