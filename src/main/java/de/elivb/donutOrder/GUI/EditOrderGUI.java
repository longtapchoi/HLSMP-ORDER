package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.EditOrderManager;
import de.elivb.donutOrder.Manager.MaterialsManager;
import de.elivb.donutOrder.Manager.OrderItem;
import de.elivb.donutOrder.Manager.OrderManager;
import de.elivb.donutOrder.utils.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EditOrderGUI implements Listener {
   private final Order plugin;
   private final EditOrderManager editOrderManager;
   private final OrderManager orderManager;
   private final MaterialsManager materialsManager;
   private final OrderDeliveryGUI deliveryGUI;
   private final Map<UUID, OrderItem> editingOrders;

   public EditOrderGUI(Order plugin, EditOrderManager editOrderManager, OrderManager orderManager, MaterialsManager materialsManager, OrderDeliveryGUI deliveryGUI) {
      this.plugin = plugin;
      this.editOrderManager = editOrderManager;
      this.orderManager = orderManager;
      this.materialsManager = materialsManager;
      this.deliveryGUI = deliveryGUI;
      this.editingOrders = new HashMap();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openEditOrderGUI(Player player, OrderItem order) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (!player.getUniqueId().equals(order.getCreator())) {
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (!order.isActive()) {
         this.plugin.getLangManager().sendMessage(player, "no-orders");
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (order.isExpired()) {
         this.plugin.getLangManager().sendMessage(player, "order-not-available");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         this.editingOrders.put(player.getUniqueId(), order);
         String title = ColorUtil.color(this.editOrderManager.getEditOrderTitle());
         int rows = this.editOrderManager.getEditOrderRows();
         Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         this.addEditOrderItems(gui, player, order);
         player.openInventory(gui);
      }

   }

   private void addEditOrderItems(Inventory gui, Player player, OrderItem order) {
      Map<String, Object> items = this.editOrderManager.getEditOrderItems();
      Map<String, Object> sourceItemConfig = this.orderManager.getSourceItemConfig();
      if (items.containsKey("background")) {
         Map<?, ?> background = (Map)items.get("background");
         String materialName = (String)background.get("material");
         String displayName = (String)background.get("name");
         List<String> lore = new ArrayList();
         Object loreObj = background.get("lore");
         if (loreObj instanceof List) {
            for(Object line : (List)loreObj) {
               lore.add(ColorUtil.color(line.toString()));
            }
         }

         List<Integer> slots = (List)background.get("slots");
         Material material = Material.getMaterial(materialName);
         if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
         }

         ItemStack bgItem = this.createItem(material, displayName, lore);

         for(int slot : slots) {
            if (slot >= 0 && slot < gui.getSize()) {
               gui.setItem(slot, bgItem);
            }
         }
      }

      if (items.containsKey("source-item")) {
         Map<?, ?> sourceItem = (Map)items.get("source-item");
         int slotx = (Integer)sourceItem.get("slot");
         ItemStack orderItem = order.toItemStack(this.orderManager, sourceItemConfig);
         if (orderItem != null) {
            gui.setItem(slotx, orderItem);
         }
      }

      if (items.containsKey("cancel-order")) {
         Map<?, ?> cancelConfig = (Map)items.get("cancel-order");
         int slotx = (Integer)cancelConfig.get("slot");
         String materialNamex = (String)cancelConfig.get("material");
         String displayNamex = (String)cancelConfig.get("name");
         List<String> lorex = new ArrayList();
         Object loreObjx = cancelConfig.get("lore");
         if (loreObjx instanceof List) {
            for(Object line : (List)loreObjx) {
               lorex.add(ColorUtil.color(line.toString()));
            }
         }

         Material material = Material.getMaterial(materialNamex);
         if (material == null) {
            material = Material.RED_TERRACOTTA;
         }

         ItemStack cancelItem = this.createItem(material, displayNamex, lorex);
         gui.setItem(slotx, cancelItem);
      }

      int deliveredAmount = order.getDeliveredAmount();
      int collectedAmount = order.getCollectedAmount();
      int availableToCollect = deliveredAmount - collectedAmount;
      boolean hasItemsToCollect = availableToCollect > 0;
      String collectConfigKey = hasItemsToCollect ? "collect-items" : "no-collect-items";
      if (items.containsKey(collectConfigKey)) {
         Map<?, ?> collectConfig = (Map)items.get(collectConfigKey);
         int slotxx = (Integer)collectConfig.get("slot");
         String materialNamexx = (String)collectConfig.get("material");
         String displayNamexx = (String)collectConfig.get("name");
         List<String> lorexx = new ArrayList();
         Object loreObjxx = collectConfig.get("lore");
         if (loreObjxx instanceof List) {
            for(Object line : (List)loreObjxx) {
               String loreLine = line.toString();
               if (hasItemsToCollect) {
                  loreLine = loreLine.replace("%delivered%", String.valueOf(order.getDeliveredAmount())).replace("%remaining%", String.valueOf(order.getRemainingAmount())).replace("%total%", String.valueOf(order.getRequestedAmount())).replace("%price%", this.orderManager.formatCurrency(order.getPricePerItem())).replace("%totalprice%", this.orderManager.formatCurrency(order.getTotalPrice()));
               }

               lorexx.add(ColorUtil.color(loreLine));
            }
         }

         Material material = Material.getMaterial(materialNamexx);
         if (material == null) {
            material = Material.CHEST;
         }

         ItemStack collectItem = this.createItem(material, displayNamexx, lorexx);
         gui.setItem(slotxx, collectItem);
      }

      if (items.containsKey("back")) {
         Map<?, ?> backConfig = (Map)items.get("back");
         int slotxxx = (Integer)backConfig.get("slot");
         String materialNamexxx = (String)backConfig.get("material");
         String displayNamexxx = (String)backConfig.get("name");
         List<String> lorexxx = new ArrayList();
         Object loreObjxxx = backConfig.get("lore");
         if (loreObjxxx instanceof List) {
            for(Object line : (List)loreObjxxx) {
               lorexxx.add(ColorUtil.color(line.toString()));
            }
         }

         Material material = Material.getMaterial(materialNamexxx);
         if (material == null) {
            material = Material.RED_STAINED_GLASS_PANE;
         }

         ItemStack backItem = this.createItem(material, displayNamexxx, lorexxx);
         gui.setItem(slotxxx, backItem);
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
            meta.setLore(lore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         String strippedTitle = title.replace("§", "");
         if (strippedTitle.contains("Edit Order")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
               OrderItem order = (OrderItem)this.editingOrders.get(player.getUniqueId());
               if (order == null) {
                  player.closeInventory();
               } else {
                  UUID orderUuid = order.getOrderUuid();
                  OrderItem latestOrder = this.orderManager.getOrderItemByUuid(orderUuid);
                  if (latestOrder != null && latestOrder.isActive()) {
                     int slot = event.getSlot();
                     Map<String, Object> items = this.editOrderManager.getEditOrderItems();
                     if (items.containsKey("cancel-order")) {
                        Map<?, ?> cancelConfig = (Map)items.get("cancel-order");
                        int cancelSlot = (Integer)cancelConfig.get("slot");
                        if (slot == cancelSlot) {
                           this.plugin.getSoundManager().playSound(player, "gui-click");
                           if (this.plugin.isFolia()) {
                              Bukkit.getGlobalRegionScheduler().run(this.plugin, (task) -> this.handleCancelOrder(player, latestOrder));
                           } else {
                              Bukkit.getScheduler().runTask(this.plugin, () -> this.handleCancelOrder(player, latestOrder));
                           }

                           return;
                        }
                     }

                     if (items.containsKey("collect-items")) {
                        Map<?, ?> collectConfig = (Map)items.get("collect-items");
                        int collectSlot = (Integer)collectConfig.get("slot");
                        int noCollectSlot = -1;
                        if (items.containsKey("no-collect-items")) {
                           Map<?, ?> noCollectConfig = (Map)items.get("no-collect-items");
                           noCollectSlot = (Integer)noCollectConfig.get("slot");
                        }

                        if (slot == collectSlot || slot == noCollectSlot) {
                           int deliveredAmount = latestOrder.getDeliveredAmount();
                           int collectedAmount = latestOrder.getCollectedAmount();
                           int availableToCollect = deliveredAmount - collectedAmount;
                           if (availableToCollect > 0) {
                              this.plugin.getSoundManager().playSound(player, "gui-click");
                              this.handleCollectItems(player, latestOrder);
                           } else {
                              this.plugin.getSoundManager().playSound(player, "gui-click");
                           }

                           return;
                        }
                     }

                     if (items.containsKey("back")) {
                        Map<?, ?> backConfig = (Map)items.get("back");
                        int backSlot = (Integer)backConfig.get("slot");
                        if (slot == backSlot) {
                           this.plugin.getSoundManager().playSound(player, "gui-click");
                           this.handleBack(player, order);
                           return;
                        }
                     }

                     if (items.containsKey("source-item")) {
                        Map<?, ?> sourceItem = (Map)items.get("source-item");
                        int sourceSlot = (Integer)sourceItem.get("slot");
                        if (slot == sourceSlot) {
                           if (player.getUniqueId().equals(order.getCreator())) {
                              this.plugin.getSoundManager().playSound(player, "gui-click");
                              return;
                           }

                           this.plugin.getSoundManager().playSound(player, "gui-click");
                           player.closeInventory();
                           this.deliveryGUI.openOrderDelivery(player, order);
                           return;
                        }
                     }
                  } else {
                     player.closeInventory();
                     this.editingOrders.remove(player.getUniqueId());
                  }
               }
            }
         }
      }

   }

   private void handleCancelOrder(Player player, OrderItem order) {
      if (!player.getUniqueId().equals(order.getCreator())) {
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (order.getDeliveredAmount() > 0) {
         Map<String, String> placeholders = new HashMap();
         placeholders.put("%amount%", String.valueOf(order.getDeliveredAmount()));
      } else if (!order.isActive()) {
         this.plugin.getLangManager().sendMessage(player, "error");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         player.closeInventory();
         if (this.plugin.getConfirmCancelGUI() != null) {
            this.plugin.getConfirmCancelGUI().openConfirmCancelGUI(player, order);
         } else {
            this.plugin.getLangManager().sendMessage(player, "error");
            this.plugin.getSoundManager().playSound(player, "error");
         }
      }

   }

   private void handleCollectItems(Player player, OrderItem order) {
      if (!player.getUniqueId().equals(order.getCreator())) {
         this.plugin.getLangManager().sendMessage(player, "cannot-deliver-own");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         int deliveredAmount = order.getDeliveredAmount();
         int collectedAmount = order.getCollectedAmount();
         int availableToCollect = deliveredAmount - collectedAmount;
         if (availableToCollect <= 0) {
            this.plugin.getLangManager().sendMessage(player, "no-items");
            this.plugin.getSoundManager().playSound(player, "error");
         } else {
            player.closeInventory();
            if (this.plugin.getCollectItemsGUI() != null) {
               this.plugin.getCollectItemsGUI().openCollectItemsGUI(player, order, 1);
            } else {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
            }
         }
      }

   }

   private void handleBack(Player player, OrderItem order) {
      player.closeInventory();
      if (this.plugin.getOrderGUI() != null) {
         this.plugin.getOrderGUI().openYourOrdersGUI(player);
      }

   }

   private String formatMaterialName(String materialName) {
      String[] words = materialName.toLowerCase().split("_");
      StringBuilder formatted = new StringBuilder();

      for(String word : words) {
         if (!word.isEmpty()) {
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
         }
      }

      return formatted.toString().trim();
   }
}
