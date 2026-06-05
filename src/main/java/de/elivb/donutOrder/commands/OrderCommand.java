package de.elivb.donutOrder.commands;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.GUI.OrderGUI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class OrderCommand implements CommandExecutor, TabCompleter {
   private final Order plugin;
   private final OrderGUI orderGUI;

   public OrderCommand(Order plugin, OrderGUI orderGUI) {
      this.plugin = plugin;
      this.orderGUI = orderGUI;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("order.use")) {
            this.plugin.getLangManager().sendMessage(player, "no-permission");
            this.plugin.getSoundManager().playSound(player, "error");
            return true;
         } else if (args.length == 0) {
            this.orderGUI.openOrderGUI(player, 1);
            return true;
         } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("order.admin")) {
               this.plugin.getOrderManager().reloadConfigs();
               this.plugin.getMaterialsManager().reloadConfig();
               this.plugin.getLangManager().reload();
               this.plugin.reloadConfig();
               this.plugin.getSoundManager().playSound(player, "reload");
               String BRIGHT_CYAN = "\u001b[96m";
               String BRIGHT_RED = "\u001b[91m";
               String RESET = "\u001b[0m";
               this.plugin.getLogger().info("\u001b[96mReloading configuration...\u001b[0m");
               this.plugin.getLogger().info("\u001b[91m╠ Reloaded lang.yml!\u001b[0m");
               this.plugin.getLogger().info("\u001b[91m╠ Reloaded config.yml!\u001b[0m");
               this.plugin.getLogger().info("\u001b[91m╠ Reloaded GUIS!\u001b[0m");
               this.plugin.getLogger().info("\u001b[91m╠ Reloaded items.yml!\u001b[0m");
               this.plugin.getLogger().info("\u001b[96m╚ Successful reload!\u001b[0m");
               this.plugin.getLangManager().sendMessage(player, "plugin-reloaded");
            } else {
               this.plugin.getLangManager().sendMessage(player, "no-permission");
               this.plugin.getSoundManager().playSound(player, "error");
            }

            return true;
         } else {
            if (player.hasPermission("order.admin")) {
               this.plugin.getLangManager().sendMultilineMessage(player, "command-usage-admin");
            } else {
               this.plugin.getLangManager().sendMultilineMessage(player, "command-usage");
            }

            this.plugin.getSoundManager().playSound(player, "error");
            return true;
         }
      } else {
         sender.sendMessage(this.plugin.getLangManager().getMessage("only-players"));
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         String partial = args[0].toLowerCase();
         if (sender.hasPermission("order.admin") && "reload".startsWith(partial)) {
            completions.add("reload");
         }
      }

      return completions;
   }
}
