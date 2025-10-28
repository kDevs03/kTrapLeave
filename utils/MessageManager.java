package ru.wishmine.ktrapleave.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ru.wishmine.ktrapleave.KTrapLeave;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class MessageManager {

    private final KTrapLeave plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(KTrapLeave plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String path) {
        return getMessage(path, (Player) null);
    }

    public String getMessage(String path, Player player) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "&cСообщение не найдено: " + path;
        }

        message = message.replace("{prefix}", messagesConfig.getString("prefix", "&7[KTrapLeave]"));
        if (player != null) {
            message = message.replace("%player%", player.getName())
                    .replace("%displayname%", player.getDisplayName());
        }

        return HexColorUtil.translateHexColors(message);
    }

    public String getMessageWithReplacements(String path, String... replacements) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "&cСообщение не найдено: " + path;
        }

        message = message.replace("{prefix}", messagesConfig.getString("prefix", "&7[KTrapLeave]"));
        if (replacements != null && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return HexColorUtil.translateHexColors(message);
    }

    public String getMessageWithReplacements(String path, Player player, String... replacements) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "&cСообщение не найдено: " + path;
        }

        message = message.replace("{prefix}", messagesConfig.getString("prefix", "&7[KTrapLeave]"));

        if (player != null) {
            message = message.replace("%player%", player.getName())
                    .replace("%displayname%", player.getDisplayName());
        }

        if (replacements != null && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return HexColorUtil.translateHexColors(message);
    }

    public List<String> getMessageList(String path) {
        List<String> messages = messagesConfig.getStringList(path);
        return messages.stream()
                .map(msg -> msg.replace("{prefix}", messagesConfig.getString("prefix", "&7[KTrapLeave]")))
                .map(HexColorUtil::translateHexColors)
                .collect(Collectors.toList());
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, (Player) null);
    }

    public void sendMessage(CommandSender sender, String path, Player player) {
        String message = getMessage(path, player);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessageWithReplacements(CommandSender sender, String path, String... replacements) {
        String message = getMessageWithReplacements(path, replacements);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessageWithReplacements(CommandSender sender, String path, Player player, String... replacements) {
        String message = getMessageWithReplacements(path, player, replacements);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessageList(CommandSender sender, String path) {
        List<String> messages = getMessageList(path);
        for (String message : messages) {
            sender.sendMessage(message);
        }
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}