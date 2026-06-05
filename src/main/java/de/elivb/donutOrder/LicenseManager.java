package de.elivb.donutOrder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LicenseManager {
   private final JavaPlugin plugin;
   private final String API_URL = "https://web14641.cweb06.gamingcontrol.de/license/admin.php";
   private final String PLUGIN_NAME = "HLSMPOrder";
   private final String PLUGIN_ID = "donutorder";
   private File licenseFile;
   private FileConfiguration licenseConfig;
   private String serverId;
   private String licenseKey;
   private boolean licenseValid = false;

   public LicenseManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.setupLicenseFile();
      this.loadServerId();
   }

   private void setupLicenseFile() {
      this.licenseFile = new File(this.plugin.getDataFolder(), "license.yml");
      if (!this.licenseFile.exists()) {
         this.plugin.saveResource("license.yml", false);
      }

      this.licenseConfig = YamlConfiguration.loadConfiguration(this.licenseFile);
   }

   private void loadServerId() {
      if (this.licenseConfig.contains("server-id") && !this.licenseConfig.getString("server-id").isEmpty()) {
         this.serverId = this.licenseConfig.getString("server-id");
      } else {
         String serverIp = this.plugin.getServer().getIp();
         String serverPort = String.valueOf(this.plugin.getServer().getPort());
         String serverMotd = this.plugin.getServer().getMotd();
         if (serverIp.isEmpty() || serverIp.equals("0.0.0.0")) {
            serverIp = "localhost";
         }

         String combined = serverIp + ":" + serverPort + ":" + serverMotd.hashCode() + ":" + System.getProperty("user.name", "unknown") + ":" + System.getProperty("user.home", "unknown");
         this.serverId = this.generateHash(combined);
         this.licenseConfig.set("server-id", this.serverId);
         this.saveLicenseFile();
      }

   }

   public boolean validateLicenseOnStartup() {
      this.licenseKey = this.licenseConfig.getString("license-key", "");
      if (!this.licenseKey.isEmpty() && !this.licenseKey.equals("MQF3SX4C-QP8KWRQ3-6IOWBYPK-JCTLS83M")) {
         String redColor = "\u001b[31m";
         String greenColor = "\u001b[32m";
         String resetColor = "\u001b[0m";
         if (this.licenseKey.startsWith("TEST")) {
            this.plugin.getLogger().info(greenColor + "██╗   ██╗ ██╗    ████████╗ ██╗ ███╗   ███╗  █████╗  ████████╗ ███████╗" + resetColor);
            this.plugin.getLogger().info(greenColor + "██║   ██║ ██║    ╚══██╔══╝ ██║ ████╗ ████║ ██╔══██╗ ╚══██╔══╝ ██╔═" + resetColor);
            this.plugin.getLogger().info(greenColor + "██║   ██║ ██║       ██║    ██║ ██╔████╔██║ ███████║    ██║    █████╗" + resetColor);
            this.plugin.getLogger().info(greenColor + "██║   ██║ ██║       ██║    ██║ ██║╚██╔╝██║ ██╔══██║    ██║    ██╔══╝" + resetColor);
            this.plugin.getLogger().info(greenColor + "╚██████╔╝ ███████╗  ██║    ██║ ██║ ╚═╝ ██║ ██║  ██║    ██║    ███████╗" + resetColor);
            this.plugin.getLogger().info(greenColor + " ╚═════╝   ╚══════╝ ╚═╝    ╚═╝ ╚═╝     ╚═╝ ╚═╝  ╚═╝    ╚═╝    ╚══════╝" + resetColor);
            this.plugin.getLogger().info("");
            this.plugin.getLogger().info(greenColor + "          ███████╗ ████████╗ ██╗   ██╗ ██████╗  ██╗  ██████╗            " + resetColor);
            this.plugin.getLogger().info(greenColor + "          ██╔════╝ ╚══██╔══╝ ██║   ██║ ██╔══██╗ ██║ ██╔═══██╗           " + resetColor);
            this.plugin.getLogger().info(greenColor + "          ███████╗    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
            this.plugin.getLogger().info(greenColor + "          ╚════██║    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
            this.plugin.getLogger().info(greenColor + "          ███████║    ██║    ╚██████╔╝ ██████╔╝ ██║ ╚██████╔╝          " + resetColor);
            this.plugin.getLogger().info(greenColor + "          ╚══════╝    ╚═╝     ╚═════╝  ╚═════╝  ╚═╝  ╚═════╝           " + resetColor);
            this.plugin.getLogger().info("");
            this.plugin.getLogger().info("");
            this.plugin.getLogger().info(greenColor + "╔══════════════════════════════════════════════════════════════╗");
            this.plugin.getLogger().info(greenColor + "║                    TEST KEY FOUND                            ║");
            this.plugin.getLogger().info(greenColor + "╠══════════════════════════════════════════════════════════════╣");
            String displayedKey = this.licenseKey == null ? "" : (this.licenseKey.length() >= 64 ? this.licenseKey.substring(0, 64) + "..." : this.licenseKey);
            this.plugin.getLogger().info(greenColor + "║ Key: " + displayedKey + "                      ║");
            this.plugin.getLogger().info(greenColor + "║ This is only a test KEY                                      ║");
            this.plugin.getLogger().info(greenColor + "╚══════════════════════════════════════════════════════════════╝");
            this.licenseValid = true;
            return true;
         } else {
            try {
               this.licenseValid = this.validateWithAPI(this.licenseKey, this.serverId);
            } catch (Exception var7) {
               var7.printStackTrace();
               this.licenseValid = false;
            }

            if (this.licenseValid) {
               this.plugin.getLogger().info(greenColor + "██╗   ██╗ ██╗    ████████╗ ██╗ ███╗   ███╗  █████╗  ████████╗ ███████╗" + resetColor);
               this.plugin.getLogger().info(greenColor + "██║   ██║ ██║    ╚══██╔══╝ ██║ ████╗ ████║ ██╔══██╗ ╚══██╔══╝ ██╔═" + resetColor);
               this.plugin.getLogger().info(greenColor + "██║   ██║ ██║       ██║    ██║ ██╔████╔██║ ███████║    ██║    █████╗" + resetColor);
               this.plugin.getLogger().info(greenColor + "██║   ██║ ██║       ██║    ██║ ██║╚██╔╝██║ ██╔══██║    ██║    ██╔══╝" + resetColor);
               this.plugin.getLogger().info(greenColor + "╚██████╔╝ ███████╗  ██║    ██║ ██║ ╚═╝ ██║ ██║  ██║    ██║    ███████╗" + resetColor);
               this.plugin.getLogger().info(greenColor + " ╚═════╝   ╚══════╝ ╚═╝    ╚═╝ ╚═╝     ╚═╝ ╚═╝  ╚═╝    ╚═╝    ╚══════╝" + resetColor);
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info(greenColor + "          ███████╗ ████████╗ ██╗   ██╗ ██████╗  ██╗  ██████╗            " + resetColor);
               this.plugin.getLogger().info(greenColor + "          ██╔════╝ ╚══██╔══╝ ██║   ██║ ██╔══██╗ ██║ ██╔═══██╗           " + resetColor);
               this.plugin.getLogger().info(greenColor + "          ███████╗    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
               this.plugin.getLogger().info(greenColor + "          ╚════██║    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
               this.plugin.getLogger().info(greenColor + "          ███████║    ██║    ╚██████╔╝ ██████╔╝ ██║ ╚██████╔╝          " + resetColor);
               this.plugin.getLogger().info(greenColor + "          ╚══════╝    ╚═╝     ╚═════╝  ╚═════╝  ╚═╝  ╚═════╝           " + resetColor);
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info(greenColor + "╔══════════════════════════════════════════════════════════════╗");
               this.plugin.getLogger().info(greenColor + "║                    LICENSE KEY VAILD                         ║");
               this.plugin.getLogger().info(greenColor + "╠══════════════════════════════════════════════════════════════╣");
               String displayedKey = this.licenseKey == null ? "" : (this.licenseKey.length() >= 64 ? this.licenseKey.substring(0, 64) + "..." : this.licenseKey);
               this.plugin.getLogger().info(greenColor + "║ Key: " + displayedKey + "                     ║");
               this.plugin.getLogger().info(greenColor + "║ Thanks for you help                                          ║");
               this.plugin.getLogger().info(greenColor + "╚══════════════════════════════════════════════════════════════╝");
               return true;
            } else {
               this.plugin.getLogger().info(redColor + "██╗   ██╗ ██╗    ████████╗ ██╗ ███╗   ███╗  █████╗  ████████╗ ███████╗" + resetColor);
               this.plugin.getLogger().info(redColor + "██║   ██║ ██║    ╚══██╔══╝ ██║ ████╗ ████║ ██╔══██╗ ╚══██╔══╝ ██╔═" + resetColor);
               this.plugin.getLogger().info(redColor + "██║   ██║ ██║       ██║    ██║ ██╔████╔██║ ███████║    ██║    █████╗" + resetColor);
               this.plugin.getLogger().info(redColor + "██║   ██║ ██║       ██║    ██║ ██║╚██╔╝██║ ██╔══██║    ██║    ██╔══╝" + resetColor);
               this.plugin.getLogger().info(redColor + "╚██████╔╝ ███████╗  ██║    ██║ ██║ ╚═╝ ██║ ██║  ██║    ██║    ███████╗" + resetColor);
               this.plugin.getLogger().info(redColor + " ╚═════╝   ╚══════╝ ╚═╝    ╚═╝ ╚═╝     ╚═╝ ╚═╝  ╚═╝    ╚═╝    ╚══════╝" + resetColor);
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info(redColor + "          ███████╗ ████████╗ ██╗   ██╗ ██████╗  ██╗  ██████╗            " + resetColor);
               this.plugin.getLogger().info(redColor + "          ██╔════╝ ╚══██╔══╝ ██║   ██║ ██╔══██╗ ██║ ██╔═══██╗           " + resetColor);
               this.plugin.getLogger().info(redColor + "          ███████╗    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
               this.plugin.getLogger().info(redColor + "          ╚════██║    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
               this.plugin.getLogger().info(redColor + "          ███████║    ██║    ╚██████╔╝ ██████╔╝ ██║ ╚██████╔╝          " + resetColor);
               this.plugin.getLogger().info(redColor + "          ╚══════╝    ╚═╝     ╚═════╝  ╚═════╝  ╚═╝  ╚═════╝           " + resetColor);
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info("");
               this.plugin.getLogger().info(redColor + "╔══════════════════════════════════════════════════════════════╗");
               this.plugin.getLogger().info(redColor + "║                    LICENSE KEY INVALID                       ║");
               this.plugin.getLogger().info(redColor + "╠══════════════════════════════════════════════════════════════╣");
               String displayedKey = this.licenseKey == null ? "" : (this.licenseKey.length() >= 64 ? this.licenseKey.substring(0, 64) + "..." : this.licenseKey);
               this.plugin.getLogger().info(redColor + "║ Key: " + displayedKey + "                     ║");
               this.plugin.getLogger().info(redColor + "║ Contact EliVB97 for help                                     ║");
               this.plugin.getLogger().info(redColor + "╚══════════════════════════════════════════════════════════════╝");

               try {
                  this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getServer().getPluginManager().disablePlugin(this.plugin), 600L);
               } catch (Exception var6) {
               }

               return false;
            }
         }
      } else {
         String redColor = "\u001b[31m";
         String greenColor = "\u001b[32m";
         String resetColor = "\u001b[0m";
         this.plugin.getLogger().info(redColor + "██╗   ██╗ ██╗    ████████╗ ██╗ ███╗   ███╗  █████╗  ████████╗ ███████╗" + resetColor);
         this.plugin.getLogger().info(redColor + "██║   ██║ ██║    ╚══██╔══╝ ██║ ████╗ ████║ ██╔══██╗ ╚══██╔══╝ ██╔═" + resetColor);
         this.plugin.getLogger().info(redColor + "██║   ██║ ██║       ██║    ██║ ██╔████╔██║ ███████║    ██║    █████╗" + resetColor);
         this.plugin.getLogger().info(redColor + "██║   ██║ ██║       ██║    ██║ ██║╚██╔╝██║ ██╔══██║    ██║    ██╔══╝" + resetColor);
         this.plugin.getLogger().info(redColor + "╚██████╔╝ ███████╗  ██║    ██║ ██║ ╚═╝ ██║ ██║  ██║    ██║    ███████╗" + resetColor);
         this.plugin.getLogger().info(redColor + " ╚═════╝   ╚══════╝ ╚═╝    ╚═╝ ╚═╝     ╚═╝ ╚═╝  ╚═╝    ╚═╝    ╚══════╝" + resetColor);
         this.plugin.getLogger().info("");
         this.plugin.getLogger().info(redColor + "          ███████╗ ████████╗ ██╗   ██╗ ██████╗  ██╗  ██████╗            " + resetColor);
         this.plugin.getLogger().info(redColor + "          ██╔════╝ ╚══██╔══╝ ██║   ██║ ██╔══██╗ ██║ ██╔═══██╗           " + resetColor);
         this.plugin.getLogger().info(redColor + "          ███████╗    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
         this.plugin.getLogger().info(redColor + "          ╚════██║    ██║    ██║   ██║ ██║  ██║ ██║ ██║   ██║           " + resetColor);
         this.plugin.getLogger().info(redColor + "          ███████║    ██║    ╚██████╔╝ ██████╔╝ ██║ ╚██████╔╝          " + resetColor);
         this.plugin.getLogger().info(redColor + "          ╚══════╝    ╚═╝     ╚═════╝  ╚═════╝  ╚═╝  ╚═════╝           " + resetColor);
         this.plugin.getLogger().info("");
         this.plugin.getLogger().info("");
         this.plugin.getLogger().info(redColor + "╔══════════════════════════════════════════════════════════════╗");
         this.plugin.getLogger().info(redColor + "║                    NO LICENSE KEY FOUND                      ║");
         this.plugin.getLogger().info(redColor + "╠══════════════════════════════════════════════════════════════╣");
         String displayedKey = this.licenseKey == null ? "" : (this.licenseKey.length() >= 64 ? this.licenseKey.substring(0, 64) + "..." : this.licenseKey);
         this.plugin.getLogger().info(redColor + "║ Key: " + displayedKey + "                                           ║");
         this.plugin.getLogger().info(redColor + "║ Contact EliVB97 for help                                     ║");
         this.plugin.getLogger().info(redColor + "╚══════════════════════════════════════════════════════════════╝");

         try {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getServer().getPluginManager().disablePlugin(this.plugin), 600L);
         } catch (Exception var8) {
         }

         return false;
      }
   }

   private boolean validateWithAPI(String licenseKey, String serverId) {
      HttpURLConnection conn = null;

      boolean var10;
      try {
         this.disableSSLVerification();
         URL url = new URL("https://web14641.cweb06.gamingcontrol.de/license/admin.php");
         conn = (HttpURLConnection)url.openConnection();
         conn.setInstanceFollowRedirects(true);
         conn.setConnectTimeout(15000);
         conn.setReadTimeout(15000);
         conn.setRequestMethod("POST");
         conn.setRequestProperty("Content-Type", "application/json");
         String var10002 = this.plugin.getName();
         conn.setRequestProperty("User-Agent", var10002 + "/" + this.plugin.getDescription().getVersion());
         conn.setRequestProperty("Accept", "application/json");
         conn.setDoOutput(true);
         String jsonData = String.format("{\"license_key\":\"%s\",\"server_id\":\"%s\",\"plugin_id\":\"%s\"}", licenseKey, serverId, "donutorder");
         OutputStream os = conn.getOutputStream();
         os.write(jsonData.getBytes("UTF-8"));
         os.flush();
         os.close();
         int responseCode = conn.getResponseCode();
         if (responseCode == 301 || responseCode == 302 || responseCode == 303) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            url = new URL(newUrl);
            conn = (HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            var10002 = this.plugin.getName();
            conn.setRequestProperty("User-Agent", var10002 + "/" + this.plugin.getDescription().getVersion());
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            os = conn.getOutputStream();
            os.write(jsonData.getBytes("UTF-8"));
            os.flush();
            os.close();
            responseCode = conn.getResponseCode();
         }

         InputStream inputStream;
         if (responseCode >= 200 && responseCode < 300) {
            inputStream = conn.getInputStream();
         } else {
            inputStream = conn.getErrorStream();
         }

         if (inputStream != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder response = new StringBuilder();

            String line;
            while((line = br.readLine()) != null) {
               response.append(line);
            }

            br.close();
            String responseStr = response.toString();
            if (!responseStr.contains("\"valid\":true") && !responseStr.contains("\"message\":\"LICENSE_VALID\"") && !responseStr.contains("\"valid\": true")) {
               if (!responseStr.contains("LICENSE_NOT_FOUND") && !responseStr.contains("LICENSE_EXPIRED") && !responseStr.contains("WRONG_PLUGIN") && !responseStr.contains("TOO_MANY_SERVERS") && responseStr.contains("LICENSE_INACTIVE")) {
               }

               boolean var13 = false;
               return var13;
            }

            boolean var14 = true;
            return var14;
         }

         var10 = false;
      } catch (Exception var18) {
         if (conn != null) {
         }

         boolean jsonData = false;
         return jsonData;
      } finally {
         if (conn != null) {
            conn.disconnect();
         }

      }

      return var10;
   }

   private void disableSSLVerification() {
      try {
         TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
               return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
         }};
         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init((KeyManager[])null, trustAllCerts, new SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
         HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
               return true;
            }
         };
         HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
      } catch (Exception var4) {
      }

   }

   private String generateHash(String input) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         byte[] hash = md.digest(input.getBytes("UTF-8"));
         StringBuilder hexString = new StringBuilder();

         for(byte b : hash) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
               hexString.append('0');
            }

            hexString.append(hex);
         }

         String fullHash = hexString.toString().toUpperCase();
         String formatted = "";

         for(int i = 0; i < 32 && i < fullHash.length(); i += 8) {
            if (i > 0) {
               formatted = formatted + "-";
            }

            int end = Math.min(i + 8, fullHash.length());
            formatted = formatted + fullHash.substring(i, end);
         }

         return formatted;
      } catch (Exception var10) {
         return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 32);
      }
   }

   private void saveLicenseFile() {
      try {
         this.licenseConfig.save(this.licenseFile);
      } catch (IOException var2) {
      }

   }

   public String getServerId() {
      return this.serverId;
   }

   public String getLicenseKey() {
      return this.licenseKey;
   }

   public boolean isLicenseValid() {
      return this.licenseValid;
   }

   public void setLicenseKey(String key) {
      this.licenseConfig.set("license-key", key);
      this.saveLicenseFile();
      this.licenseKey = key;
      this.licenseValid = this.validateWithAPI(key, this.serverId);
   }
}
