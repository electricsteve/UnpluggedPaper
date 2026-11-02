package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

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
            Player bukkitPlayer = fakePlayer.getPlayer();
            bukkitPlayer.getScheduler().run(plugin, _ -> {
                if (options.getHealth() != -1.0) bukkitPlayer.setHealth(options.getHealth());
                if (options.getFoodLevel() != -1) bukkitPlayer.setFoodLevel(options.getFoodLevel());
                if (!options.isSleepingIgnored()) bukkitPlayer.setSleepingIgnored(false);
                if (options.isSetClientLoaded()) fakePlayer.getNms().setClientLoaded();
                if (options.isEnableAdvancements()) fakePlayer.getNms().enableAdvancements();
            }, () -> {});

            if (options.getPlayerTextures() != null) {
                try {
                    FakePlayerPlusJava.setTextures(plugin, fakePlayer, options.getPlayerTextures()).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                FakePlayerPlusJava.saveSkin(fpm, fakePlayer);
            }

            if (options.isHidePlayer()) plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> (options.isHidePlayerOps() || !player.isOp()) && !player.getUniqueId().equals(fakePlayer.getUuid()))
                .forEach(player -> player.getScheduler().run(plugin, _ -> player.hidePlayer(plugin, bukkitPlayer), () -> {}));

            if (options.getDurationMillis() > 0) {
                Bukkit.getAsyncScheduler().runDelayed(plugin, _ -> bukkitPlayer.getScheduler().run(plugin, _ -> {
                    fakePlayer.quit("The unplugged session has ended.");
                    if (options.getDurationOverCallback() != null) {
                        options.getDurationOverCallback().accept(fakePlayer);
                    }
                }, () -> {}), options.getDurationMillis(), TimeUnit.MILLISECONDS);
            }
        });
    }
}
