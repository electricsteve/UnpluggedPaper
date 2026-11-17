package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings;
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

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
        if (durationMinutes == 0)
            durationMinutes = plugin.getConfiguration().getUnplugged().defaultUnpluggedTimeout();
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

        // We do it here, because the kick event listener needs it to detect if a player was kicked for unplugging
        long startTime = System.currentTimeMillis();
        plugin.updatePlayerEntry(uuid, name, UnpluggedPlayers.UnpluggedStatus.ACTIVE, durationMillis, startTime, reason);
        player.kick(Component.translatable("unplugged.kickMessage", NamedTextColor.GOLD, formatTimeDifference(durationMillis)));
        // We have to delay this, because there seems some kind of race condition where the fake player is in a "ghost" state,
        // where it is as if the player is connected but no packets are being sent.
        Bukkit.getScheduler().runTaskLater(plugin, _ -> {
            FakePlayerManager fpm = plugin.getFppComponent().getFpm();
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
                        plugin.getPlayerEntry(uuid).ifPresent(playerEntry -> {
                            playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.EXPIRED);
                            playerEntry.setReason(MiniMessage.miniMessage().serialize(
                                Component.text()
                                    .append(plugin.getConfiguration().getMessages().displayDuration() ?
                                        Component.translatable("unplugged.feedback.successful.time", NamedTextColor.YELLOW, formatTimeDifference(durationMillis))
                                        : Component.translatable("unplugged.feedback.successful", NamedTextColor.YELLOW))
                                    .append(Component.translatable("unplugged.feedback.reason", NamedTextColor.GRAY,
                                        Component.translatable("unplugged.feedback.expired", NamedTextColor.YELLOW)))
                                    .build()
                            ));
                        });
                        plugin.broadcastPlugBack(displayName, "unplugged.broadcast.expired", System.currentTimeMillis() - startTime);
                        plugin.savePlayersConfiguration();
                    })
                    .hidePlayer(plugin.getConfiguration().getUnplugged().unpluggedHidePlayer())
                    .hidePlayerOps(plugin.getConfiguration().getUnplugged().unpluggedHideFromOps())
                    .build();
                UnpluggedPaperApiImpl.spawnPlayerWithUuidAndSkin(name, plugin, options);

                Component broadcast = reason.isEmpty() ?
                    Component.translatable("unplugged.broadcast.unplugged", NamedTextColor.YELLOW, displayName)
                    : Component.translatable("unplugged.broadcast.unplugged.reason", NamedTextColor.YELLOW, displayName, Component.text(reason, NamedTextColor.WHITE));
                plugin.broadcast(broadcast);
            });
        }, 10); // We wait 10 ticks, this was increased because the fake player was sometimes summoned before the real player left
    }

    public static @NonNull TextComponent formatTimeDifference(long durationMillis) {
        return Component.text(DurationFormatUtils.formatDuration(durationMillis, "[d' days 'H' hours 'm' minutes 's' seconds ']", true), NamedTextColor.GREEN);
    }
}
