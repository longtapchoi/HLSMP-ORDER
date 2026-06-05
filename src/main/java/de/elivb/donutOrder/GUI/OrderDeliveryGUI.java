package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
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

public class OrderDeliveryGUI implements Listener {
private final Order plugin;
private final OrderManager orderManager;
private final OrderDeliveryManager deliveryManager;
private final MaterialsManager materialsManager;
private final Map<UUID, OrderItem> viewingOrders;
private final Set<UUID> processingCloses;

public OrderDeliveryGUI(Order plugin, OrderManager orderManager, OrderDeliveryManager deliveryManager, MaterialsManager materialsManager) {
this.plugin = plugin;
this.orderManager = orderManager;
this.deliveryManager = deliveryManager;
this.materialsManager = materialsManager;
this.viewingOrders = new HashMap();
this.processingCloses = new HashSet();
plugin.getServer().getPluginManager().registerEvents(this, plugin);
}

public void openOrderDelivery(Player player, OrderItem order) {
if (order.getCreator() != null && order.getCreator().equals(player.getUniqueId())) {
this.plugin.getLangManager().sendMessage(player, “cannot-deliver-own”);
this.plugin.getSoundManager().playSound(player, “error”);
if (this.plugin.getOrderGUI() != null) {
this.plugin.getOrderGUI().openOrderGUI(player, 1);
}
} else if (!order.isActive()) {
this.plugin.getLangManager().sendMessage(player, “order-not-available”);
this.plugin.getSoundManager().playSound(player, “error”);
} else if (order.isExpired()) {
this.plugin.getLangManager().sendMessage(player, “expired”);
this.plugin.getSoundManager().playSound(player, “error”);
} else if (order.getRemainingAmount() <= 0) {
this.plugin.getLangManager().sendMessage(player, “already-delivered”);
this.plugin.getSoundManager().playSound(player, “error”);
} else {
this.viewingOrders.put(player.getUniqueId(), order);
String title = ColorUtil.color(”&8ᴏʀᴅᴇʀꜱ -> Delivery”);
Inventory gui = Bukkit.createInventory((InventoryHolder)null, 36, title);
player.openInventory(gui);
Map<String, String> placeholders = new HashMap();
placeholders.put(”%item%”, this.getItemDisplayName(order));
placeholders.put(”%amount%”, String.valueOf(order.getRemainingAmount()));
placeholders.put(”%price%”, this.orderManager.formatCurrency(order.getPricePerItem()));
}

}

@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
if (event.getWhoClicked() instanceof Player) {
Player player = (Player)event.getWhoClicked();
String title = event.getView().getTitle();
if (title.contains(“Delivery”)) {
OrderItem order = (OrderItem)this.viewingOrders.get(player.getUniqueId());
if (order != null) {
Inventory clickedInventory = event.getClickedInventory();
ItemStack currentItem = event.getCurrentItem();
ItemStack cursorItem = event.getCursor();
if (event.isShiftClick()) {
if (clickedInventory != null && clickedInventory.equals(player.getOpenInventory().getBottomInventory()) && currentItem != null && currentItem.getType() != Material.AIR) {
if (!this.isCorrectItem(currentItem, order)) {
Map<String, String> placeholders = new HashMap();
placeholders.put(”%expected%”, this.getItemDisplayName(order));
event.setCancelled(true);
return;
}

```
                 return;
              }

              if (clickedInventory != null && clickedInventory.equals(player.getOpenInventory().getTopInventory())) {
                 return;
              }
           }

           if ((clickedInventory == null || !clickedInventory.equals(player.getOpenInventory().getBottomInventory())) && clickedInventory != null && clickedInventory.equals(player.getOpenInventory().getTopInventory())) {
              if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                 if (!this.isCorrectItem(cursorItem, order)) {
                    Map<String, String> placeholders = new HashMap();
                    placeholders.put("%expected%", this.getItemDisplayName(order));
                    event.setCancelled(true);
                 }
              } else if (currentItem != null && currentItem.getType() == Material.AIR) {
              }
           }
        }
     }
  }
```

}

@EventHandler
public void onInventoryDrag(InventoryDragEvent event) {
if (event.getWhoClicked() instanceof Player) {
Player player = (Player)event.getWhoClicked();
String title = event.getView().getTitle();
if (title.contains(“Delivery”)) {
OrderItem order = (OrderItem)this.viewingOrders.get(player.getUniqueId());
if (order != null) {
ItemStack draggedItem = event.getOldCursor();
Inventory topInventory = event.getView().getTopInventory();
boolean draggingToTop = false;

```
           for(int slot : event.getRawSlots()) {
              if (slot < topInventory.getSize()) {
                 draggingToTop = true;
                 break;
              }
           }

           if (draggingToTop && draggedItem != null && draggedItem.getType() != Material.AIR && !this.isCorrectItem(draggedItem, order)) {
              Map<String, String> placeholders = new HashMap();
              placeholders.put("%expected%", this.getItemDisplayName(order));
              event.setCancelled(true);
           }
        }
     }
  }
```

}

@EventHandler
public void onInventoryClose(InventoryCloseEvent event) {
if (event.getPlayer() instanceof Player) {
Player player = (Player)event.getPlayer();
UUID playerId = player.getUniqueId();
String title = event.getView().getTitle();
if (title.contains(“Delivery”) && !this.processingCloses.contains(playerId)) {
final OrderItem order = (OrderItem)this.viewingOrders.get(playerId);
if (order != null) {
this.processingCloses.add(playerId);

```
           try {
              Inventory gui = event.getView().getTopInventory();
              List<ItemStack> itemsToDeliver = new ArrayList();
              int totalAmount = 0;

              for(int slot = 0; slot < 36; ++slot) {
                 ItemStack item = gui.getItem(slot);
                 if (item != null && item.getType() != Material.AIR) {
                    if (item.getItemMeta() instanceof PotionMeta) {
                    }

                    if (!this.isCorrectItem(item, order)) {
                       Map<String, String> placeholders = new HashMap();
                       placeholders.put("%type%", order.getItemType() != null ? order.getItemType() : "Normal");
                       placeholders.put("%material%", order.getMaterial().name());
                       placeholders.put("%material%", item.getType().name());
                       player.getInventory().addItem(new ItemStack[]{item});

                       for(int j = slot + 1; j < 36; ++j) {
                          ItemStack remainingItem = gui.getItem(j);
                          if (remainingItem != null && remainingItem.getType() != Material.AIR) {
                             player.getInventory().addItem(new ItemStack[]{remainingItem});
                          }
                       }

                       this.viewingOrders.remove(playerId);
                       this.processingCloses.remove(playerId);
                       if (this.plugin.isFolia()) {
                          Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
                             if (player.isOnline() && this.plugin.getOrderGUI() != null) {
                                this.plugin.getOrderGUI().openOrderGUI(player, 1);
                             }

                          }, 2L);
                       } else {
                          Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                             if (player.isOnline() && this.plugin.getOrderGUI() != null) {
                                this.plugin.getOrderGUI().openOrderGUI(player, 1);
                             }

                          }, 2L);
                       }

                       return;
                    }

                    itemsToDeliver.add(item.clone());
                    totalAmount += item.getAmount();
                 }
              }

              if (itemsToDeliver.isEmpty()) {
                 this.viewingOrders.remove(playerId);
                 this.processingCloses.remove(playerId);
                 return;
              }

              final int finalTotalAmount = totalAmount;
              List<ItemStack> finalItemsToDeliver = new ArrayList(itemsToDeliver);
              if (this.plugin.getConfirmDeliveryGUI() == null) {
                 this.plugin.getLangManager().sendMessage(player, "order-not-available");
                 this.plugin.getSoundManager().playSound(player, "error");
                 this.processDirectDelivery(player, order, totalAmount, finalItemsToDeliver);
                 this.processingCloses.remove(playerId);
              } else {
                 for(int slotx = 0; slotx < gui.getSize(); ++slotx) {
                    gui.setItem(slotx, (ItemStack)null);
                 }

                 if (this.plugin.isFolia()) {
                    Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> {
                       if (player.isOnline()) {
                          this.plugin.getConfirmDeliveryGUI().openConfirmDeliveryGUI(player, order, finalTotalAmount, finalItemsToDeliver);
                       }

                       this.processingCloses.remove(playerId);
                    }, 1L);
                 } else {
                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                       if (player.isOnline()) {
                          this.plugin.getConfirmDeliveryGUI().openConfirmDeliveryGUI(player, order, finalTotalAmount, finalItemsToDeliver);
                       }

                       this.processingCloses.remove(playerId);
                    }, 1L);
                 }
              }

              this.viewingOrders.remove(playerId);
           } catch (Exception var14) {
              this.processingCloses.remove(playerId);
              this.viewingOrders.remove(playerId);
              var14.printStackTrace();
           }
        }
     }
  }
```

}

private void processDirectDelivery(Player player, OrderItem order, int totalAmount, List<ItemStack> itemsToDeliver) {
int remainingAmount = order.getRemainingAmount();
if (totalAmount > remainingAmount) {
int excessAmount = totalAmount - remainingAmount;
Map<String, String> tooManyPlaceholders = new HashMap();
tooManyPlaceholders.put(”%total%”, String.valueOf(totalAmount));
tooManyPlaceholders.put(”%remaining%”, String.valueOf(remainingAmount));
Map<String, String> acceptPlaceholders = new HashMap();
acceptPlaceholders.put(”%remaining%”, String.valueOf(remainingAmount));
List<ItemStack> itemsToKeep = new ArrayList();
List<ItemStack> itemsToReturn = new ArrayList();
int amountNeeded = remainingAmount;

```
     for(ItemStack item : itemsToDeliver) {
        if (amountNeeded <= 0) {
           itemsToReturn.add(item);
        } else {
           int itemAmount = item.getAmount();
           if (itemAmount <= amountNeeded) {
              itemsToKeep.add(item);
              amountNeeded -= itemAmount;
           } else {
              ItemStack keepPart = item.clone();
              keepPart.setAmount(amountNeeded);
              itemsToKeep.add(keepPart);
              ItemStack returnPart = item.clone();
              returnPart.setAmount(itemAmount - amountNeeded);
              itemsToReturn.add(returnPart);
              amountNeeded = 0;
           }
        }
     }

     for(ItemStack returnItem : itemsToReturn) {
        player.getInventory().addItem(new ItemStack[]{returnItem});
     }

     OrderDeliveryManager.DeliveryResult result = this.deliveryManager.deliverItems(player, order, remainingAmount, itemsToKeep);
     if (result.isSuccess()) {
        Map<String, String> excessPlaceholders = new HashMap();
        excessPlaceholders.put("%excess%", String.valueOf(excessAmount));
        UUID orderUuid = order.getOrderUuid();
        OrderItem updatedOrder = this.orderManager.getOrderItemByUuid(orderUuid);
        if (updatedOrder != null && updatedOrder.getRemainingAmount() > 0) {
           Map<String, String> neededPlaceholders = new HashMap();
           neededPlaceholders.put("%remaining%", String.valueOf(updatedOrder.getRemainingAmount()));
        } else {
           this.plugin.getLangManager().sendMessage(player, "order-finished");
        }

        this.plugin.getSoundManager().playSound(player, "deliver_success");
     } else {
        player.sendMessage(ColorUtil.color(result.getMessage()));
        this.plugin.getSoundManager().playSound(player, "error");

        for(ItemStack itemx : itemsToDeliver) {
           player.getInventory().addItem(new ItemStack[]{itemx});
        }
     }
  } else {
     OrderDeliveryManager.DeliveryResult result = this.deliveryManager.deliverItems(player, order, totalAmount, itemsToDeliver);
     if (result.isSuccess()) {
        this.plugin.getSoundManager().playSound(player, "deliver_success");
        UUID orderUuid = order.getOrderUuid();
        OrderItem updatedOrder = this.orderManager.getOrderItemByUuid(orderUuid);
        if (updatedOrder != null && updatedOrder.getRemainingAmount() > 0) {
           Map<String, String> neededPlaceholders = new HashMap();
           neededPlaceholders.put("%remaining%", String.valueOf(updatedOrder.getRemainingAmount()));
        } else {
           this.plugin.getLangManager().sendMessage(player, "order-finished");
        }
     } else {
        player.sendMessage(ColorUtil.color(result.getMessage()));
        this.plugin.getSoundManager().playSound(player, "error");

        for(ItemStack itemx : itemsToDeliver) {
           player.getInventory().addItem(new ItemStack[]{itemx});
        }
     }
  }
```

}

private boolean isCorrectItem(ItemStack item, OrderItem order) {
if (item != null && item.getType() != Material.AIR) {
if (!item.getType().isItem()) {
return false;
} else if (item.getType() != order.getMaterial()) {
return false;
} else if (!order.isSpecialItem()) {
return true;
} else if (!this.orderManager.isDeliveringCheckIgnoreLore() && !this.isNameMatching(item, order)) {
return false;
} else {
String itemType = order.getItemType();
String subType = order.getSubType();
if (itemType != null && subType != null) {
switch (itemType.toUpperCase()) {
case “ENCHANTED_BOOK”:
return item.getItemMeta() instanceof EnchantmentStorageMeta;
case “POTION”:
if (!(item.getItemMeta() instanceof PotionMeta)) {
return false;
} else {
boolean orderIsSplash = subType.contains(“SPLASH_”);
boolean orderIsLingering = subType.contains(“LINGERING_”);
if (orderIsSplash && item.getType() != Material.SPLASH_POTION) {
return false;
} else if (orderIsLingering && item.getType() != Material.LINGERING_POTION) {
return false;
} else {
if (!orderIsSplash && !orderIsLingering && item.getType() != Material.POTION) {
return false;
}

```
                       return true;
                    }
                 }
              case "TIPPED_ARROW":
                 return item.getType() == Material.TIPPED_ARROW && item.getItemMeta() instanceof PotionMeta;
              default:
                 return true;
           }
        } else {
           return true;
        }
     }
  } else {
     return false;
  }
```

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
String[] parts = expectedSubType.split(”_”);
int expectedLevel = 1;
String expectedEnchantName;
if (parts.length >= 2) {
try {
expectedLevel = Integer.parseInt(parts[parts.length - 1]);
StringBuilder nameBuilder = new StringBuilder();

```
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
```

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
if (expectedSubType.startsWith(“LONG_”)) {
expectedIsLong = true;
expectedDuration = 9600;
cleanName = expectedSubType.substring(5);
} else if (expectedSubType.startsWith(“STRONG_”)) {
expectedIsStrong = true;
expectedAmplifier = 1;
cleanName = expectedSubType.substring(7);
}

```
     if (cleanName.startsWith("SPLASH_")) {
        expectedIsSplash = true;
        cleanName = cleanName.substring(7);
     } else if (cleanName.startsWith("LINGERING_")) {
        expectedIsLingering = true;
        cleanName = cleanName.substring(10);
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
                    boolean ambientMatches = true;
                    boolean particlesMatches = true;
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
                 }
              }
           } catch (NoSuchMethodError var24) {
           }

           try {
              if (meta.getClass().getMethod("hasBasePotionData") != null) {
                 Object potionData = meta.getClass().getMethod("getBasePotionData").invoke(meta);
                 if (potionData != null) {
                    Object type = potionData.getClass().getMethod("getType").invoke(potionData);
                    boolean upgraded = (Boolean)potionData.getClass().getMethod("isUpgraded").invoke(potionData);
                    boolean extended = (Boolean)potionData.getClass().getMethod("isExtended").invoke(potionData);
                    if (type != null) {
                       String typeName = type.toString();
                       StringBuilder reconstructed = new StringBuilder();
                       if (extended) {
                          reconstructed.append("LONG_");
                       }

                       if (upgraded) {
                          reconstructed.append("STRONG_");
                       }

                       reconstructed.append(typeName);
                       if (reconstructed.toString().equals(originalSubType)) {
                          return true;
                       }
                    }
                 }
              }
           } catch (Exception var23) {
           }

           return false;
        }
     }
  }
```

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
if (expectedSubType.startsWith(“LONG_”)) {
expectedDuration = 9600;
cleanName = expectedSubType.substring(5);
} else if (expectedSubType.startsWith(“STRONG_”)) {
expectedAmplifier = 1;
cleanName = expectedSubType.substring(7);
}

```
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
```

}

private PotionEffectType getPotionEffectType(String name) {
String upperName = name.toUpperCase();
if (upperName.equals(“FIRE_RESISTANCE”)) {
return PotionEffectType.FIRE_RESISTANCE;
} else if (upperName.equals(“STRENGTH”)) {
return PotionEffectType.STRENGTH;
} else if (!upperName.equals(“SPEED”) && !upperName.equals(“SWIFTNESS”)) {
if (upperName.equals(“SLOWNESS”)) {
return PotionEffectType.SLOWNESS;
} else if (upperName.equals(“POISON”)) {
return PotionEffectType.POISON;
} else if (upperName.equals(“REGENERATION”)) {
return PotionEffectType.REGENERATION;
} else if (upperName.equals(“WEAKNESS”)) {
return PotionEffectType.WEAKNESS;
} else if (upperName.equals(“NIGHT_VISION”)) {
return PotionEffectType.NIGHT_VISION;
} else if (upperName.equals(“INVISIBILITY”)) {
return PotionEffectType.INVISIBILITY;
} else if (upperName.equals(“WATER_BREATHING”)) {
return PotionEffectType.WATER_BREATHING;
} else if (!upperName.equals(“HEALING”) && !upperName.equals(“INSTANT_HEALTH”)) {
if (upperName.equals(“HARMING”)) {
return PotionEffectType.INSTANT_DAMAGE;
} else if (!upperName.equals(“JUMP”) && !upperName.equals(“LEAPING”)) {
if (upperName.equals(“LUCK”)) {
return PotionEffectType.LUCK;
} else if (upperName.equals(“SLOW_FALLING”)) {
return PotionEffectType.SLOW_FALLING;
} else if (upperName.equals(“TURTLE_MASTER”)) {
return PotionEffectType.getByName(“TURTLE_MASTER”);
} else if (upperName.equals(“DOLPHINS_GRACE”)) {
return PotionEffectType.getByName(“DOLPHINS_GRACE”);
} else if (upperName.equals(“CONDUIT_POWER”)) {
return PotionEffectType.getByName(“CONDUIT_POWER”);
} else if (upperName.equals(“BAD_OMEN”)) {
return PotionEffectType.getByName(“BAD_OMEN”);
} else if (upperName.equals(“HERO_OF_THE_VILLAGE”)) {
return PotionEffectType.getByName(“HERO_OF_THE_VILLAGE”);
} else if (upperName.equals(“DARKNESS”)) {
return PotionEffectType.getByName(“DARKNESS”);
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
return text == null ? “” : text.replaceAll(”§[0-9a-fk-or]”, “”).replaceAll(”&[0-9a-fk-or]”, “”);
}

private String getItemDisplayName(OrderItem order) {
if (order.isSpecialItem()) {
MaterialsManager.SpecialItemEntry entry = this.materialsManager.getItemByIdentifier(order.getItemId());
if (entry != null) {
return entry.displayName;
}

```
     if (order.getItemType() != null) {
        switch (order.getItemType()) {
           case "ENCHANTED_BOOK" -> {
              return "Book " + order.getSubType();
           }
           case "POTION" -> {
              return "Potion " + order.getSubType();
           }
           case "TIPPED_ARROW" -> {
              return "Enchanted Arrow " + order.getSubType();
           }
        }
     }
  }

  return this.formatMaterialName(order.getMaterial().name());
```

}

private String formatMaterialName(String materialName) {
String[] words = materialName.toLowerCase().split(”_”);
StringBuilder formatted = new StringBuilder();

```
  for(String word : words) {
     if (!word.isEmpty()) {
        formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
     }
  }

  return formatted.toString().trim();
```

}
}
