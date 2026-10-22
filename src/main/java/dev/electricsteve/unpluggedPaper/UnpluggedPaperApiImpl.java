package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public class UnpluggedPaperApiImpl {
    public static void spawnPlayerWithSkin(BukkitCommandActor actor, String name, UnpluggedPaper plugin, @Nullable Location location, @Nullable UUID uuid) {
        FakePlayerManager fpm = plugin.getFppComponent().getFpm();
        CommandSender console = Bukkit.getConsoleSender();
        FakePlayerPlusJava.spawn(
            fpm,
            name,
            console,
            location,
            uuid
        ).thenAccept(fakePlayer -> {
            if (fakePlayer == null) return;
            actor.reply("Spawned fake player " + fakePlayer.getName() + "!");
            FakePlayerPlusJava.getPlayerTexturesByName(name, true).thenAccept(playerTextures -> {
                if (playerTextures == null) {
                    actor.reply("Failed to get player textures for " + name + "!");
                    return;
                }
                FakePlayerPlusJava.setTextures(plugin, fakePlayer, playerTextures).thenAccept(_ -> {
                    actor.reply("Set textures for fake player " + fakePlayer.getName() + "!");
                });
                FakePlayerPlusJava.saveSkin(fpm, fakePlayer);
            });
        });
    }
}
