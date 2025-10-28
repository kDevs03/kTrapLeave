package ru.wishmine.ktrapleave.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.data.PlayerSkinData;
import ru.wishmine.ktrapleave.data.PlayerSkinDataManager;

public class PlaceholderHook extends PlaceholderExpansion {
   private KTrapLeave plugin;

   public PlaceholderHook(KTrapLeave plugin) {
      this.plugin = plugin;
   }

   @Override
   public @NotNull String getIdentifier() {
      return "ktrapleave";
   }

   @Override
   public @NotNull String getAuthor() {
      return "KOPEHHOU";
   }

   @Override
   public @NotNull String getVersion() {
      return "1.0";
   }

   @Override
   public String onPlaceholderRequest(Player player, @NotNull String identifier) {
      if (player == null) {
         return "";
      }

      if (identifier.startsWith("skin_have:")) {
         String skinName = identifier.substring("skin_have:".length());
         PlayerSkinData playerSkinData = PlayerSkinDataManager.getPlayerSkinData(player.getName());
         return playerSkinData != null && playerSkinData.getPurchasedSkins().contains(skinName) ? "yes" : "no";
      }

      return null;
   }
}