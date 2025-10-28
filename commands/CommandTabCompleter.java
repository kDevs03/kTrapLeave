package ru.wishmine.ktrapleave.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.wishmine.ktrapleave.KTrapLeave;
import ru.wishmine.ktrapleave.data.TrapData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    private final KTrapLeave plugin;
    private final TrapData trapData;

    public CommandTabCompleter(KTrapLeave plugin) {
        this.plugin = plugin;
        this.trapData = plugin.getTrapData();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("give", "help"));

            if (sender.hasPermission("ktrapleave.admin")) {
                subCommands.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "reload", "list"));
            }

            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give":
                    List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[1], playerNames, completions);
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give":
                    List<String> amounts = Arrays.asList("1", "5", "10", "16", "32", "64");
                    StringUtil.copyPartialMatches(args[2], amounts, completions);
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "give":
                    List<String> trapTypes = new ArrayList<>(trapData.getAllTraps().keySet());
                    StringUtil.copyPartialMatches(args[3], trapTypes, completions);
                    break;
            }
        }

        Collections.sort(completions);
        return completions;
    }
}