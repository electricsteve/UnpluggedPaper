package dev.electricsteve.unpluggedPaper.commands;

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

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

@Command("unplugged-admin")
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
        // TODO: create player entry on join
        // Remove all players from the data file and resyncs from online players
        List<UnpluggedPlayers.PlayerEntry> activeFakePlayers = new java.util.ArrayList<>();
        int i = 0;
        for (UnpluggedPlayers.PlayerEntry entry : plugin.getUnpluggedPlayersConfig().emptyList()) {
            i++;
            if (entry.getStatus() == UnpluggedPlayers.UnpluggedStatus.ACTIVE
            && plugin.getServer().getPlayer(entry.getUuid()) != null) activeFakePlayers.add(entry);
        }
        for (UnpluggedPlayers.PlayerEntry entry : activeFakePlayers) {
            plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers().add(entry);
        }
        int shadows = activeFakePlayers.size();
        actor.reply(Component.text("Purged: ", NamedTextColor.YELLOW)
            .append(Component.text(i, NamedTextColor.RED))
            .append(Component.text(" players, and then resynced ", NamedTextColor.YELLOW))
            .append(Component.text(shadows, NamedTextColor.GREEN))
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
    public void spawn(BukkitCommandActor actor) {
    }

    @Subcommand("kick")
    public void kick(BukkitCommandActor actor) {
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
