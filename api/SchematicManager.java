package ru.wishmine.ktrapleave.api;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.utils.MessageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SchematicManager {
    private final KTrapLeave plugin;
    private final MessageManager messageManager;
    private final Map<UUID, BlockVector3> pos1Map;
    private final Map<UUID, BlockVector3> pos2Map;

    public SchematicManager(KTrapLeave plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.pos1Map = new HashMap<>();
        this.pos2Map = new HashMap<>();
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lВолшебный топор трапки");
            meta.setLore(Arrays.asList(
                    "§7Левый клик по блоку - установить pos1",
                    "§7Правый клик по блоку - установить pos2",
                    "§7Используйте /trap save <название> для сохранения"
            ));
            wand.setItemMeta(meta);
        }

        player.getInventory().addItem(wand);
        messageManager.sendMessage(player, "wand-received");
    }

    public void setPos1(Player player) {
        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        BlockVector3 position = BlockVector3.at(
                bukkitPlayer.getBlockLocation().getBlockX(),
                bukkitPlayer.getBlockLocation().getBlockY(),
                bukkitPlayer.getBlockLocation().getBlockZ()
        );

        pos1Map.put(player.getUniqueId(), position);

        messageManager.sendMessageWithReplacements(player, "pos1-set",
                "%x%", String.valueOf(position.getX()),
                "%y%", String.valueOf(position.getY()),
                "%z%", String.valueOf(position.getZ())
        );
    }

    public void setPos2(Player player) {
        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        BlockVector3 position = BlockVector3.at(
                bukkitPlayer.getBlockLocation().getBlockX(),
                bukkitPlayer.getBlockLocation().getBlockY(),
                bukkitPlayer.getBlockLocation().getBlockZ()
        );

        pos2Map.put(player.getUniqueId(), position);

        messageManager.sendMessageWithReplacements(player, "pos2-set",
                "%x%", String.valueOf(position.getX()),
                "%y%", String.valueOf(position.getY()),
                "%z%", String.valueOf(position.getZ())
        );
    }

    public boolean saveSchematic(Player player, String name) {
        UUID playerId = player.getUniqueId();
        BlockVector3 pos1 = pos1Map.get(playerId);
        BlockVector3 pos2 = pos2Map.get(playerId);

        if (pos1 == null || pos2 == null) {
            messageManager.sendMessage(player, "positions-not-set");
            return false;
        }

        if (name == null || name.isEmpty()) {
            messageManager.sendMessage(player, "invalid-name");
            return false;
        }

        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            messageManager.sendMessage(player, "invalid-name-format");
            return false;
        }

        try {
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);

            Region region = new CuboidRegion(bukkitPlayer.getWorld(), pos1, pos2);

            Clipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(pos1);

            File schematicFile = new File(plugin.getTrapsFolder(), name + ".schem");

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }

            messageManager.sendMessageWithReplacements(player, "schematic-saved",
                    "%name%", name
            );

            clearPositions(player);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при сохранении схемы: " + e.getMessage());
            e.printStackTrace();
            messageManager.sendMessage(player, "save-error");
            return false;
        }
    }

    public void clearPositions(Player player) {
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
        messageManager.sendMessage(player, "positions-cleared");
    }

    public BlockVector3 getPos1(Player player) {
        return pos1Map.get(player.getUniqueId());
    }

    public BlockVector3 getPos2(Player player) {
        return pos2Map.get(player.getUniqueId());
    }
}