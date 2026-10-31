package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.electricsteve.unpluggedPaper.commands.UnplugCommand;
import dev.electricsteve.unpluggedPaper.config.UnpluggedConfiguration;
import lombok.Getter;
import revxrsal.commands.bukkit.BukkitLamp;

@Getter
public final class UnpluggedPaper extends JavaPlugin {
    private static final String REQUIRED_FPP_VERSION = "2.2.50";

    public static Logger LOGGER = LogManager.getLogger("UnpluggedPaper");
    private static boolean debug = Boolean.getBoolean("unplugged.debug"); // Set by command-line argument

    private UnpluggedFppComponent fppComponent;
    private UnpluggedConfiguration configuration;

    /// Prints a debug log if debug logging is enabled. Only works after configuration loading (or if in IDE).
    /// @param key message
    /// @param args parameters
    private static void debugLog(String key, Object... args) {
        if (debug) {
            LOGGER.info(String.format("[DEBUG] %s", key), args);
        }
    }

    @Override
    public void onEnable() {
        String fppVersion = Bukkit.getPluginManager().getPlugin("FakePlayerPlus").getPluginMeta().getVersion();
        if (!fppVersion.equals(REQUIRED_FPP_VERSION)) {
            getComponentLogger().error("FakePlayerPlus version " + REQUIRED_FPP_VERSION + " is required, but version {} is installed. Disabling plugin.", fppVersion);
            return;
        }

        // Config
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
            .header(
                """
                UnpluggedPaper Configuration
                """
            )
            .footer("Author: ElectricSteve")
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .build();
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.configuration = YamlConfigurations.update(
            configFile,
            UnpluggedConfiguration.class,
            properties
        );

        if (this.configuration.getMain().debugMode()) debug = true;
        if (!this.configuration.getMain().unpluggedPaperEnabled()) {
            debugLog("unpluggedPaperEnabled set to false, exiting");
            return;
        }

        this.fppComponent = new UnpluggedFppComponent();
        FakePlayerPlusPluginApi.Companion.getApi().registerComponent(fppComponent);
        var lamp = BukkitLamp.builder(this).build();
        lamp.register(new UnplugCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
