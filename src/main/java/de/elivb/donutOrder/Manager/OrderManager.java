package de.elivb.donutOrder.Manager;

import de.elivb.donutOrder.Order;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class OrderManager {
   private final Order plugin;
   private final DataManager dataManager;
   private final Map<UUID, OrderItem> orderItemsMap;
   private final Map<String, List<UUID>> ordersByItemId;
   private Economy economy = null;
   private boolean vaultEnabled = false;
   private String currencyFormat = "#,##0.##";
   private boolean abbreviationsEnabled = true;
   private List<String> abbreviationFormats = Arrays.asList("k", "m", "b", "t");
   private int minPricePerItem = 1;
   private int maxPricePerItem = -1;
   private int minItemPerOrder = 1;
   private int maxItemPerOrder = -1;
   private boolean deliveringCheckIgnoreLore = false;
   private boolean announceOrders = true;
   private String announceMessage = "&#00fc88Orders &7» &a%owner% &7 has created an order for &6%requested-amount% %requested-material%&7 at &#00fc88%price%&7 each!";
   private TimeChecker timeChecker;
   private FileConfiguration orderViewConfig;
   private FileConfiguration yourOrdersConfig;
   private FileConfiguration newOrderConfig;
   private File orderViewFile;
   private File yourOrdersFile;
   private File newOrderFile;

   public OrderManager(Order plugin, DataManager dataManager) {
      this.plugin = plugin;
      this.dataManager = dataManager;
      this.orderItemsMap = new ConcurrentHashMap();
      this.ordersByItemId = new HashMap();
      this.loadConfig();
      this.loadEconomy();
      this.loadGUIConfigs();
      this.loadOrdersFromDatabase();
      this.timeChecker = new TimeChecker(plugin, this);
      this.timeChecker.start();
   }

   public int getMaxOrdersForPlayer(Player player) {
      int maxOrders = 1;

      for(int i = 44; i >= 1; --i) {
         if (player.hasPermission("order.limit." + i)) {
            maxOrders = i;
            break;
         }
      }

      return maxOrders;
   }

   public boolean canCreateNewOrder(Player player) {
      List<OrderItem> playerOrders = this.getOrderItemsByPlayer(player.getUniqueId());
      long activeOrders = playerOrders.stream().filter((order) -> order.isActive() && !order.isExpired()).count();
      int maxOrders = this.getMaxOrdersForPlayer(player);
      return activeOrders < (long)maxOrders;
   }

   private void loadOrdersFromDatabase() {
      this.orderItemsMap.clear();
      this.ordersByItemId.clear();

      for(OrderItem order : this.dataManager.loadAllOrders()) {
         UUID orderUuid = order.getOrderUuid();
         if (orderUuid != null) {
            this.orderItemsMap.put(orderUuid, order);
            String itemId = order.getItemId();
            ((List)this.ordersByItemId.computeIfAbsent(itemId, (k) -> new ArrayList())).add(orderUuid);
         }
      }

   }

   private void loadConfig() {
      FileConfiguration config = this.plugin.getConfig();
      this.currencyFormat = config.getString("economy.currency-format", "#,##0.##");
      this.minPricePerItem = config.getInt("minimum-price-per-item", 1);
      this.maxPricePerItem = config.getInt("maximum-price-per-item", -1);
      this.minItemPerOrder = config.getInt("minimum-item-per-order", 1);
      this.maxItemPerOrder = config.getInt("maximum-item-per-order", -1);
      this.deliveringCheckIgnoreLore = config.getBoolean("delivering-check-ignore-lore", false);
      this.announceOrders = config.getBoolean("announce-orders", true);
      this.announceMessage = config.getString("announce-message", "&#00fc88Orders &7» &a%owner% &7 has created an order for &6%requested-amount% %requested-material%&7 at &#00fc88%price%&7 each!");
      if (config.contains("economy.abbreviations")) {
         ConfigurationSection abbrevSection = config.getConfigurationSection("economy.abbreviations");
         if (abbrevSection != null) {
            this.abbreviationsEnabled = abbrevSection.getBoolean("enabled", true);
            this.abbreviationFormats = abbrevSection.getStringList("formats");
            if (this.abbreviationFormats.isEmpty()) {
               this.abbreviationFormats = Arrays.asList("k", "m", "b", "t");
            }
         }
      }

   }

   private void loadEconomy() {
      if (this.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
         this.vaultEnabled = false;
         this.plugin.getLogger().info("Vault not found, economy features will be disabled.");
      } else {
         RegisteredServiceProvider<Economy> rsp = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            this.vaultEnabled = false;
         } else {
            this.economy = (Economy)rsp.getProvider();
            this.vaultEnabled = true;
         }
      }

   }

   private void loadGUIConfigs() {
      File guiFolder = new File(this.plugin.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      this.orderViewFile = new File(guiFolder, "order-view.yml");
      if (!this.orderViewFile.exists()) {
         this.plugin.saveResource("gui/order-view.yml", false);
      }

      this.orderViewConfig = YamlConfiguration.loadConfiguration(this.orderViewFile);
      this.yourOrdersFile = new File(guiFolder, "your-orders.yml");
      if (!this.yourOrdersFile.exists()) {
         this.plugin.saveResource("gui/your-orders.yml", false);
      }

      this.yourOrdersConfig = YamlConfiguration.loadConfiguration(this.yourOrdersFile);
      this.newOrderFile = new File(guiFolder, "new-order.yml");
      if (!this.newOrderFile.exists()) {
         this.plugin.saveResource("gui/new-order.yml", false);
      }

      this.newOrderConfig = YamlConfiguration.loadConfiguration(this.newOrderFile);
   }

   public boolean hasEconomy() {
      return this.vaultEnabled && this.economy != null;
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public String formatCurrency(double amount) {
      try {
         if (this.abbreviationsEnabled && Math.abs(amount) >= (double)1000.0F) {
            String formatted = this.formatWithAbbreviations(amount);
            if (formatted != null) {
               return formatted;
            }
         }

         DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
         symbols.setDecimalSeparator(',');
         symbols.setGroupingSeparator('.');
         DecimalFormat formatter = new DecimalFormat(this.currencyFormat, symbols);
         return formatter.format(amount);
      } catch (Exception var5) {
         return String.valueOf(amount);
      }
   }

   private String formatWithAbbreviations(double amount) {
      boolean negative = amount < (double)0.0F;
      double absAmount = Math.abs(amount);
      String[] suffixes = (String[])this.abbreviationFormats.toArray(new String[0]);
      double[] values = new double[]{(double)1000.0F, (double)1000000.0F, (double)1.0E9F, 1.0E12};

      for(int i = 0; i < Math.min(suffixes.length, 4); ++i) {
         if (absAmount < values[i] * (double)1000.0F || i == suffixes.length - 1) {
            double formatted = absAmount / values[i];
            return String.format("%.2f%s", negative ? -formatted : formatted, suffixes[i]);
         }
      }

      return null;
   }

   public int getMinPricePerItem() {
      return this.minPricePerItem;
   }

   public int getMaxPricePerItem() {
      return this.maxPricePerItem;
   }

   public int getMinItemPerOrder() {
      return this.minItemPerOrder;
   }

   public int getMaxItemPerOrder() {
      return this.maxItemPerOrder;
   }

   public boolean isDeliveringCheckIgnoreLore() {
      return this.deliveringCheckIgnoreLore;
   }

   public boolean isAnnounceOrders() {
      return this.announceOrders;
   }

   public String getAnnounceMessage() {
      return this.announceMessage;
   }

   public boolean isPriceValid(double price) {
      return price < (double)this.minPricePerItem ? false : this.maxPricePerItem == -1 || !(price > (double)this.maxPricePerItem);
   }

   public boolean isAmountValid(int amount) {
      return amount < this.minItemPerOrder ? false : this.maxItemPerOrder == -1 || amount <= this.maxItemPerOrder;
   }

   public List<OrderItem> getOrderItems() {
      return new ArrayList(this.orderItemsMap.values());
   }

   public void addOrderItem(OrderItem item) {
      this.dataManager.saveOrder(item);
      UUID orderUuid = item.getOrderUuid();
      if (orderUuid != null) {
         this.orderItemsMap.put(orderUuid, item);
         ((List)this.ordersByItemId.computeIfAbsent(item.getItemId(), (k) -> new ArrayList())).add(orderUuid);
      }

   }

   public void updateOrderItem(OrderItem item) {
      UUID orderUuid = item.getOrderUuid();
      if (orderUuid != null && this.orderItemsMap.containsKey(orderUuid)) {
         this.dataManager.updateOrder(item);
         this.orderItemsMap.put(orderUuid, item);
      }

   }

   public boolean removeOrderItem(UUID orderUuid) {
      if (!this.orderItemsMap.containsKey(orderUuid)) {
         return false;
      } else {
         OrderItem item = (OrderItem)this.orderItemsMap.get(orderUuid);
         String itemId = item.getItemId();
         this.dataManager.deleteOrderByUuid(orderUuid);
         this.orderItemsMap.remove(orderUuid);
         List<UUID> idsForItem = (List)this.ordersByItemId.get(itemId);
         if (idsForItem != null) {
            idsForItem.remove(orderUuid);
            if (idsForItem.isEmpty()) {
               this.ordersByItemId.remove(itemId);
            }
         }

         return true;
      }
   }

   public OrderItem getOrderItemByUuid(UUID orderUuid) {
      return (OrderItem)this.orderItemsMap.get(orderUuid);
   }

   public List<OrderItem> getOrderItemsByItemId(String itemId) {
      List<UUID> uuids = (List)this.ordersByItemId.get(itemId);
      if (uuids != null && !uuids.isEmpty()) {
         List<OrderItem> result = new ArrayList();

         for(UUID uuid : uuids) {
            OrderItem item = (OrderItem)this.orderItemsMap.get(uuid);
            if (item != null) {
               result.add(item);
            }
         }

         return result;
      } else {
         return new ArrayList();
      }
   }

   public OrderItem getLatestOrderByItemId(String itemId) {
      List<UUID> uuids = (List)this.ordersByItemId.get(itemId);
      if (uuids != null && !uuids.isEmpty()) {
         OrderItem latest = null;

         for(UUID uuid : uuids) {
            OrderItem item = (OrderItem)this.orderItemsMap.get(uuid);
            if (item != null && (latest == null || item.getCreationDate().after(latest.getCreationDate()))) {
               latest = item;
            }
         }

         return latest;
      } else {
         return null;
      }
   }

   public List<OrderItem> getOrderItemsByPlayer(UUID playerId) {
      List<OrderItem> playerItems = new ArrayList();

      for(OrderItem item : this.orderItemsMap.values()) {
         if (item.getCreator() != null && item.getCreator().equals(playerId) && item.isActive() && !item.isExpired() && (item.getRemainingAmount() > 0 || item.getAvailableToCollect() > 0)) {
            playerItems.add(item);
         }
      }

      playerItems.sort((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()));
      return playerItems;
   }

   public String getOrderViewTitle(int page) {
      String title = this.orderViewConfig.getString("order-items.title", "&8ᴏʀᴅᴇʀs (Page %page%)");
      return title.replace("%page%", String.valueOf(page));
   }

   public String getYourOrdersTitle() {
      return this.yourOrdersConfig.getString("your-orders.title", "&8ᴏʀᴅᴇʀs -> Your Orders");
   }

   public String getNewOrderTitle() {
      return this.newOrderConfig.getString("order-news.title", "&8ᴏʀᴅᴇʀs -> New Order");
   }

   public int getOrderViewRows() {
      return this.orderViewConfig.getInt("order-items.rows", 6);
   }

   public int getYourOrdersRows() {
      return this.yourOrdersConfig.getInt("your-orders.rows", 3);
   }

   public int getNewOrderRows() {
      return this.newOrderConfig.getInt("order-news.rows", 3);
   }

   public Map<String, Object> getOrderViewItems() {
      Map<String, Object> items = new HashMap();
      ConfigurationSection itemsSection = this.orderViewConfig.getConfigurationSection("order-items.items");
      if (itemsSection != null) {
         items.put("previous-page-slot", itemsSection.getInt("previous-page-slot", 45));
         items.put("next-page-slot", itemsSection.getInt("next-page-slot", 53));
         if (itemsSection.contains("previous-page")) {
            ConfigurationSection prevSection = itemsSection.getConfigurationSection("previous-page");
            if (prevSection != null) {
               Map<String, Object> prevPage = new HashMap();
               prevPage.put("material", prevSection.getString("material", "ARROW"));
               prevPage.put("displayname", prevSection.getString("displayname", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ"));
               prevPage.put("lore", prevSection.getStringList("lore"));
               items.put("previous-page", prevPage);
            }
         }

         if (itemsSection.contains("next-page")) {
            ConfigurationSection nextSection = itemsSection.getConfigurationSection("next-page");
            if (nextSection != null) {
               Map<String, Object> nextPage = new HashMap();
               nextPage.put("material", nextSection.getString("material", "ARROW"));
               nextPage.put("displayname", nextSection.getString("displayname", "&#00fc88ɴᴇxᴛ"));
               nextPage.put("lore", nextSection.getStringList("lore"));
               items.put("next-page", nextPage);
            }
         }

         if (itemsSection.contains("refresh")) {
            ConfigurationSection refreshSection = itemsSection.getConfigurationSection("refresh");
            if (refreshSection != null) {
               Map<String, Object> refresh = new HashMap();
               refresh.put("material", refreshSection.getString("material", "PAPER"));
               refresh.put("slot", refreshSection.getInt("slot", 49));
               refresh.put("name", refreshSection.getString("name", "&#00fc88ᴏʀᴅᴇʀs"));
               refresh.put("lore", refreshSection.getStringList("lore"));
               items.put("refresh", refresh);
            }
         }

         if (itemsSection.contains("your-orders")) {
            ConfigurationSection yourOrdersSection = itemsSection.getConfigurationSection("your-orders");
            if (yourOrdersSection != null) {
               Map<String, Object> yourOrders = new HashMap();
               yourOrders.put("material", yourOrdersSection.getString("material", "CHEST"));
               yourOrders.put("slot", yourOrdersSection.getInt("slot", 51));
               yourOrders.put("name", yourOrdersSection.getString("name", "&#00fc88ʏᴏᴜʀ ᴏʀᴅᴇʀs"));
               yourOrders.put("lore", yourOrdersSection.getStringList("lore"));
               items.put("your-orders", yourOrders);
            }
         }

         if (itemsSection.contains("sort")) {
            ConfigurationSection sortSection = itemsSection.getConfigurationSection("sort");
            if (sortSection != null) {
               Map<String, Object> sort = new HashMap();
               sort.put("material", sortSection.getString("material", "CAULDRON"));
               sort.put("slot", sortSection.getInt("slot", 47));
               sort.put("name", sortSection.getString("name", "&#00fc88ꜱᴏʀᴛ"));
               sort.put("active_prefix", sortSection.getString("active_prefix", "&#00fc88● "));
               sort.put("inactive_prefix", sortSection.getString("inactive_prefix", "&f● "));
               sort.put("keys", sortSection.getStringList("keys"));
               sort.put("options", sortSection.getStringList("options"));
               items.put("sort", sort);
            }
         }

         if (itemsSection.contains("filter")) {
            ConfigurationSection filterSection = itemsSection.getConfigurationSection("filter");
            if (filterSection != null) {
               Map<String, Object> filter = new HashMap();
               filter.put("material", filterSection.getString("material", "HOPPER"));
               filter.put("slot", filterSection.getInt("slot", 48));
               filter.put("name", filterSection.getString("name", "&#00fc88ꜰɪʟᴛᴇʀ"));
               filter.put("active_prefix", filterSection.getString("active_prefix", "&#00fc88● "));
               filter.put("inactive_prefix", filterSection.getString("inactive_prefix", "&f● "));
               filter.put("keys", filterSection.getStringList("keys"));
               filter.put("options", filterSection.getStringList("options"));
               items.put("filter", filter);
            }
         }

         if (itemsSection.contains("search")) {
            ConfigurationSection searchSection = itemsSection.getConfigurationSection("search");
            if (searchSection != null) {
               Map<String, Object> search = new HashMap();
               search.put("material", searchSection.getString("material", "PALE_OAK_SIGN"));
               search.put("slot", searchSection.getInt("slot", 50));
               search.put("name", searchSection.getString("name", "&#00fc88sᴇᴀʀᴄʜ"));
               search.put("lore", searchSection.getStringList("lore"));
               items.put("search", search);
            }
         }
      } else {
         items = this.getDefaultOrderViewItems();
      }

      return items.isEmpty() ? this.getDefaultOrderViewItems() : items;
   }

   public Map<String, Object> getSourceItemConfig() {
      Map<String, Object> sourceItem = new HashMap();
      FileConfiguration config = this.plugin.getConfig();
      if (config.contains("source-item")) {
         ConfigurationSection sourceItemSection = config.getConfigurationSection("source-item");
         if (sourceItemSection != null) {
            sourceItem.put("material", sourceItemSection.getString("material", "%material_selected_item%"));
            sourceItem.put("name", sourceItemSection.getString("name", "&#00fc88%owner%''s Order"));
            if (sourceItemSection.contains("lore")) {
               sourceItem.put("lore", sourceItemSection.getStringList("lore"));
            }

            return sourceItem;
         }
      }

      return this.getDefaultSourceItem();
   }

   public Map<String, Object> getYourOrdersItems() {
      Map<String, Object> items = new HashMap();
      ConfigurationSection itemsSection = this.yourOrdersConfig.getConfigurationSection("items");
      if (itemsSection != null && itemsSection.contains("new-order")) {
         ConfigurationSection newOrderSection = itemsSection.getConfigurationSection("new-order");
         if (newOrderSection != null) {
            Map<String, Object> newOrder = new HashMap();
            newOrder.put("material", newOrderSection.getString("material", "MAP"));
            newOrder.put("slot", newOrderSection.getInt("slot", 0));
            newOrder.put("name", newOrderSection.getString("name", "&#00fc88ɴᴇᴡ ᴏʀᴅᴇʀ"));
            newOrder.put("lore", newOrderSection.getStringList("lore"));
            items.put("new-order", newOrder);
            return items;
         }
      }

      return this.getDefaultYourOrdersItems();
   }

   public Map<String, Object> getNewOrderItems() {
      Map<String, Object> items = new HashMap();
      ConfigurationSection itemsSection = this.newOrderConfig.getConfigurationSection("items");
      if (itemsSection != null) {
         for(String key : itemsSection.getKeys(false)) {
            if (itemsSection.isConfigurationSection(key)) {
               ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
               if (itemSection != null) {
                  Map<String, Object> itemData = new HashMap();
                  itemData.put("material", itemSection.getString("material"));
                  itemData.put("slot", itemSection.getInt("slot"));
                  if (itemSection.contains("name")) {
                     itemData.put("name", itemSection.getString("name"));
                  } else if (itemSection.contains("displayname")) {
                     itemData.put("name", itemSection.getString("displayname"));
                  }

                  if (itemSection.contains("lore")) {
                     itemData.put("lore", itemSection.getStringList("lore"));
                  }

                  items.put(key, itemData);
               }
            }
         }
      }

      return items.isEmpty() ? this.getDefaultNewOrderItems() : items;
   }

   private Map<String, Object> getDefaultOrderViewItems() {
      Map<String, Object> items = new HashMap();
      Map<String, Object> prevPage = new HashMap();
      prevPage.put("material", "ARROW");
      prevPage.put("displayname", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
      prevPage.put("lore", Arrays.asList("&fClick to go to the previous page"));
      Map<String, Object> nextPage = new HashMap();
      nextPage.put("material", "ARROW");
      nextPage.put("displayname", "&#00fc88ɴᴇxᴛ");
      nextPage.put("lore", Arrays.asList("&fClick to go to the next page"));
      Map<String, Object> refresh = new HashMap();
      refresh.put("material", "PAPER");
      refresh.put("slot", 49);
      refresh.put("name", "&#00fc88ᴏʀᴅᴇʀs");
      refresh.put("lore", Arrays.asList("&fClick to refresh"));
      Map<String, Object> yourOrders = new HashMap();
      yourOrders.put("material", "CHEST");
      yourOrders.put("slot", 51);
      yourOrders.put("name", "&#00fc88ʏᴏᴜʀ ᴏʀᴅᴇʀs");
      yourOrders.put("lore", Arrays.asList("&fClick to view your orders"));
      items.put("previous-page-slot", 45);
      items.put("previous-page", prevPage);
      items.put("next-page-slot", 53);
      items.put("next-page", nextPage);
      items.put("refresh", refresh);
      items.put("your-orders", yourOrders);
      return items;
   }

   private Map<String, Object> getDefaultSourceItem() {
      Map<String, Object> sourceItem = new HashMap();
      sourceItem.put("material", "%material_selected_item%");
      sourceItem.put("name", "&#00fc88%owner%''s Order");
      sourceItem.put("lore", Arrays.asList("&#00fc88%requested-amount% &f%requested-material%", "&#00fc88%price% &feach", "", "&#80802a%delivered-amount%/&#008f4d%required-amount% &7Delivered", "&#80802a%paid-amount%/&#008f4d%total-paid-amount% &7Paid", "", "&fClick to deliver &#00fc88%owner% &f%requested-material%", "&8%expire-time% Until Order expires"));
      return sourceItem;
   }

   private Map<String, Object> getDefaultYourOrdersItems() {
      Map<String, Object> items = new HashMap();
      Map<String, Object> newOrder = new HashMap();
      newOrder.put("material", "MAP");
      newOrder.put("slot", 0);
      newOrder.put("name", "&#00fc88ɴᴇᴡ ᴏʀᴅᴇʀ");
      newOrder.put("lore", Arrays.asList("&fClick to create a new order"));
      items.put("new-order", newOrder);
      return items;
   }

   private Map<String, Object> getDefaultNewOrderItems() {
      Map<String, Object> items = new HashMap();
      Map<String, Object> cancel = new HashMap();
      cancel.put("material", "RED_STAINED_GLASS_PANE");
      cancel.put("slot", 10);
      cancel.put("name", "&#fc0404ᴄᴀɴᴄᴇʟ");
      cancel.put("lore", Arrays.asList("&fClick to return"));
      Map<String, Object> confirm = new HashMap();
      confirm.put("material", "LIME_STAINED_GLASS_PANE");
      confirm.put("slot", 16);
      confirm.put("name", "&#04fc04ᴄᴏɴғɪʀᴍ");
      confirm.put("lore", Arrays.asList("&fClick to confirm order", "&7(Total: %price%&7)"));
      Map<String, Object> item = new HashMap();
      item.put("material", "STONE");
      item.put("slot", 12);
      item.put("name", "&#04fc04ɪᴛᴇᴍ");
      item.put("lore", Arrays.asList("&7(%item%)"));
      Map<String, Object> amount = new HashMap();
      amount.put("material", "CHEST");
      amount.put("slot", 13);
      amount.put("name", "&#04fc04ᴀᴍᴏᴜɴᴛ");
      amount.put("lore", Arrays.asList("&fClick to type amount of items", "&7(%amount%)"));
      Map<String, Object> price = new HashMap();
      price.put("material", "EMERALD");
      price.put("slot", 14);
      price.put("name", "&#04fc04ᴘʀɪᴄᴇ");
      price.put("lore", Arrays.asList("&fClick to type the price per item", "&7(Total: %price%&7)"));
      items.put("cancel", cancel);
      items.put("confirm", confirm);
      items.put("item", item);
      items.put("amount", amount);
      items.put("price", price);
      return items;
   }

   public int getTotalOrderPages(int itemsPerPage) {
      return Math.max(1, (int)Math.ceil((double)this.orderItemsMap.size() / (double)itemsPerPage));
   }

   public List<OrderItem> getOrderItemsPage(int page, int itemsPerPage) {
      List<OrderItem> allItems = new ArrayList(this.orderItemsMap.values());
      allItems.sort((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()));
      int start = (page - 1) * itemsPerPage;
      int end = Math.min(start + itemsPerPage, allItems.size());
      return (List<OrderItem>)(start < allItems.size() && start >= 0 ? allItems.subList(start, end) : new ArrayList());
   }

   public void reloadConfigs() {
      this.plugin.reloadConfig();
      this.loadConfig();
      this.loadGUIConfigs();
      this.loadOrdersFromDatabase();
   }

   public void onDisable() {
      if (this.timeChecker != null) {
         this.timeChecker.stop();
      }

   }

   public int getDatabaseOrderCount() {
      return this.dataManager.getTotalOrderCount();
   }

   public double getDatabaseTotalValue() {
      return this.dataManager.getTotalOrderValue();
   }

   public List<OrderItem> getActiveOrders() {
      List<OrderItem> activeOrders = new ArrayList();

      for(OrderItem item : this.orderItemsMap.values()) {
         if (item.isActive() && !item.isExpired() && item.getRemainingAmount() > 0) {
            activeOrders.add(item);
         }
      }

      activeOrders.sort((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()));
      return activeOrders;
   }

   public int getExpireTimeSeconds() {
      return this.plugin.getConfig().getInt("expire-time-seconds", 604800);
   }

   public int getDeletionTimeSeconds() {
      return this.plugin.getConfig().getInt("deletion-time-seconds", 604800);
   }

   public void checkExpiredOrders() {
      List<OrderItem> toUpdate = new ArrayList();
      List<OrderItem> toDelete = new ArrayList();

      for(OrderItem order : this.orderItemsMap.values()) {
         if (order.isActive() && order.isExpired()) {
            if (order.shouldNotifyExpired()) {
               order.setExpiredNotified(true);
               this.notifyOrderExpired(order);
               toUpdate.add(order);
            }

            if (order.shouldBeDeleted()) {
               toDelete.add(order);
            }
         }
      }

      for(OrderItem orderx : toUpdate) {
         this.updateOrderItem(orderx);
      }

      for(OrderItem orderx : toDelete) {
         this.removeOrderItem(orderx.getOrderUuid());
      }

   }

   private void notifyOrderExpired(OrderItem order) {
      if (order.getCreator() != null) {
         Player creator = Bukkit.getPlayer(order.getCreator());
         if (creator != null && creator.isOnline()) {
         }
      }

   }
}
