package dev.electricsteve.unpluggedPaper.commands;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
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

    // list, info, spawn, kick
}
