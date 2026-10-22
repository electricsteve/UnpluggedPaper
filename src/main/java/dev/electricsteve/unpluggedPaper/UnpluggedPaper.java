package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;

import org.bukkit.plugin.java.JavaPlugin;

import dev.electricsteve.unpluggedPaper.commands.TestingCommand;
import revxrsal.commands.bukkit.BukkitLamp;

public final class UnpluggedPaper extends JavaPlugin {
    private UnpluggedFppComponent fppComponent;

    @Override
    public void onEnable() {
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
