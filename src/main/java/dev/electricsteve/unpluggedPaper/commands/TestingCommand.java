package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.UnpluggedPaperApiImpl;
import dev.electricsteve.unpluggedPaper.api.PlayerSpawnOptions;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
import revxrsal.commands.annotation.Command;
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
        Set<ProfileProperty> profileProperties = player.getPlayerProfile().getProperties();
        ProfileProperty textureProperty = profileProperties.stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);
        PlayerTextures playerTextures = textureProperty != null && textureProperty.getSignature() != null ?
            new PlayerTextures(textureProperty.getValue(), textureProperty.getSignature()) : null;
        double health = player.getHealth();
        int foodLevel = player.getFoodLevel();

        player.kick(Component.text("You are now afk!"));
        // We have to delay this, because there seems some kind of race condition where the fake player is in a "ghost" state,
        // where it is as if the player is connected but no packets are being sent.
        Bukkit.getScheduler().runTaskLater(this.plugin, _ -> {
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
                    }
                }
                PlayerSpawnOptions options = PlayerSpawnOptions.builder()
                    .location(location)
                    .uuid(uuid)
                    .playerTextures(playerTextures)
                    .sleepingIgnored(false)
                    .setClientLoaded(true)
                    .health(health)
                    .foodLevel(foodLevel)
                    .build();
                UnpluggedPaperApiImpl.spawnPlayerWithUuidAndSkin(name, plugin, options);
            });
        }, 1); // We wait 1 tick
    }
}
