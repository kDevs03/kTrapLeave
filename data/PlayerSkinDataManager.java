package ru.wishmine.ktrapleave.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlayerSkinDataManager {
   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
   private static final Map<String, PlayerSkinData> playerSkinDataMap = new HashMap<>();
   private static final File dataFolder = new File("plugins/KTrapLeave");
   private static final File playerSkinDataFile = new File(dataFolder, "playerskins.json");

   public static void loadPlayerSkinData() {
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      if (playerSkinDataFile.exists()) {
         try (FileReader reader = new FileReader(playerSkinDataFile)) {
            PlayerSkinData[] playerSkinDataArray = gson.fromJson(reader, PlayerSkinData[].class);
            if (playerSkinDataArray != null) {
               for (PlayerSkinData playerSkinData : playerSkinDataArray) {
                  playerSkinDataMap.put(playerSkinData.getPlayerName(), playerSkinData);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public static void savePlayerSkinData() {
      try (FileWriter writer = new FileWriter(playerSkinDataFile)) {
         gson.toJson(playerSkinDataMap.values(), writer);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static PlayerSkinData getPlayerSkinData(String playerName) {
      return playerSkinDataMap.computeIfAbsent(playerName, PlayerSkinData::new);
   }
}