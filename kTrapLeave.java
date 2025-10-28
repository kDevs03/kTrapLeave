package ru.wishmine.ktrapleave;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.wishmine.ktrapleave.api.TrapAPI;
import ru.wishmine.ktrapleave.api.SchematicManager;
import ru.wishmine.ktrapleave.commands.CommandListener;
import ru.wishmine.ktrapleave.commands.CommandTabCompleter;
import ru.wishmine.ktrapleave.data.TrapData;
import ru.wishmine.ktrapleave.events.ItemListener;
import ru.wishmine.ktrapleave.events.WandListener;
import ru.wishmine.ktrapleave.utils.MessageManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class KTrapLeave extends JavaPlugin {
    private MessageManager messageManager;
    private File trapsFolder;
    private TrapAPI trapAPI;
    private SchematicManager schematicManager;
    private TrapData trapData;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.trapsFolder = new File(getDataFolder(), "traps");
        if (!trapsFolder.exists()) {
            trapsFolder.mkdirs();
            createDefaultFiles();
        }

        this.messageManager = new MessageManager(this);
        this.trapData = new TrapData(getConfig());

        this.trapAPI = new TrapAPI(this, messageManager, trapData);
        this.schematicManager = new SchematicManager(this, messageManager);

        this.getServer().getPluginManager().registerEvents(new ItemListener(this, trapAPI, messageManager, trapData), this);
        this.getServer().getPluginManager().registerEvents(new WandListener(this, schematicManager, messageManager), this);

        this.getCommand("ktrapleave").setExecutor(new CommandListener(this, messageManager, schematicManager, trapData));
        this.getCommand("ktrapleave").setTabCompleter(new CommandTabCompleter(this));

        getLogger().info("KTrapLeave успешно запущен!");
    }

    private void createDefaultFiles() {
        File readmeFile = new File(trapsFolder, "README.txt");
        if (!readmeFile.exists()) {
            try (FileWriter writer = new FileWriter(readmeFile)) {
                String readmeText = "KTrapLeave - Папка с схемами трапок\n\n" +
                        "Разместите файлы схем (.schem) в этой папке\n" +
                        "И укажите путь в config.yml в разделе trap.schematic\n\n" +
                        "Пример:\n" +
                        "trap:\n" +
                        "  schematic: \"traps/моя_схема.schem\"\n\n" +
                        "Как создать схему:\n" +
                        "1. Используйте /ktrapleave wand для получения инструмента\n" +
                        "2. Установите pos1 и pos2 (ЛКМ и ПКМ по блокам)\n" +
                        "3. Сохраните: /ktrapleave save <название>";
                writer.write(readmeText);
            } catch (IOException e) {
                getLogger().warning("Не удалось создать README: " + e.getMessage());
            }
        }
    }

    public File getTrapsFolder() {
        return trapsFolder;
    }

    public TrapAPI getTrapAPI() {
        return trapAPI;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public TrapData getTrapData() {
        return trapData;
    }

    @Override
    public void onDisable() {
        if (trapAPI != null) {
            trapAPI.onDisable();
        }
        getLogger().info("KTrapLeave успешно выключен!");
    }
}
