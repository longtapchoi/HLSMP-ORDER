package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.ConfirmDeliveryManager;
import de.elivb.donutOrder.Manager.MaterialsManager;
import de.elivb.donutOrder.Manager.OrderDeliveryManager;
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
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConfirmDeliveryGUI implements Listener {
   private final Order plugin;
   private final OrderManager orderManager;
   private final OrderDeliveryManager deliveryManager;
   private final MaterialsManager materialsManager;
   private final ConfirmDeliveryManager confirmDeliveryManager;
   private final OrderGUI orderGUI;
   private final Map<UUID, DeliveryData> deliveryDataMap;
   private final Map<UUID, Inventory> openInventories;
   private final Set<UUID> processedDeliveries;

   public ConfirmDeliveryGUI(Order plugin, OrderManager orderManager, OrderDeliveryManager deliveryManager, MaterialsManager materialsManager, ConfirmDeliveryManager confirmDeliveryManager, OrderGUI orderGUI) {
      this.plugin = plugin;
      this.orderManager = orderManager;
      this.deliveryManager = deliveryManager;
      this.materialsManager = materialsManager;
      this.confirmDeliveryManager = confirmDeliveryManager;
      this.orderGUI = orderGUI;
      this.deliveryDataMap = new HashMap();
      this.openInventories = new HashMap();
      this.processedDeliveries = new HashSet();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openConfirmDeliveryGUI(Player player, OrderItem order, int totalAmount, List<ItemStack> items) {
      UUID playerId = player.getUniqueId();
      this.deliveryDataMap.remove(playerId);
      this.openInventories.remove(playerId);
      this.processedDeliveries.remove(playerId);
      this.deliveryDataMap.put(playerId, new DeliveryData(order, items, totalAmount));
      String title = ColorUtil.color(this.confirmDeliveryManager.getConfirmDeliveryTitle());
      int rows = this.confirmDeliveryManager.getConfirmDeliveryRows();
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
      this.openInventories.put(playerId, gui);
      this.addGuiItems(gui, player, order, totalAmount);
      player.openInventory(gui);
      Map<String, String> placeholders = new HashMap();
      placeholders.put("%amount%", String.valueOf(totalAmount));
      placeholders.put("%item%", this.getItemDisplayName(order));
      int remainingAmount = order.getRemainingAmount();
      int acceptedAmount = Math.min(totalAmount, remainingAmount);
      double acceptedReward = order.getPricePerItem() * (double)acceptedAmount;
   }

   private void addGuiItems(Inventory gui, Player player, OrderItem order, int totalAmount) {
      Map<String, Object> items = this.confirmDeliveryManager.getConfirmDeliveryItems();
      Map<String, Object> sourceItemConfig = this.orderManager.getSourceItemConfig();

      for(int i = 0; i < gui.getSize(); ++i) {
         gui.setItem(i, (ItemStack)null);
      }

      if (items.containsKey("cancel")) {
         Map<?, ?> cancelConfig = (Map)items.get("cancel");
         int slot = this.getIntFromObject(cancelConfig.get("slot"), 30);
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
         int slot = this.getIntFromObject(confirmConfig.get("slot"), 32);
         String materialName = (String)confirmConfig.get("material");
         String displayName = (String)confirmConfig.get("name");
         List<String> lore = (List)confirmConfig.get("lore");
         int remainingAmount = order.getRemainingAmount();
         int acceptedAmount = Math.min(totalAmount, remainingAmount);
         double acceptedReward = order.getPricePerItem() * (double)acceptedAmount;
         String formattedAccepted = this.orderManager.formatCurrency(acceptedReward);
         List<String> processedLore = new ArrayList();

         for(String line : lore) {
            String processedLine = line.replace("%price_receive%", formattedAccepted);
            processedLore.add(processedLine);
         }

         Material material = Material.getMaterial(materialName);
         if (material == null || !material.isItem()) {
            material = Material.LIME_STAINED_GLASS_PANE;
         }

         ItemStack confirmItem = this.createItem(material, displayName, processedLore);
         gui.setItem(slot, confirmItem);
      }

      if (items.containsKey("source-item")) {
         Map<?, ?> sourceItem = (Map)items.get("source-item");
         int slot = this.getIntFromObject(sourceItem.get("slot"), 31);
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
         if (title.contains("Confirm Delivery")) {
            DeliveryData data = (DeliveryData)this.deliveryDataMap.get(player.getUniqueId());
            if (data == null) {
               player.closeInventory();
            } else {
               int slot = event.getSlot();
               Map<String, Object> items = this.confirmDeliveryManager.getConfirmDeliveryItems();
               int cancelSlot = -1;
               int confirmSlot = -1;
               if (items.containsKey("cancel")) {
                  cancelSlot = this.getIntFromObject(((Map)items.get("cancel")).get("slot"), 30);
               }

               if (items.containsKey("confirm")) {
                  confirmSlot = this.getIntFromObject(((Map)items.get("confirm")).get("slot"), 32);
               }

               if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
                  event.setCancelled(true);
                  if (slot == cancelSlot) {
                     this.plugin.getSoundManager().playSound(player, "gui-click");
                     this.handleCancel(player, data);
                  } else if (slot == confirmSlot) {
                     this.plugin.getSoundManager().playSound(player, "gui-click");
                     this.handleConfirm(player, data);
                  } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                     this.plugin.getSoundManager().playSound(player, "error");
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
         if (title.contains("Confirm Delivery")) {
            DeliveryData data = (DeliveryData)this.deliveryDataMap.get(player.getUniqueId());
            if (data != null) {
               Inventory topInventory = event.getView().getTopInventory();

               for(int slot : event.getRawSlots()) {
                  if (slot < topInventory.getSize()) {
                     event.setCancelled(true);
                     return;
                  }
               }
            }
         }
      }

   }

   private void handleCancel(Player player, DeliveryData data) {
      UUID playerId = player.getUniqueId();
      if (!this.processedDeliveries.contains(playerId)) {
         this.processedDeliveries.add(playerId);

         for(ItemStack item : data.getItems()) {
            player.getInventory().addItem(new ItemStack[]{item});
         }

         player.closeInventory();
         this.plugin.getLangManager().sendMessage(player, "delivery-cancelled");
         this.deliveryDataMap.remove(playerId);
         this.openInventories.remove(playerId);
         if (this.plugin.getOrderGUI() != null) {
            this.plugin.getOrderGUI().openOrderGUI(player, 1);
         }

         if (this.plugin.isFolia()) {
            player.getScheduler().runDelayed(this.plugin, (task) -> this.processedDeliveries.remove(playerId), (Runnable)null, 20L);
         } else {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processedDeliveries.remove(playerId), 20L);
         }
      }

   }

   private void handleConfirm(Player player, DeliveryData data) {
      UUID playerId = player.getUniqueId();
      if (!this.processedDeliveries.contains(playerId)) {
         this.processedDeliveries.add(playerId);
         OrderItem order = data.getOrder();
         int totalAmount = data.getTotalAmount();
         List<ItemStack> itemsToDeliver = data.getItems();
         this.deliveryDataMap.remove(playerId);

         for(ItemStack item : itemsToDeliver) {
            if (!this.isCorrectItem(item, order)) {
               for(ItemStack returnItem : itemsToDeliver) {
                  player.getInventory().addItem(new ItemStack[]{returnItem});
               }

               this.processedDeliveries.remove(playerId);
               player.closeInventory();
               return;
            }
         }

         OrderItem latestOrder = this.orderManager.getOrderItemByUuid(order.getOrderUuid());
         if (latestOrder != null && latestOrder.isActive() && latestOrder.getRemainingAmount() > 0) {
            int remainingAmount = latestOrder.getRemainingAmount();
            int acceptedAmount = Math.min(totalAmount, remainingAmount);
            data.setAcceptedAmount(acceptedAmount);
            if (totalAmount > remainingAmount) {
               int excessAmount = totalAmount - remainingAmount;
               Map<String, String> tooManyPlaceholders = new HashMap();
               tooManyPlaceholders.put("%total%", String.valueOf(totalAmount));
               tooManyPlaceholders.put("%remaining%", String.valueOf(remainingAmount));
               Map<String, String> acceptPlaceholders = new HashMap();
               acceptPlaceholders.put("%remaining%", String.valueOf(remainingAmount));
               acceptPlaceholders.put("total%", String.valueOf(totalAmount));
               List<ItemStack> itemsToKeep = new ArrayList();
               List<ItemStack> itemsToReturn = new ArrayList();
               int amountNeeded = remainingAmount;

               for(ItemStack itemx : itemsToDeliver) {
                  if (amountNeeded <= 0) {
                     itemsToReturn.add(itemx);
                  } else {
                     int itemAmount = itemx.getAmount();
                     if (itemAmount <= amountNeeded) {
                        itemsToKeep.add(itemx);
                        amountNeeded -= itemAmount;
                     } else {
                        ItemStack keepPart = itemx.clone();
                        keepPart.setAmount(amountNeeded);
                        itemsToKeep.add(keepPart);
                        ItemStack returnPart = itemx.clone();
                        returnPart.setAmount(itemAmount - amountNeeded);
                        itemsToReturn.add(returnPart);
                        amountNeeded = 0;
                     }
                  }
               }

               for(ItemStack returnItem : itemsToReturn) {
                  player.getInventory().addItem(new ItemStack[]{returnItem});
               }

               OrderDeliveryManager.DeliveryResult result = this.deliveryManager.deliverItems(player, latestOrder, remainingAmount, itemsToKeep);
               if (result.isSuccess()) {
                  double acceptedReward = order.getPricePerItem() * (double)acceptedAmount;
                  Map<String, String> excessPlaceholders = new HashMap();
                  excessPlaceholders.put("%excess%", String.valueOf(excessAmount));
                  this.plugin.getSoundManager().playSound(player, "delivery-notify");
                  OrderItem updatedOrder = this.orderManager.getOrderItemByUuid(order.getOrderUuid());
                  if (updatedOrder != null && updatedOrder.getRemainingAmount() > 0) {
                     Map<String, String> neededPlaceholders = new HashMap();
                     neededPlaceholders.put("%remaining%", String.valueOf(updatedOrder.getRemainingAmount()));
                  } else {
                     this.plugin.getLangManager().sendMessage(player, "order-finished");
                  }
               } else {
                  player.sendMessage(ColorUtil.color(result.getMessage()));
                  this.plugin.getSoundManager().playSound(player, "error");

                  for(ItemStack itemxx : itemsToDeliver) {
                     player.getInventory().addItem(new ItemStack[]{itemxx});
                  }
               }

               player.closeInventory();
               this.deliveryDataMap.remove(playerId);
               this.openInventories.remove(playerId);
               if (this.plugin.getOrderGUI() != null) {
                  this.plugin.getOrderGUI().openOrderGUI(player, 1);
               }

               if (this.plugin.isFolia()) {
                  player.getScheduler().runDelayed(this.plugin, (task) -> this.processedDeliveries.remove(playerId), (Runnable)null, 20L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processedDeliveries.remove(playerId), 20L);
               }
            } else {
               OrderDeliveryManager.DeliveryResult resultx = this.deliveryManager.deliverItems(player, latestOrder, totalAmount, itemsToDeliver);
               if (resultx.isSuccess()) {
                  double acceptedReward = order.getPricePerItem() * (double)acceptedAmount;
                  this.plugin.getSoundManager().playSound(player, "delivery-notify");
                  OrderItem updatedOrder = this.orderManager.getOrderItemByUuid(order.getOrderUuid());
                  if (updatedOrder != null && updatedOrder.getRemainingAmount() > 0) {
                     Map<String, String> neededPlaceholders = new HashMap();
                     neededPlaceholders.put("%remaining%", String.valueOf(updatedOrder.getRemainingAmount()));
                  } else {
                     this.plugin.getLangManager().sendMessage(player, "order-finished");
                  }
               } else {
                  player.sendMessage(ColorUtil.color(resultx.getMessage()));
                  this.plugin.getSoundManager().playSound(player, "error");

                  for(ItemStack itemxx : itemsToDeliver) {
                     player.getInventory().addItem(new ItemStack[]{itemxx});
                  }
               }

               player.closeInventory();
               this.deliveryDataMap.remove(playerId);
               this.openInventories.remove(playerId);
               if (this.plugin.getOrderGUI() != null) {
                  this.plugin.getOrderGUI().openOrderGUI(player, 1);
               }

               if (this.plugin.isFolia()) {
                  player.getScheduler().runDelayed(this.plugin, (task) -> this.processedDeliveries.remove(playerId), (Runnable)null, 20L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processedDeliveries.remove(playerId), 20L);
               }
            }
         } else {
            this.plugin.getLangManager().sendMessage(player, "error");
            this.plugin.getSoundManager().playSound(player, "error");
            this.handleCancel(player, data);
            this.processedDeliveries.remove(playerId);
         }
      }

   }

   private boolean isCorrectItem(ItemStack item, OrderItem order) {
      if (item != null && item.getType() != Material.AIR) {
         if (!item.getType().isItem()) {
            return false;
         } else if (item.getType() != order.getMaterial()) {
            return false;
         } else if (!this.orderManager.isDeliveringCheckIgnoreLore() && !this.isNameMatching(item, order)) {
            return false;
         } else if (order.isSpecialItem()) {
            String itemType = order.getItemType();
            String subType = order.getSubType();
            if (itemType != null && subType != null) {
               switch (itemType.toUpperCase()) {
                  case "ENCHANTED_BOOK" -> {
                     return this.isCorrectEnchantedBook(item, subType);
                  }
                  case "POTION" -> {
                     return this.isCorrectPotion(item, subType);
                  }
                  case "TIPPED_ARROW" -> {
                     return this.isCorrectTippedArrow(item, subType);
                  }
                  default -> {
                     return true;
                  }
               }
            } else {
               return false;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean isNameMatching(ItemStack item, OrderItem order) {
      String orderName = this.stripColors(order.getName());
      if (!item.hasItemMeta()) {
         return true;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return true;
         } else if (meta.hasDisplayName()) {
            String itemName = this.stripColors(meta.getDisplayName());
            return itemName.equalsIgnoreCase(orderName);
         } else {
            return true;
         }
      }
   }

   private boolean isCorrectEnchantedBook(ItemStack item, String expectedSubType) {
      if (!(item.getItemMeta() instanceof EnchantmentStorageMeta)) {
         return false;
      } else {
         EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
         Map<Enchantment, Integer> storedEnchants = meta.getStoredEnchants();
         if (storedEnchants.isEmpty()) {
            return false;
         } else {
            String[] parts = expectedSubType.split("_");
            int expectedLevel = 1;
            String expectedEnchantName;
            if (parts.length >= 2) {
               try {
                  expectedLevel = Integer.parseInt(parts[parts.length - 1]);
                  StringBuilder nameBuilder = new StringBuilder();

                  for(int i = 0; i < parts.length - 1; ++i) {
                     if (i > 0) {
                        nameBuilder.append("_");
                     }

                     nameBuilder.append(parts[i]);
                  }

                  expectedEnchantName = nameBuilder.toString();
               } catch (NumberFormatException var12) {
                  expectedEnchantName = expectedSubType;
               }
            } else {
               expectedEnchantName = expectedSubType;
            }

            for(Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
               String enchantKey = ((Enchantment)entry.getKey()).getKey().getKey().toLowerCase();
               String expectedKey = expectedEnchantName.toLowerCase();
               if (enchantKey.equals(expectedKey) || enchantKey.replace("_", "").equals(expectedKey.replace("_", ""))) {
                  return (Integer)entry.getValue() == expectedLevel;
               }
            }

            return false;
         }
      }
   }

   private boolean isCorrectPotion(ItemStack item, String expectedSubType) {
      if (!(item.getItemMeta() instanceof PotionMeta)) {
         return false;
      } else {
         PotionMeta meta = (PotionMeta)item.getItemMeta();
         String originalSubType = expectedSubType;
         boolean isSplash = item.getType() == Material.SPLASH_POTION;
         boolean isLingering = item.getType() == Material.LINGERING_POTION;
         boolean isNormal = item.getType() == Material.POTION;
         String cleanName = expectedSubType;
         boolean expectedIsSplash = false;
         boolean expectedIsLingering = false;
         boolean expectedIsLong = false;
         boolean expectedIsStrong = false;
         int expectedDuration = 3600;
         int expectedAmplifier = 0;
         if (expectedSubType.startsWith("SPLASH_")) {
            expectedIsSplash = true;
            cleanName = expectedSubType.substring(7);
         } else if (expectedSubType.startsWith("LINGERING_")) {
            expectedIsLingering = true;
            cleanName = expectedSubType.substring(10);
         }

         if (cleanName.startsWith("LONG_")) {
            expectedIsLong = true;
            expectedDuration = 9600;
            cleanName = cleanName.substring(5);
         } else if (cleanName.startsWith("STRONG_")) {
            expectedIsStrong = true;
            expectedAmplifier = 1;
            cleanName = cleanName.substring(7);
         }

         if (expectedIsSplash && !isSplash) {
            return false;
         } else if (expectedIsLingering && !isLingering) {
            return false;
         } else if (!expectedIsSplash && !expectedIsLingering && !isNormal) {
            return false;
         } else {
            PotionEffectType expectedEffectType = this.getPotionEffectType(cleanName);
            if (expectedEffectType == null) {
               return false;
            } else {
               if (meta.hasCustomEffects()) {
                  for(PotionEffect effect : meta.getCustomEffects()) {
                     if (effect.getType().equals(expectedEffectType)) {
                        boolean typeMatches = effect.getType().equals(expectedEffectType);
                        boolean durationMatches = effect.getDuration() == expectedDuration;
                        boolean amplifierMatches = effect.getAmplifier() == expectedAmplifier;
                        if (typeMatches && durationMatches && amplifierMatches) {
                           return true;
                        }
                     }
                  }
               }

               try {
                  if (meta.hasBasePotionType()) {
                     Object basePotionType = meta.getBasePotionType();
                     if (basePotionType != null) {
                        String baseName = basePotionType.toString();
                        if (baseName.equals(originalSubType)) {
                           return true;
                        }

                        if (baseName.contains(cleanName)) {
                        }
                     }
                  }
               } catch (NoSuchMethodError var21) {
               }

               return false;
            }
         }
      }
   }

   private boolean isCorrectTippedArrow(ItemStack item, String expectedSubType) {
      if (!(item.getItemMeta() instanceof PotionMeta)) {
         return false;
      } else if (item.getType() != Material.TIPPED_ARROW) {
         return false;
      } else {
         PotionMeta meta = (PotionMeta)item.getItemMeta();
         String cleanName = expectedSubType;
         int expectedDuration = 3600;
         int expectedAmplifier = 0;
         if (expectedSubType.startsWith("LONG_")) {
            expectedDuration = 9600;
            cleanName = expectedSubType.substring(5);
         } else if (expectedSubType.startsWith("STRONG_")) {
            expectedAmplifier = 1;
            cleanName = expectedSubType.substring(7);
         }

         if (cleanName.startsWith("SPLASH_")) {
            cleanName = cleanName.substring(7);
         } else if (cleanName.startsWith("LINGERING_")) {
            cleanName = cleanName.substring(10);
         }

         PotionEffectType expectedEffectType = this.getPotionEffectType(cleanName);
         if (expectedEffectType == null) {
            return false;
         } else {
            if (meta.hasCustomEffects()) {
               for(PotionEffect effect : meta.getCustomEffects()) {
                  if (effect.getType().equals(expectedEffectType)) {
                     boolean typeMatches = effect.getType().equals(expectedEffectType);
                     boolean durationMatches = effect.getDuration() == expectedDuration;
                     boolean amplifierMatches = effect.getAmplifier() == expectedAmplifier;
                     if (typeMatches && durationMatches && amplifierMatches) {
                        return true;
                     }
                  }
               }
            }

            try {
               if (meta.hasBasePotionType()) {
                  Object basePotionType = meta.getBasePotionType();
                  if (basePotionType != null) {
                     String baseName = basePotionType.toString();
                     if (baseName.contains(cleanName)) {
                        return true;
                     }
                  }
               }
            } catch (NoSuchMethodError var13) {
            }

            return false;
         }
      }
   }

   private PotionEffectType getPotionEffectType(String name) {
      String upperName = name.toUpperCase();
      if (upperName.equals("FIRE_RESISTANCE")) {
         return PotionEffectType.FIRE_RESISTANCE;
      } else if (upperName.equals("STRENGTH")) {
         return PotionEffectType.STRENGTH;
      } else if (!upperName.equals("SPEED") && !upperName.equals("SWIFTNESS")) {
         if (upperName.equals("SLOWNESS")) {
            return PotionEffectType.SLOWNESS;
         } else if (upperName.equals("POISON")) {
            return PotionEffectType.POISON;
         } else if (upperName.equals("REGENERATION")) {
            return PotionEffectType.REGENERATION;
         } else if (upperName.equals("WEAKNESS")) {
            return PotionEffectType.WEAKNESS;
         } else if (upperName.equals("NIGHT_VISION")) {
            return PotionEffectType.NIGHT_VISION;
         } else if (upperName.equals("INVISIBILITY")) {
            return PotionEffectType.INVISIBILITY;
         } else if (upperName.equals("WATER_BREATHING")) {
            return PotionEffectType.WATER_BREATHING;
         } else if (!upperName.equals("HEALING") && !upperName.equals("INSTANT_HEALTH")) {
            if (upperName.equals("HARMING")) {
               return PotionEffectType.INSTANT_DAMAGE;
            } else if (!upperName.equals("JUMP") && !upperName.equals("LEAPING")) {
               if (upperName.equals("LUCK")) {
                  return PotionEffectType.LUCK;
               } else if (upperName.equals("SLOW_FALLING")) {
                  return PotionEffectType.SLOW_FALLING;
               } else if (upperName.equals("TURTLE_MASTER")) {
                  return PotionEffectType.getByName("TURTLE_MASTER");
               } else if (upperName.equals("DOLPHINS_GRACE")) {
                  return PotionEffectType.getByName("DOLPHINS_GRACE");
               } else if (upperName.equals("CONDUIT_POWER")) {
                  return PotionEffectType.getByName("CONDUIT_POWER");
               } else if (upperName.equals("BAD_OMEN")) {
                  return PotionEffectType.getByName("BAD_OMEN");
               } else if (upperName.equals("HERO_OF_THE_VILLAGE")) {
                  return PotionEffectType.getByName("HERO_OF_THE_VILLAGE");
               } else if (upperName.equals("DARKNESS")) {
                  return PotionEffectType.getByName("DARKNESS");
               } else {
                  PotionEffectType result = PotionEffectType.getByName(upperName);
                  return result != null ? result : null;
               }
            } else {
               return PotionEffectType.JUMP_BOOST;
            }
         } else {
            return PotionEffectType.INSTANT_HEALTH;
         }
      } else {
         return PotionEffectType.SPEED;
      }
   }

   private String stripColors(String text) {
      return text == null ? "" : text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         UUID playerId = player.getUniqueId();
         String title = event.getView().getTitle();
         if (title.contains("Confirm Delivery")) {
            Inventory closedInventory = event.getInventory();
            Inventory trackedInventory = (Inventory)this.openInventories.get(playerId);
            if (trackedInventory != null && trackedInventory.equals(closedInventory)) {
               if (!this.processedDeliveries.contains(playerId)) {
                  DeliveryData data = (DeliveryData)this.deliveryDataMap.get(playerId);
                  if (data != null) {
                     for(ItemStack item : data.getItems()) {
                        player.getInventory().addItem(new ItemStack[]{item});
                     }

                     this.plugin.getLangManager().sendMessage(player, "delivery-cancelled");
                  }
               }

               this.deliveryDataMap.remove(playerId);
               this.openInventories.remove(playerId);
               this.processedDeliveries.remove(playerId);
            }
         }
      }

   }

   private String getItemDisplayName(OrderItem order) {
      if (order.isSpecialItem()) {
         MaterialsManager.SpecialItemEntry entry = this.materialsManager.getItemByIdentifier(order.getItemId());
         if (entry != null) {
            return entry.displayName;
         }

         if (order.getItemType() != null) {
            switch (order.getItemType()) {
               case "ENCHANTED_BOOK" -> {
                  return "Book " + order.getSubType();
               }
               case "POTION" -> {
                  return "Potion" + order.getSubType();
               }
               case "TIPPED_ARROW" -> {
                  return "Enchanted Arrow " + order.getSubType();
               }
            }
         }
      }

      return this.formatMaterialName(order.getMaterial().name());
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

   private static class DeliveryData {
      private final OrderItem order;
      private final List<ItemStack> items;
      private final int totalAmount;
      private int acceptedAmount;

      public DeliveryData(OrderItem order, List<ItemStack> items, int totalAmount) {
         this.order = order;
         this.items = items;
         this.totalAmount = totalAmount;
         this.acceptedAmount = totalAmount;
      }

      public OrderItem getOrder() {
         return this.order;
      }

      public List<ItemStack> getItems() {
         return this.items;
      }

      public int getTotalAmount() {
         return this.totalAmount;
      }

      public int getAcceptedAmount() {
         return this.acceptedAmount;
      }

      public void setAcceptedAmount(int acceptedAmount) {
         this.acceptedAmount = acceptedAmount;
      }
   }
}
