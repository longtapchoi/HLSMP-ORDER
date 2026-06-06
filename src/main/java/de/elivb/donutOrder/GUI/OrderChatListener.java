package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class OrderChatListener implements Listener {
   private final Order plugin;
   private final OrderGUI orderGUI;
   private final Map<UUID, String> awaitingInput;
   private final Map<UUID, Location> signLocations;
   private final Map<UUID, Material> signOriginalTypes;

   public OrderChatListener(Order plugin, OrderGUI orderGUI) {
      this.plugin = plugin;
      this.orderGUI = orderGUI;
      this.awaitingInput = new HashMap();
      this.signLocations = new HashMap();
      this.signOriginalTypes = new HashMap();
   }

   public void openSignInput(Player player, String inputType) {
      UUID playerId = player.getUniqueId();
      Location loc = player.getLocation().clone();
      loc.setY(loc.getY() - 5);
      if (loc.getY() < -64) loc.setY(-64);
      Block block = loc.getBlock();
      final Material originalType = block.getType();
      block.setType(Material.OAK_WALL_SIGN, false);
      if (!(block.getState() instanceof Sign)) {
         block.setType(originalType, false);
         return;
      }
      Sign sign = (Sign) block.getState();
      String title = inputType.equals("amount") ? "SO LUONG" : "NHAP GIA";
      sign.getSide(Side.FRONT).setLine(0, title);
      sign.getSide(Side.FRONT).setLine(1, "");
      sign.update(true, false);
      this.signLocations.put(playerId, loc);
      this.signOriginalTypes.put(playerId, originalType);
      final Location signLocFinal = loc;
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         player.openSign(sign, Side.FRONT);
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (signLocFinal.getBlock().getType() == Material.OAK_WALL_SIGN) {
               signLocFinal.getBlock().setType(originalType, false);
            }
            this.signLocations.remove(playerId);
            this.signOriginalTypes.remove(playerId);
         }, 600L);
      }, 2L);
   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = (Player) event.getPlayer();
      UUID playerId = player.getUniqueId();
      if (!this.signLocations.containsKey(playerId)) return;
      event.setCancelled(true);
      Location signLoc = this.signLocations.remove(playerId);
      Material origType = this.signOriginalTypes.remove(playerId);
      if (signLoc != null) {
         final Material restore = origType != null ? origType : Material.AIR;
         Bukkit.getScheduler().runTask(this.plugin, () -> signLoc.getBlock().setType(restore, false));
      }
      String text = event.getLine(1);
      if (text == null) text = "";
      text = text.trim();
      Map<String, Object> playerData = this.orderGUI.getPlayerNewOrderData(player);
      if (!playerData.containsKey("awaiting_input")) return;
      String inputType = (String) playerData.get("awaiting_input");
      if (text.isEmpty() || text.equalsIgnoreCase("cancel")) {
         playerData.remove("awaiting_input");
         this.orderGUI.getNewOrderData().put(playerId, playerData);
         this.plugin.getLangManager().sendMessage(player, "chat-input-cancel");
         this.plugin.getSoundManager().playSound(player, "error");
         Bukkit.getScheduler().runTask(this.plugin, () -> this.orderGUI.openNewOrderGUI(player));
         return;
      }
      try {
         long value = this.parseAbbreviatedNumber(text);
         if (value < 0L) {
            this.plugin.getSoundManager().playSound(player, "error");
            Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
            return;
         }
         if (inputType.equals("amount")) {
            int min = this.plugin.getOrderManager().getMinItemPerOrder();
            int max = this.plugin.getOrderManager().getMaxItemPerOrder();
            if (value < (long) min) {
               Map<String, String> ph = new HashMap();
               ph.put("%min%", String.valueOf(min));
               this.plugin.getLangManager().sendMessage(player, "amount-too-low", ph);
               this.plugin.getSoundManager().playSound(player, "error");
               Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
               return;
            }
            if (max != -1 && value > (long) max) {
               Map<String, String> ph = new HashMap();
               ph.put("%max%", String.valueOf(max));
               this.plugin.getLangManager().sendMessage(player, "amount-too-high", ph);
               this.plugin.getSoundManager().playSound(player, "error");
               Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
               return;
            }
            playerData.put("amount", (int) value);
            Map<String, String> ph = new HashMap();
            ph.put("%amount%", this.formatNumber(value));
            this.plugin.getLangManager().sendMessage(player, "amount-set", ph);
         } else if (inputType.equals("price")) {
            int min = this.plugin.getOrderManager().getMinPricePerItem();
            int max = this.plugin.getOrderManager().getMaxPricePerItem();
            if (value < (long) min) {
               Map<String, String> ph = new HashMap();
               ph.put("%min%", String.valueOf(min));
               this.plugin.getLangManager().sendMessage(player, "price-too-low", ph);
               this.plugin.getSoundManager().playSound(player, "error");
               Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
               return;
            }
            if (max != -1 && value > (long) max) {
               Map<String, String> ph = new HashMap();
               ph.put("%max%", String.valueOf(max));
               this.plugin.getLangManager().sendMessage(player, "price-too-high", ph);
               this.plugin.getSoundManager().playSound(player, "error");
               Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
               return;
            }
            playerData.put("pricePerItem", (double) value);
            Map<String, String> ph = new HashMap();
            ph.put("%price%", this.formatNumber(value));
            this.plugin.getLangManager().sendMessage(player, "price-set", ph);
         }
         playerData.remove("awaiting_input");
         this.orderGUI.getNewOrderData().put(playerId, playerData);
         this.plugin.getSoundManager().playSound(player, "gui-click");
         Bukkit.getScheduler().runTask(this.plugin, () -> this.orderGUI.openNewOrderGUI(player));
      } catch (NumberFormatException e) {
         this.plugin.getSoundManager().playSound(player, "error");
         Bukkit.getScheduler().runTask(this.plugin, () -> this.openSignInput(player, inputType));
      }
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      Map<String, Object> playerData = this.orderGUI.getPlayerNewOrderData(player);
      if (playerData.containsKey("awaiting_input")) {
         event.setCancelled(true);
      }
   }

   private long parseAbbreviatedNumber(String input) {
      if (input != null && !input.isEmpty()) {
         input = input.trim().toLowerCase().replace(",", "");
         char lastChar = input.charAt(input.length() - 1);
         if (Character.isLetter(lastChar)) {
            String numberPart = input.substring(0, input.length() - 1);
            try {
               double value = Double.parseDouble(numberPart);
               switch (lastChar) {
                  case 'b' -> { return (long)(value * 1.0E9); }
                  case 'k' -> { return (long)(value * 1000.0); }
                  case 'm' -> { return (long)(value * 1000000.0); }
                  case 't' -> { return (long)(value * 1.0E12); }
                  default -> { return -1L; }
               }
            } catch (NumberFormatException e) { return -1L; }
         } else {
            try { return Long.parseLong(input); } catch (NumberFormatException e) { return -1L; }
         }
      }
      return -1L;
   }

   private String formatNumber(long number) {
      if (number >= 1000000000000L) return String.format("%.2fT", (double) number / 1.0E12);
      else if (number >= 1000000000L) return String.format("%.2fB", (double) number / 1.0E9);
      else if (number >= 1000000L) return String.format("%.2fM", (double) number / 1000000.0);
      else if (number >= 1000L) return String.format("%.2fK", (double) number / 1000.0);
      else return String.valueOf(number);
   }
}
