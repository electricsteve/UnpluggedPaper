package dev.electricsteve.unpluggedPaper.commands;

import static dev.electricsteve.unpluggedPaper.commands.UnplugCommand.DEFAULT_FAKE_PLAYER_SETTINGS;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer;
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.UnpluggedPaperApiImpl;
import dev.electricsteve.unpluggedPaper.UnpluggedPlayer;
import dev.electricsteve.unpluggedPaper.api.PlayerSpawnOptions;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("unplugged-admin")
@CommandPermission("unplugged.admin")
public class AdminCommand {
    // TODO: save & set commands (changing configuration)
    private final UnpluggedPaper plugin;

    public AdminCommand(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    public void reload(BukkitCommandActor actor) {
        plugin.reloadConfiguration();
    }

    @Subcommand("purge")
    public void purge(BukkitCommandActor actor) {
        List<UnpluggedPlayers.PlayerEntry> activeFakePlayers = new java.util.ArrayList<>();
        int purged = 0;
        int resynced = 0;
        int shadows = 0;
        for (UnpluggedPlayers.PlayerEntry entry : plugin.getUnpluggedPlayersConfig().emptyList()) {
            purged++;
            if (entry.getStatus() == UnpluggedPlayers.UnpluggedStatus.ACTIVE
            && plugin.getServer().getPlayer(entry.getUuid()) != null) activeFakePlayers.add(entry);
        }
        for (UnpluggedPlayers.PlayerEntry entry : activeFakePlayers) {
            resynced++;
            shadows++;
            plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers().add(entry);
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = plugin.getPlayerEntry(player.getUniqueId());
            if (optionalPlayerEntry.isEmpty()) {
                resynced++;
                plugin.updatePlayerEntry(player.getUniqueId(), player.getName(), UnpluggedPlayers.UnpluggedStatus.INACTIVE, -1, -1, "");
            }
        }
        actor.reply(Component.text("Purged: ", NamedTextColor.YELLOW)
            .append(Component.text(purged, NamedTextColor.RED))
            .append(Component.text(" players, and then resynced ", NamedTextColor.YELLOW))
            .append(Component.text(resynced, NamedTextColor.GREEN))
            .append(Component.text(" current players, with ", NamedTextColor.YELLOW))
            .append(Component.text(shadows, NamedTextColor.GOLD))
            .append(Component.text(" shadows", NamedTextColor.YELLOW)));
    }

    @Subcommand("list")
    public void list(BukkitCommandActor actor, ListArgument listArgument) {
        TextComponent.Builder finalBuilder = Component.text();
        if (listArgument == ListArgument.ALL || listArgument == ListArgument.PLAYERS) playerMap(finalBuilder);
        if (listArgument == ListArgument.ALL) finalBuilder.append(Component.text("\n\n"));
        if (listArgument == ListArgument.ALL || listArgument == ListArgument.UNPLUGGED) unpluggedMap(finalBuilder);
        actor.reply(finalBuilder.build());
    }

    private void unpluggedMap(TextComponent.Builder finalBuilder) {
        int i = -1;
        finalBuilder.append(Component.text("Unplugged Map:\n", NamedTextColor.LIGHT_PURPLE));
        for (UnpluggedPlayers.PlayerEntry entry : plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers()) {
            if (entry.getStatus() != UnpluggedPlayers.UnpluggedStatus.ACTIVE) continue;
            i++;
            TextComponent entryText = getUnpluggedDebugText(entry, i);
            finalBuilder.append(entryText);
        }
        finalBuilder.append(Component.text("(%s total)".formatted(i + 1), NamedTextColor.GOLD));
    }

    private void playerMap(TextComponent.Builder finalBuilder) {
        int i = -1;
        finalBuilder.append(Component.text("Player Map:\n", NamedTextColor.LIGHT_PURPLE));
        for (UnpluggedPlayers.PlayerEntry entry : plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers()) {
            i++;
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(entry.getUuid());
            TextComponent entryText = getPlayerDebugText(entry, i, player);
            finalBuilder.append(entryText);
        }
        finalBuilder.append(Component.text("(%s total)".formatted(i + 1), NamedTextColor.GOLD));
    }

    @Subcommand("info")
    public void info(BukkitCommandActor actor, @Default("me") OfflinePlayer target) {
        Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = plugin.getPlayerEntry(target.getUniqueId());
        if (optionalPlayerEntry.isEmpty()) {
            actor.reply(Component.text("Error: player not in player map", NamedTextColor.RED));
            return;
        }
        actor.reply(Component.text("\n").append(getPlayerDebugText(optionalPlayerEntry.get(), -1, target)));
    }

    @Subcommand("spawn")
    public void spawn(BukkitCommandActor actor, @Named("player") UnpluggedPlayers.PlayerEntry playerEntry,
                      @Default("0") @Range(min = 0) long durationMinutes, @Default("\"\"") String reason) {
        String name = playerEntry.getName();
        UUID uuid = playerEntry.getUuid();
        if (durationMinutes == 0) durationMinutes = plugin.getConfiguration().getUnplugged().defaultUnpluggedTimeout();
        long durationMillis = durationMinutes * 60L * 1000L;
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer.isOnline()) {
            actor.reply(Component.text("Error: player is online", NamedTextColor.RED));
            return;
        } else if (offlinePlayer.isBanned()) {
            actor.reply(Component.text("Error: player is banned", NamedTextColor.RED));
            return;
        }
        Location location = offlinePlayer.getLocation();
        if (location == null) {
            actor.reply(Component.text("Error: no location found", NamedTextColor.RED));
            return;
        }
        double health = 20.0; // TODO: fix this, currently waiting on response from paper devs (https://discord.com/channels/289587909051416579/925530366192779286/1566422373270421515)
        int foodLevel = 20;

        // Player profile textures
        PlayerProfile playerProfile = offlinePlayer.getPlayerProfile();
        playerProfile.complete();
        Set<ProfileProperty> profileProperties = playerProfile.getProperties();
        ProfileProperty textureProperty = profileProperties.stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);
        PlayerTextures playerTextures = textureProperty != null && textureProperty.getSignature() != null ?
            new PlayerTextures(textureProperty.getValue(), textureProperty.getSignature()) : null;

        long startTime = System.currentTimeMillis();
        this.plugin.updatePlayerEntry(uuid, name, UnpluggedPlayers.UnpluggedStatus.ACTIVE, durationMillis, startTime, reason);

        // TODO: merge this with the one in `UnplugCommand`
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
                .fakePlayerSettings(DEFAULT_FAKE_PLAYER_SETTINGS.apply(location.getWorld()))
                .enableAdvancements(true)
                .durationMillis(durationMillis)
                .durationOverCallback(_ -> {
                    plugin.getPlayerEntry(uuid).ifPresent(entry -> entry.setStatus(UnpluggedPlayers.UnpluggedStatus.EXPIRED));
                    plugin.broadcastPlugBack(Component.text(name), " AFK session expired", System.currentTimeMillis() - playerEntry.getStartTime());
                    plugin.savePlayersConfiguration();
                })
                .hidePlayer(plugin.getConfiguration().getUnplugged().unpluggedHidePlayer())
                .hidePlayerOps(plugin.getConfiguration().getUnplugged().unpluggedHideFromOps())
                .build();
            UnpluggedPaperApiImpl.spawnPlayerWithUuidAndSkin(name, plugin, options);

            Component broadcast = Component.text(name, NamedTextColor.YELLOW).append(Component.text(" has been unplugged", NamedTextColor.YELLOW));
            if (!reason.isEmpty()) broadcast = broadcast.append(Component.text(", ", NamedTextColor.YELLOW)).append(Component.text(reason, NamedTextColor.WHITE));
            this.plugin.broadcast(broadcast);
        });
    }

    @Subcommand("kick")
    public void kick(BukkitCommandActor actor, UnpluggedPlayer unpluggedPlayer) {
        FakePlayer fakePlayer = unpluggedPlayer.fakePlayer();
        UnpluggedPlayers.PlayerEntry playerEntry = unpluggedPlayer.playerEntry();
        Component displayName = fakePlayer.getPlayer().displayName();
        actor.reply(Component.text(" - Kicking unplugged player: ", NamedTextColor.GRAY).append(displayName.color(NamedTextColor.YELLOW)));
        playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.INTERRUPTED);
        plugin.broadcastPlugBack(displayName.color(NamedTextColor.YELLOW), " AFK session terminated", System.currentTimeMillis() - playerEntry.getStartTime());
        plugin.savePlayersConfiguration();
        fakePlayer.quit("The unplugged session was terminated by an admin.");
    }

    @Subcommand("save")
    public void save(BukkitCommandActor actor) {

    }

    @Subcommand("set")
    public void set(BukkitCommandActor actor) {

    }

    private @NotNull TextComponent getUnpluggedDebugText(UnpluggedPlayers.PlayerEntry entry, int i) {
        // We assume this is an active session
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("[Entry: %s]\n".formatted(String.format("%02d", i)), NamedTextColor.BLUE));
        builder.append(detailLine("Name", entry.getName(), NamedTextColor.WHITE));
        builder.append(detailLine("UUID", entry.getUuid().toString(), NamedTextColor.WHITE));
        builder.append(detailLine("Status", entry.getStatus().getName(), NamedTextColor.GREEN));
        long timeLeftMillis = (entry.getTime() + entry.getStartTime()) - System.currentTimeMillis();
        if (timeLeftMillis < 0) timeLeftMillis = 1;
        String timeLeft = DurationFormatUtils.formatDuration(
            timeLeftMillis,
            "[d' days 'H' hours 'm' minutes 's' seconds']", true);
        String duration = DurationFormatUtils.formatDuration(entry.getTime(),
            "[d' days 'H' hours 'm' minutes 's' seconds']", true);
        String startTime = Instant.ofEpochMilli(entry.getStartTime()).toString();
        builder.append(detailLine("Time left", timeLeft, NamedTextColor.GOLD));
        if (!plugin.getConfiguration().getMain().reducedListDebugInfo()) {
            builder.append(detailLine("Total duration", duration, NamedTextColor.GREEN));
            builder.append(detailLine("Since", startTime, NamedTextColor.GREEN));
        }
        builder.append(detailLine("Reason", entry.getReason(), NamedTextColor.YELLOW));
        return builder.build();
    }

    private @NotNull TextComponent getPlayerDebugText(UnpluggedPlayers.PlayerEntry entry, int i, OfflinePlayer offlinePlayer) {
        UnpluggedPlayers.UnpluggedStatus status = entry.getStatus();
        NamedTextColor statusColor = switch (status) {
            case ACTIVE -> NamedTextColor.GOLD;
            case INACTIVE -> NamedTextColor.GREEN;
            case EXPIRED -> NamedTextColor.AQUA;
            case INTERRUPTED, TERMINATED -> NamedTextColor.RED;
            case RETURNED -> NamedTextColor.LIGHT_PURPLE;
        };
        Player player = offlinePlayer.getPlayer();
        String gameMode = player != null ? player.getGameMode().name().toLowerCase() : "[offline]";
        boolean flying = player != null && player.isFlying();
        Location location = offlinePlayer.getLocation();
        Component position = location == null ? Component.text("null", NamedTextColor.RED) : Component.text()
            .append(Component.text((location.getWorld() != null ? location.getWorld().getKey().toString() : "null") + " ", NamedTextColor.AQUA))
            .append(Component.text("[%.2f, %.2f, %.2f]".formatted(location.getX(), location.getY(), location.getZ())))
            .build();
        String totalDurationMinutes = String.valueOf(entry.getTime() / 60 / 1000);
        String timeLeftMillis = String.valueOf((entry.getTime() + entry.getStartTime()) - System.currentTimeMillis());
        String startTime = Instant.ofEpochMilli(entry.getStartTime()).toString();
        if (entry.getStatus() == UnpluggedPlayers.UnpluggedStatus.INACTIVE) {
            totalDurationMinutes = "-1";
            timeLeftMillis = "-1";
            startTime = "-1";
        }
        String reason = entry.getReason().isEmpty() ? "<>" : entry.getReason();

        TextComponent.Builder builder = Component.text();
        if (i >= 0) {
            builder.append(Component.text("[Entry: %s]\n".formatted(String.format("%02d", i)), NamedTextColor.BLUE));
        } else {
            builder.append(Component.text("Player Info:\n", NamedTextColor.BLUE));
        }
        builder.append(detailLine("Name", entry.getName(), NamedTextColor.WHITE));
        builder.append(detailLine("UUID", entry.getUuid().toString(), NamedTextColor.WHITE));
        builder.append(detailLine("Game", Component.text(gameMode, NamedTextColor.AQUA)
            .append(miniDetail("F", String.valueOf(flying), NamedTextColor.YELLOW, true))));
        builder.append(detailLine("Position", position));
        builder.append(detailLine("State", miniDetail("ST", status.getName(), statusColor, false)
            .append(miniDetail("TD", totalDurationMinutes, NamedTextColor.YELLOW, true))
            .append(miniDetail("TL", timeLeftMillis, NamedTextColor.YELLOW, true))
            .append(miniDetail("ST", startTime, NamedTextColor.YELLOW, true))
            .append(miniDetail("R", reason, NamedTextColor.YELLOW, true))));
        return builder.build();
    }

    private Component miniDetail(String key, String value, TextColor valueColor, boolean leadingSlash) {
        return Component.text((leadingSlash ? " / " : "") + key + ": ", NamedTextColor.WHITE).append(Component.text(value, valueColor));
    }

    private Component detailLine(String key, String value, TextColor valueColor) {
        return detailLine(key, Component.text(value, valueColor));
    }

    private Component detailLine(String key, Component value) {
        return Component.text(" - ", NamedTextColor.WHITE)
            .append(Component.text(key + ": ", NamedTextColor.GRAY))
            .append(value)
            .append(Component.text("\n"));
    }

    public enum ListArgument {
        ALL,
        UNPLUGGED,
        PLAYERS
    }
}
