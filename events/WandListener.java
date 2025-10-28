package ru.wishmine.ktrapleave.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.api.SchematicManager;
import ru.wishmine.ktrapleave.utils.MessageManager;

public class WandListener implements Listener {
    private final KTrapLeave plugin;
    private final SchematicManager schematicManager;
    private final MessageManager messageManager;

    public WandListener(KTrapLeave plugin, SchematicManager schematicManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.schematicManager = schematicManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isWand(item)) {
            return;
        }

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            schematicManager.setPos1(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            schematicManager.setPos2(player);
        }
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals("§6§lВолшебный топор трапки");
    }
}