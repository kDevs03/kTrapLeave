package ru.wishmine.ktrapleave.data;

import java.util.ArrayList;
import java.util.List;

public class PlayerSkinData {
   private String playerName;
   private List<String> purchasedSkins = new ArrayList<>();
   private String activeSkin;

   public PlayerSkinData(String playerName) {
      this.playerName = playerName;
      if (!purchasedSkins.contains("free")) {
         purchasedSkins.add("free");
      }
      if (activeSkin == null) {
         activeSkin = "free";
      }
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public List<String> getPurchasedSkins() {
      return this.purchasedSkins;
   }

   public String getActiveSkin() {
      return this.activeSkin;
   }

   public void setActiveSkin(String activeSkin) {
      this.activeSkin = activeSkin;
   }
}