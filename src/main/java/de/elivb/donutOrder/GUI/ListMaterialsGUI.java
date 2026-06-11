package de.elivb.donutOrder.GUI;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.Manager.MaterialsManager;
import de.elivb.donutOrder.utils.ColorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ListMaterialsGUI implements Listener {
   private final Order plugin;
   private final MaterialsManager materialsManager;
   private final OrderGUI orderGUI;
   private final Map<UUID, Integer> playerPages;
   private final Set<UUID> processingClicks;
   private final Set<UUID> ignoreCloseEvents;
   private final Set<UUID> signSearchMode;
   private final Map<UUID, String> searchQuery;
   private final Map<UUID, String> sortMode;
   private final Map<UUID, String> filterMode;
   private final Map<UUID, Location> signLocations;
   private final Map<UUID, Material> signOriginalTypes;
   private final int ITEMS_PER_PAGE = 45;
   private final Set<Material> BLOCK_MATERIALS;
   private final Set<Material> TOOL_MATERIALS;
   private final Set<Material> FOOD_MATERIALS;
   private final Set<Material> COMBAT_MATERIALS;
   private final Set<Material> INGREDIENT_MATERIALS;
   private final Set<Material> UTILITY_MATERIALS;

   public ListMaterialsGUI(Order plugin, MaterialsManager materialsManager, OrderGUI orderGUI) {
      this.BLOCK_MATERIALS = new HashSet(Arrays.asList(Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.COBBLESTONE, Material.BEDROCK, Material.SAND, Material.GRAVEL, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES));
      this.TOOL_MATERIALS = new HashSet(Arrays.asList(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK, Material.LEAD, Material.NAME_TAG));
      this.FOOD_MATERIALS = new HashSet(Arrays.asList(Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.BREAD, Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.BEEF, Material.COOKED_BEEF, Material.CHICKEN, Material.COOKED_CHICKEN, Material.RABBIT, Material.COOKED_RABBIT, Material.MUTTON, Material.COOKED_MUTTON, Material.COD, Material.COOKED_COD, Material.SALMON, Material.COOKED_SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH, Material.CAKE, Material.COOKIE, Material.MELON_SLICE, Material.PUMPKIN_PIE, Material.CARROT, Material.POTATO, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP, Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW, Material.HONEY_BOTTLE));
      this.COMBAT_MATERIALS = new HashSet(Arrays.asList(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, Material.TOTEM_OF_UNDYING, Material.ENDER_PEARL, Material.ENDER_EYE));
      this.INGREDIENT_MATERIALS = new HashSet(Arrays.asList(Material.COAL, Material.CHARCOAL, Material.RAW_IRON, Material.RAW_COPPER, Material.RAW_GOLD, Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT, Material.NETHERITE_INGOT, Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI, Material.REDSTONE, Material.QUARTZ, Material.AMETHYST_SHARD, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS, Material.LEATHER, Material.FEATHER, Material.FLINT, Material.GUNPOWDER, Material.STRING, Material.SLIME_BALL, Material.CLAY_BALL, Material.BRICK, Material.NETHER_BRICK, Material.PAPER, Material.BOOK, Material.SUGAR, Material.STICK, Material.BLAZE_ROD, Material.BLAZE_POWDER, Material.GHAST_TEAR, Material.MAGMA_CREAM, Material.ENDER_PEARL, Material.SHULKER_SHELL, Material.POPPED_CHORUS_FRUIT, Material.PHANTOM_MEMBRANE));
      this.UTILITY_MATERIALS = new HashSet(Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.CRAFTING_TABLE, Material.ANVIL, Material.GRINDSTONE, Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.CAULDRON, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL, Material.RESPAWN_ANCHOR, Material.HOPPER, Material.DISPENSER, Material.DROPPER, Material.OBSERVER, Material.PISTON, Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.MINECART, Material.CHEST_MINECART, Material.FURNACE_MINECART, Material.TNT_MINECART, Material.HOPPER_MINECART));
      this.plugin = plugin;
      this.materialsManager = materialsManager;
      this.orderGUI = orderGUI;
      this.playerPages = new HashMap();
      this.processingClicks = new HashSet();
      this.ignoreCloseEvents = new HashSet();
      this.signSearchMode = new HashSet();
      this.searchQuery = new HashMap();
      this.sortMode = new HashMap();
      this.filterMode = new HashMap();
      this.signLocations = new HashMap();
      this.signOriginalTypes = new HashMap();
   }

   public void openListMaterialsGUI(Player player, int page) {
      this.openListMaterialsGUI(player, page, (String)null);
   }

   public void openListMaterialsGUI(Player player, int page, String search) {
      if (!player.hasPermission("order.use")) {
         this.plugin.getLangManager().sendMessage(player, "no-permission");
         this.plugin.getSoundManager().playSound(player, "error");
         return;
      }
      UUID playerId = player.getUniqueId();
      if (!this.sortMode.containsKey(playerId)) this.sortMode.put(playerId, "alphabetical_az");
      if (!this.filterMode.containsKey(playerId)) this.filterMode.put(playerId, "all");
      this.playerPages.put(playerId, page);
      if (search != null) this.searchQuery.put(playerId, search);
      String title = this.materialsManager.getListMaterialsTitle(page);
      title = ColorUtil.color(title);
      int rows = this.materialsManager.getListMaterialsRows();
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
      this.addNavigationItems(gui, player);
      this.addMaterialItems(gui, player, page, search);
      this.ignoreCloseEvents.add(playerId);
      player.openInventory(gui);
      if (this.plugin.isFolia()) {
         Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.ignoreCloseEvents.remove(playerId), 5L);
      } else {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.ignoreCloseEvents.remove(playerId), 5L);
      }
   }

   private void openSignSearch(Player player) {
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
      sign.getSide(Side.FRONT).setLine(0, "> TÌM KIẾM <");
      sign.getSide(Side.FRONT).setLine(1, ">>");
      sign.getSide(Side.FRONT).setLine(2, "");
      sign.getSide(Side.FRONT).setLine(3, "");
      sign.update(true, false);
      this.signSearchMode.add(playerId);
      this.signLocations.put(playerId, loc);
      this.signOriginalTypes.put(playerId, originalType);
      final Location signLocFinal = loc;
      // Delay nhỏ để block kịp render
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (signLocFinal.getBlock().getState() instanceof Sign freshSign) {
            player.openSign(freshSign, Side.FRONT);
         } else {
            player.openSign(sign, Side.FRONT);
         }
      }, 2L);
      // Xóa block sau 1200 tick (30 giây)
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (signLocFinal.getBlock().getType() == Material.OAK_WALL_SIGN) {
            signLocFinal.getBlock().setType(originalType, false);
         }
         this.signLocations.remove(playerId);
         this.signOriginalTypes.remove(playerId);
      }, 1200L);
   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = (Player) event.getPlayer();
      UUID playerId = player.getUniqueId();
      if (!this.signSearchMode.contains(playerId)) return;
      event.setCancelled(true);
      String text = event.getLine(1);
      if (text == null) text = "";
      text = text.trim();
      if (text.startsWith(">>")) text = text.substring(2).trim();
      // fallback line 0 nếu line 1 trống
      if (text.isEmpty()) {
         String line0 = event.getLine(0);
         if (line0 != null && !line0.trim().isEmpty() && !line0.contains("TÌM KIẾM")) {
            text = line0.trim();
         }
      }
      final String finalSearch = text.isEmpty() ? null : text;
      if (finalSearch != null) this.searchQuery.put(playerId, finalSearch);
      else this.searchQuery.remove(playerId);
      this.plugin.getSoundManager().playSound(player, "gui-click");
      if (this.plugin.isFolia()) {
         player.getScheduler().run(this.plugin, (task) -> this.openListMaterialsGUI(player, 1, finalSearch), null);
      } else {
         Bukkit.getScheduler().runTask(this.plugin, () -> this.openListMaterialsGUI(player, 1, finalSearch));
      }
   }

   private void addNavigationItems(Inventory gui, Player player) {
      Map<String, Object> items = this.materialsManager.getListMaterialsItems();
      UUID playerId = player.getUniqueId();
      if (items.containsKey("previous-page")) {
         int prevSlot = this.getIntFromObject(items.get("previous-page-slot"), 45);
         Map<?, ?> prevConfig = (Map)items.get("previous-page");
         if (prevConfig != null) gui.setItem(prevSlot, this.createNavigationItem(prevConfig, "&#00fc88PREVIOUS"));
      }
      if (items.containsKey("sort")) {
         Map<?, ?> sortConfig = (Map)items.get("sort");
         if (sortConfig != null) {
            int sortSlot = this.getIntFromObject(sortConfig.get("slot"), 48);
            gui.setItem(sortSlot, this.createSortFilterItem(sortConfig, "Sort", (String)this.sortMode.get(playerId)));
         }
      }
      if (items.containsKey("filter")) {
         Map<?, ?> filterConfig = (Map)items.get("filter");
         if (filterConfig != null) {
            int filterSlot = this.getIntFromObject(filterConfig.get("slot"), 49);
            gui.setItem(filterSlot, this.createSortFilterItem(filterConfig, "Filter", (String)this.filterMode.get(playerId)));
         }
      }
      if (items.containsKey("search")) {
         Map<?, ?> searchConfig = (Map)items.get("search");
         if (searchConfig != null) {
            int searchSlot = this.getIntFromObject(searchConfig.get("slot"), 50);
            gui.setItem(searchSlot, this.createNavigationItem(searchConfig, "Search"));
         }
      }
      if (items.containsKey("next-page")) {
         int nextSlot = this.getIntFromObject(items.get("next-page-slot"), 53);
         Map<?, ?> nextConfig = (Map)items.get("next-page");
         if (nextConfig != null) gui.setItem(nextSlot, this.createNavigationItem(nextConfig, "&#00fc88NEXT"));
      }
   }

   private ItemStack createSortFilterItem(Map<?, ?> config, String defaultName, String currentValue) {
      Material material = Material.HOPPER;
      String displayName = defaultName;
      List<String> lore = new ArrayList();
      if (config.containsKey("material")) {
         try { Material tmp = Material.valueOf(config.get("material").toString().toUpperCase()); if (tmp != null && tmp.isItem()) material = tmp; } catch (IllegalArgumentException e) {}
      }
      if (config.containsKey("name")) displayName = config.get("name").toString();
      if (config.containsKey("lore")) { Object lo = config.get("lore"); if (lo instanceof List) for (Object l : (List)lo) lore.add(l.toString()); }
      if (config.containsKey("options") && config.containsKey("keys")) {
         List<?> options = (List)config.get("options");
         List<?> keys = (List)config.get("keys");
         String activePrefix = config.containsKey("active_prefix") ? config.get("active_prefix").toString() : "&#00fc88 ";
         String inactivePrefix = config.containsKey("inactive_prefix") ? config.get("inactive_prefix").toString() : "&f ";
         for (int i = 0; i < options.size() && i < keys.size(); i++)
            lore.add(ColorUtil.color((keys.get(i).toString().equals(currentValue) ? activePrefix : inactivePrefix) + options.get(i)));
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

   private void addMaterialItems(Inventory gui, Player player, int page, String search) {
      List<MaterialsManager.SpecialItemEntry> allItems = this.materialsManager.getAllItems();
      UUID playerId = player.getUniqueId();
      List<MaterialsManager.SpecialItemEntry> filteredItems = allItems;
      if (search != null && !search.isEmpty()) {
         String sl = search.toLowerCase();
         filteredItems = (List)allItems.stream().filter(i -> i.displayName.toLowerCase().contains(sl) || i.baseMaterial.name().toLowerCase().contains(sl) || i.subType != null && i.subType.toLowerCase().contains(sl)).collect(Collectors.toList());
      }
      String currentFilter = (String)this.filterMode.get(playerId);
      if (currentFilter != null && !currentFilter.equals("all")) filteredItems = (List)filteredItems.stream().filter(i -> this.passesFilter(i, currentFilter)).collect(Collectors.toList());
      String currentSort = (String)this.sortMode.get(playerId);
      if (currentSort != null) {
         switch (currentSort) {
            case "alphabetical_az" -> filteredItems.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
            case "alphabetical_za" -> filteredItems.sort((a, b) -> b.displayName.compareToIgnoreCase(a.displayName));
         }
      }
      int start = (page - 1) * 45;
      int end = Math.min(start + 45, filteredItems.size());
      if (start < filteredItems.size() && start >= 0) {
         List<MaterialsManager.SpecialItemEntry> pageItems = filteredItems.subList(start, end);
         for (int i = 0; i < pageItems.size() && i < 45; i++) gui.setItem(i, this.createSpecialItemDisplay(pageItems.get(i)));
      }
   }

   private boolean passesFilter(MaterialsManager.SpecialItemEntry item, String filter) {
      Material m = item.baseMaterial;
      switch (filter) {
         case "blocks" -> { return this.BLOCK_MATERIALS.contains(m) || m.name().endsWith("_BLOCK") || m.name().endsWith("_ORE") || m.name().contains("STONE"); }
         case "tools" -> { return this.TOOL_MATERIALS.contains(m) || m.name().contains("PICKAXE") || m.name().contains("AXE") || m.name().contains("SHOVEL") || m.name().contains("HOE"); }
         case "food" -> { return this.FOOD_MATERIALS.contains(m) || m.name().contains("COOKED") || m.name().endsWith("_STEW") || m == Material.APPLE || m == Material.BREAD; }
         case "combat" -> { return this.COMBAT_MATERIALS.contains(m) || m.name().contains("SWORD") || m.name().contains("HELMET") || m.name().contains("CHESTPLATE") || m.name().contains("LEGGINGS") || m.name().contains("BOOTS") || m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT || m == Material.SHIELD; }
         case "potions" -> { return item.type == MaterialsManager.ItemType.POTION || item.type == MaterialsManager.ItemType.TIPPED_ARROW || m == Material.POTION || m == Material.LINGERING_POTION || m == Material.SPLASH_POTION || m == Material.GLASS_BOTTLE; }
         case "books" -> { return item.type == MaterialsManager.ItemType.ENCHANTED_BOOK || m == Material.BOOK || m == Material.WRITABLE_BOOK || m == Material.WRITTEN_BOOK || m == Material.ENCHANTED_BOOK || m == Material.KNOWLEDGE_BOOK; }
         case "ingredients" -> { return this.INGREDIENT_MATERIALS.contains(m) || m.name().endsWith("_INGOT") || m.name().endsWith("_DUST") || m.name().endsWith("_POWDER"); }
         case "utilities" -> { return this.UTILITY_MATERIALS.contains(m) || m.name().endsWith("_CHEST") || m.name().endsWith("_FURNACE") || m.name().endsWith("_TABLE") || m.name().endsWith("_ANVIL"); }
         default -> { return true; }
      }
   }

   private ItemStack createSpecialItemDisplay(MaterialsManager.SpecialItemEntry entry) {
      ItemStack item = entry.itemStack.clone();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ColorUtil.color("&f" + entry.displayName));
         List<String> lore = new ArrayList();
         Map<String, String> ph = new HashMap();
         ph.put("%id%", entry.getIdentifier());
         ph.put("%price%", String.valueOf(this.materialsManager.getItemPrice(entry)));
         lore.add(this.plugin.getLangManager().getMessageWithoutPrefix("select-material", ph));
         meta.setLore(lore);
         item.setItemMeta(meta);
      }
      return item;
   }

   private ItemStack createNavigationItem(Map<?, ?> config, String defaultName) {
      Material material = Material.ARROW;
      String displayName = defaultName;
      List<String> lore = new ArrayList();
      if (config.containsKey("material")) {
         try { Material tmp = Material.valueOf(config.get("material").toString().toUpperCase()); if (tmp != null && tmp.isItem()) material = tmp; } catch (IllegalArgumentException e) {}
      }
      if (config.containsKey("displayname")) displayName = config.get("displayname").toString();
      else if (config.containsKey("name")) displayName = config.get("name").toString();
      if (config.containsKey("lore")) { Object lo = config.get("lore"); if (lo instanceof List) for (Object l : (List)lo) lore.add(l.toString()); }
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ColorUtil.color(displayName));
         if (!lore.isEmpty()) { List<String> cl = new ArrayList(); for (String l : lore) cl.add(ColorUtil.color(l)); meta.setLore(cl); }
         item.setItemMeta(meta);
      }
      return item;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) return;
      UUID playerId = player.getUniqueId();
      if (this.processingClicks.contains(playerId)) { event.setCancelled(true); return; }
      String title = event.getView().getTitle().replace("\u00a7", "&");
      if (!title.contains("Select Item")) return;
      if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) event.setCancelled(true);
      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) return;
      if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) { event.setCancelled(true); return; }
      this.processingClicks.add(playerId);
      try {
         int slot = event.getSlot();
         int currentPage = this.playerPages.getOrDefault(playerId, 1);
         String currentSearch = this.searchQuery.get(playerId);
         Map<String, Object> items = this.materialsManager.getListMaterialsItems();
         int prevSlot = this.getIntFromObject(items.get("previous-page-slot"), 45);
         int sortSlot = this.getIntFromObject(items.get("sort") != null ? ((Map)items.get("sort")).get("slot") : null, 48);
         int filterSlot = this.getIntFromObject(items.get("filter") != null ? ((Map)items.get("filter")).get("slot") : null, 49);
         int searchSlot = this.getIntFromObject(items.get("search") != null ? ((Map)items.get("search")).get("slot") : null, 50);
         int nextSlot = this.getIntFromObject(items.get("next-page-slot"), 53);
         if (slot == searchSlot) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            player.closeInventory();
            if (this.plugin.isFolia()) {
               Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.openSignSearch(player), 2L);
            } else {
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSignSearch(player), 2L);
            }
            return;
         }
         if (slot == prevSlot) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            if (currentPage <= 1) {
               player.closeInventory();
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.orderGUI.openNewOrderGUI(player), 2L);
               return;
            }
            this.openListMaterialsGUI(player, currentPage - 1, currentSearch);
            return;
         }
         if (slot == nextSlot) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            List<MaterialsManager.SpecialItemEntry> all = this.getFilteredAndSortedItems(playerId, currentSearch);
            int totalPages = Math.max(1, (int)Math.ceil((double)all.size() / 45.0));
            if (currentPage < totalPages) this.openListMaterialsGUI(player, currentPage + 1, currentSearch);
            else this.plugin.getSoundManager().playSound(player, "error");
            return;
         }
         if (slot == sortSlot) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            Map<?, ?> sortConfig = (Map)items.get("sort");
            if (sortConfig != null && sortConfig.containsKey("keys")) {
               List<?> keys = (List)sortConfig.get("keys");
               String cur = (String)this.sortMode.get(playerId);
               int next = 0;
               if (cur != null) for (int i = 0; i < keys.size(); i++) if (keys.get(i).toString().equals(cur)) { next = (i + 1) % keys.size(); break; }
               this.sortMode.put(playerId, keys.get(next).toString());
               this.openListMaterialsGUI(player, 1, currentSearch);
            }
            return;
         }
         if (slot == filterSlot) {
            this.plugin.getSoundManager().playSound(player, "gui-click");
            Map<?, ?> filterConfig = (Map)items.get("filter");
            if (filterConfig != null && filterConfig.containsKey("keys")) {
               List<?> keys = (List)filterConfig.get("keys");
               String cur = (String)this.filterMode.get(playerId);
               int next = 0;
               if (cur != null) for (int i = 0; i < keys.size(); i++) if (keys.get(i).toString().equals(cur)) { next = (i + 1) % keys.size(); break; }
               this.filterMode.put(playerId, keys.get(next).toString());
               this.openListMaterialsGUI(player, 1, currentSearch);
            }
            return;
         }
         if (slot >= 0 && slot < 45) {
            List<MaterialsManager.SpecialItemEntry> all = this.getFilteredAndSortedItems(playerId, currentSearch);
            int index = (currentPage - 1) * 45 + slot;
            if (index >= all.size()) return;
            MaterialsManager.SpecialItemEntry selected = all.get(index);
            this.plugin.getSoundManager().playSound(player, "gui-click");
            this.orderGUI.setSelectedMaterial(player, selected);
            this.searchQuery.remove(playerId);
            this.signSearchMode.remove(playerId);
            player.closeInventory();
            if (this.plugin.isFolia()) {
               Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.orderGUI.openNewOrderGUI(player), 2L);
            } else {
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.orderGUI.openNewOrderGUI(player), 2L);
            }
         }
      } finally {
         if (this.plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, (task) -> this.processingClicks.remove(playerId), 2L);
         } else {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingClicks.remove(playerId), 2L);
         }
      }
   }

   private List<MaterialsManager.SpecialItemEntry> getFilteredAndSortedItems(UUID playerId, String search) {
      List<MaterialsManager.SpecialItemEntry> all = this.materialsManager.getAllItems();
      if (search != null && !search.isEmpty()) {
         String sl = search.toLowerCase();
         all = (List)all.stream().filter(i -> i.displayName.toLowerCase().contains(sl) || i.baseMaterial.name().toLowerCase().contains(sl) || i.subType != null && i.subType.toLowerCase().contains(sl)).collect(Collectors.toList());
      }
      String cf = (String)this.filterMode.get(playerId);
      if (cf != null && !cf.equals("all")) all = (List)all.stream().filter(i -> this.passesFilter(i, cf)).collect(Collectors.toList());
      String cs = (String)this.sortMode.get(playerId);
      if (cs != null) {
         switch (cs) {
            case "alphabetical_az" -> all.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
            case "alphabetical_za" -> all.sort((a, b) -> b.displayName.compareToIgnoreCase(a.displayName));
         }
      }
      return all;
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      UUID playerId = player.getUniqueId();
      String title = event.getView().getTitle().replace("\u00a7", "&");
      if (!title.contains("Select Item")) return;
      if (this.ignoreCloseEvents.contains(playerId)) return;
      this.playerPages.remove(playerId);
      this.processingClicks.remove(playerId);
      this.ignoreCloseEvents.remove(playerId);
      this.searchQuery.remove(playerId);
      this.signSearchMode.remove(playerId);
   }

   private int getIntFromObject(Object obj, int def) {
      if (obj == null) return def;
      try { if (obj instanceof Integer) return (Integer)obj; return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return def; }
   }
}
