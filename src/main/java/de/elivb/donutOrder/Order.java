package de.elivb.donutOrder;

import de.elivb.donutOrder.GUI.CollectItemsGUI;
import de.elivb.donutOrder.GUI.ConfirmCancelGUI;
import de.elivb.donutOrder.GUI.ConfirmDeliveryGUI;
import de.elivb.donutOrder.GUI.EditOrderGUI;
import de.elivb.donutOrder.GUI.OrderChatListener;
import de.elivb.donutOrder.GUI.OrderDeliveryGUI;
import de.elivb.donutOrder.GUI.OrderGUI;
import de.elivb.donutOrder.Manager.CollectItemsManager;
import de.elivb.donutOrder.Manager.ConfirmCancelManager;
import de.elivb.donutOrder.Manager.ConfirmDeliveryManager;
import de.elivb.donutOrder.Manager.DataManager;
import de.elivb.donutOrder.Manager.EditOrderManager;
import de.elivb.donutOrder.Manager.LangManager;
import de.elivb.donutOrder.Manager.MaterialsManager;
import de.elivb.donutOrder.Manager.OrderDeliveryManager;
import de.elivb.donutOrder.Manager.OrderManager;
import de.elivb.donutOrder.Manager.SoundManager;
import de.elivb.donutOrder.commands.OrderCommand;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Order extends JavaPlugin {
   private static Order instance;
   private OrderManager orderManager;
   private MaterialsManager materialsManager;
   private OrderGUI orderGUI;
   private OrderDeliveryManager deliveryManager;
   private OrderDeliveryGUI deliveryGUI;
   private EditOrderManager editOrderManager;
   private EditOrderGUI editOrderGUI;
   private CollectItemsManager collectItemsManager;
   private CollectItemsGUI collectItemsGUI;
   private OrderChatListener chatListener;
   private DataManager dataManager;
   private LicenseManager licenseManager;
   private ConfirmDeliveryManager confirmDeliveryManager;
   private ConfirmDeliveryGUI confirmDeliveryGUI;
   private ConfirmCancelManager confirmCancelManager;
   private ConfirmCancelGUI confirmCancelGUI;
   private SoundManager soundManager;
   private LangManager langManager;
   private boolean isFolia = false;

   public void onEnable() {
      instance = this;
      this.isFolia = this.checkFolia();
      this.licenseManager = new LicenseManager(this);
      if (!this.getDataFolder().exists()) {
         this.getDataFolder().mkdirs();
      }

      File guiFolder = new File(this.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      this.saveDefaultConfig();
      this.saveResourceIfNotExists("order-view.yml", guiFolder);
      this.saveResourceIfNotExists("your-orders.yml", guiFolder);
      this.saveResourceIfNotExists("new-order.yml", guiFolder);
      this.saveResourceIfNotExists("list-materials.yml", guiFolder);
      this.saveResourceIfNotExists("edit-order.yml", guiFolder);
      this.saveResourceIfNotExists("collect-items.yml", guiFolder);
      this.saveResourceIfNotExists("confirm-delivery.yml", guiFolder);
      this.saveResourceIfNotExists("confirm-cancel.yml", guiFolder);
      File itemsFile = new File(this.getDataFolder(), "items.yml");
      if (!itemsFile.exists()) {
         try {
            this.saveResource("items.yml", false);
         } catch (IllegalArgumentException var6) {
            try {
               itemsFile.createNewFile();
            } catch (IOException var5) {
            }
         }
      }

      this.dataManager = new DataManager(this);
      this.materialsManager = new MaterialsManager(this);
      this.orderManager = new OrderManager(this, this.dataManager);
      this.editOrderManager = new EditOrderManager(this);
      this.collectItemsManager = new CollectItemsManager(this);
      this.confirmDeliveryManager = new ConfirmDeliveryManager(this);
      this.confirmCancelManager = new ConfirmCancelManager(this);
      this.soundManager = new SoundManager(this);
      this.langManager = new LangManager(this);
      this.deliveryManager = new OrderDeliveryManager(this, this.orderManager);
      this.deliveryGUI = new OrderDeliveryGUI(this, this.orderManager, this.deliveryManager, this.materialsManager);
      this.editOrderGUI = new EditOrderGUI(this, this.editOrderManager, this.orderManager, this.materialsManager, this.deliveryGUI);
      this.orderGUI = new OrderGUI(this, this.orderManager, this.materialsManager, this.editOrderGUI);
      this.collectItemsGUI = new CollectItemsGUI(this, this.collectItemsManager, this.orderManager, this.orderGUI);
      this.confirmDeliveryGUI = new ConfirmDeliveryGUI(this, this.orderManager, this.deliveryManager, this.materialsManager, this.confirmDeliveryManager, this.orderGUI);
      this.confirmCancelGUI = new ConfirmCancelGUI(this, this.orderManager, this.confirmCancelManager, this.editOrderGUI);
      this.chatListener = new OrderChatListener(this, this.orderGUI);
      this.registerCommands();
      this.getServer().getPluginManager().registerEvents(this.orderGUI, this);
      this.getServer().getPluginManager().registerEvents(this.deliveryGUI, this);
      this.getServer().getPluginManager().registerEvents(this.editOrderGUI, this);
      this.getServer().getPluginManager().registerEvents(this.collectItemsGUI, this);
      this.getServer().getPluginManager().registerEvents(this.confirmDeliveryGUI, this);
      this.getServer().getPluginManager().registerEvents(this.confirmCancelGUI, this);
      this.getServer().getPluginManager().registerEvents(this.chatListener, this);
   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   // Getter cho OrderChatListener
   public OrderChatListener getOrderChatListener() {
      return this.chatListener;
   }

   private void registerCommands() {
      try {
         Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         commandMapField.setAccessible(true);
         CommandMap commandMap = (CommandMap)commandMapField.get(Bukkit.getServer());

         try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map)knownCommandsField.get(commandMap);
            knownCommands.remove("order");
            knownCommands.remove("order:order");

            for(String alias : this.getConfig().getStringList("command-aliases")) {
               knownCommands.remove(alias);
               knownCommands.remove("order:" + alias);
            }
         } catch (Exception var8) {
         }

         OrderCommand executor = new OrderCommand(this, this.orderGUI);
         PluginCommand mainCommand = this.createCommand("order");
         if (mainCommand != null) {
            mainCommand.setExecutor(executor);
            mainCommand.setTabCompleter(executor);
            mainCommand.setPermission("order.use");
            mainCommand.setUsage("/order");
            mainCommand.setLabel("order");
            mainCommand.setAliases(new ArrayList());
            commandMap.register("", mainCommand);
         }

         for(String alias : this.getConfig().getStringList("command-aliases")) {
            if (!alias.equals("order")) {
               PluginCommand aliasCommand = this.createCommand(alias);
               if (aliasCommand != null) {
                  aliasCommand.setExecutor(executor);
                  aliasCommand.setTabCompleter(executor);
                  aliasCommand.setPermission("order.use");
                  aliasCommand.setUsage("/" + alias);
                  aliasCommand.setLabel(alias);
                  aliasCommand.setAliases(new ArrayList());
                  commandMap.register("", aliasCommand);
               }
            }
         }
      } catch (Exception var10) {
         var10.printStackTrace();
      }
   }

   private PluginCommand createCommand(String name) {
      try {
         Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
         constructor.setAccessible(true);
         return (PluginCommand)constructor.newInstance(name, this);
      } catch (Exception var3) {
         return null;
      }
   }

   public void onDisable() {
      if (this.dataManager != null) {
         this.dataManager.close();
      }
      if (this.orderManager != null) {
         this.orderManager.onDisable();
      }
   }

   public static Order getInstance() { return instance; }
   public OrderManager getOrderManager() { return this.orderManager; }
   public MaterialsManager getMaterialsManager() { return this.materialsManager; }
   public OrderGUI getOrderGUI() { return this.orderGUI; }
   public OrderDeliveryManager getDeliveryManager() { return this.deliveryManager; }
   public OrderDeliveryGUI getDeliveryGUI() { return this.deliveryGUI; }
   public EditOrderManager getEditOrderManager() { return this.editOrderManager; }
   public EditOrderGUI getEditOrderGUI() { return this.editOrderGUI; }
   public CollectItemsManager getCollectItemsManager() { return this.collectItemsManager; }
   public CollectItemsGUI getCollectItemsGUI() { return this.collectItemsGUI; }
   public DataManager getDataManager() { return this.dataManager; }
   public ConfirmDeliveryManager getConfirmDeliveryManager() { return this.confirmDeliveryManager; }
   public ConfirmDeliveryGUI getConfirmDeliveryGUI() { return this.confirmDeliveryGUI; }
   public ConfirmCancelManager getConfirmCancelManager() { return this.confirmCancelManager; }
   public ConfirmCancelGUI getConfirmCancelGUI() { return this.confirmCancelGUI; }
   public SoundManager getSoundManager() { return this.soundManager; }
   public LangManager getLangManager() { return this.langManager; }
   public boolean isFolia() { return this.isFolia; }

   private boolean checkFolia() {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }

   private void saveResourceIfNotExists(String fileName, File folder) {
      File file = new File(folder, fileName);
      if (!file.exists()) {
         try {
            this.saveResource("gui/" + fileName, false);
         } catch (IllegalArgumentException var7) {
            try {
               file.createNewFile();
            } catch (IOException var6) {
            }
         }
      }
   }
}
