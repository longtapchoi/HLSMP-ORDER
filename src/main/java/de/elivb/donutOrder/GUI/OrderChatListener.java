package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class OrderChatListener implements Listener {
   private final Order plugin;
   private final OrderGUI orderGUI;
   private final Map<UUID, String> awaitingInput;

   public OrderChatListener(Order plugin, OrderGUI orderGUI) {
      this.plugin = plugin;
      this.orderGUI = orderGUI;
      this.awaitingInput = new HashMap();
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      Map<String, Object> playerData = this.orderGUI.getPlayerNewOrderData(player);
      if (playerData.containsKey("awaiting_input")) {
         String inputType = (String)playerData.get("awaiting_input");
         String message = event.getMessage().trim();
         if (message.equalsIgnoreCase("cancel")) {
            event.setCancelled(true);
            playerData.remove("awaiting_input");
            this.orderGUI.getNewOrderData().put(playerId, playerData);
            this.plugin.getLangManager().sendMessage(player, "chat-input-cancel");
            this.plugin.getSoundManager().playSound(player, "error");
            if (this.plugin.isFolia()) {
               player.getScheduler().run(this.plugin, (task) -> this.orderGUI.openNewOrderGUI(player), (Runnable)null);
            } else {
               Bukkit.getScheduler().runTask(this.plugin, () -> this.orderGUI.openNewOrderGUI(player));
            }
         } else {
            try {
               long value = this.parseAbbreviatedNumber(message);
               if (value < 0L) {
                  event.setCancelled(true);
                  Map<String, String> placeholders = new HashMap();
                  if (inputType.equals("amount")) {
                     placeholders.put("%type%", "amount");
                  } else if (inputType.equals("price")) {
                     placeholders.put("%type%", "price");
                  }

                  this.plugin.getLangManager().sendMessage(player, "chat-input", placeholders);
                  this.plugin.getSoundManager().playSound(player, "error");
                  return;
               }

               if (inputType.equals("amount")) {
                  int min = this.plugin.getOrderManager().getMinItemPerOrder();
                  int max = this.plugin.getOrderManager().getMaxItemPerOrder();
                  if (value < (long)min) {
                     event.setCancelled(true);
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("%min%", String.valueOf(min));
                     this.plugin.getLangManager().sendMessage(player, "amount-too-low", placeholders);
                     this.plugin.getSoundManager().playSound(player, "error");
                     return;
                  }

                  if (max != -1 && value > (long)max) {
                     event.setCancelled(true);
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("%max%", String.valueOf(max));
                     this.plugin.getLangManager().sendMessage(player, "amount-too-high", placeholders);
                     this.plugin.getSoundManager().playSound(player, "error");
                     return;
                  }

                  playerData.put("amount", (int)value);
                  Map<String, String> placeholders = new HashMap();
                  placeholders.put("%amount%", this.formatNumber(value));
                  this.plugin.getLangManager().sendMessage(player, "amount-set", placeholders);
                  this.plugin.getSoundManager().playSound(player, "gui-click");
               } else if (inputType.equals("price")) {
                  int minx = this.plugin.getOrderManager().getMinPricePerItem();
                  int maxx = this.plugin.getOrderManager().getMaxPricePerItem();
                  if (value < (long)minx) {
                     event.setCancelled(true);
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("%min%", String.valueOf(minx));
                     this.plugin.getLangManager().sendMessage(player, "price-too-low", placeholders);
                     this.plugin.getSoundManager().playSound(player, "error");
                     return;
                  }

                  if (maxx != -1 && value > (long)maxx) {
                     event.setCancelled(true);
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("%max%", String.valueOf(maxx));
                     this.plugin.getLangManager().sendMessage(player, "price-too-high", placeholders);
                     this.plugin.getSoundManager().playSound(player, "error");
                     return;
                  }

                  playerData.put("pricePerItem", (double)value);
                  Map<String, String> placeholders = new HashMap();
                  placeholders.put("%price%", this.formatNumber(value));
                  this.plugin.getLangManager().sendMessage(player, "price-set", placeholders);
                  this.plugin.getSoundManager().playSound(player, "gui-click");
               }

               playerData.remove("awaiting_input");
               this.orderGUI.getNewOrderData().put(playerId, playerData);
               if (this.plugin.isFolia()) {
                  player.getScheduler().run(this.plugin, (task) -> this.orderGUI.openNewOrderGUI(player), (Runnable)null);
               } else {
                  Bukkit.getScheduler().runTask(this.plugin, () -> this.orderGUI.openNewOrderGUI(player));
               }

               event.setCancelled(true);
            } catch (NumberFormatException var12) {
               event.setCancelled(true);
               Map<String, String> placeholders = new HashMap();
               if (inputType.equals("amount")) {
                  placeholders.put("%type%", "amount");
               } else if (inputType.equals("price")) {
                  placeholders.put("%type%", "price");
               }

               this.plugin.getLangManager().sendMessage(player, "chat-input", placeholders);
               this.plugin.getSoundManager().playSound(player, "error");
            }
         }
      }

   }

   private long parseAbbreviatedNumber(String input) {
      if (input != null && !input.isEmpty()) {
         input = input.trim().toLowerCase();
         input = input.replace(",", "");
         char lastChar = input.charAt(input.length() - 1);
         if (Character.isLetter(lastChar)) {
            String numberPart = input.substring(0, input.length() - 1);
            char suffix = lastChar;

            try {
               double value = Double.parseDouble(numberPart);
               switch (suffix) {
                  case 'b' -> {
                     return (long)(value * (double)1.0E9F);
                  }
                  case 'k' -> {
                     return (long)(value * (double)1000.0F);
                  }
                  case 'm' -> {
                     return (long)(value * (double)1000000.0F);
                  }
                  case 't' -> {
                     return (long)(value * 1.0E12);
                  }
                  default -> {
                     return -1L;
                  }
               }
            } catch (NumberFormatException var7) {
               return -1L;
            }
         } else {
            try {
               return Long.parseLong(input);
            } catch (NumberFormatException var8) {
               return -1L;
            }
         }
      } else {
         return -1L;
      }
   }

   private String formatNumber(long number) {
      if (number >= 1000000000000L) {
         double result = (double)number / 1.0E12;
         return String.format("%.2fT", result);
      } else if (number >= 1000000000L) {
         double result = (double)number / (double)1.0E9F;
         return String.format("%.2fB", result);
      } else if (number >= 1000000L) {
         double result = (double)number / (double)1000000.0F;
         return String.format("%.2fM", result);
      } else if (number >= 1000L) {
         double result = (double)number / (double)1000.0F;
         return String.format("%.2fK", result);
      } else {
         return String.valueOf(number);
      }
   }
}
