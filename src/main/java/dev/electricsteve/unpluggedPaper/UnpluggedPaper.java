package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import dev.electricsteve.unpluggedPaper.commands.TestingCommand;
import revxrsal.commands.bukkit.BukkitLamp;

public final class UnpluggedPaper extends JavaPlugin {
    private static final String REQUIRED_FPP_VERSION = "2.2.50";

    private UnpluggedFppComponent fppComponent;

    @Override
    public void onEnable() {
        String fppVersion = Bukkit.getPluginManager().getPlugin("FakePlayerPlus").getPluginMeta().getVersion();
        if (!fppVersion.equals(REQUIRED_FPP_VERSION)) {
            getComponentLogger().error("FakePlayerPlus version " + REQUIRED_FPP_VERSION + " is required, but version {} is installed. Disabling plugin.", fppVersion);
            return;
        }
        this.fppComponent = new UnpluggedFppComponent();
        FakePlayerPlusPluginApi.Companion.getApi().registerComponent(fppComponent);
        var lamp = BukkitLamp.builder(this).build();
        lamp.register(new TestingCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Should really lombok ts
    public UnpluggedFppComponent getFppComponent() {
        return fppComponent;
    }
}
