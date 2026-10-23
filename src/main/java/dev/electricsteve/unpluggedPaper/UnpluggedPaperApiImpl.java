package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import dev.electricsteve.unpluggedPaper.api.PlayerSpawnOptions;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;

public class UnpluggedPaperApiImpl {
    public static void spawnPlayerWithUuidAndSkin(String name, UnpluggedPaper plugin, PlayerSpawnOptions options) {
        FakePlayerManager fpm = plugin.getFppComponent().getFpm();
        CommandSender console = Bukkit.getConsoleSender();
        FakePlayerPlusJava.spawn(
            fpm,
            name,
            console,
            options.getLocation(),
            options.getUuid()
        ).thenAccept(fakePlayer -> {
            if (fakePlayer == null) return;
            if (options.getFakePlayerSettings() != null) {
                options.getFakePlayerSettings().sync(fakePlayer.getSettings());
            }
            fakePlayer.getPlayer().getScheduler().run(plugin, _ -> {
                if (options.getHealth() != -1.0) fakePlayer.getPlayer().setHealth(options.getHealth());
                if (options.getFoodLevel() != -1) fakePlayer.getPlayer().setFoodLevel(options.getFoodLevel());
                if (!options.isSleepingIgnored()) fakePlayer.getPlayer().setSleepingIgnored(false);
                if (options.isSetClientLoaded()) fakePlayer.getNms().setClientLoaded();
            }, () -> {});

            if (options.getPlayerTextures() != null) {
                FakePlayerPlusJava.setTextures(plugin, fakePlayer, options.getPlayerTextures()).thenAccept(_ -> {});
                FakePlayerPlusJava.saveSkin(fpm, fakePlayer);
            }
        });
    }
}
