package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.MaterialsManager;
import de.elivb.donutOrder.Manager.OrderItem;
import de.elivb.donutOrder.Manager.OrderManager;
import de.elivb.donutOrder.utils.ColorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OrderGUI implements Listener {
   private final Order plugin;
   private final OrderManager orderManager;
   private final MaterialsManager materialsManager;
   private ListMaterialsGUI listMaterialsGUI;
   private EditOrderGUI editOrderGUI;
   private OrderViewHandler orderViewHandler;
   private final Map<UUID, Integer> playerPages;
   private final Map<UUID, Map<String, Object>> newOrderData;

   public OrderGUI(Order plugin, OrderManager orderManager, MaterialsManager materialsManager, EditOrderGUI editOrderGUI) {
      this.plugin = plugin;
      this.orderManager = orderManager;
      this.materialsManager = materialsManager;
      this.editOrderGUI = editOrderGUI;
      this.playerPages = new HashMap();
      this.newOrderData = new HashMap();
      this.orderViewHandler = new OrderViewHandler(plugin, orderManager, this);
      this.listMaterialsGUI = new ListMaterialsGUI(plugin, materialsManager, this);
      plugin.getServer().getPluginManager().registerEvents(this.listMaterialsGUI, plugin);
   }

   public void openOrderGUI(Player player, int page) {
      this.openOrderGUI(player, page, (String)null);
   }

   public void openOrderGUI(Player player, int page, String search) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         this.orderViewHandler.openOrderGUI(player, page, search);
      }

   }

   public void openYourOrdersGUI(Player player) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         String title = this.orderManager.getYourOrdersTitle();
         title = ColorUtil.color(title);
         int rows = this.orderManager.getYourOrdersRows();
         Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         this.addYourOrdersItems(gui, player);
         player.openInventory(gui);
      }

   }

   public void openNewOrderGUI(Player player) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (!this.orderManager.canCreateNewOrder(player)) {
         int maxOrders = this.orderManager.getMaxOrdersForPlayer(player);
         Map<String, String> placeholders = new HashMap();
         placeholders.put("%max%", String.valueOf(maxOrders));
         this.plugin.getLangManager().sendMessage(player, "max-orders-reached", placeholders);
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         String title = this.orderManager.getNewOrderTitle();
         title = ColorUtil.color(title);
         int rows = this.orderManager.getNewOrderRows();
         Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         this.addNewOrderItems(gui, player);
         player.openInventory(gui);
      }

   }

   public void openListMaterialsGUI(Player player, int page) {
      this.listMaterialsGUI.openListMaterialsGUI(player, page);
   }

   public void setSelectedMaterial(Player player, MaterialsManager.SpecialItemEntry specialItemEntry) {
      Map<String, Object> playerData = (Map)this.newOrderData.getOrDefault(player.getUniqueId(), new HashMap());
      playerData.put("specialItemEntry", specialItemEntry);
      playerData.put("material", specialItemEntry.baseMaterial);
      playerData.put("itemType", specialItemEntry.type.name());
      if (specialItemEntry.type != MaterialsManager.ItemType.NORMAL_ITEM) {
         playerData.put("subType", specialItemEntry.subType);
      }

      this.newOrderData.put(player.getUniqueId(), playerData);
      Map<String, String> placeholders = new HashMap();
      placeholders.put("%item%", specialItemEntry.displayName);
      this.plugin.getLangManager().sendMessage(player, "item-set", placeholders);
      this.plugin.getSoundManager().playSound(player, "gui-click");
      this.openNewOrderGUI(player);
   }

   public Map<String, Object> getPlayerNewOrderData(Player player) {
      return (Map)this.newOrderData.getOrDefault(player.getUniqueId(), new HashMap());
   }

   public Map<UUID, Map<String, Object>> getNewOrderData() {
      return this.newOrderData;
   }

   private void addYourOrdersItems(Inventory gui, Player player) {
      Map<String, Object> items = this.orderManager.getYourOrdersItems();
      List<OrderItem> playerOrders = this.orderManager.getOrderItemsByPlayer(player.getUniqueId());
      List<OrderItem> displayOrders = (List)playerOrders.stream().filter((orderx) -> {
         if (!orderx.isActive()) {
            return false;
         } else {
            return orderx.isExpired() ? false : orderx.getRemainingAmount() > 0 || orderx.getAvailableToCollect() > 0;
         }
      }).collect(Collectors.toList());
      Map<String, Object> sourceItemConfig = this.orderManager.getSourceItemConfig();
      if (items.containsKey("new-order")) {
         Map<?, ?> newOrderConfig = (Map)items.get("new-order");
         if (newOrderConfig != null) {
            ItemStack newOrderItem = this.createNavigationItem(newOrderConfig, "New Order");
            gui.setItem(0, newOrderItem);
         }
      }

      if (!displayOrders.isEmpty()) {
         for(int i = 0; i < displayOrders.size() && i < 26; ++i) {
            OrderItem order = (OrderItem)displayOrders.get(i);
            ItemStack orderItem = order.toItemStack(this.orderManager, sourceItemConfig);
            if (orderItem != null) {
               gui.setItem(i + 1, orderItem);
            }
         }
      }

   }

   private void addNewOrderItems(Inventory gui, Player player) {
      Map<String, Object> items = this.orderManager.getNewOrderItems();
      Map<String, Object> playerData = this.getPlayerNewOrderData(player);
      int amount = (Integer)playerData.getOrDefault("amount", 1);
      double pricePerItem = (Double)playerData.getOrDefault("pricePerItem", (double)100.0F);
      double totalPrice = (double)amount * pricePerItem;
      String formattedAmount = this.formatNumber((long)amount);
      MaterialsManager.SpecialItemEntry selectedEntry = (MaterialsManager.SpecialItemEntry)playerData.getOrDefault("specialItemEntry", (Object)null);
      Material selectedMaterial = Material.STONE;
      String itemName = "Stone";
      String materialDisplayName = "Stone";
      if (selectedEntry != null) {
         selectedMaterial = selectedEntry.baseMaterial;
         itemName = selectedEntry.displayName;
         materialDisplayName = selectedEntry.displayName;
      } else {
         selectedMaterial = (Material)playerData.getOrDefault("material", Material.STONE);
         itemName = this.formatMaterialName(selectedMaterial.name());
         this.formatMaterialName(selectedMaterial.name());
      }

      if (items.containsKey("cancel")) {
         Map<?, ?> cancelConfig = (Map)items.get("cancel");
         if (cancelConfig != null) {
            int slot = this.getIntFromObject(cancelConfig.get("slot"), 10);
            ItemStack cancelItem = this.createNavigationItem(cancelConfig, "Cancel");
            gui.setItem(slot, cancelItem);
         }
      }

      if (items.containsKey("confirm")) {
         Map<?, ?> confirmConfig = (Map)items.get("confirm");
         if (confirmConfig != null) {
            int slot = this.getIntFromObject(confirmConfig.get("slot"), 16);
            ItemStack confirmItem = this.createNavigationItemWithPlaceholders(confirmConfig, "Confirm", Map.of("%price%", this.orderManager.formatCurrency(totalPrice)));
            gui.setItem(slot, confirmItem);
         }
      }

      if (items.containsKey("item")) {
         Map<?, ?> itemConfig = (Map)items.get("item");
         if (itemConfig != null) {
            int slot = this.getIntFromObject(itemConfig.get("slot"), 12);
            ItemStack itemStack;
            if (selectedEntry != null) {
               itemStack = selectedEntry.itemStack.clone();
            } else {
               Material material = selectedMaterial;
               if (itemConfig.containsKey("material")) {
                  String materialName = itemConfig.get("material").toString();
                  if ("%material_selected_item%".equals(materialName)) {
                     material = selectedMaterial;
                  } else {
                     try {
                        Material tmp = Material.valueOf(materialName.toUpperCase());
                        if (tmp != null && tmp.isItem()) {
                           material = tmp;
                        }
                     } catch (IllegalArgumentException var24) {
                     }
                  }
               }

               itemStack = new ItemStack(material, 1);
            }

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
               String displayName = ColorUtil.color((String)itemConfig.get("name"));
               meta.setDisplayName(displayName);
               List<String> lore = new ArrayList();
               if (itemConfig.containsKey("lore")) {
                  for(Object line : (List)itemConfig.get("lore")) {
                     String loreLine = line.toString().replace("%item%", itemName);
                     lore.add(ColorUtil.color(loreLine));
                  }
               }

               meta.setLore(lore);
               itemStack.setItemMeta(meta);
            }

            gui.setItem(slot, itemStack);
         }
      }

      if (items.containsKey("amount")) {
         Map<?, ?> amountConfig = (Map)items.get("amount");
         if (amountConfig != null) {
            int slotx = this.getIntFromObject(amountConfig.get("slot"), 13);
            Map<String, String> amountPlaceholders = new HashMap();
            amountPlaceholders.put("%amount%", String.valueOf(amount));
            amountPlaceholders.put("%amount_formatted%", formattedAmount);
            ItemStack amountItem = this.createNavigationItemWithPlaceholders(amountConfig, "Amount", amountPlaceholders);
            gui.setItem(slotx, amountItem);
         }
      }

      if (items.containsKey("price")) {
         Map<?, ?> priceConfig = (Map)items.get("price");
         if (priceConfig != null) {
            int slotx = this.getIntFromObject(priceConfig.get("slot"), 14);
            Map<String, String> pricePlaceholders = new HashMap();
            pricePlaceholders.put("%price_per_item%", this.orderManager.formatCurrency(pricePerItem));
            pricePlaceholders.put("%price%", this.orderManager.formatCurrency(totalPrice));
            ItemStack priceItem = this.createNavigationItemWithPlaceholders(priceConfig, "Price", pricePlaceholders);
            gui.setItem(slotx, priceItem);
         }
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

   private int getIntFromObject(Object obj, int defaultValue) {
      if (obj instanceof Integer) {
         return (Integer)obj;
      } else if (obj instanceof String) {
         try {
            return Integer.parseInt((String)obj);
         } catch (NumberFormatException var4) {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private ItemStack createNavigationItem(Map<?, ?> config, String defaultName) {
      return this.createNavigationItemWithPlaceholders(config, defaultName, Collections.emptyMap());
   }

   private ItemStack createNavigationItemWithPlaceholders(Map<?, ?> config, String defaultName, Map<String, String> placeholders) {
      Material material = Material.ARROW;
      String displayName = defaultName;
      List<String> lore = new ArrayList();
      if (config.containsKey("material")) {
         try {
            String materialName = config.get("material").toString().toUpperCase();
            Material tmp = Material.valueOf(materialName);
            if (tmp != null && tmp.isItem()) {
               material = tmp;
            }
         } catch (IllegalArgumentException var12) {
         }
      }

      if (config.containsKey("displayname")) {
         displayName = config.get("displayname").toString();
      } else if (config.containsKey("name")) {
         displayName = config.get("name").toString();
      }

      if (config.containsKey("lore")) {
         Object loreObj = config.get("lore");
         if (loreObj instanceof List) {
            for(Object line : (List)loreObj) {
               lore.add(line.toString());
            }
         }
      }

      for(Map.Entry<String, String> entry : placeholders.entrySet()) {
         displayName = displayName.replace((CharSequence)entry.getKey(), (CharSequence)entry.getValue());
         List<String> newLore = new ArrayList();

         for(String line : lore) {
            newLore.add(line.replace((CharSequence)entry.getKey(), (CharSequence)entry.getValue()));
         }

         lore = newLore;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ColorUtil.color(displayName));
         if (!lore.isEmpty()) {
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
      priority = EventPriority.LOW
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         InventoryView view = event.getView();
         String title = view.getTitle();
         String cleanTitle = title.replace("§", "&");
         if (!cleanTitle.contains("Select Item")) {
            String strippedTitle = this.stripColors(title);
            boolean isOrderView = strippedTitle.contains("ᴏʀᴅᴇʀs") && strippedTitle.contains("Page");
            boolean isYourOrders = strippedTitle.contains("Your Orders");
            boolean isNewOrder = strippedTitle.contains("New Order");
            if (isOrderView || isYourOrders || isNewOrder) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  if (!player.hasPermission("order.use")) {
                     player.closeInventory();
                     this.plugin.getLangManager().sendMessage(player, "no-permission");
                     this.plugin.getSoundManager().playSound(player, "error");
                  } else if (isOrderView) {
                     this.orderViewHandler.handleOrderViewClick(event, player);
                  } else if (isYourOrders) {
                     this.handleYourOrdersClick(event, player);
                  } else if (isNewOrder) {
                     this.handleNewOrderClick(event, player);
                  }
               }
            }
         }
      }

   }

   private String stripColors(String text) {
      return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
   }

   private void handleYourOrdersClick(InventoryClickEvent event, Player player) {
      Map<String, Object> items = this.orderManager.getYourOrdersItems();
      int slot = event.getSlot();
      if (slot == 0) {
         this.plugin.getSoundManager().playSound(player, "gui-click");
         this.openNewOrderGUI(player);
      } else if (slot >= 1 && slot <= 26) {
         List<OrderItem> playerOrders = this.orderManager.getOrderItemsByPlayer(player.getUniqueId());
         List<OrderItem> displayOrders = (List)playerOrders.stream().filter((order) -> {
            if (!order.isActive()) {
               return false;
            } else {
               return order.isExpired() ? false : order.getRemainingAmount() > 0 || order.getAvailableToCollect() > 0;
            }
         }).collect(Collectors.toList());
         int orderIndex = slot - 1;
         if (orderIndex < displayOrders.size()) {
            OrderItem clickedOrder = (OrderItem)displayOrders.get(orderIndex);
            if (this.editOrderGUI != null) {
               this.plugin.getSoundManager().playSound(player, "gui-click");
               this.editOrderGUI.openEditOrderGUI(player, clickedOrder);
            }
         }
      }

   }

   private void handleNewOrderClick(InventoryClickEvent event, Player player) {
      Map<String, Object> items = this.orderManager.getNewOrderItems();
      int slot = event.getSlot();
      if (items.containsKey("cancel")) {
         Map<?, ?> cancelConfig = (Map)items.get("cancel");
         if (cancelConfig != null && slot == this.getIntFromObject(cancelConfig.get("slot"), 10)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.openYourOrdersGUI(player);
            return;
         }
      }

      if (items.containsKey("confirm")) {
         Map<?, ?> confirmConfig = (Map)items.get("confirm");
         if (confirmConfig != null && slot == this.getIntFromObject(confirmConfig.get("slot"), 16)) {
            if (!this.orderManager.canCreateNewOrder(player)) {
               int maxOrders = this.orderManager.getMaxOrdersForPlayer(player);
               Map<String, String> placeholders = new HashMap();
               placeholders.put("%max%", String.valueOf(maxOrders));
               this.plugin.getLangManager().sendMessage(player, "max-orders-reached", placeholders);
               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            Map<String, Object> playerData = this.getPlayerNewOrderData(player);
            MaterialsManager.SpecialItemEntry selectedEntry = (MaterialsManager.SpecialItemEntry)playerData.getOrDefault("specialItemEntry", (Object)null);
            if (selectedEntry == null && !playerData.containsKey("material")) {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            if (!playerData.containsKey("amount")) {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            if (!playerData.containsKey("pricePerItem")) {
               this.plugin.getLangManager().sendMessage(player, "error");
               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            int amount = (Integer)playerData.get("amount");
            double pricePerItem = (Double)playerData.get("pricePerItem");
            double totalPrice = (double)amount * pricePerItem;
            if (!this.orderManager.isAmountValid(amount)) {
               int max = this.orderManager.getMaxItemPerOrder();
               int min = this.orderManager.getMinItemPerOrder();
               Map<String, String> placeholders = new HashMap();
               placeholders.put("%min%", String.valueOf(min));
               if (max == -1) {
                  this.plugin.getLangManager().sendMessage(player, "amount-too-low", placeholders);
               } else {
                  placeholders.put("%max%", String.valueOf(max));
                  this.plugin.getLangManager().sendMessage(player, "amount-too-high", placeholders);
               }

               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            if (!this.orderManager.isPriceValid(pricePerItem)) {
               int max = this.orderManager.getMaxPricePerItem();
               int min = this.orderManager.getMinPricePerItem();
               Map<String, String> placeholders = new HashMap();
               placeholders.put("%min%", String.valueOf(min));
               if (max == -1) {
                  this.plugin.getLangManager().sendMessage(player, "price-too-low", placeholders);
               } else {
                  placeholders.put("%max%", String.valueOf(max));
                  this.plugin.getLangManager().sendMessage(player, "price-too-high", placeholders);
               }

               this.plugin.getSoundManager().playSound(player, "error");
               return;
            }

            if (this.orderManager.hasEconomy()) {
               double balance = this.orderManager.getEconomy().getBalance(player);
               if (balance < totalPrice) {
                  this.plugin.getLangManager().sendMessage(player, "no-money");
                  this.plugin.getSoundManager().playSound(player, "error");
                  return;
               }

               this.orderManager.getEconomy().withdrawPlayer(player, totalPrice);
               Map<String, String> withdrawPlaceholders = new HashMap();
               withdrawPlaceholders.put("%amount%", this.orderManager.formatCurrency(totalPrice));
            }

            String materialDisplayName;
            String itemId;
            Material material;
            String itemName;
            if (selectedEntry != null) {
               material = selectedEntry.baseMaterial;
               itemName = selectedEntry.displayName;
               itemId = selectedEntry.getIdentifier();
               materialDisplayName = selectedEntry.displayName;
               Map<String, String> orderPlaceholders = new HashMap();
               orderPlaceholders.put("%amount%", String.valueOf(amount));
               orderPlaceholders.put("%item%", itemName);
               orderPlaceholders.put("%price%", this.orderManager.formatCurrency(pricePerItem));
               orderPlaceholders.put("%total_price%", this.orderManager.formatCurrency(totalPrice));
               this.plugin.getLangManager().sendMessage(player, "order-created", orderPlaceholders);
               if (selectedEntry.type == MaterialsManager.ItemType.ENCHANTED_BOOK) {
                  Map<String, String> typePlaceholders = new HashMap();
                  typePlaceholders.put("%type%", "Book");
                  typePlaceholders.put("%enchant%", selectedEntry.subType);
               } else if (selectedEntry.type == MaterialsManager.ItemType.POTION) {
                  Map<String, String> typePlaceholders = new HashMap();
                  typePlaceholders.put("%type%", "Potion");
                  typePlaceholders.put("%effect%", selectedEntry.subType);
               } else if (selectedEntry.type == MaterialsManager.ItemType.TIPPED_ARROW) {
                  Map<String, String> typePlaceholders = new HashMap();
                  typePlaceholders.put("%type%", "Enchanted Arrow");
                  typePlaceholders.put("%effect%", selectedEntry.subType);
               }
            } else {
               material = (Material)playerData.get("material");
               itemName = this.formatMaterialName(material.name());
               itemId = material.name().toLowerCase();
               materialDisplayName = this.formatMaterialName(material.name());
               Map<String, String> orderPlaceholders = new HashMap();
               orderPlaceholders.put("%amount%", String.valueOf(amount));
               orderPlaceholders.put("%item%", itemName);
               orderPlaceholders.put("%price%", this.orderManager.formatCurrency(pricePerItem));
               orderPlaceholders.put("%total_price%", this.orderManager.formatCurrency(totalPrice));
               this.plugin.getLangManager().sendMessage(player, "order-created", orderPlaceholders);
            }

            OrderItem orderItem;
            if (selectedEntry != null) {
               orderItem = new OrderItem(itemName, Arrays.asList(""), pricePerItem, material, itemId, Collections.emptyList(), player.getUniqueId(), selectedEntry.type.name(), selectedEntry.subType, amount);
            } else {
               orderItem = new OrderItem(itemName, Arrays.asList(""), pricePerItem, material, itemId, Collections.emptyList(), player.getUniqueId(), amount);
            }

            this.orderManager.addOrderItem(orderItem);
            if (this.orderManager.isAnnounceOrders()) {
               String announceMessage = this.orderManager.getAnnounceMessage().replace("%owner%", player.getName()).replace("%requested-amount%", String.valueOf(amount)).replace("%requested-material%", materialDisplayName).replace("%price%", this.orderManager.formatCurrency(pricePerItem));
               String coloredMessage = ColorUtil.color(announceMessage);

               for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  if (onlinePlayer.hasPermission("order.use")) {
                     onlinePlayer.sendMessage(coloredMessage);
                  }
               }

               Bukkit.getConsoleSender().sendMessage(coloredMessage);
            }

            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.newOrderData.remove(player.getUniqueId());
            this.openYourOrdersGUI(player);
            return;
         }
      }

      if (items.containsKey("item")) {
         Map<?, ?> itemConfig = (Map)items.get("item");
         if (itemConfig != null && slot == this.getIntFromObject(itemConfig.get("slot"), 12)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.openListMaterialsGUI(player, 1);
            return;
         }
      }

      if (items.containsKey("amount")) {
         Map<?, ?> amountConfig = (Map)items.get("amount");
         if (amountConfig != null && slot == this.getIntFromObject(amountConfig.get("slot"), 13)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            Map<String, String> placeholders = new HashMap();
            int min = this.orderManager.getMinItemPerOrder();
            int max = this.orderManager.getMaxItemPerOrder();
            placeholders.put("%min%", String.valueOf(min));
            placeholders.put("%max%", max == -1 ? "unlimited" : String.valueOf(max));
            placeholders.put("%type%", "amount");
            this.plugin.getLangManager().sendMessage(player, "chat-input", placeholders);
            player.closeInventory();
            this.plugin.getOrderChatListener().openSignInput(player, "amount");
            return;
         }
      }

      if (items.containsKey("price")) {
         Map<?, ?> priceConfig = (Map)items.get("price");
         if (priceConfig != null && slot == this.getIntFromObject(priceConfig.get("slot"), 14)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            Map<String, String> placeholders = new HashMap();
            int min = this.orderManager.getMinPricePerItem();
            int max = this.orderManager.getMaxPricePerItem();
            placeholders.put("%min%", String.valueOf(min));
            placeholders.put("%max%", max == -1 ? "unlimited" : String.valueOf(max));
            placeholders.put("%type%", "price");
            this.plugin.getLangManager().sendMessage(player, "chat-input", placeholders);
            player.closeInventory();
            this.plugin.getOrderChatListener().openSignInput(player, "price");
            return;
         }
      }

   }

   private void startChatInput(Player player, String type) {
      Map<String, Object> playerData = this.getPlayerNewOrderData(player);
      playerData.put("awaiting_input", type);
      this.newOrderData.put(player.getUniqueId(), playerData);
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      UUID playerId = player.getUniqueId();
      String title = event.getView().getTitle();
      String cleanTitle = title.replace("§", "&");
      if (cleanTitle.contains("Page") || cleanTitle.contains("Select Item") || cleanTitle.contains("Your Orders") || cleanTitle.contains("New Order")) {
         if (this.orderViewHandler.getIgnoreCloseEvents().contains(playerId)) {
            return;
         }

         this.playerPages.remove(playerId);
         this.orderViewHandler.getSearchQuery().remove(playerId);
         this.orderViewHandler.getSignSearchMode().remove(playerId);
         this.orderViewHandler.getIgnoreCloseEvents().remove(playerId);
      }

   }

   public OrderViewHandler getOrderViewHandler() {
      return this.orderViewHandler;
   }
}
