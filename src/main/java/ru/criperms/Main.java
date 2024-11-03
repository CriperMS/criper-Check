package ru.criperms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private Set<String> checkedPlayers = new HashSet<>();
    private Map<String, String> playerModerators = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = getConfig();

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.not_a_player")));
            return true;
        }

        Player moderator = (Player) sender;

        if (command.getName().equalsIgnoreCase("check") || command.getName().equalsIgnoreCase("anycheck")) {
            if (args.length == 0) {
                for (String checkedPlayerName : checkedPlayers) {
                    if (playerModerators.get(checkedPlayerName).equals(moderator.getName())) {
                        finishCheck(Bukkit.getPlayer(checkedPlayerName));
                        moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.moderator_check_end").replace("%player%", checkedPlayerName)));
                        return true;
                    }
                }
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.no_player_checked")));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.player_not_found")));
                return true;
            }

            if (checkedPlayers.contains(target.getName())) {
                finishCheck(target);
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.moderator_check_end").replace("%player%", target.getName())));
                return true;
            }

            String title = ChatColor.translateAlternateColorCodes('&', config.getString(command.getName().equalsIgnoreCase("check") ? "check_title" : "anycheck_title"));
            String subtitle = ChatColor.translateAlternateColorCodes('&', config.getString(command.getName().equalsIgnoreCase("check") ? "check_subtitle" : "anycheck_subtitle"));
            target.sendTitle(title, subtitle, 10, 1000000000, 20);

            checkedPlayers.add(target.getName());
            playerModerators.put(target.getName(), moderator.getName());

            target.setWalkSpeed(0);
            target.setFlySpeed(0);

            for (String line : config.getStringList(command.getName().equalsIgnoreCase("check") ? "check_chat_messages" : "anycheck_chat_messages")) {
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }

            moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.moderator_check_start").replace("%player%", target.getName())));
            return true;
        }

        if (command.getName().equalsIgnoreCase("mod")) {
            if (!checkedPlayers.contains(moderator.getName())) {
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.not_in_check")));
                return true;
            }

            if (args.length == 0) {
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.invalid_mod_usage")));
                return false;
            }

            String moderatorName = playerModerators.get(moderator.getName());
            Player mod = Bukkit.getPlayer(moderatorName);
            if (mod == null) {
                moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.mod_offline")));
                return true;
            }

            String messageTemplate = config.getString("message_for_moderator_format");
            if (messageTemplate == null) {
                moderator.sendMessage(ChatColor.RED + "Ошибка: Шаблон сообщения 'message_for_moderator_format' не найден в конфигурации!");
                return true;
            }

            String message = messageTemplate.replace("%player%", moderator.getName()).replace("%message%", String.join(" ", args));
            mod.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            moderator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.message_sent_confirm")));
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();

        if (checkedPlayers.contains(player.getName())) {
            String command = event.getMessage().split(" ")[0].toLowerCase();
            if (!command.equalsIgnoreCase("/mod")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("command_messages.only_mod_command")));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (checkedPlayers.contains(player.getName())) {
            if (event.getFrom().getY() < event.getTo().getY()) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.resetTitle();
        }
        checkedPlayers.clear();
        playerModerators.clear();
    }

    public void finishCheck(Player player) {
        FileConfiguration config = getConfig();

        if (checkedPlayers.contains(player.getName())) {
            checkedPlayers.remove(player.getName());
            playerModerators.remove(player.getName());

            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.resetTitle();
        }
    }
}
