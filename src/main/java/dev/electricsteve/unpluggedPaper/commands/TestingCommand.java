package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

@Command("testing")
public class TestingCommand {
    private final UnpluggedPaper plugin;

    public TestingCommand(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Subcommand("greet")
    public void greet(BukkitCommandActor actor) {
        actor.reply("Hello, " + actor.name() + "!");
    }

    @Subcommand("fake")
    @Description("Spawns a fake player with this username")
    public void fake(BukkitCommandActor actor, String name) {
        FakePlayerManager fpm = this.plugin.getFppComponent().getFpm();
        CommandSender console = Bukkit.getConsoleSender();
        FakePlayerPlusJava.spawn(
            fpm,
            name,
            console,
            null
        ).thenAccept(fakePlayer -> {
            if (fakePlayer == null) return;
            actor.reply("Spawned fake player " + fakePlayer.getName() + "!");
            FakePlayerPlusJava.getPlayerTexturesByName(name, true).thenAccept(playerTextures -> {
                if (playerTextures == null) {
                    actor.reply("Failed to get player textures for " + name + "!");
                    return;
                }
                FakePlayerPlusJava.setTextures(this.plugin, fakePlayer, playerTextures).thenAccept(_ -> {
                    actor.reply("Set textures for fake player " + fakePlayer.getName() + "!");
                });
                FakePlayerPlusJava.saveSkin(fpm, fakePlayer);
            });
        });
    }
}
