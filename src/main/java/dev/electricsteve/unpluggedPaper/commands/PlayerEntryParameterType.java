package dev.electricsteve.unpluggedPaper.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public final class PlayerEntryParameterType implements ParameterType<BukkitCommandActor, UnpluggedPlayers.PlayerEntry> {

    private final UnpluggedPaper plugin;

    public PlayerEntryParameterType(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public UnpluggedPlayers.PlayerEntry parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        String playerName = input.readString();
        Optional<UnpluggedPlayers.PlayerEntry> playerEntry = plugin.getPlayerEntry(playerName);
        if (playerEntry.isEmpty()) throw new CommandErrorException("No player found");
        return playerEntry.get();
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return (context) -> plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers()
            .stream().map(UnpluggedPlayers.PlayerEntry::getName).toList();
    }
}
