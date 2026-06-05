package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.ConfirmCancelManager;
import de.elivb.donutOrder.Manager.OrderItem;
import de.elivb.donutOrder.Manager.OrderManager;
import de.elivb.donutOrder.utils.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConfirmCancelGUI implements Listener {
   private final Order plugin;
   private final OrderManager orderManager;
   private final ConfirmCancelManager confirmCancelManager;
   private final EditOrderGUI editOrderGUI;
   private final Map<UUID, OrderItem> cancellingOrders;
   private final Map<UUID, Inventory> openInventories;
   private final Set<UUID> processedCancels;

   public ConfirmCancelGUI(Order plugin, OrderManager orderManager, ConfirmCancelManager confirmCancelManager, EditOrderGUI editOrderGUI) {
      this.plugin = plugin;
      this.orderManager = orderManager;
      this.confirmCancelManager = confirmCancelManager;
      this.editOrderGUI = editOrderGUI;
      this.cancellingOrders = new HashMap();
      this.openInventories = new HashMap();
      this.processedCancels = new HashSet();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openConfirmCancelGUI(Player player, OrderItem order) {
      UUID playerId = player.getUniqueId();
      this.cancellingOrders.remove(playerId);
      this.openInventories.remove(playerId);
      this.processedCancels.remove(playerId);
      this.cancellingOrders.put(playerId, order);
      String title = ColorUtil.color(this.confirmCancelManager.getConfirmCancelTitle());
      int rows = this.confirmCancelManager.getConfirmCancelRows();
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
      this.openInventories.put(playerId, gui);
      this.addGuiItems(gui, player, order);
      player.openInventory(gui);
   }

   private void addGuiItems(Inventory gui, Player player, OrderItem order) {
      Map<String, Object> items = this.confirmCancelManager.getConfirmCancelItems();
      Map<String, Object> sourceItemConfig = this.orderManager.getSourceItemConfig();

      for(int i = 0; i < gui.getSize(); ++i) {
         gui.setItem(i, (ItemStack)null);
      }

      if (items.containsKey("cancel")) {
         Map<?, ?> cancelConfig = (Map)items.get("cancel");
         int slot = this.getIntFromObject(cancelConfig.get("slot"), 11);
         String materialName = (String)cancelConfig.get("material");
         String displayName = (String)cancelConfig.get("name");
         List<String> lore = (List)cancelConfig.get("lore");
         Material material = Material.getMaterial(materialName);
         if (material == null || !material.isItem()) {
            material = Material.RED_STAINED_GLASS_PANE;
         }

         ItemStack cancelItem = this.createItem(material, displayName, lore);
         gui.setItem(slot, cancelItem);
      }

      if (items.containsKey("confirm")) {
         Map<?, ?> confirmConfig = (Map)items.get("confirm");
         int slot = this.getIntFromObject(confirmConfig.get("slot"), 15);
         String materialName = (String)confirmConfig.get("material");
         String displayName = (String)confirmConfig.get("name");
         List<String> lore = (List)confirmConfig.get("lore");
         Material material = Material.getMaterial(materialName);
         if (material == null || !material.isItem()) {
            material = Material.LIME_STAINED_GLASS_PANE;
         }

         ItemStack confirmItem = this.createItem(material, displayName, lore);
         gui.setItem(slot, confirmItem);
      }

      if (items.containsKey("source-item")) {
         Map<?, ?> sourceItem = (Map)items.get("source-item");
         int slot = this.getIntFromObject(sourceItem.get("slot"), 13);
         ItemStack orderItem = order.toItemStack(this.orderManager, sourceItemConfig);
         if (orderItem != null) {
            gui.setItem(slot, orderItem);
         }
      }

   }

   private ItemStack createItem(Material material, String displayName, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtil.color(displayName));
         }

         if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList();

            for(String line : lore) {
               coloredLore.add(ColorUtil.color(line));
            }

            meta.setLore(coloredLore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         if (title.contains("Cancel Order")) {
            OrderItem order = (OrderItem)this.cancellingOrders.get(player.getUniqueId());
            if (order == null) {
               player.closeInventory();
            } else {
               int slot = event.getSlot();
               Map<String, Object> items = this.confirmCancelManager.getConfirmCancelItems();
               int cancelSlot = -1;
               int confirmSlot = -1;
               if (items.containsKey("cancel")) {
                  cancelSlot = this.getIntFromObject(((Map)items.get("cancel")).get("slot"), 11);
               }

               if (items.containsKey("confirm")) {
                  confirmSlot = this.getIntFromObject(((Map)items.get("confirm")).get("slot"), 15);
               }

               if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
                  event.setCancelled(true);
                  if (slot == cancelSlot) {
                     this.plugin.getSoundManager().playSound(player, "gui-click");
                     this.handleCancel(player, order);
                  } else if (slot == confirmSlot) {
                     this.plugin.getSoundManager().playSound(player, "gui-click");
                     this.handleConfirm(player, order);
                  } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  }
               }
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         if (title.contains("Cancel Order")) {
            OrderItem order = (OrderItem)this.cancellingOrders.get(player.getUniqueId());
            if (order != null) {
               Inventory topInventory = event.getView().getTopInventory();

               for(int slot : event.getRawSlots()) {
                  if (slot < topInventory.getSize()) {
                     event.setCancelled(true);
                     this.plugin.getLangManager().sendMessage(player, "error");
                     this.plugin.getSoundManager().playSound(player, "error");
                     return;
                  }
               }
            }
         }
      }

   }

   private void handleCancel(Player player, OrderItem order) {
      UUID playerId = player.getUniqueId();
      if (!this.processedCancels.contains(playerId)) {
         this.processedCancels.add(playerId);
         player.closeInventory();
         if (this.plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
               if (player.isOnline() && this.editOrderGUI != null) {
                  this.editOrderGUI.openEditOrderGUI(player, order);
               }

               this.processedCancels.remove(playerId);
            }, 2L);
         } else {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (player.isOnline() && this.editOrderGUI != null) {
                  this.editOrderGUI.openEditOrderGUI(player, order);
               }

               this.processedCancels.remove(playerId);
            }, 2L);
         }

         this.cancellingOrders.remove(playerId);
         this.openInventories.remove(playerId);
      }

   }

   private void handleConfirm(Player player, OrderItem order) {
      UUID playerId = player.getUniqueId();
      if (!this.processedCancels.contains(playerId)) {
         this.processedCancels.add(playerId);
         if (!player.getUniqueId().equals(order.getCreator())) {
            this.plugin.getSoundManager().playSound(player, "error");
            player.closeInventory();
            this.processedCancels.remove(playerId);
            this.cancellingOrders.remove(playerId);
         } else if (order.getDeliveredAmount() > 0) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("%amount%", String.valueOf(order.getDeliveredAmount()));
            this.plugin.getLangManager().sendMessage(player, "already-delivered", placeholders);
            this.plugin.getSoundManager().playSound(player, "error");
            player.closeInventory();
            this.processedCancels.remove(playerId);
            this.cancellingOrders.remove(playerId);
         } else {
            UUID orderUuid = order.getOrderUuid();
            OrderItem latestOrder = this.orderManager.getOrderItemByUuid(orderUuid);
            if (latestOrder == null) {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
               player.closeInventory();
               this.processedCancels.remove(playerId);
               this.cancellingOrders.remove(playerId);
            } else if (!latestOrder.isActive()) {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
               player.closeInventory();
               this.processedCancels.remove(playerId);
               this.cancellingOrders.remove(playerId);
            } else {
               double paidAmount = latestOrder.getPaidAmount();
               if (paidAmount > (double)0.0F && this.orderManager.hasEconomy()) {
                  this.orderManager.getEconomy().depositPlayer(player, paidAmount);
                  latestOrder.setPaidAmount((double)0.0F);
                  Map<String, String> placeholders = new HashMap();
                  placeholders.put("%amount%", this.orderManager.formatCurrency(paidAmount));
                  this.plugin.getLangManager().sendMessage(player, "refund-success", placeholders);
               } else {
                  if (paidAmount > (double)0.0F && !this.orderManager.hasEconomy()) {
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("%amount%", this.orderManager.formatCurrency(paidAmount));
                     this.plugin.getLangManager().sendMessage(player, "error", placeholders);
                     this.plugin.getSoundManager().playSound(player, "error");
                     player.closeInventory();
                     this.processedCancels.remove(playerId);
                     this.cancellingOrders.remove(playerId);
                     return;
                  }

                  this.plugin.getLangManager().sendMessage(player, "error");
                  this.plugin.getSoundManager().playSound(player, "error");
               }

               latestOrder.setActive(false);
               this.orderManager.updateOrderItem(latestOrder);
               player.closeInventory();
               if (this.plugin.isFolia()) {
                  Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
                     if (player.isOnline() && this.plugin.getOrderGUI() != null) {
                        this.plugin.getOrderGUI().openYourOrdersGUI(player);
                     }

                     this.processedCancels.remove(playerId);
                  }, 2L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                     if (player.isOnline() && this.plugin.getOrderGUI() != null) {
                        this.plugin.getOrderGUI().openYourOrdersGUI(player);
                     }

                     this.processedCancels.remove(playerId);
                  }, 2L);
               }

               this.cancellingOrders.remove(playerId);
               this.openInventories.remove(playerId);
            }
         }
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         UUID playerId = player.getUniqueId();
         String title = event.getView().getTitle();
         if (title.contains("Cancel Order")) {
            Inventory closedInventory = event.getInventory();
            Inventory trackedInventory = (Inventory)this.openInventories.get(playerId);
            if (trackedInventory != null && trackedInventory.equals(closedInventory)) {
               if (!this.processedCancels.contains(playerId)) {
                  OrderItem order = (OrderItem)this.cancellingOrders.get(playerId);
                  if (order != null) {
                  }
               }

               this.cancellingOrders.remove(playerId);
               this.openInventories.remove(playerId);
               this.processedCancels.remove(playerId);
            }
         }
      }

   }

   private int getIntFromObject(Object obj, int defaultValue) {
      if (obj == null) {
         return defaultValue;
      } else {
         try {
            if (obj instanceof Integer) {
               return (Integer)obj;
            } else {
               return obj instanceof String ? Integer.parseInt((String)obj) : Integer.parseInt(obj.toString());
            }
         } catch (NumberFormatException var4) {
            return defaultValue;
         }
      }
   }
}
