package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings;
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.UnpluggedPaperApiImpl;
import dev.electricsteve.unpluggedPaper.api.PlayerSpawnOptions;
import dev.electricsteve.unpluggedPaper.api.UPFakePlayerSettings;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public class UnplugCommand {
    public static final Function<World, FakePlayerSettings> DEFAULT_FAKE_PLAYER_SETTINGS = (world) -> new UPFakePlayerSettings(
        true, // collidable
        true, // pickupItems
        false, // invulnerable
        false, // infiniteFoodLevel
        false, // autoReplenish
        false, // autoFish
        Bukkit.getServer().getSimulationDistance(), // simulationDistance
        false, // xpNoCooldown
        false, // autoEquipTool
        world.getGameRuleValue(GameRules.KEEP_INVENTORY), // keepInventory
        true, // deathMessage
        FakePlayerSettings.InteractedAction.NONE, // interactedAction
        FakePlayerSettings.InteractedAction.NONE, // shiftInteractedAction
        FakePlayerSettings.DeathAction.QUIT, // deathAction
        FakePlayerSettings.KeepingMode.ALWAYS // keepingMode
    );

    private final UnpluggedPaper plugin;

    public UnplugCommand(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Command("unplug")
    public void unplug(BukkitCommandActor actor, @Default("0") @Range(min = 0) long durationMinutes, @Default("\"\"") String reason) {
        Player player = actor.asPlayer();
        if (player == null) {
            actor.reply("You must be a player to use this command!");
            return;
        }
        if (durationMinutes == 0) durationMinutes = this.plugin.getConfiguration().getUnplugged().defaultUnpluggedTimeout();
        long durationMillis = durationMinutes * 60L * 1000L;
        World world = player.getWorld();
        String name = player.getName();
        Location location = player.getLocation();
        UUID uuid = player.getUniqueId();
        Component displayName = player.displayName();
        Set<ProfileProperty> profileProperties = player.getPlayerProfile().getProperties();
        ProfileProperty textureProperty = profileProperties.stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);
        PlayerTextures playerTextures = textureProperty != null && textureProperty.getSignature() != null ?
            new PlayerTextures(textureProperty.getValue(), textureProperty.getSignature()) : null;
        double health = player.getHealth();
        int foodLevel = player.getFoodLevel();

        player.kick(Component.text("Your player will be AFK for: ", NamedTextColor.GOLD)
            .append(Component.text(DurationFormatUtils.formatDuration(durationMillis, "[d' days 'H' hours 'm' minutes 's' seconds']", true), NamedTextColor.GREEN)));
        // We have to delay this, because there seems some kind of race condition where the fake player is in a "ghost" state,
        // where it is as if the player is connected but no packets are being sent.
        Bukkit.getScheduler().runTaskLater(this.plugin, _ -> {
            FakePlayerManager fpm = this.plugin.getFppComponent().getFpm();
            FakePlayerPlusJava.getFromRepository(fpm, name).thenAccept(fakePlayerInRepository -> {
                if (fakePlayerInRepository != null) {
                    UUID existingUuid = fakePlayerInRepository.getUuid();
                    if (!existingUuid.equals(uuid)) {
                        try {
                            FakePlayerPlusJava.deleteFromRepository(fpm, existingUuid).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                long startTime = System.currentTimeMillis();
                PlayerSpawnOptions options = PlayerSpawnOptions.builder()
                    .location(location)
                    .uuid(uuid)
                    .playerTextures(playerTextures)
                    .sleepingIgnored(false)
                    .setClientLoaded(true)
                    .health(health)
                    .foodLevel(foodLevel)
                    .fakePlayerSettings(DEFAULT_FAKE_PLAYER_SETTINGS.apply(world))
                    .enableAdvancements(true)
                    .durationMillis(durationMillis)
                    .durationOverCallback(_ -> {
                        this.plugin.getPlayerEntry(uuid).ifPresent(playerEntry -> playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.EXPIRED));
                        this.plugin.broadcastPlugBack(displayName.color(NamedTextColor.YELLOW), " AFK session expired", System.currentTimeMillis() - startTime);
                        this.plugin.savePlayersConfiguration();
                    })
                    .hidePlayer(this.plugin.getConfiguration().getUnplugged().unpluggedHidePlayer())
                    .hidePlayerOps(this.plugin.getConfiguration().getUnplugged().unpluggedHideFromOps())
                    .build();
                UnpluggedPaperApiImpl.spawnPlayerWithUuidAndSkin(name, plugin, options);
                this.plugin.updatePlayerEntry(uuid, name, UnpluggedPlayers.UnpluggedStatus.ACTIVE, durationMillis, startTime, reason);

                Component broadcast = displayName.color(NamedTextColor.YELLOW).append(Component.text(" has been unplugged", NamedTextColor.YELLOW));
                if (reason != null && !reason.isEmpty()) broadcast = broadcast.append(Component.text(", ", NamedTextColor.YELLOW)).append(Component.text(reason));
                this.plugin.broadcast(broadcast);
            });
        }, 1); // We wait 1 tick
    }
}
