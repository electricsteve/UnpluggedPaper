package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.UnpluggedPaperApiImpl;
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
        UnpluggedPaperApiImpl.spawnPlayerWithSkin(actor, name, plugin, null, null);
    }

    @Subcommand("unplug")
    public void unplug(BukkitCommandActor actor) {
        Player player = actor.asPlayer();
        if (player == null) {
            actor.reply("You must be a player to use this command!");
            return;
        }
        String name = player.getName();
        Location location = player.getLocation();
        UUID uuid = player.getUniqueId();

        player.kick(Component.text("You are now afk!"));

        FakePlayerManager fpm = this.plugin.getFppComponent().getFpm();
        FakePlayerPlusJava.getFromRepository(fpm, name).thenAccept(fakePlayer -> {
            if (fakePlayer != null) {
                UUID existingUuid = fakePlayer.getUuid();
                if (existingUuid != uuid) {
                    try {
                        FakePlayerPlusJava.deleteFromRepository(fpm, existingUuid).get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    UnpluggedPaperApiImpl.spawnPlayerWithSkin(actor, name, plugin, location, uuid);
                }
            }
        });
    }
}
