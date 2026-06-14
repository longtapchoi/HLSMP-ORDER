package de.elivb.donutOrder.Manager;

import de.elivb.donutOrder.Order;
import de.elivb.donutOrder.utils.ColorUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MaterialsManager {
   private final Order plugin;
   private FileConfiguration materialsConfig;
   private FileConfiguration listMaterialsConfig;
   private File materialsFile;
   private File listMaterialsFile;
   private final List<SpecialItemEntry> allItems;
   private final Map<String, SpecialItemEntry> itemByIdentifier;
   private final Map<String, Material> specialTypeMapping = new HashMap();

   public MaterialsManager(Order plugin) {
      this.plugin = plugin;
      this.allItems = new ArrayList();
      this.itemByIdentifier = new HashMap();
      MaterialsManager.SpecialItemEntry.setPlugin(plugin);
      this.initSpecialItemsMapping();
      this.loadConfigs();
   }

   private void initSpecialItemsMapping() {
      this.specialTypeMapping.put("enchanted_books", Material.ENCHANTED_BOOK);
      this.specialTypeMapping.put("potions", Material.POTION);
      this.specialTypeMapping.put("tipped_arrows", Material.TIPPED_ARROW);
      this.specialTypeMapping.put("firework_rockets", Material.FIREWORK_ROCKET);
   }

   private void loadConfigs() {
      this.materialsFile = new File(this.plugin.getDataFolder(), "items.yml");
      if (!this.materialsFile.exists()) {
         this.materialsConfig = new YamlConfiguration();
      } else {
         this.materialsConfig = YamlConfiguration.loadConfiguration(this.materialsFile);
      }

      File guiFolder = new File(this.plugin.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      this.listMaterialsFile = new File(guiFolder, "list-materials.yml");
      if (!this.listMaterialsFile.exists()) {
         this.plugin.saveResource("gui/list-materials.yml", false);
      }

      this.listMaterialsConfig = YamlConfiguration.loadConfiguration(this.listMaterialsFile);
      this.loadAllItems();
   }

   private void loadAllItems() {
      this.allItems.clear();
      this.itemByIdentifier.clear();
      int normalLoaded = this.loadNormalMaterials();
      int specialLoaded = this.loadSpecialItems();
      if (this.allItems.isEmpty()) {
         this.loadAllMinecraftMaterials();
      }

      Map<ItemType, Integer> typeCount = new HashMap();

      for(SpecialItemEntry item : this.allItems) {
         typeCount.put(item.type, (Integer)typeCount.getOrDefault(item.type, 0) + 1);
      }

      for(Map.Entry var8 : typeCount.entrySet()) {
         ;
      }

      if (!this.allItems.isEmpty()) {
         for(int i = 0; i < Math.min(10, this.allItems.size()); ++i) {
            SpecialItemEntry var9 = (SpecialItemEntry)this.allItems.get(i);
         }
      }

   }

   private int loadNormalMaterials() {
      int loaded = 0;

      try {
         BufferedReader reader = new BufferedReader(new FileReader(this.materialsFile));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  if (line.endsWith(":")) {
                     String materialName = line.substring(0, line.length() - 1).trim();
                     if (!this.specialTypeMapping.containsKey(materialName.toLowerCase())) {
                        this.loadMaterial(materialName, loaded);
                        ++loaded;
                     }
                  } else {
                     this.loadMaterial(line, loaded);
                     ++loaded;
                  }
               }
            }
         } catch (Throwable var7) {
            try {
               reader.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         reader.close();
      } catch (IOException var8) {
      }

      return loaded;
   }

   private int loadSpecialItems() {
      int loaded = 0;

      try {
         String currentCategory = null;
         Material currentBaseMaterial = null;
         ItemType currentType = null;
         BufferedReader reader = new BufferedReader(new FileReader(this.materialsFile));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  if (line.endsWith(":")) {
                     String category = line.substring(0, line.length() - 1).trim();
                     if (this.specialTypeMapping.containsKey(category.toLowerCase())) {
                        currentCategory = category.toLowerCase();
                        currentBaseMaterial = (Material)this.specialTypeMapping.get(currentCategory);
                        currentType = this.getItemTypeFromCategory(currentCategory);
                     } else {
                        currentCategory = null;
                        currentBaseMaterial = null;
                        currentType = null;
                     }
                  } else if (currentCategory != null && currentBaseMaterial != null && currentType != null) {
                     String itemName = line.trim();
                     if (itemName.startsWith("-")) {
                        itemName = itemName.substring(1).trim();
                     }

                     if (!itemName.isEmpty()) {
                        SpecialItemEntry entry = new SpecialItemEntry(currentType, currentBaseMaterial, itemName, this.formatItemName(currentCategory, itemName), (double)100.0F);
                        this.allItems.add(entry);
                        this.itemByIdentifier.put(entry.getIdentifier(), entry);
                        ++loaded;
                     }
                  }
               }
            }
         } catch (Throwable var10) {
            try {
               reader.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         reader.close();
      } catch (IOException var11) {
      }

      return loaded;
   }

   private ItemType getItemTypeFromCategory(String category) {
      switch (category.toLowerCase()) {
         case "enchanted_books" -> {
            return MaterialsManager.ItemType.ENCHANTED_BOOK;
         }
         case "potions" -> {
            return MaterialsManager.ItemType.POTION;
         }
         case "tipped_arrows" -> {
            return MaterialsManager.ItemType.TIPPED_ARROW;
         }
         case "firework_rockets" -> {
            return MaterialsManager.ItemType.FIREWORK_ROCKET;
         }
         default -> {
            return MaterialsManager.ItemType.NORMAL_ITEM;
         }
      }
   }

   private void loadMaterial(String materialName, int index) {
      if (materialName != null && !materialName.isEmpty()) {
         if (materialName.endsWith(":")) {
            materialName = materialName.substring(0, materialName.length() - 1).trim();
         }

         try {
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material != null && material.isItem()) {
               SpecialItemEntry entry = new SpecialItemEntry(material, (double)100.0F);
               this.allItems.add(entry);
               this.itemByIdentifier.put(entry.getIdentifier(), entry);
            }
         } catch (Exception var5) {
         }
      }

   }

   private void loadAllMinecraftMaterials() {
      for(Material material : Material.values()) {
         if (material.isItem() && material != Material.AIR) {
            SpecialItemEntry entry = new SpecialItemEntry(material, (double)100.0F);
            this.allItems.add(entry);
            this.itemByIdentifier.put(entry.getIdentifier(), entry);
         }
      }

   }

   private String formatItemName(String category, String itemName) {
      String var10000 = category.substring(0, 1).toUpperCase();
      String formattedCategory = var10000 + category.substring(1);
      formattedCategory = formattedCategory.replace("_", " ");
      String formattedName = itemName.replace("_", " ").toLowerCase();
      String[] words = formattedName.split(" ");
      StringBuilder result = new StringBuilder();

      for(String word : words) {
         if (!word.isEmpty()) {
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
         }
      }

      var10000 = result.toString().trim();
      return var10000 + " (" + formattedCategory + ")";
   }

   public String getListMaterialsTitle(int page) {
      String title = this.listMaterialsConfig.getString("list-materials.title", "&8ᴏʀᴅᴇʀs -> Select Item");
      return ColorUtil.color(title);
   }

   public int getListMaterialsRows() {
      return this.listMaterialsConfig.getInt("list-materials.rows", 6);
   }

   public Map<String, Object> getListMaterialsItems() {
      Map<String, Object> items = new HashMap();
      ConfigurationSection itemsSection = this.listMaterialsConfig.getConfigurationSection("items");
      if (itemsSection != null) {
         int prevSlot = itemsSection.getInt("previous-page-slot", 45);
         items.put("previous-page-slot", prevSlot);
         int nextSlot = itemsSection.getInt("next-page-slot", 53);
         items.put("next-page-slot", nextSlot);
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

         if (itemsSection.contains("sort")) {
            ConfigurationSection sortSection = itemsSection.getConfigurationSection("sort");
            if (sortSection != null) {
               Map<String, Object> sort = new HashMap();
               sort.put("material", sortSection.getString("material", "CAULDRON"));
               sort.put("slot", sortSection.getInt("slot", 48));
               sort.put("name", sortSection.getString("name", "&#00fc88Sort"));
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
               filter.put("slot", filterSection.getInt("slot", 49));
               filter.put("name", filterSection.getString("name", "&#00fc88Filter"));
               filter.put("active_prefix", filterSection.getString("active_prefix", "&#00fc88● "));
               filter.put("inactive_prefix", filterSection.getString("inactive_prefix", "&f● "));
               filter.put("keys", filterSection.getStringList("keys"));
               filter.put("options", filterSection.getStringList("options"));
               items.put("filter", filter);
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
      } else {
         Map<String, Object> prevPage = new HashMap();
         prevPage.put("material", "ARROW");
         prevPage.put("displayname", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
         prevPage.put("lore", Arrays.asList("&fClick to go to the previous page"));
         Map<String, Object> nextPage = new HashMap();
         nextPage.put("material", "ARROW");
         nextPage.put("displayname", "&#00fc88ɴᴇxᴛ");
         nextPage.put("lore", Arrays.asList("&fClick to go to the next page"));
         items.put("previous-page-slot", 45);
         items.put("previous-page", prevPage);
         items.put("next-page-slot", 53);
         items.put("next-page", nextPage);
      }

      return items;
   }

   public List<SpecialItemEntry> getAllItems() {
      return new ArrayList(this.allItems);
   }

   public List<SpecialItemEntry> getItemsPage(int page, int itemsPerPage) {
      int start = (page - 1) * itemsPerPage;
      int end = Math.min(start + itemsPerPage, this.allItems.size());
      return (List<SpecialItemEntry>)(start < this.allItems.size() && start >= 0 ? this.allItems.subList(start, end) : new ArrayList());
   }

   public int getTotalPages(int itemsPerPage) {
      return Math.max(1, (int)Math.ceil((double)this.allItems.size() / (double)itemsPerPage));
   }

   public SpecialItemEntry getItemByIdentifier(String identifier) {
      return (SpecialItemEntry)this.itemByIdentifier.get(identifier);

   }

   public SpecialItemEntry getNormalItem(Material material) {
      String identifier = material.name();
      return (SpecialItemEntry)this.itemByIdentifier.get(identifier);
   }

   public ItemStack getItemStackForGUI(SpecialItemEntry entry) {
      return entry == null ? new ItemStack(Material.STONE) : entry.itemStack.clone();
   }

   public ItemStack getOrderItemStack(SpecialItemEntry entry, int amount) {
      if (entry == null) {
         return new ItemStack(Material.STONE, amount);
      } else {
         ItemStack item = entry.itemStack.clone();
         item.setAmount(amount);
         return item;
      }
   }

   public double getItemPrice(SpecialItemEntry entry) {
      return entry != null ? entry.price : (double)100.0F;
   }

   public void setItemPrice(SpecialItemEntry entry, double price) {
      if (entry != null) {
      }

   }

   public void reloadConfig() {
      this.loadConfigs();
   }

   public void debugInfo() {
      Map<ItemType, Integer> counts = new HashMap();

      for(SpecialItemEntry item : this.allItems) {
         counts.put(item.type, (Integer)counts.getOrDefault(item.type, 0) + 1);
      }

      for(Map.Entry var5 : counts.entrySet()) {
         ;
      }

   }

   public static enum ItemType {
      ENCHANTED_BOOK,
      POTION,
      TIPPED_ARROW,
      FIREWORK_ROCKET,
      NORMAL_ITEM;

      // $FF: synthetic method
      private static ItemType[] $values() {
         return new ItemType[]{ENCHANTED_BOOK, POTION, TIPPED_ARROW, FIREWORK_ROCKET, NORMAL_ITEM};
      }
   }

   public static class SpecialItemEntry {
      public final ItemType type;
      public final Material baseMaterial;
      public final String subType;
      public final String displayName;
      public final double price;
      public ItemStack itemStack;
      private static Order plugin;

      public static void setPlugin(Order pluginInstance) {
         plugin = pluginInstance;
      }

      public SpecialItemEntry(ItemType type, Material baseMaterial, String subType, String displayName, double price) {
         this.type = type;
         this.baseMaterial = baseMaterial;
         this.subType = subType;
         this.displayName = displayName;
         this.price = price;
         this.itemStack = this.createItemStack();
      }

      public SpecialItemEntry(Material material, double price) {
         this.type = MaterialsManager.ItemType.NORMAL_ITEM;
         this.baseMaterial = material;
         this.subType = "";
         this.displayName = this.formatMaterialName(material.name());
         this.price = price;
         this.itemStack = new ItemStack(material, 1);
      }

      private ItemStack createItemStack() {
         ItemStack item = new ItemStack(this.baseMaterial, 1);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(ColorUtil.color(this.displayName));
            List<String> lore = new ArrayList();
            if (!this.subType.isEmpty()) {
            }

            meta.setLore(lore);
            if (this.type == MaterialsManager.ItemType.POTION && meta instanceof PotionMeta) {
               PotionMeta potionMeta = (PotionMeta)meta;
               this.createPotion(potionMeta, this.subType);
            }

            if (this.type == MaterialsManager.ItemType.TIPPED_ARROW && meta instanceof PotionMeta) {
               PotionMeta potionMeta = (PotionMeta)meta;
               this.createPotion(potionMeta, this.subType);
            }

            if (this.type == MaterialsManager.ItemType.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
               EnchantmentStorageMeta esm = (EnchantmentStorageMeta)meta;

               try {
                  String[] parts = this.subType.split("_");
                  if (parts.length >= 2) {
                     String enchantName = parts[0];
                     int level = Integer.parseInt(parts[1]);

                     for(Enchantment enchant : Enchantment.values()) {
                        if (enchant.getKey().getKey().equalsIgnoreCase(enchantName)) {
                           esm.addStoredEnchant(enchant, level, true);
                           break;
                        }
                     }
                  }
               } catch (Exception var12) {
                  if (plugin != null) {
                  }
               }
            }

            item.setItemMeta(meta);
         }

         return item;
      }

      private void createPotion(PotionMeta potionMeta, String potionName) {
         try {
            if (plugin != null) {
            }

            String cleanName = potionName;
            boolean isSplash = potionName.startsWith("SPLASH_");
            boolean isLingering = potionName.startsWith("LINGERING_");
            if (isSplash) {
               cleanName = potionName.substring(7);
            } else if (isLingering) {
               cleanName = potionName.substring(10);
            }

            int duration = 3600;
            int amplifier = 0;
            if (cleanName.startsWith("LONG_")) {
               duration = 9600;
               cleanName = cleanName.substring(5);
            } else if (cleanName.startsWith("STRONG_")) {
               amplifier = 1;
               cleanName = cleanName.substring(7);
            }

            if (cleanName.equals("WATER") || cleanName.equals("MUNDANE") || cleanName.equals("THICK") || cleanName.equals("AWKWARD") || cleanName.equals("UNCRAFTABLE")) {
               Color color = this.getOriginalMinecraftColor(cleanName);
               potionMeta.setColor(color);
               return;
            }

            PotionEffectType effectType = this.getPotionEffectTypeFromName(cleanName);
            if (effectType != null) {
               PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true, true);
               potionMeta.addCustomEffect(effect, true);
               Color color = this.getOriginalMinecraftColor(effectType);
               potionMeta.setColor(color);
            } else {
               for(PotionEffectType type : PotionEffectType.values()) {
                  if (type != null && type.getName().equalsIgnoreCase(cleanName)) {
                     PotionEffect effect = new PotionEffect(type, duration, amplifier, true, true, true);
                     potionMeta.addCustomEffect(effect, true);
                     Color color = this.getOriginalMinecraftColor(type);
                     potionMeta.setColor(color);
                     return;
                  }
               }

               Color color = this.tryFindPotionColor(cleanName);
               if (color != null) {
                  potionMeta.setColor(color);
               } else {
                  potionMeta.setColor(Color.fromRGB(162, 0, 255));
               }
            }
         } catch (Exception var15) {
            if (plugin != null) {
            }

            potionMeta.setColor(Color.fromRGB(162, 0, 255));
         }

      }

      private Color tryFindPotionColor(String potionName) {
         Map<String, Color> colorMapping = new HashMap();
         colorMapping.put("NIGHT_VISION", Color.fromRGB(0, 0, 51));
         colorMapping.put("INVISIBILITY", Color.fromRGB(127, 131, 146));
         colorMapping.put("JUMP", Color.fromRGB(34, 255, 76));
         colorMapping.put("LEAPING", Color.fromRGB(34, 255, 76));
         colorMapping.put("FIRE_RESISTANCE", Color.fromRGB(228, 154, 58));
         colorMapping.put("SPEED", Color.fromRGB(124, 175, 198));
         colorMapping.put("SWIFTNESS", Color.fromRGB(124, 175, 198));
         colorMapping.put("SLOWNESS", Color.fromRGB(90, 108, 129));
         colorMapping.put("TURTLE_MASTER", Color.fromRGB(100, 150, 100));
         colorMapping.put("WATER_BREATHING", Color.fromRGB(46, 82, 153));
         colorMapping.put("HEALING", Color.fromRGB(248, 36, 35));
         colorMapping.put("HARMING", Color.fromRGB(67, 10, 9));
         colorMapping.put("STRENGTH", Color.fromRGB(147, 36, 35));
         colorMapping.put("POISON", Color.fromRGB(78, 147, 49));
         colorMapping.put("REGENERATION", Color.fromRGB(205, 92, 171));
         colorMapping.put("WEAKNESS", Color.fromRGB(72, 77, 72));
         colorMapping.put("LUCK", Color.fromRGB(51, 153, 0));
         colorMapping.put("SLOW_FALLING", Color.fromRGB(255, 255, 255));
         String upperName = potionName.toUpperCase();
         return colorMapping.containsKey(upperName) ? (Color)colorMapping.get(upperName) : null;
      }

      private Color getOriginalMinecraftColor(String potionName) {
         switch (potionName.toUpperCase()) {
            case "WATER" -> {
               return Color.fromRGB(59, 92, 188);
            }
            case "MUNDANE" -> {
               return Color.fromRGB(60, 60, 60);
            }
            case "THICK" -> {
               return Color.fromRGB(67, 10, 9);
            }
            case "AWKWARD" -> {
               return Color.fromRGB(56, 56, 207);
            }
            case "UNCRAFTABLE" -> {
               return Color.fromRGB(67, 10, 9);
            }
            default -> {
               return Color.fromRGB(162, 0, 255);
            }
         }
      }

      private Color getOriginalMinecraftColor(PotionEffectType effectType) {
         if (effectType == null) {
            return Color.fromRGB(162, 0, 255);
         } else {
            switch (effectType.getName().toUpperCase()) {
               case "NIGHT_VISION" -> {
                  return Color.fromRGB(0, 0, 51);
               }
               case "INVISIBILITY" -> {
                  return Color.fromRGB(127, 131, 146);
               }
               case "JUMP_BOOST" -> {
                  return Color.fromRGB(34, 255, 76);
               }
               case "FIRE_RESISTANCE" -> {
                  return Color.fromRGB(228, 154, 58);
               }
               case "SPEED" -> {
                  return Color.fromRGB(124, 175, 198);
               }
               case "SLOWNESS" -> {
                  return Color.fromRGB(90, 108, 129);
               }
               case "TURTLE_MASTER" -> {
                  return Color.fromRGB(100, 150, 100);
               }
               case "WATER_BREATHING" -> {
                  return Color.fromRGB(46, 82, 153);
               }
               case "INSTANT_HEALTH" -> {
                  return Color.fromRGB(248, 36, 35);
               }
               case "HARM" -> {
                  return Color.fromRGB(67, 10, 9);
               }
               case "STRENGTH" -> {
                  return Color.fromRGB(147, 36, 35);
               }
               case "SLOW_FALLING" -> {
                  return Color.fromRGB(255, 255, 255);
               }
               case "LUCK" -> {
                  return Color.fromRGB(51, 153, 0);
               }
               case "POISON" -> {
                  return Color.fromRGB(78, 147, 49);
               }
               case "REGENERATION" -> {
                  return Color.fromRGB(205, 92, 171);
               }
               case "WEAKNESS" -> {
                  return Color.fromRGB(72, 77, 72);
               }
               default -> {
                  return effectType.getColor() != null ? effectType.getColor() : Color.fromRGB(162, 0, 255);
               }
            }
         }
      }

      private PotionEffectType getPotionEffectTypeFromName(String potionName) {
         Map<String, PotionEffectType> potionMapping = new HashMap();
         potionMapping.put("NIGHT_VISION", PotionEffectType.NIGHT_VISION);
         potionMapping.put("INVISIBILITY", PotionEffectType.INVISIBILITY);
         potionMapping.put("JUMP", PotionEffectType.JUMP_BOOST);
         potionMapping.put("LEAPING", PotionEffectType.JUMP_BOOST);
         potionMapping.put("FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE);
         potionMapping.put("SPEED", PotionEffectType.SPEED);
         potionMapping.put("SWIFTNESS", PotionEffectType.SPEED);
         potionMapping.put("SLOWNESS", PotionEffectType.SLOWNESS);
         potionMapping.put("WATER_BREATHING", PotionEffectType.WATER_BREATHING);
         potionMapping.put("HEALING", PotionEffectType.INSTANT_HEALTH);
         potionMapping.put("STRENGTH", PotionEffectType.STRENGTH);
         potionMapping.put("POISON", PotionEffectType.POISON);
         potionMapping.put("REGENERATION", PotionEffectType.REGENERATION);
         potionMapping.put("WEAKNESS", PotionEffectType.WEAKNESS);
         potionMapping.put("LUCK", PotionEffectType.LUCK);
         potionMapping.put("SLOW_FALLING", PotionEffectType.SLOW_FALLING);
         String upperName = potionName.toUpperCase();
         if (potionMapping.containsKey(upperName)) {
            return (PotionEffectType)potionMapping.get(upperName);
         } else {
            try {
               return PotionEffectType.getByName(upperName);
            } catch (Exception var5) {
               return null;
            }
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

      public String getIdentifier() {
         return this.type == MaterialsManager.ItemType.NORMAL_ITEM ? this.baseMaterial.name() : this.type.name() + ":" + this.subType;
      }

      public String toString() {
         String var10000 = this.displayName;
         return var10000 + " (" + String.valueOf(this.type) + ")";
      }
   }
}
