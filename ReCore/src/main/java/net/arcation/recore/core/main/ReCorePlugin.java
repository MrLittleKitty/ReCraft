package net.arcation.recore.core.main;

import net.arcation.recore.api.Database;
import net.arcation.recore.api.PlayerProfile;
import net.arcation.recore.api.ReCore;
import net.arcation.recore.core.database.DatabaseQueryManager;
import net.arcation.recore.core.database.MySQLDatabase;
import net.arcation.recore.core.database.SQLiteDatabase;
import net.arcation.recore.core.utils.CommonErrors;
import net.arcation.recore.core.utils.ConfigWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReCorePlugin extends JavaPlugin implements Listener
{
    private static boolean debug;
    public static boolean isDebugOn()
    {
        return debug;
    }

    private static JavaPlugin instance;
    public static JavaPlugin getInstance()
    {
        return instance;
    }

    private static ErrorLogger errorLogger;
    public static net.arcation.recore.api.ErrorLogger getErrorLogger()
    {
        return errorLogger;
    }

    private Core core;

    private ConfigWrapper wrapper;
    private ProfileManager profileManager;

    private Database database;
    private boolean active;

    @Override
    public void onEnable()
    {
        //static stuff
        instance = this;
        ReCorePlugin.errorLogger = new ErrorLogger(this);
        active = true;
        core = new Core();
        Bukkit.getServicesManager().register(ReCore.class, core, this, ServicePriority.Highest);
        setupConfig();

        database = setupDatabase();
        if(database == null)
        {
            active = false;
            getErrorLogger().logWarning("There was an error connecting to the database. The plugin will not run.");
            return;
        }
        loadDataConfig();

        //groupManager = new GroupManager(wrapper);
        profileManager = ProfileManager.getInstance(wrapper);

        for(Player player : Bukkit.getOnlinePlayers())
            profileManager.loadProfile(player);

        new GroupCommand(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
        Bukkit.getScheduler().cancelTasks(this);
    }

    public Core getCore()
    {
        return core;
    }

    private void setupConfig()
    {
        boolean b = setIfNotSet(getConfig(), "Show-Debug-Messages", true);

        if(b)
            saveConfig();

        debug = getConfig().getBoolean("Show-Debug-Messages");
    }


    private void loadDataConfig()
    {
        //if(wrapper == null)
        //{
            File file = new File(this.getDataFolder(), "Data.yml");
            if (!file.exists())
            {
                try
                {
                    file.createNewFile();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            wrapper = new ConfigWrapper(YamlConfiguration.loadConfiguration(file),file);
        //}
    }

    private Database setupDatabase()
    {
        createConfigDefaults();
        database = null;
        boolean mysql = getConfig().getBoolean("Database.Use-MySQL");
        if(mysql)
        {
            ConfigurationSection mysqlSec = getConfig().getConfigurationSection("Database.MySQL");

            String user,database,password,port,hostname;
            user = mysqlSec.getString("User");
            database = mysqlSec.getString("Database");
            password = mysqlSec.getString("Password");
            port = mysqlSec.getString("Port");
            hostname = mysqlSec.getString("Host-Name");

            this.database = new MySQLDatabase(hostname,port,database,user,password);
        }
        else
        {
            File file = new File(this.getDataFolder(),"VanillaCore.db");
            if(!file.exists())
            {
                try
                {
                    file.createNewFile();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            database = new SQLiteDatabase(file);
            //logInfoMessage("MySQL is turned off. It is currently the only database option and must be enabled.");
            //getErrorLogger().logInfoMessage("MySQL is turned off. It is currently the only database option.");
        }

        if(database == null || !database.isUseable())
            return null;

        int selectThreads = getConfig().getInt("Database.Select-Threads");
        int insertThreads = getConfig().getInt("Database.Insert-Threads");

        //Run the database handlers to prepare themselves to start logging queries
        new DatabaseQueryManager(database, selectThreads, insertThreads);
        //new DBLogQueryAsync(db);
        //new DBLogQueryAsync(db);

        return database;
    }

    private void createConfigDefaults()
    {
        boolean save = false;
        ConfigurationSection config = getConfig();

        ConfigurationSection db = config.getConfigurationSection("Database");
        if(db == null)
        {
            db = config.createSection("Database");
            save = true;
        }
        save = setIfNotSet(db,"Use-MySQL",true);
        save = setIfNotSet(db, "Select-Threads", 2);
        save = setIfNotSet(db, "Insert-Threads", 1);
        ConfigurationSection mysql = db.getConfigurationSection("MySQL");
        if(mysql == null)
        {
            mysql = db.createSection("MySQL");
            save = true;
        }
        save = setIfNotSet(mysql,"Host-Name", "Test");
        save = setIfNotSet(mysql,"Port", "Test");
        save = setIfNotSet(mysql,"User", "Test");
        save = setIfNotSet(mysql,"Password", "Test");
        save = setIfNotSet(mysql,"Database", "Test");

        if(save)
            saveConfig();
    }

    private boolean setIfNotSet(ConfigurationSection section, String path, Object value)
    {
        if(!section.isSet(path))
        {
            section.set(path, value);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
    public void permissionsCheck(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage().split(" ")[0];
        if (command.startsWith("/"))
            command = command.substring(1);

        PlayerProfile profile = profileManager.getProfile(event.getPlayer().getUniqueId());
        assert profile != null;
        if(profile == null || !profile.getGroup().hasPermission("Comm_" + command))
        {
            event.getPlayer().sendMessage(CommonErrors.NO_PERMISSION.format("command"));
            event.setCancelled(true);
        }

    }

    class Core implements ReCore
    {
        @Override
        public boolean isActive()
        {
            return active;
        }

        @Override
        public Database getDatabase()
        {
            return database;
        }

        @Override
        public ProfileManager getAssetManager()
        {
            return ProfileManager.getInstance(wrapper);
        }

        @Override
        public UUID registerDelay(final long delayTime, final TimeUnit unit)
        {
            UUID id = UUID.randomUUID();
            ProfileManager.getInstance(wrapper).registerDelay(id,TimeUnit.MILLISECONDS.convert(delayTime,unit));
            return id;
        }

        @Override
        public net.arcation.recore.api.ErrorLogger registerErrorLogger(final JavaPlugin plugin)
        {
            return new ErrorLogger(plugin);
        }
    }

}
