package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.CollectItemsManager;
import de.elivb.donutOrder.Manager.MaterialsManager;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class CollectItemsGUI implements Listener {
   private final Order plugin;
   private final CollectItemsManager collectItemsManager;
   private final OrderManager orderManager;
   private final MaterialsManager materialsManager;
   private final OrderGUI orderGUI;
   private final Map<UUID, OrderItem> collectingOrders;
   private final Map<UUID, Integer> playerPages;
   private final Map<UUID, Inventory> openInventories;
   private final Set<UUID> processingClicks;
   private final int ITEMS_PER_PAGE = 45;

   public CollectItemsGUI(Order plugin, CollectItemsManager collectItemsManager, OrderManager orderManager, OrderGUI orderGUI) {
      this.plugin = plugin;
      this.collectItemsManager = collectItemsManager;
      this.orderManager = orderManager;
      this.materialsManager = plugin.getMaterialsManager();
      this.orderGUI = orderGUI;
      this.collectingOrders = new HashMap();
      this.playerPages = new HashMap();
      this.openInventories = new HashMap();
      this.processingClicks = new HashSet();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openCollectItemsGUI(Player player, OrderItem order, int page) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (!player.getUniqueId().equals(order.getCreator())) {
         this.plugin.getLangManager().sendMessage(player, "cannot-deliver-own");
         this.plugin.getSoundManager().playSound(player, "error");
      } else if (order.getAvailableToCollect() <= 0) {
         this.plugin.getLangManager().sendMessage(player, "no-items");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         this.collectingOrders.put(player.getUniqueId(), order);
         this.playerPages.put(player.getUniqueId(), page);
         String title = ColorUtil.color(this.collectItemsManager.getCollectItemsTitle());
         int rows = this.collectItemsManager.getCollectItemsRows();
         Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         this.openInventories.put(player.getUniqueId(), gui);
         this.updateCollectItems(gui, player, order, page);
         player.openInventory(gui);
         if (page == 1) {
            int maxStackSize = this.getMaxStackSize(order);
            Map<String, String> placeholders = new HashMap();
            placeholders.put("%total%", String.valueOf(order.getAvailableToCollect()));
            placeholders.put("%maxstack%", String.valueOf(maxStackSize));
         }
      }

   }

   private void updateCollectItems(Inventory gui, Player player, OrderItem order, int page) {
      Map<String, Object> items = this.collectItemsManager.getCollectItemsItems();
      this.updateNavigationItems(gui, items, page, order);
      this.updateLootItems(gui, order, page);
   }

   private void updateNavigationItems(Inventory gui, Map<String, Object> items, int page, OrderItem order) {
      int totalItems = order.getAvailableToCollect();
      int maxStackSize = this.getMaxStackSize(order);
      int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
      int totalPages = (int)Math.ceil((double)totalStacks / (double)45.0F);
      if (items.containsKey("previous-page")) {
         int prevSlot = this.getIntFromObject(items.get("previous-page-slot"), 45);
         Map<?, ?> prevConfig = (Map)items.get("previous-page");
         if (prevConfig != null) {
            ItemStack prevItem = this.createNavigationItem(prevConfig, "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
            ItemMeta meta = prevItem.getItemMeta();
            if (meta != null) {
               List<String> lore = meta.getLore();
               if (lore == null) {
                  lore = new ArrayList();
               }

               if (page <= 1) {
               }

               meta.setLore(lore);
               prevItem.setItemMeta(meta);
            }

            gui.setItem(prevSlot, prevItem);
         }
      }

      if (items.containsKey("next-page")) {
         int nextSlot = this.getIntFromObject(items.get("next-page-slot"), 53);
         Map<?, ?> nextConfig = (Map)items.get("next-page");
         if (nextConfig != null) {
            ItemStack nextItem = this.createNavigationItem(nextConfig, "&#00fc88ɴᴇxᴛ");
            ItemMeta meta = nextItem.getItemMeta();
            if (meta != null) {
               List<String> lorex = meta.getLore();
               if (lorex == null) {
                  lorex = new ArrayList();
               }

               if (page >= totalPages) {
               }

               meta.setLore(lorex);
               nextItem.setItemMeta(meta);
            }

            gui.setItem(nextSlot, nextItem);
         }
      }

      if (items.containsKey("drop-loot")) {
         Map<?, ?> dropConfig = (Map)items.get("drop-loot");
         if (dropConfig != null) {
            int dropSlot = this.getIntFromObject(dropConfig.get("slot"), 49);
            ItemStack dropItem = this.createNavigationItem(dropConfig, "&#00fc88ᴅʀᴏᴘ ʟᴏᴏᴛ");
            int startStack = (page - 1) * 45 + 1;
            int endStack = Math.min(startStack + 45 - 1, totalStacks);
            int stacksOnPage = endStack - startStack + 1;
            ItemMeta meta = dropItem.getItemMeta();
            if (meta != null) {
               List<String> lorexx = meta.getLore();
               if (lorexx == null) {
                  lorexx = new ArrayList();
               }

               meta.setLore(lorexx);
               dropItem.setItemMeta(meta);
            }

            gui.setItem(dropSlot, dropItem);
         }
      }

   }

   private void updateLootItems(Inventory gui, OrderItem order, int page) {
      int totalItems = order.getAvailableToCollect();
      int maxStackSize = this.getMaxStackSize(order);
      int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
      int stacksPerPage = 45;
      int startStackIndex = (page - 1) * stacksPerPage;
      int endStackIndex = Math.min(startStackIndex + stacksPerPage, totalStacks);

      for(int slot = 0; slot < stacksPerPage; ++slot) {
         gui.setItem(slot, new ItemStack(Material.AIR));
      }

      for(int stackIndex = startStackIndex; stackIndex < endStackIndex; ++stackIndex) {
         int guiSlot = stackIndex - startStackIndex;
         int itemsInThisStack;
         if (stackIndex == totalStacks - 1) {
            int remainingItems = totalItems % maxStackSize;
            itemsInThisStack = remainingItems == 0 ? maxStackSize : remainingItems;
         } else {
            itemsInThisStack = maxStackSize;
         }

         ItemStack itemStack = this.createItemStack(order, itemsInThisStack);
         gui.setItem(guiSlot, itemStack);
      }

   }

   private int getMaxStackSize(OrderItem order) {
      if (order.isSpecialItem()) {
         String itemType = order.getItemType();
         if (itemType != null) {
            switch (itemType.toUpperCase()) {
               case "ENCHANTED_BOOK":
               case "POTION":
                  return 1;
               case "TIPPED_ARROW":
                  return 64;
               case "NORMAL_ITEM":
                  return order.getMaterial().getMaxStackSize();
               default:
                  return 64;
            }
         }
      }

      return order.getMaterial().getMaxStackSize();
   }

   private ItemStack createItemStack(OrderItem order, int amount) {
      if (order.isSpecialItem()) {
         String itemType = order.getItemType();
         String subType = order.getSubType();
         if ("ENCHANTED_BOOK".equalsIgnoreCase(itemType)) {
            return this.createEnchantedBook(subType, amount);
         } else if ("POTION".equalsIgnoreCase(itemType)) {
            return this.createPotion(subType, amount);
         } else if ("TIPPED_ARROW".equalsIgnoreCase(itemType)) {
            return this.createTippedArrow(subType, amount);
         } else {
            return "NORMAL_ITEM".equalsIgnoreCase(itemType) ? new ItemStack(order.getMaterial(), amount) : new ItemStack(order.getMaterial(), amount);
         }
      } else {
         return new ItemStack(order.getMaterial(), amount);
      }
   }

   private ItemStack createEnchantedBook(String subType, int amount) {
      ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
      EnchantmentStorageMeta meta = (EnchantmentStorageMeta)book.getItemMeta();
      if (meta == null) {
         return book;
      } else {
         String[] parts = subType.split("_");
         int level = 1;
         String enchantName;
         if (parts.length >= 2) {
            try {
               level = Integer.parseInt(parts[parts.length - 1]);
               StringBuilder nameBuilder = new StringBuilder();

               for(int i = 0; i < parts.length - 1; ++i) {
                  if (i > 0) {
                     nameBuilder.append("_");
                  }

                  nameBuilder.append(parts[i]);
               }

               enchantName = nameBuilder.toString();
            } catch (NumberFormatException var14) {
               enchantName = subType;
            }
         } else {
            enchantName = subType;
         }

         for(Enchantment enchant : Enchantment.values()) {
            String keyName = enchant.getKey().getKey().toLowerCase();
            String compareName = enchantName.toLowerCase();
            if (keyName.equals(compareName) || keyName.replace("_", "").equals(compareName.replace("_", ""))) {
               meta.addStoredEnchant(enchant, level, true);
               break;
            }
         }

         book.setItemMeta(meta);
         return book;
      }
   }

   private ItemStack createPotion(String subType, int amount) {
      ItemStack potion = new ItemStack(Material.POTION, amount);
      PotionMeta meta = (PotionMeta)potion.getItemMeta();
      if (meta == null) {
         return potion;
      } else {
         String cleanName = subType;
         int duration = 3600;
         int amplifier = 0;
         if (subType.startsWith("LONG_")) {
            duration = 9600;
            cleanName = subType.substring(5);
         } else if (subType.startsWith("STRONG_")) {
            amplifier = 1;
            cleanName = subType.substring(7);
         }

         PotionEffectType effectType = PotionEffectType.getByName(cleanName);
         if (effectType != null) {
            PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true, true);
            meta.addCustomEffect(effect, true);
         }

         potion.setItemMeta(meta);
         return potion;
      }
   }

   private ItemStack createTippedArrow(String subType, int amount) {
      ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, amount);
      PotionMeta meta = (PotionMeta)arrow.getItemMeta();
      if (meta == null) {
         return arrow;
      } else {
         String cleanName = subType;
         int duration = 3600;
         int amplifier = 0;
         if (subType.startsWith("LONG_")) {
            duration = 9600;
            cleanName = subType.substring(5);
         } else if (subType.startsWith("STRONG_")) {
            amplifier = 1;
            cleanName = subType.substring(7);
         }

         PotionEffectType effectType = PotionEffectType.getByName(cleanName);
         if (effectType != null) {
            PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true, true);
            meta.addCustomEffect(effect, true);
         }

         arrow.setItemMeta(meta);
         return arrow;
      }
   }

   private ItemStack createNavigationItem(Map<?, ?> config, String defaultName) {
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
         } catch (IllegalArgumentException var11) {
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
         UUID playerId = player.getUniqueId();
         if (this.processingClicks.contains(playerId)) {
            event.setCancelled(true);
         } else {
            String title = event.getView().getTitle();
            String strippedTitle = title.replace("§", "");
            if (strippedTitle.contains("Collect Items")) {
               if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                  event.setCancelled(true);
                  if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                     OrderItem order = (OrderItem)this.collectingOrders.get(playerId);
                     if (order == null) {
                        player.closeInventory();
                     } else {
                        this.processingClicks.add(playerId);

                        try {
                           int slot = event.getSlot();
                           int currentPage = (Integer)this.playerPages.getOrDefault(playerId, 1);
                           Map<String, Object> items = this.collectItemsManager.getCollectItemsItems();
                           int prevSlot = this.getIntFromObject(items.get("previous-page-slot"), 45);
                           int nextSlot = this.getIntFromObject(items.get("next-page-slot"), 53);
                           int dropSlot = this.getIntFromObject(items.get("drop-loot") != null ? ((Map)items.get("drop-loot")).get("slot") : null, 49);
                           if (slot != prevSlot) {
                              if (slot == nextSlot) {
                                 int totalItems = order.getAvailableToCollect();
                                 int maxStackSize = this.getMaxStackSize(order);
                                 int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
                                 int totalPages = (int)Math.ceil((double)totalStacks / (double)45.0F);
                                 if (currentPage < totalPages) {
                                    this.plugin.getSoundManager().playSound(player, "gui-click");
                                    this.playerPages.put(playerId, currentPage + 1);
                                    Inventory gui = (Inventory)this.openInventories.get(playerId);
                                    if (gui != null) {
                                       this.updateCollectItems(gui, player, order, currentPage + 1);
                                       return;
                                    }
                                 } else {
                                    this.plugin.getSoundManager().playSound(player, "error");
                                 }

                                 return;
                              }

                              if (slot == dropSlot) {
                                 this.plugin.getSoundManager().playSound(player, "gui-click");
                                 this.dropPageStacks(player, order, currentPage);
                                 return;
                              }

                              if (slot >= 0 && slot < 45) {
                                 this.plugin.getSoundManager().playSound(player, "deliver_success");
                                 this.collectStack(player, order, currentPage, slot);
                              }

                              return;
                           }

                           int totalItems = order.getAvailableToCollect();
                           int maxStackSize = this.getMaxStackSize(order);
                           int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
                           int totalPages = (int)Math.ceil((double)totalStacks / (double)45.0F);
                           if (currentPage <= 1) {
                              this.plugin.getSoundManager().playSound(player, "error");
                              return;
                           }

                           this.plugin.getSoundManager().playSound(player, "gui-click");
                           this.playerPages.put(playerId, currentPage - 1);
                           Inventory gui = (Inventory)this.openInventories.get(playerId);
                           if (gui == null) {
                              return;
                           }

                           this.updateCollectItems(gui, player, order, currentPage - 1);
                        } finally {
                           if (this.plugin.isFolia()) {
                              Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.processingClicks.remove(playerId), 2L);
                           } else {
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingClicks.remove(playerId), 2L);
                           }

                        }

                        return;
                     }
                  }
               } else if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                  event.setCancelled(true);
               }
            }
         }
      }

   }

   private void collectStack(Player player, OrderItem order, int page, int slot) {
      int totalItems = order.getAvailableToCollect();
      int maxStackSize = this.getMaxStackSize(order);
      int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
      int stacksPerPage = 45;
      int stackIndex = (page - 1) * stacksPerPage + slot;
      if (stackIndex >= totalStacks) {
         this.plugin.getLangManager().sendMessage(player, "error");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         int itemsInStack;
         if (stackIndex == totalStacks - 1) {
            int remainingItems = totalItems % maxStackSize;
            itemsInStack = remainingItems == 0 ? maxStackSize : remainingItems;
         } else {
            itemsInStack = maxStackSize;
         }

         int freeSlots = 0;

         for(ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
               ++freeSlots;
            }
         }

         if (freeSlots < 1) {
            this.plugin.getLangManager().sendMessage(player, "inventory-full");
            this.plugin.getSoundManager().playSound(player, "error");
         } else {
            ItemStack itemToGive = this.createItemStack(order, itemsInStack);
            player.getInventory().addItem(new ItemStack[]{itemToGive});
            order.addCollectedAmount(itemsInStack);
            this.orderManager.updateOrderItem(order);
            String itemName = this.formatMaterialName(order.getMaterial().name());
            Map<String, String> placeholders = new HashMap();
            placeholders.put("%amount%", String.valueOf(itemsInStack));
            placeholders.put("%item%", itemName);
            this.plugin.getLangManager().sendMessage(player, "items-collected", placeholders);
            if (order.getAvailableToCollect() <= 0) {
               player.closeInventory();
               if (order.getRemainingAmount() == 0) {
                  order.setActive(false);
                  this.orderManager.updateOrderItem(order);
               }

               if (this.plugin.isFolia()) {
                  Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
                     if (player.isOnline() && this.orderGUI != null) {
                        this.orderGUI.openYourOrdersGUI(player);
                     }

                  }, 2L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                     if (player.isOnline() && this.orderGUI != null) {
                        this.orderGUI.openYourOrdersGUI(player);
                     }

                  }, 2L);
               }
            } else {
               int newPage = page;
               int remainingStacks = (int)Math.ceil((double)order.getAvailableToCollect() / (double)maxStackSize);
               int startStackThisPage = (page - 1) * stacksPerPage;
               if (startStackThisPage >= remainingStacks && page > 1) {
                  newPage = page - 1;
               }

               if (newPage != page) {
                  this.playerPages.put(player.getUniqueId(), newPage);
               }

               Inventory gui = (Inventory)this.openInventories.get(player.getUniqueId());
               if (gui != null) {
                  this.updateCollectItems(gui, player, order, newPage);
               }
            }
         }
      }

   }

   private void dropPageStacks(Player player, OrderItem order, int page) {
      int totalItems = order.getAvailableToCollect();
      int maxStackSize = this.getMaxStackSize(order);
      int totalStacks = (int)Math.ceil((double)totalItems / (double)maxStackSize);
      int stacksPerPage = 45;
      int startStackIndex = (page - 1) * stacksPerPage;
      int endStackIndex = Math.min(startStackIndex + stacksPerPage, totalStacks);
      int stacksOnPage = endStackIndex - startStackIndex;
      if (stacksOnPage <= 0) {
         this.plugin.getLangManager().sendMessage(player, "error");
         this.plugin.getSoundManager().playSound(player, "error");
      } else {
         int itemsOnPage = 0;

         for(int i = startStackIndex; i < endStackIndex; ++i) {
            if (i == totalStacks - 1) {
               int remainingItems = totalItems % maxStackSize;
               itemsOnPage += remainingItems == 0 ? maxStackSize : remainingItems;
            } else {
               itemsOnPage += maxStackSize;
            }
         }

         Location dropLocation = player.getLocation();
         Vector smallPush = player.getLocation().getDirection().normalize().multiply(0.3);
         smallPush.setY(0.1);

         for(int ix = startStackIndex; ix < endStackIndex; ++ix) {
            int itemsInStack;
            if (ix == totalStacks - 1) {
               int remainingItems = totalItems % maxStackSize;
               itemsInStack = remainingItems == 0 ? maxStackSize : remainingItems;
            } else {
               itemsInStack = maxStackSize;
            }

            ItemStack dropStack = this.createItemStack(order, itemsInStack);
            player.getWorld().dropItem(dropLocation, dropStack).setVelocity(smallPush);
         }

         order.addCollectedAmount(itemsOnPage);
         this.orderManager.updateOrderItem(order);
         String itemName = this.formatMaterialName(order.getMaterial().name());
         Map<String, String> placeholders = new HashMap();
         placeholders.put("%amount%", String.valueOf(itemsOnPage));
         placeholders.put("%item%", itemName);
         this.plugin.getLangManager().sendMessage(player, "items-collected", placeholders);
         if (order.getAvailableToCollect() <= 0) {
            player.closeInventory();
            if (order.getRemainingAmount() == 0) {
               order.setActive(false);
               this.orderManager.updateOrderItem(order);
            }

            if (this.plugin.isFolia()) {
               Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
                  if (player.isOnline() && this.orderGUI != null) {
                     this.orderGUI.openYourOrdersGUI(player);
                  }

               }, 2L);
            } else {
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  if (player.isOnline() && this.orderGUI != null) {
                     this.orderGUI.openYourOrdersGUI(player);
                  }

               }, 2L);
            }
         } else {
            int newPage = page;
            int remainingStacks = (int)Math.ceil((double)order.getAvailableToCollect() / (double)maxStackSize);
            int startStackThisPage = (page - 1) * stacksPerPage;
            if (startStackThisPage >= remainingStacks && page > 1) {
               newPage = page - 1;
            }

            if (newPage != page) {
               this.playerPages.put(player.getUniqueId(), newPage);
            }

            Inventory gui = (Inventory)this.openInventories.get(player.getUniqueId());
            if (gui != null) {
               this.updateCollectItems(gui, player, order, newPage);
            }
         }
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      String title = event.getView().getTitle();
      String strippedTitle = title.replace("§", "");
      if (strippedTitle.contains("Collect Items")) {
         this.collectingOrders.remove(player.getUniqueId());
         this.playerPages.remove(player.getUniqueId());
         this.openInventories.remove(player.getUniqueId());
         this.processingClicks.remove(player.getUniqueId());
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
