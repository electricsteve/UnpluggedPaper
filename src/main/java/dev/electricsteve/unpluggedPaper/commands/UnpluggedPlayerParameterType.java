package dev.electricsteve.unpluggedPaper.commands;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer;

import org.jetbrains.annotations.NotNull;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.UnpluggedPlayer;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public class UnpluggedPlayerParameterType implements ParameterType<BukkitCommandActor, UnpluggedPlayer> {

    private final UnpluggedPaper plugin;

    public UnpluggedPlayerParameterType(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public UnpluggedPlayer parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        String name = input.readString();
        FakePlayer fakePlayer = plugin.getFppComponent().getFpm().get(name);
        UnpluggedPlayers.PlayerEntry playerEntry = plugin.getPlayerEntry(name).orElse(null);
        if (fakePlayer == null || playerEntry == null) throw new CommandErrorException("No player found");
        return new UnpluggedPlayer(playerEntry, fakePlayer);
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return _ -> plugin.getUnpluggedPlayersConfig().getUnpluggedPlayers()
            .stream().filter(playerEntry -> plugin.getFppComponent().getFpm().get(playerEntry.getUuid()) != null)
            .map(UnpluggedPlayers.PlayerEntry::getName).toList();
    }
}
