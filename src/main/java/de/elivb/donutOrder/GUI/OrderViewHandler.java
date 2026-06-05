package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.OrderItem;
import de.elivb.donutOrder.Manager.OrderManager;
import de.elivb.donutOrder.utils.ColorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OrderViewHandler implements Listener {
   private final Order plugin;
   private final OrderManager orderManager;
   private final OrderGUI orderGUI;
   private final Map<UUID, String> orderViewSortMode;
   private final Map<UUID, String> orderViewFilterMode;
   private final Map<UUID, String> orderViewSearchQuery;
   private final Set<UUID> orderViewSignSearchMode;
   private final Set<UUID> orderViewIgnoreCloseEvents;
   private final Map<UUID, Integer> orderViewCurrentPage;
   private final Set<Material> BLOCK_MATERIALS;
   private final Set<Material> TOOL_MATERIALS;
   private final Set<Material> FOOD_MATERIALS;
   private final Set<Material> COMBAT_MATERIALS;
   private final Set<Material> POTION_MATERIALS;
   private final Set<Material> BOOK_MATERIALS;
   private final Set<Material> INGREDIENT_MATERIALS;
   private final Set<Material> UTILITY_MATERIALS;

   public OrderViewHandler(Order plugin, OrderManager orderManager, OrderGUI orderGUI) {
      this.BLOCK_MATERIALS = new HashSet(Arrays.asList(Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.COBBLESTONE, Material.BEDROCK, Material.SAND, Material.GRAVEL, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES));
      this.TOOL_MATERIALS = new HashSet(Arrays.asList(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK, Material.LEAD, Material.NAME_TAG));
      this.FOOD_MATERIALS = new HashSet(Arrays.asList(Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.BREAD, Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.BEEF, Material.COOKED_BEEF, Material.CHICKEN, Material.COOKED_CHICKEN, Material.RABBIT, Material.COOKED_RABBIT, Material.MUTTON, Material.COOKED_MUTTON, Material.COD, Material.COOKED_COD, Material.SALMON, Material.COOKED_SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH, Material.CAKE, Material.COOKIE, Material.MELON_SLICE, Material.PUMPKIN_PIE, Material.CARROT, Material.POTATO, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP, Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW, Material.HONEY_BOTTLE));
      this.COMBAT_MATERIALS = new HashSet(Arrays.asList(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, Material.TOTEM_OF_UNDYING, Material.ENDER_PEARL, Material.ENDER_EYE));
      this.POTION_MATERIALS = new HashSet(Arrays.asList(Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.GLASS_BOTTLE, Material.BREWING_STAND));
      this.BOOK_MATERIALS = new HashSet(Arrays.asList(Material.BOOK, Material.WRITABLE_BOOK, Material.WRITTEN_BOOK, Material.ENCHANTED_BOOK, Material.KNOWLEDGE_BOOK));
      this.INGREDIENT_MATERIALS = new HashSet(Arrays.asList(Material.COAL, Material.CHARCOAL, Material.RAW_IRON, Material.RAW_COPPER, Material.RAW_GOLD, Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT, Material.NETHERITE_INGOT, Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI, Material.REDSTONE, Material.QUARTZ, Material.AMETHYST_SHARD, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS, Material.LEATHER, Material.FEATHER, Material.FLINT, Material.GUNPOWDER, Material.STRING, Material.SLIME_BALL, Material.CLAY_BALL, Material.BRICK, Material.NETHER_BRICK, Material.PAPER, Material.SUGAR, Material.STICK, Material.BLAZE_ROD, Material.BLAZE_POWDER, Material.GHAST_TEAR, Material.MAGMA_CREAM, Material.ENDER_PEARL, Material.SHULKER_SHELL, Material.POPPED_CHORUS_FRUIT, Material.PHANTOM_MEMBRANE));
      this.UTILITY_MATERIALS = new HashSet(Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.CRAFTING_TABLE, Material.ANVIL, Material.GRINDSTONE, Material.ENCHANTING_TABLE, Material.CAULDRON, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL, Material.RESPAWN_ANCHOR, Material.HOPPER, Material.DISPENSER, Material.DROPPER, Material.OBSERVER, Material.PISTON, Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.MINECART, Material.CHEST_MINECART, Material.FURNACE_MINECART, Material.TNT_MINECART, Material.HOPPER_MINECART));
      this.plugin = plugin;
      this.orderManager = orderManager;
      this.orderGUI = orderGUI;
      this.orderViewSortMode = new HashMap();
      this.orderViewFilterMode = new HashMap();
      this.orderViewSearchQuery = new HashMap();
      this.orderViewSignSearchMode = new HashSet();
      this.orderViewIgnoreCloseEvents = new HashSet();
      this.orderViewCurrentPage = new HashMap();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openOrderGUI(Player player, int page, String search) {
      UUID playerId = player.getUniqueId();
      if (!this.orderViewSortMode.containsKey(playerId)) this.orderViewSortMode.put(playerId, "most_recent");
      if (!this.orderViewFilterMode.containsKey(playerId)) this.orderViewFilterMode.put(playerId, "all");
      this.orderViewCurrentPage.put(playerId, page);
      if (search != null) this.orderViewSearchQuery.put(playerId, search);

      String title = this.orderManager.getOrderViewTitle(page);
      title = ColorUtil.color(title);
      int rows = this.orderManager.getOrderViewRows();
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
      this.addOrderViewItems(gui, player, page, search);
      this.orderViewIgnoreCloseEvents.add(playerId);
      player.openInventory(gui);
      if (this.plugin.isFolia()) {
         player.getScheduler().runDelayed(this.plugin, (task) -> this.orderViewIgnoreCloseEvents.remove(playerId), null, 5L);
      } else {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.orderViewIgnoreCloseEvents.remove(playerId), 5L);
      }
   }

   private void openSignSearch(Player player) {
      UUID playerId = player.getUniqueId();
      this.orderViewSignSearchMode.add(playerId);
      org.bukkit.Location loc = player.getLocation().clone();
      loc.setY(loc.getY() - 10);
      Block block = loc.getBlock();
      Material old = block.getType();
      block.setType(Material.OAK_WALL_SIGN, false);
      if (block.getState() instanceof Sign sign) {
         sign.getSide(Side.FRONT).setLine(0, ColorUtil.color("&#00fc88TIM KIEM"));
         sign.getSide(Side.FRONT).setLine(1, "");
         sign.getSide(Side.FRONT).setLine(2, "");
         sign.getSide(Side.FRONT).setLine(3, "");
         sign.update(true);
         player.openSign(sign, Side.FRONT);
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> block.setType(old, false), 2L);
      } else {
         block.setType(old, false);
         this.orderViewSignSearchMode.remove(playerId);
      }
   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = (Player) event.getPlayer();
      UUID playerId = player.getUniqueId();
      if (!this.orderViewSignSearchMode.contains(playerId)) return;
      event.setCancelled(true);
      this.orderViewSignSearchMode.remove(playerId);
      String text = event.getLine(1);
      if (text == null) text = "";
      text = text.trim();
      final String finalSearch = text.isEmpty() ? null : text;
      if (finalSearch != null) this.orderViewSearchQuery.put(playerId, finalSearch);
      else this.orderViewSearchQuery.remove(playerId);
      this.plugin.getSoundManager().playSound(player, "gui-click");
      if (this.plugin.isFolia()) {
         player.getScheduler().run(this.plugin, (task) -> this.openOrderGUI(player, 1, finalSearch), null);
      } else {
         Bukkit.getScheduler().runTask(this.plugin, () -> this.openOrderGUI(player, 1, finalSearch));
      }
   }

   private void addOrderViewItems(Inventory gui, Player player, int currentPage, String search) {
      UUID playerId = player.getUniqueId();
      List<OrderItem> filteredOrders = this.getFilteredAndSortedOrders(player, search);
      int itemsPerPage = 45;
      int totalPages = Math.max(1, (int)Math.ceil((double)filteredOrders.size() / itemsPerPage));
      currentPage = Math.max(1, Math.min(currentPage, totalPages));
      this.orderViewCurrentPage.put(playerId, currentPage);
      int startIndex = (currentPage - 1) * itemsPerPage;
      int endIndex = Math.min(startIndex + itemsPerPage, filteredOrders.size());
      List<OrderItem> pageOrders = filteredOrders.subList(startIndex, endIndex);
      this.addOrderViewNavigationItems(gui, player, currentPage, totalPages);
      Map<String, Object> sourceItemConfig = this.orderManager.getSourceItemConfig();
      for (int i = 0; i < pageOrders.size() && i < 45; i++) {
         ItemStack orderItem = pageOrders.get(i).toItemStack(this.orderManager, sourceItemConfig);
         if (orderItem != null) gui.setItem(i, orderItem);
      }
      for (int ix = pageOrders.size(); ix < 45; ix++) gui.setItem(ix, new ItemStack(Material.AIR));
   }

   private List<OrderItem> getFilteredAndSortedOrders(Player player, String search) {
      UUID playerId = player.getUniqueId();
      List<OrderItem> allOrders = this.orderManager.getOrderItems();
      List<OrderItem> active = (List)allOrders.stream().filter(o -> o.isActive() && !o.isExpired() && o.getRemainingAmount() > 0).collect(Collectors.toList());
      List<OrderItem> filtered = active;
      if (search != null && !search.isEmpty()) {
         String sl = search.toLowerCase();
         filtered = (List)active.stream().filter(o -> o.getName().toLowerCase().contains(sl) || o.getMaterial().name().toLowerCase().contains(sl) || o.isSpecialItem() && o.getSubType() != null && o.getSubType().toLowerCase().contains(sl)).collect(Collectors.toList());
      }
      String cf = (String)this.orderViewFilterMode.get(playerId);
      if (cf != null && !cf.equals("all")) filtered = (List)filtered.stream().filter(o -> this.passesFilter(o, cf)).collect(Collectors.toList());
      String cs = (String)this.orderViewSortMode.get(playerId);
      if (cs != null) {
         switch (cs) {
            case "most_money" -> filtered.sort((a, b) -> Double.compare(b.getPricePerItem() * b.getRequestedAmount(), a.getPricePerItem() * a.getRequestedAmount()));
            case "most_delivered" -> filtered.sort((a, b) -> Integer.compare(b.getDeliveredAmount(), a.getDeliveredAmount()));
            case "most_recent" -> filtered.sort((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()));
            case "most_paid_per_item" -> filtered.sort((a, b) -> Double.compare(b.getPricePerItem(), a.getPricePerItem()));
            default -> filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
         }
      }
      return filtered;
   }

   private void addOrderViewNavigationItems(Inventory gui, Player player, int currentPage, int totalPages) {
      Map<String, Object> items = this.orderManager.getOrderViewItems();
      UUID playerId = player.getUniqueId();
      if (items.containsKey("previous-page")) {
         int prevSlot = this.getIntFromObject(items.get("previous-page-slot"), 45);
         Map<?, ?> prevConfig = (Map)items.get("previous-page");
         if (prevConfig != null) gui.setItem(prevSlot, this.createNavigationItem(prevConfig, "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ"));
      }
      if (items.containsKey("next-page")) {
         int nextSlot = this.getIntFromObject(items.get("next-page-slot"), 53);
         Map<?, ?> nextConfig = (Map)items.get("next-page");
         if (nextConfig != null) gui.setItem(nextSlot, this.createNavigationItem(nextConfig, "&#00fc88ɴᴇxᴛ"));
      }
      if (items.containsKey("refresh")) {
         Map<?, ?> rc = (Map)items.get("refresh");
         if (rc != null) gui.setItem(this.getIntFromObject(rc.get("slot"), 49), this.createNavigationItem(rc, "Refresh"));
      }
      if (items.containsKey("your-orders")) {
         Map<?, ?> yoc = (Map)items.get("your-orders");
         if (yoc != null) gui.setItem(this.getIntFromObject(yoc.get("slot"), 51), this.createNavigationItem(yoc, "Your Orders"));
      }
      if (items.containsKey("sort")) {
         Map<?, ?> sc = (Map)items.get("sort");
         if (sc != null) gui.setItem(this.getIntFromObject(sc.get("slot"), 47), this.createSortFilterItem(sc, "Sort", (String)this.orderViewSortMode.get(playerId)));
      }
      if (items.containsKey("filter")) {
         Map<?, ?> fc = (Map)items.get("filter");
         if (fc != null) gui.setItem(this.getIntFromObject(fc.get("slot"), 48), this.createSortFilterItem(fc, "Filter", (String)this.orderViewFilterMode.get(playerId)));
      }
      if (items.containsKey("search")) {
         Map<?, ?> sec = (Map)items.get("search");
         if (sec != null) gui.setItem(this.getIntFromObject(sec.get("slot"), 50), this.createNavigationItem(sec, "Search"));
      }
   }

   public void handleOrderViewClick(InventoryClickEvent event, Player player) {
      UUID playerId = player.getUniqueId();
      int currentPage = this.orderViewCurrentPage.getOrDefault(playerId, 1);
      int slot = event.getSlot();
      Map<String, Object> items = this.orderManager.getOrderViewItems();
      String currentSearch = this.orderViewSearchQuery.get(playerId);

      if (slot >= 0 && slot < 45) {
         OrderItem clickedOrder = this.getOrderItemAtSlot(player, currentPage, slot);
         if (clickedOrder != null) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            if (player.getUniqueId().equals(clickedOrder.getCreator())) {
               if (this.plugin.getEditOrderGUI() != null) this.plugin.getEditOrderGUI().openEditOrderGUI(player, clickedOrder);
            } else {
               if (this.plugin.getDeliveryGUI() != null) this.plugin.getDeliveryGUI().openOrderDelivery(player, clickedOrder);
               else { this.plugin.getLangManager().sendMessage(player, "error"); this.plugin.getSoundManager().playSound(player, "error"); }
            }
         }
         return;
      }

      if (items == null || items.isEmpty()) return;

      // Sort
      if (items.containsKey("sort")) {
         Map<?, ?> sc = (Map)items.get("sort");
         if (sc != null && slot == this.getIntFromObject(sc.get("slot"), 47) && sc.containsKey("keys")) {
            List<?> keys = (List)sc.get("keys");
            if (keys != null && !keys.isEmpty()) {
               this.plugin.getSoundManager().playSound(player, "gui-click");
               String cur = (String)this.orderViewSortMode.get(playerId);
               int next = 0;
               if (cur != null) for (int i = 0; i < keys.size(); i++) if (keys.get(i).toString().equals(cur)) { next = (i + 1) % keys.size(); break; }
               this.orderViewSortMode.put(playerId, keys.get(next).toString());
               this.openOrderGUI(player, 1, currentSearch);
            }
            return;
         }
      }

      // Filter
      if (items.containsKey("filter")) {
         Map<?, ?> fc = (Map)items.get("filter");
         if (fc != null && slot == this.getIntFromObject(fc.get("slot"), 48) && fc.containsKey("keys")) {
            List<?> keys = (List)fc.get("keys");
            if (keys != null && !keys.isEmpty()) {
               this.plugin.getSoundManager().playSound(player, "gui-click");
               String cur = (String)this.orderViewFilterMode.get(playerId);
               int next = 0;
               if (cur != null) for (int i = 0; i < keys.size(); i++) if (keys.get(i).toString().equals(cur)) { next = (i + 1) % keys.size(); break; }
               this.orderViewFilterMode.put(playerId, keys.get(next).toString());
               this.openOrderGUI(player, 1, currentSearch);
            }
            return;
         }
      }

      // Search - mở bảng gỗ
      if (items.containsKey("search")) {
         Map<?, ?> sec = (Map)items.get("search");
         if (sec != null && slot == this.getIntFromObject(sec.get("slot"), 50)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            player.closeInventory();
            if (this.plugin.isFolia()) {
               Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.openSignSearch(player), 2L);
            } else {
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSignSearch(player), 2L);
            }
            return;
         }
      }

      // Prev page
      if (items.containsKey("previous-page") && slot == this.getIntFromObject(items.get("previous-page-slot"), 45)) {
         if (currentPage > 1) { this.plugin.getSoundManager().playSound(player, "gui-click"); this.openOrderGUI(player, currentPage - 1, currentSearch); }
         else this.plugin.getSoundManager().playSound(player, "error");
         return;
      }

      // Next page
      if (items.containsKey("next-page") && slot == this.getIntFromObject(items.get("next-page-slot"), 53)) {
         List<OrderItem> filtered = this.getFilteredAndSortedOrders(player, currentSearch);
         int totalPages = Math.max(1, (int)Math.ceil((double)filtered.size() / 45.0));
         if (currentPage < totalPages) { this.plugin.getSoundManager().playSound(player, "gui-click"); this.openOrderGUI(player, currentPage + 1, currentSearch); }
         else this.plugin.getSoundManager().playSound(player, "error");
         return;
      }

      // Refresh
      if (items.containsKey("refresh")) {
         Map<?, ?> rc = (Map)items.get("refresh");
         if (rc != null && slot == this.getIntFromObject(rc.get("slot"), 49)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.openOrderGUI(player, 1, currentSearch);
            return;
         }
      }

      // Your orders
      if (items.containsKey("your-orders")) {
         Map<?, ?> yoc = (Map)items.get("your-orders");
         if (yoc != null && slot == this.getIntFromObject(yoc.get("slot"), 51)) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.orderGUI.openYourOrdersGUI(player);
         }
      }
   }

   private OrderItem getOrderItemAtSlot(Player player, int currentPage, int slot) {
      List<OrderItem> filtered = this.getFilteredAndSortedOrders(player, this.orderViewSearchQuery.get(player.getUniqueId()));
      int index = (currentPage - 1) * 45 + slot;
      return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
   }

   private boolean passesFilter(OrderItem order, String filter) {
      Material m = order.getMaterial();
      switch (filter) {
         case "blocks" -> { return this.BLOCK_MATERIALS.contains(m) || m.name().endsWith("_BLOCK") || m.name().endsWith("_ORE") || m.name().contains("STONE"); }
         case "tools" -> { return this.TOOL_MATERIALS.contains(m) || m.name().contains("PICKAXE") || m.name().contains("AXE") || m.name().contains("SHOVEL") || m.name().contains("HOE"); }
         case "food" -> { return this.FOOD_MATERIALS.contains(m) || m.name().contains("COOKED") || m.name().endsWith("_STEW") || m == Material.APPLE || m == Material.BREAD; }
         case "combat" -> { return this.COMBAT_MATERIALS.contains(m) || m.name().contains("SWORD") || m.name().contains("HELMET") || m.name().contains("CHESTPLATE") || m.name().contains("LEGGINGS") || m.name().contains("BOOTS") || m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT || m == Material.SHIELD; }
         case "potions" -> { return this.POTION_MATERIALS.contains(m) || order.isSpecialItem() && "POTION".equals(order.getItemType()) || "TIPPED_ARROW".equals(order.getItemType()); }
         case "books" -> { return this.BOOK_MATERIALS.contains(m) || order.isSpecialItem() && "ENCHANTED_BOOK".equals(order.getItemType()); }
         case "ingredients" -> { return this.INGREDIENT_MATERIALS.contains(m) || m.name().endsWith("_INGOT") || m.name().endsWith("_DUST") || m.name().endsWith("_POWDER"); }
         case "utilities" -> { return this.UTILITY_MATERIALS.contains(m) || m.name().endsWith("_CHEST") || m.name().endsWith("_FURNACE") || m.name().endsWith("_TABLE") || m.name().endsWith("_ANVIL"); }
         default -> { return true; }
      }
   }

   private ItemStack createSortFilterItem(Map<?, ?> config, String defaultName, String currentValue) {
      Material material = Material.HOPPER;
      String displayName = defaultName;
      List<String> lore = new ArrayList();
      if (config.containsKey("material")) { try { Material tmp = Material.valueOf(config.get("material").toString().toUpperCase()); if (tmp != null && tmp.isItem()) material = tmp; } catch (IllegalArgumentException e) {} }
      if (config.containsKey("name")) displayName = config.get("name").toString();
      if (config.containsKey("options") && config.containsKey("keys")) {
         List<?> options = (List)config.get("options");
         List<?> keys = (List)config.get("keys");
         String ap = config.containsKey("active_prefix") ? config.get("active_prefix").toString() : "&#00fc88● ";
         String ip = config.containsKey("inactive_prefix") ? config.get("inactive_prefix").toString() : "&f● ";
         for (int i = 0; i < options.size() && i < keys.size(); i++)
            lore.add(ColorUtil.color((keys.get(i).toString().equals(currentValue) ? ap : ip) + options.get(i)));
      }
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) { meta.setDisplayName(ColorUtil.color(displayName)); if (!lore.isEmpty()) meta.setLore(lore); item.setItemMeta(meta); }
      return item;
   }

   private ItemStack createNavigationItem(Map<?, ?> config, String defaultName) {
      return this.createNavigationItemWithPlaceholders(config, defaultName, Collections.emptyMap());
   }

   private ItemStack createNavigationItemWithPlaceholders(Map<?, ?> config, String defaultName, Map<String, String> placeholders) {
      Material material = Material.ARROW;
      String displayName = defaultName;
      List<String> lore = new ArrayList();
      if (config.containsKey("material")) { try { Material tmp = Material.valueOf(config.get("material").toString().toUpperCase()); if (tmp != null && tmp.isItem()) material = tmp; } catch (IllegalArgumentException e) {} }
      if (config.containsKey("displayname")) displayName = config.get("displayname").toString();
      else if (config.containsKey("name")) displayName = config.get("name").toString();
      if (config.containsKey("lore")) { Object lo = config.get("lore"); if (lo instanceof List) for (Object l : (List)lo) lore.add(l.toString()); }
      for (Map.Entry<String, String> e : placeholders.entrySet()) {
         displayName = displayName.replace(e.getKey(), e.getValue());
         List<String> nl = new ArrayList();
         for (String l : lore) nl.add(l.replace(e.getKey(), e.getValue()));
         lore = nl;
      }
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ColorUtil.color(displayName));
         if (!lore.isEmpty()) { List<String> cl = new ArrayList(); for (String l : lore) cl.add(ColorUtil.color(l)); meta.setLore(cl); }
         item.setItemMeta(meta);
      }
      return item;
   }

   private int getIntFromObject(Object obj, int def) {
      if (obj instanceof Integer) return (Integer)obj;
      if (obj instanceof String) { try { return Integer.parseInt((String)obj); } catch (NumberFormatException e) { return def; } }
      return def;
   }

   public Map<UUID, String> getSearchQuery() { return this.orderViewSearchQuery; }
   public Set<UUID> getSignSearchMode() { return this.orderViewSignSearchMode; }
   public Set<UUID> getIgnoreCloseEvents() { return this.orderViewIgnoreCloseEvents; }
   public Map<UUID, Integer> getCurrentPage() { return this.orderViewCurrentPage; }
}
