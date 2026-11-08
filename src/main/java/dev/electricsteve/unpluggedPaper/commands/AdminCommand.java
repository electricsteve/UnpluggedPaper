package dev.electricsteve.unpluggedPaper.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Instant;
import java.time.ZonedDateTime;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import revxrsal.commands.annotation.Command;
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
        // Remove fake players (INTERRUPTED), reload the config, load all players from config again (as if adding players at server start)
    }

    @Subcommand("list")
    public void list(BukkitCommandActor actor) {
        TextComponent.Builder finalBuilder = Component.text();
        int i = -1;
        finalBuilder.append(Component.text("Unplugged Map:\n", NamedTextColor.LIGHT_PURPLE));
        for (UnpluggedPlayers.PlayerEntry entry : plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers()) {
            i++;
            UnpluggedPlayers.UnpluggedStatus status = entry.getStatus();
            NamedTextColor statusColor = switch (status) {
                case ACTIVE -> NamedTextColor.GOLD;
                case INACTIVE -> NamedTextColor.GREEN;
                case EXPIRED -> NamedTextColor.AQUA;
                case INTERRUPTED, TERMINATED -> NamedTextColor.RED;
                case RETURNED -> NamedTextColor.LIGHT_PURPLE;
            };

            TextComponent.Builder entryTextBuilder = Component.text()
                .content("[Entry: %s]\n".formatted(String.format("%02d", i))).color(NamedTextColor.BLUE)
                .append(detailLine("Name", entry.getName(), NamedTextColor.WHITE))
                .append(detailLine("UUID", entry.getUuid().toString(), NamedTextColor.WHITE))
                .append(detailLine("Status", status.getName(), statusColor));
            if (status == UnpluggedPlayers.UnpluggedStatus.ACTIVE) {
                long timeLeftMillis = (entry.getTime() + entry.getStartTime()) - System.currentTimeMillis();
                if (timeLeftMillis < 0) timeLeftMillis = 1;
                String timeLeft = DurationFormatUtils.formatDuration(
                    timeLeftMillis,
                    "[d' days 'H' hours 'm' minutes 's' seconds']", true);
                String duration = DurationFormatUtils.formatDuration(entry.getTime(),
                    "[d' days 'H' hours 'm' minutes 's' seconds']", true);
                String startTime = Instant.ofEpochMilli(entry.getStartTime()).toString();
                entryTextBuilder.append(detailLine("Time left", timeLeft, NamedTextColor.GOLD));
                if (!plugin.getConfiguration().getMain().reducedListDebugInfo()) {
                    entryTextBuilder.append(detailLine("Total duration", duration, NamedTextColor.GREEN));
                    entryTextBuilder.append(detailLine("Since", startTime, NamedTextColor.GREEN));
                }
                entryTextBuilder.append(detailLine("Reason", entry.getReason(), NamedTextColor.YELLOW));
            }
            finalBuilder.append(entryTextBuilder);
        }
        finalBuilder.append(Component.text("(%s total)".formatted(i + 1), NamedTextColor.GOLD));
        actor.reply(finalBuilder.build());
    }

    @Subcommand("info")
    public void info(BukkitCommandActor actor) {
    }

    @Subcommand("spawn")
    public void spawn(BukkitCommandActor actor) {
    }

    @Subcommand("kick")
    public void kick(BukkitCommandActor actor) {
    }

    private Component detailLine(String key, String value, NamedTextColor valueColor) {
        return Component.text(" - ", NamedTextColor.WHITE)
            .append(Component.text(key + ": ", NamedTextColor.GRAY))
            .append(Component.text(value, valueColor))
            .append(Component.text("\n"));
    }
}
