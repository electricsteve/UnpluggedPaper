package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;

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

    private UnpluggedFppComponent fppComponent;
    private UnpluggedConfiguration configuration;

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
